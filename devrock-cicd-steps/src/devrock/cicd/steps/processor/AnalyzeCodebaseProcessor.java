// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package devrock.cicd.steps.processor;

import static com.braintribe.console.ConsoleOutputs.brightBlue;
import static com.braintribe.console.ConsoleOutputs.println;
import static com.braintribe.console.ConsoleOutputs.sequence;
import static com.braintribe.console.ConsoleOutputs.text;
import static com.braintribe.console.ConsoleOutputs.yellow;
import static com.braintribe.devrock.mc.core.commons.McOutputs.versionedArtifactIdentification;
import static com.braintribe.utils.lcd.CollectionTools2.newMap;
import static com.braintribe.utils.lcd.StringTools.isEmpty;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.braintribe.cc.lcd.EqProxy;
import com.braintribe.console.ConsoleOutputs;
import com.braintribe.console.output.ConfigurableConsoleOutputContainer;
import com.braintribe.devrock.mc.api.transitive.RangedTerminals;
import com.braintribe.devrock.mc.api.transitive.TransitiveDependencyResolver;
import com.braintribe.devrock.mc.api.transitive.TransitiveResolutionContext;
import com.braintribe.devrock.mc.api.transitive.TransitiveResolutionContextBuilder;
import com.braintribe.devrock.mc.core.declared.DeclaredArtifactIdentificationExtractor;
import com.braintribe.devrock.mc.core.declared.commons.HashComparators;
import com.braintribe.devrock.mc.core.wirings.transitive.contract.TransitiveResolverContract;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.ConfigurationError;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.gm.model.reason.essential.ParseError;
import com.braintribe.model.artifact.analysis.AnalysisArtifact;
import com.braintribe.model.artifact.analysis.AnalysisArtifactResolution;
import com.braintribe.model.artifact.analysis.AnalysisDependency;
import com.braintribe.model.artifact.analysis.AnalysisTerminal;
import com.braintribe.model.artifact.compiled.CompiledArtifact;
import com.braintribe.model.artifact.compiled.CompiledDependencyIdentification;
import com.braintribe.model.artifact.declared.DeclaredArtifact;
import com.braintribe.model.artifact.essential.ArtifactIdentification;
import com.braintribe.model.artifact.essential.VersionedArtifactIdentification;
import com.braintribe.model.version.HasMajorMinor;
import com.braintribe.model.version.Version;
import com.braintribe.utils.CollectionTools;
import com.braintribe.utils.FileTools;
import com.braintribe.wire.api.Wire;
import com.braintribe.wire.api.context.WireContext;

import devrock.cicd.model.api.AnalyzeCodebase;
import devrock.cicd.model.api.AnalyzeCodebaseResponse;
import devrock.cicd.model.api.data.BuildReason;
import devrock.cicd.model.api.data.CodebaseAnalysis;
import devrock.cicd.model.api.data.CodebaseDependencyAnalysis;
import devrock.cicd.model.api.data.CodebaseSummary;
import devrock.cicd.model.api.data.GitContext;
import devrock.cicd.model.api.data.LocalArtifact;
import devrock.cicd.model.api.reason.FolderArtifactIdentificationFailed;
import devrock.cicd.model.api.reason.GitAnalysisFailure;
import devrock.cicd.steps.processing.ArtifactAvailabilityCheck;
import devrock.cicd.steps.processing.RangeParser;
import devrock.cicd.steps.processing.SolutionHashResolver;
import devrock.git.GitTools;

public class AnalyzeCodebaseProcessor extends SpawningServiceProcessor<AnalyzeCodebase, AnalyzeCodebaseResponse> {
	private static EnumSet<BuildReason> BUILD_REASONS_RELEASE_VIEW = EnumSet.of(BuildReason.ARTIFACT_CHANGED, BuildReason.ARTIFACT_UNTRACKED, BuildReason.UNPUBLISHED, BuildReason.PARENT_CHANGED);

	@Override
	protected StatefulServiceProcessor spawn() {
		return new StatefulCodebaseAnalysis();
	}
	
	private class StatefulCodebaseAnalysis extends StatefulServiceProcessor {
		
		private final CodebaseAnalysis codebaseAnalysis = CodebaseAnalysis.T.create();
		private final CodebaseSummary codebaseSummary = CodebaseSummary.T.create();
		private CodebaseDependencyAnalysis dependencyAnalysis;
		private final GitContext gitContext = GitContext.T.create();
		private final Map<String, LocalArtifact> localArtifactsByFolderName = new TreeMap<>();
		private File codebasePath;
		private boolean isGitAssociated;

		@Override
		protected Maybe<? extends AnalyzeCodebaseResponse> process() {
			codebasePath = new File(request.getPath());
			codebaseAnalysis.setBasePath(codebasePath.getAbsolutePath());
			isGitAssociated = GitTools.isGitCheckoutRoot(codebasePath);
			
			initializeGitContext();
			
			Reason error = findLocalArtifacts(codebasePath);
			if (error != null)
				return error.asMaybe();
			
			identifyCodebase();

			error = attachCommitHashesIfSuitable();
			if (error != null)
				return error.asMaybe();
			
			error = resolveCodebaseDependencies();
			if (error != null)
				return error.asMaybe();
			
			error = determineBuildArtifacts();
			if (error != null)
				return error.asMaybe();
			
			error = determineUnpublishedArtifacts();
			if (error != null)
				return error.asMaybe();
			
			transferLocalArtifactToAnalysis();
			
			trimResolutions();
			
			AnalyzeCodebaseResponse response = AnalyzeCodebaseResponse.T.create();
			response.setAnalysis(codebaseAnalysis);
			response.setSummary(codebaseSummary);
			response.setDependencyAnalysis(dependencyAnalysis);
			response.setDependencyResolution(dependencyAnalysis.getResolution());
			response.setGitContext(gitContext);
			
			return Maybe.complete(response);
		}

		private void initializeGitContext() {
			gitContext.setBaseBranch(request.getBaseBranch());
			gitContext.setBaseHash(request.getBaseHash());
			gitContext.setBaseRemote(request.getBaseRemote());
		}

		private Reason attachCommitHashesIfSuitable() {
			if (!request.getCi())
				return null;
							
			if (!isGitAssociated)
				return null;
			
			// TODO: parallelize
			for (LocalArtifact localArtifact: localArtifactsByFolderName.values()) {
				String folderName = localArtifact.getFolderName();
				Maybe<String> hashMaybe = GitTools.getLatestCommitHash(codebasePath, folderName);
				
				if (hashMaybe.isUnsatisfied())
					return hashMaybe.whyUnsatisfied();
				
				localArtifact.setCommitHash(hashMaybe.get());
			}
			
			return null;
		}

		private void identifyCodebase() {
			LocalArtifact localArtifact = findGroupIdentifyingArtifact();
			VersionedArtifactIdentification vai = localArtifact.getArtifactIdentification();
			Version parentVersion = Version.parse(vai.getVersion());
			Version groupVersion = Version.from((HasMajorMinor)parentVersion);
			String groupVersionAsStr = groupVersion.asString();
			String groupId = vai.getGroupId();
			
			codebaseAnalysis.setGroupId(groupId);
			codebaseAnalysis.setGroupVersion(groupVersionAsStr);
		}

		private Reason determineBuildArtifacts() {
			String buildArtifacts = request.getBuildArtifacts();
			
			if (buildArtifacts != null) {
				return determineBuildArtifactsFromRangeExpression(buildArtifacts);
			}
			else {
				return determineBuildArtifactsByChanges();
			}
		}

		private Reason determineBuildArtifactsFromRangeExpression(String rangeExpression) {
			// handle special cases
			if (".".equals(rangeExpression)) {
				useAllArtifactsAsBuildArtifacts(BuildReason.EXPLICIT);
				return null;
			}

			try {
				String groupId = codebaseAnalysis.getGroupId();
				String groupVersion = codebaseAnalysis.getGroupVersion();
				
				if (groupId == null || groupVersion == null)
					return Reasons.build(ConfigurationError.T).text("Cannot parse build artifacts as group is yet unidentified as there is no single artifact in it").toReason();
				
				Maybe<RangedTerminals> rangedTerminalsMaybe = RangeParser.parse(rangeExpression, groupId, groupVersion);
				
				if (rangedTerminalsMaybe.isUnsatisfied())
					return rangedTerminalsMaybe.whyUnsatisfied();
				
				RangedTerminals rangedTerminals = rangedTerminalsMaybe.get();
				
				TransitiveResolutionContext resolutionContext = buildCodebaseResolutionContext() //
						.buildRange(rangedTerminals.range()) //
						.done();
				
				try (WireContext<TransitiveResolverContract> context = Wire.context(new CodebaseDependencyResolverWireModule(codebasePath, localArtifactsByFolderName.values()))) {
					
					TransitiveDependencyResolver transitiveDependencyResolver = context.contract().transitiveDependencyResolver();
					AnalysisArtifactResolution resolution = transitiveDependencyResolver.resolve(resolutionContext, rangedTerminals.terminals());
					
					if (resolution.hasFailed())
						return resolution.getFailure();
					
					
					for (AnalysisArtifact artifact: resolution.getSolutions()) {
						LocalArtifact localArtifact = localArtifactsByFolderName.get(artifact.getArtifactId());
						localArtifact.setBuildReason(BuildReason.EXPLICIT);
					}
				}
				
				return null;
			}
			catch (Exception e) {
				return Reasons.build(InvalidArgument.T) //
						.text("Invalid range expression: " + rangeExpression) //
						.cause(ParseError.create(e.getMessage())) //
						.toReason();
			}
		}
		
		private LocalArtifact findGroupIdentifyingArtifact() {
			LocalArtifact parent = localArtifactsByFolderName.get("parent");
			
			if (parent != null)
				return parent;
			
			return CollectionTools.getFirstElementOrNull(localArtifactsByFolderName.values());
		}

		private void trimResolutions() {
			
			AnalysisArtifactResolution resolution = dependencyAnalysis.getResolution();
			resolution.getFilteredDependencies().clear();
			
			for (AnalysisArtifact solution: resolution.getSolutions()) {
				solution.setOrigin(null);
				solution.getDependencies().stream().forEach(d -> d.setOrigin(null));
				solution.getParts().clear();
			}
		}

		private Reason markParentChangeAffectedArtifacts() {
			List<AnalysisArtifact> parents = findParents();
			
			Set<AnalysisArtifact> parentTerminals = new LinkedHashSet<>();
			
			for (AnalysisArtifact parent: parents) {
				collectParentTerminals(parent, parentTerminals);
			}
			
			for (AnalysisArtifact parentTerminal: parentTerminals) {
				LocalArtifact localArtifact = localArtifactsByFolderName.get(parentTerminal.getArtifactId());
				if (localArtifact.getBuildReason() == BuildReason.NONE) {
					// TODO: make a more differentiated decision for affected terminals by analysing the usage of parent properties that were incrementally changed (requires a comparison to an already published parent)
					localArtifact.setBuildReason(BuildReason.PARENT_CHANGED);
				}
			}
			
			return null;
		}
		
		private void collectParentTerminals(AnalysisArtifact parent, Set<AnalysisArtifact> parentTerminals) {
			for (AnalysisDependency dependerDependency: parent.getDependers()) {
				if ("parent".equals(dependerDependency.getScope())) {
					AnalysisArtifact parentTerminal = dependerDependency.getDepender();

					if (parentTerminal != null) {
						Set<String> consumableProperties = parentTerminal.getOrigin().getProperties().keySet();
						
						DeclaredArtifact declaredArtifact = parentTerminal.getOrigin().getOrigin();
						
						Set<String> consumedProperties = PropertyReferenceCollector.scanPropertyReferences(declaredArtifact);
						
						consumedProperties.retainAll(consumableProperties);
						
						if (!consumedProperties.isEmpty()) {
							if (parentTerminals.add(parent)) {
								collectParentTerminals(parentTerminal, parentTerminals);
							}
						}
					}
				}
			}
		}
		
		private List<AnalysisArtifact> findParents() {
			List<AnalysisArtifact> parents = new ArrayList<>();

			for (AnalysisArtifact artifact: dependencyAnalysis.getResolution().getSolutions()) {
				for (AnalysisDependency dependerDependency: artifact.getDependers()) {
					if ("parent".equals(dependerDependency.getScope())) {
						parents.add(artifact);
						continue;
					}
				}
			}
			
			return parents;
		}

		private Set<LocalArtifact> getDirectDependersNotPresentInArgument(Collection<LocalArtifact> localArtifacts) {
			Set<LocalArtifact> dependers = new HashSet<>();
			
			for (LocalArtifact localArtifact: localArtifacts) {
				String artifactId = localArtifact.getArtifactIdentification().getArtifactId();
				
				AnalysisArtifact analysisArtifact = dependencyAnalysis.getArtifactIndex().get(artifactId);
				
				for (AnalysisDependency dependency : analysisArtifact.getDependers()) {
					AnalysisArtifact depender = dependency.getDepender();
					if (depender != null) {
						dependers.add(localArtifactsByFolderName.get(depender.getArtifactId()));
					}
				}
			}
			
			dependers.removeAll(localArtifacts);
			
			return dependers;
		}
		
		private Set<LocalArtifact> getTransitiveDependersNotPresentInArgument(List<LocalArtifact> localArtifacts) {
			Set<LocalArtifact> dependers = new HashSet<>();

			for (LocalArtifact localArtifact : localArtifacts)
				addToDependers(localArtifact, dependers);

			dependers.removeAll(localArtifacts);

			return dependers;
		}

		private void addToDependers(LocalArtifact localArtifact, Set<LocalArtifact> dependers) {
			if (!dependers.add(localArtifact))
				return;

			String artifactId = localArtifact.getArtifactIdentification().getArtifactId();

			AnalysisArtifact analysisArtifact = dependencyAnalysis.getArtifactIndex().get(artifactId);

			for (AnalysisDependency dependency : analysisArtifact.getDependers()) {
				AnalysisArtifact depender = dependency.getDepender();
				if (depender == null)
					continue;

				LocalArtifact localDepender = localArtifactsByFolderName.get(depender.getArtifactId());
				addToDependers(localDepender, dependers);
			}
		}

		private void transferLocalArtifactToAnalysis() {
			
			List<LocalArtifact> artifacts = codebaseAnalysis.getArtifacts();
			List<LocalArtifact> buildLinkingChecks = codebaseAnalysis.getBuildLinkingChecks();
			List<LocalArtifact> buildTests = codebaseAnalysis.getBuildTests();
			List<LocalArtifact> integrationTests = codebaseAnalysis.getIntegrationTests();
			
			List<LocalArtifact> builds = new ArrayList<>();
			List<LocalArtifact> testArtifacts = new ArrayList<>();
			List<LocalArtifact> integrationTestArtifacts = new ArrayList<>();
			
			for (LocalArtifact localArtifact: localArtifactsByFolderName.values()) {
				artifacts.add(localArtifact);

				if (localArtifact.getBuildReason() != BuildReason.NONE) {
					if (localArtifact.getTest()) {
						localArtifact.setBuildReason(BuildReason.NONE);
						testArtifacts.add(localArtifact);
					}
					else if (localArtifact.getIntegrationTest()) {
						localArtifact.setBuildReason(BuildReason.NONE);
						integrationTestArtifacts.add(localArtifact);
					}
					else if (localArtifact.getReleaseView() //
							&& !request.getAllowReleaseViewBuilding() //
							&& !BUILD_REASONS_RELEASE_VIEW.contains(localArtifact.getBuildReason())) {
						localArtifact.setBuildReason(BuildReason.NONE);
					}
					else {
						builds.add(localArtifact);
						if (localArtifact.getNpmPackage())
							codebaseSummary.setHasNpmBuild(true);
					}
				}
			}
			
			List<LocalArtifact> orderedBuilds = ArtifactsSequencer.orderSequential(dependencyAnalysis.getResolution(), builds);
			codebaseAnalysis.setBuilds(orderedBuilds);
			
			Set<LocalArtifact> directDependers = getDirectDependersNotPresentInArgument(builds);
			Set<LocalArtifact> transitiveDependers = getTransitiveDependersNotPresentInArgument(builds);
			
			Comparator<LocalArtifact> comparator = Comparator.comparing(a -> a.getArtifactIdentification().getArtifactId());
			
			// transfer compile check artifacts from directDependers and artifacts to be built
			Stream.of(directDependers, testArtifacts, integrationTestArtifacts) //
				.flatMap(Collection::stream) //
				.distinct() //
				.sorted(comparator) //
				.forEach(buildLinkingChecks::add);
			
			// transfer test artifacts from directDependers and artifacts to be built
			Stream.concat(directDependers.stream().filter(LocalArtifact::getTest), testArtifacts.stream()) //
				.distinct() //
				.sorted(comparator) //
				.forEach(buildTests::add);
			
			// transfer integration test artifacts from transitiveDependers and artifacts to be built
			Stream.concat(transitiveDependers.stream().filter(LocalArtifact::getIntegrationTest), integrationTestArtifacts.stream()) //
					.distinct() //
					.sorted(comparator) //
					.forEach(integrationTests::add);

			println();
			println(text("Build artifacts (" + orderedBuilds.size() + ") in build order:"));
			printArtifactList(orderedBuilds, true);

			println();
			println(text("Linking check artifacts (" + buildLinkingChecks.size() + "):"));
			printArtifactList(buildLinkingChecks, false);
			
			println();
			println(text("Unit-test artifacts (" + buildTests.size() + "):"));
			printArtifactList(buildTests, false);

			println();
			println(text("Integration-test artifacts (" + integrationTests.size() + "):"));
			printArtifactList(integrationTests, false);
		}
		
		private void printArtifactList(Collection<LocalArtifact> artifacts, boolean withBuildReason) {
			int i = 1;
			for (LocalArtifact localArtifact: artifacts) {
				ConfigurableConsoleOutputContainer sequence = ConsoleOutputs.configurableSequence();
				
				sequence //
					.append(String.valueOf(i++) + ". ") //
					.append(versionedArtifactIdentification(localArtifact.getArtifactIdentification()));
				
				if (withBuildReason) {
					sequence //
						.append(" - ") //
						.append(brightBlue(format(localArtifact.getBuildReason())));
				}
				
				println(sequence);
			}
		}
		
		private String format(BuildReason buildReason) {
			return buildReason.name().toLowerCase().replace('_', ' ');
		}

		private Set<String> getIgnores(File path) {
			File ingoreFile = new File(path, ".dontbuild");
			
			if (!ingoreFile.exists())
				return Collections.emptySet();
			
			Set<String> ignores = new LinkedHashSet<>();
			
			for (String line: FileTools.read(ingoreFile).asLines()) {
				int index = line.indexOf(":");
				
				if (index == -1) 
					ignores.add(line);
				else
					ignores.add(line.substring(index + 1));
			}
			
			return ignores;
		}
		
		private Reason findLocalArtifacts(File path) {
			Set<String> ignores = getIgnores(path);

			CompiledArtifact parentCa = readParentCompiledArtifact(path);
			
			for (File folder: path.listFiles()) {
				if (!folder.isDirectory())
					continue;
				
				if (ignores.contains(folder.getName())) 
					continue;
				
				File pomFile = new File(folder, "pom.xml");
				if (!pomFile.exists())
					continue;
				
				Maybe<CompiledArtifact> compiledArtifactMaybe = readCompiledArtifact(pomFile);
				if (compiledArtifactMaybe.isUnsatisfied())
					return compiledArtifactMaybe.whyUnsatisfied();

				CompiledArtifact compiledArtifact = compiledArtifactMaybe.get();

				Maybe<LocalArtifact> localArtifactMaybe = readLocalArtifact(folder, compiledArtifact, parentCa);
				
				if (localArtifactMaybe.isUnsatisfied())
					return localArtifactMaybe.whyUnsatisfied();
				
				LocalArtifact localArtifact = localArtifactMaybe.get();
				
				localArtifactsByFolderName.put(localArtifact.getFolderName(), localArtifact);
			}

			if (localArtifactsByFolderName.isEmpty())
				return NotFound.create("No artifacts found in: " + path.getAbsolutePath());

			return null;
		}
		
		private CompiledArtifact readParentCompiledArtifact(File path) {
			File pomFile = new File(path, "parent/pom.xml");
			if (!pomFile.exists())
				return null;

			Maybe<CompiledArtifact> compiledArtifactMaybe = readCompiledArtifact(pomFile);
			if (compiledArtifactMaybe.isUnsatisfied())
				return null;

			return compiledArtifactMaybe.get();
		}

		private Maybe<LocalArtifact> readLocalArtifact(File folder, CompiledArtifact ca, CompiledArtifact parentCa) {
			LocalArtifact localArtifact = buildLocalArtifact(folder, ca);

			classifyArtifact(localArtifact, folder, ca, parentCa);

			return Maybe.complete(localArtifact);
		}

		private Maybe<CompiledArtifact> readCompiledArtifact(File pomFile) {
			File folder = pomFile.getParentFile();

			Maybe<CompiledArtifact> caiMaybe = DeclaredArtifactIdentificationExtractor.extractMinimalArtifact(pomFile);
			if (caiMaybe.isUnsatisfied())
				return Reasons.build(FolderArtifactIdentificationFailed.T) //
						.text("Could identify artifact from folder: " + folder.getAbsolutePath()) //
						.cause(caiMaybe.whyUnsatisfied()) //
						.toMaybe();

			CompiledArtifact ca = caiMaybe.get();
			if (!folder.getName().equals(ca.getArtifactId()))
				return Reasons.build(ConfigurationError.T) //
						.text("Artifact id " + ca.getArtifactId() + " doesn't match folder: " + folder.getName()) //
						.toMaybe();
		
			return caiMaybe;
		}

		private LocalArtifact buildLocalArtifact(File folder, CompiledArtifact ca) {
			VersionedArtifactIdentification vai = VersionedArtifactIdentification.create(ca.getGroupId(), ca.getArtifactId(), ca.getVersion().asString());
			LocalArtifact localArtifact = LocalArtifact.T.create();
			localArtifact.setFolderName(folder.getName());
			localArtifact.setArtifactIdentification(vai);
			localArtifact.setIdentification(vai.asString());
			localArtifact.setPackaging(ca.getPackaging());
			localArtifact.setBuildReason(BuildReason.NONE);
			return localArtifact;
		}
		
		private void classifyArtifact(LocalArtifact localArtifact, File folder, CompiledArtifact ca, CompiledArtifact parentCa) {
			String packaging = Optional.ofNullable(localArtifact.getPackaging()).map(String::toLowerCase).orElse("jar");

			switch (packaging) {
				case "war":
				case "ear":
				case "bundle":
				case "zip":
					localArtifact.setBundle(true);
					break;
				default:
					break;
			}

			String artifactId = localArtifact.getArtifactIdentification().getArtifactId();

			if (artifactId.endsWith("-integration-test")) {
				if (hasSetup(artifactId, folder))
					localArtifact.setIntegrationTest(true);
				else
					println(sequence(text("Ignoring integration test as there is no corresponding setup: "), yellow(artifactId)));

			} else if (artifactId.endsWith("-test")) {
				localArtifact.setTest(true);

			} else if ("true".equals(ca.getProperties().get("release-view"))) {
				localArtifact.setReleaseView(true);
			}

			if (isNpmPackage(ca, parentCa))
				localArtifact.setNpmPackage(true);
		}

		private boolean hasSetup(String artifactId, File folder) {
			return new File(folder.getParent(), artifactId + "-setup").isDirectory();
		}

		private boolean isNpmPackage(CompiledArtifact ca, CompiledArtifact parentCa) {
			Map<String, String> props = newMap();

			if (parentCa != null)
				props.putAll(parentCa.getProperties());

			props.putAll(ca.getProperties());

			if (isEmpty(props.get("npmRegistryUrl")))
				return false;

			return "model".equals(props.get("archetype")) || //
					!isEmpty(props.get("npmPackaging"));
		}

		private Reason determineBuildArtifactsByChanges() {
			Reason error = determineBuildArtifactsByFolderChanges();
			if (error != null)
				return error;
			
			error = markParentChangeAffectedArtifacts();
			if (error != null)
				return error;
			
			error = markChangedBundleArtifacts();
			if (error != null)
				return error;
			
			return null;
		}
		
		private Reason determineUnpublishedArtifacts() {
			if (!request.getDetectUnpublishedArtifacts())
				return null;
			
			List<LocalArtifact> unpublishedArtifactCandidates = new ArrayList<>();
			
			for (LocalArtifact candidate : localArtifactsByFolderName.values()) {
				if (candidate.getBuildReason() == BuildReason.NONE && !candidate.getTest() && !candidate.getIntegrationTest())
					unpublishedArtifactCandidates.add(candidate);
			}
			
			Maybe<Map<LocalArtifact, Boolean>> availabilityMaybe = ArtifactAvailabilityCheck.resolveArtifactsAvailability(unpublishedArtifactCandidates, codebasePath);
			
			if (availabilityMaybe.isUnsatisfied())
				return availabilityMaybe.whyUnsatisfied();
			
			Map<LocalArtifact, Boolean> availability = availabilityMaybe.get();
			
			for (LocalArtifact localArtifact: unpublishedArtifactCandidates) {
				if (!availability.get(localArtifact))
					localArtifact.setBuildReason(BuildReason.UNPUBLISHED);
			}
			
			return null;
		}

		private Reason determineBuildArtifactsByFolderChanges() {
			
			if (isGitAssociated) {
				Reason error = ensureHash();
				
				if (error != null)
					return error;
				
				String hash = gitContext.getBaseHash();
				
				for (File untrackedFolder: GitTools.getUntrackedFolders(codebasePath)) {
					LocalArtifact localArtifact = localArtifactsByFolderName.get(untrackedFolder.getName());
					if (localArtifact != null)
						localArtifact.setBuildReason(BuildReason.ARTIFACT_UNTRACKED);
				}
				
				for (File changedFolder: GitTools.getChangedFolders(codebasePath, hash)) {
					LocalArtifact localArtifact = localArtifactsByFolderName.get(changedFolder.getName());
					if (localArtifact != null)
						localArtifact.setBuildReason(BuildReason.ARTIFACT_CHANGED);
				}
			}
			else {
				useAllArtifactsAsBuildArtifacts(BuildReason.ARTIFACT_UNTRACKED);
			}
			
			return null;
		}
		
		private void useAllArtifactsAsBuildArtifacts(BuildReason buildReason) {
			for (LocalArtifact localArtifact: localArtifactsByFolderName.values()) {
				localArtifact.setBuildReason(buildReason);
			}
		}
		
		private Reason ensureHash() {
			String baseHash = request.getBaseHash();
			
			if (baseHash != null) 
				return null;
			
			String baseBranch = request.getBaseBranch();
			
			if (baseBranch == null) {
				gitContext.setBaseHash("HEAD");
				return null;
			}
			
			Maybe<String> maybe = GitTools.getBranchHash(codebasePath, gitContext.getBaseRemote(), baseBranch);
			
			if (maybe.isUnsatisfied())
				return Reasons.build(GitAnalysisFailure.T) //
						.text("Could not determine git hash for base branch " + baseBranch) //
						.cause(maybe.whyUnsatisfied()) //
						.toReason();
			
			gitContext.setBaseHash(maybe.get());
			
			return null;
		}

		private Reason markChangedBundleArtifacts() {
			List<LocalArtifact> unchangedBundles = new ArrayList<>();
			List<LocalArtifact> changedArtifacts = new ArrayList<>();
			
			for (LocalArtifact localArtifact: localArtifactsByFolderName.values()) {
				if (localArtifact.getBuildReason() != BuildReason.NONE) 
					changedArtifacts.add(localArtifact);

				else if (localArtifact.getBundle())
						unchangedBundles.add(localArtifact);
			}
			
			if (unchangedBundles.isEmpty())
				return null;
			
			return markChangedBundleArtifacts(changedArtifacts, unchangedBundles);
		}

		private Reason markChangedBundleArtifacts(Collection<LocalArtifact> changedArtifacts, Collection<LocalArtifact> unchangedBundles) {
			List<LocalArtifact> bundlersWithSolutionHashChange = new ArrayList<>();
			try (SolutionHashResolver solutionHashResolver = new SolutionHashResolver(changedArtifacts, codebasePath)) {
				for (LocalArtifact localArtifact: unchangedBundles) {
					Maybe<Boolean> changedMaybe = solutionHashResolver.hasSolutionHashChange(localArtifact);
					
					if (changedMaybe.isUnsatisfied())
						return changedMaybe.whyUnsatisfied();
					
					if (changedMaybe.get()) {
						localArtifact.setBuildReason(BuildReason.DEPENDENCY_RESOLUTION_CHANGED);
						bundlersWithSolutionHashChange.add(localArtifact);
					}
				}
			}
			
			if (bundlersWithSolutionHashChange.isEmpty())
				return null;
			
			return markDependerBundlesChanged(unchangedBundles, bundlersWithSolutionHashChange);
		}

		private Reason markDependerBundlesChanged(Collection<LocalArtifact> unchangedBundles, List<LocalArtifact> bundlesWithSolutionChanges) {
			Map<String, AnalysisArtifact> artifactIndex = dependencyAnalysis.getArtifactIndex();

			// index unchanged bundles
			Map<String, LocalArtifact> localBundles = new HashMap<>();
			for (LocalArtifact localBundle: unchangedBundles) {
				localBundles.put(localBundle.getArtifactIdentification().getArtifactId(), localBundle);
			}
			
			// run through bundle artifacts with solution hash change and collect all of their dependers
			Set<AnalysisArtifact> dependers = new HashSet<>();
			for (LocalArtifact artifact: bundlesWithSolutionChanges) {
				AnalysisArtifact analysisArtifact = artifactIndex.get(artifact.getArtifactIdentification().getArtifactId());
				collectDependers(analysisArtifact, dependers);
			}
			
			// run through all dependers and mark bundlers with no build reason with DEPENDENCY_RESOLUTION_CHANGED
			for (AnalysisArtifact depender: dependers) {
				LocalArtifact la = localBundles.get(depender.getArtifactId());

				if (la != null && la.getBuildReason() == BuildReason.NONE) {
					la.setBuildReason(BuildReason.DEPENDENCY_RESOLUTION_CHANGED);
				}
			}

			return null;
		}

		private void collectDependers(AnalysisArtifact artifact, Set<AnalysisArtifact> dependers) {
			for (AnalysisDependency dependency : artifact.getDependers()) {
				AnalysisArtifact depender = dependency.getDepender();
				if (depender == null)
					continue;
				
				if (!dependers.add(depender))
					continue;
				
				collectDependers(depender, dependers);
			}
		}

		private TransitiveResolutionContextBuilder buildCodebaseResolutionContext() {
			Map<EqProxy<ArtifactIdentification>, LocalArtifact> localIdentifications = new HashMap<>();
			
			for (LocalArtifact localArtifact: localArtifactsByFolderName.values()) {
				CompiledDependencyIdentification terminal = CompiledDependencyIdentification.from(localArtifact.getArtifactIdentification());
				localIdentifications.put(HashComparators.artifactIdentification.eqProxy(terminal), localArtifact);
			}
			
			return TransitiveResolutionContext.build() //
					.dependencyFilter(d -> {
						LocalArtifact la = localIdentifications.get(HashComparators.artifactIdentification.eqProxy(d)); //
						// exclude codebase-foreign dependencies
						if (la == null)
							return false;
						
						// exclude direct-versioned dependencies that are not satisfiable
						if (d.getOrigin().getVersion() instanceof Version)
							return d.getVersion().equals(la.getArtifactIdentification().getVersion());
							
						return true;
					}) //
					.includeParentDependencies(true) //
					.includeImportDependencies(true) //
					.lenient(true);
		}
		
		private Reason resolveCodebaseDependencies() {
			try (WireContext<TransitiveResolverContract> context = Wire.context(new CodebaseDependencyResolverWireModule(codebasePath, localArtifactsByFolderName.values()))) {
				List<CompiledDependencyIdentification> terminals = localArtifactsByFolderName.values().stream() //
					.map(LocalArtifact::getArtifactIdentification) //
					.map(CompiledDependencyIdentification::from) //
					.collect(Collectors.toList());
				
				TransitiveResolutionContext resolutionContext = buildCodebaseResolutionContext().done();
				
				AnalysisArtifactResolution resolution = context.contract().transitiveDependencyResolver().resolve(resolutionContext, terminals);
				
				if (resolution.hasFailed())
					return resolution.getFailure();
				
				Iterator<AnalysisTerminal> it = resolution.getTerminals().iterator();
				
				while (it.hasNext()) {
					AnalysisTerminal terminal = it.next();
					
					if (terminal instanceof AnalysisDependency) {
						AnalysisDependency dependency = (AnalysisDependency) terminal;
						
						
						if (dependency.getSolution().getDependers().size() > 1) {
							it.remove();
						}
					}
				}
				
				// index artifacts
				Map<String, AnalysisArtifact> artifactIndex = new HashMap<>();
				
				for (AnalysisArtifact solution: resolution.getSolutions()) {
					artifactIndex.put(solution.getArtifactId(), solution);
				}
				
				CodebaseDependencyAnalysis dependencyAnalysis = CodebaseDependencyAnalysis.T.create();
				dependencyAnalysis.setResolution(resolution);
				dependencyAnalysis.setArtifactIndex(artifactIndex);
				
				this.dependencyAnalysis = dependencyAnalysis;
				
				return null;
			}
		}
	}
}

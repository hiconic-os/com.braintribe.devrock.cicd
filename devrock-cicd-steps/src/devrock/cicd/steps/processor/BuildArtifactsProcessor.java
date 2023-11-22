package devrock.cicd.steps.processor;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.braintribe.common.attribute.common.CallerEnvironment;
import com.braintribe.devrock.mc.api.commons.PartIdentifications;
import com.braintribe.devrock.mc.api.download.PartEnricher;
import com.braintribe.devrock.mc.api.download.PartEnrichingContext;
import com.braintribe.devrock.mc.api.resolver.PartAvailabilityReflection;
import com.braintribe.devrock.mc.core.wirings.configuration.contract.DevelopmentEnvironmentContract;
import com.braintribe.devrock.mc.core.wirings.configuration.contract.RepositoryConfigurationContract;
import com.braintribe.devrock.mc.core.wirings.resolver.ArtifactDataResolverModule;
import com.braintribe.devrock.mc.core.wirings.resolver.contract.ArtifactDataResolverContract;
import com.braintribe.devrock.model.mc.reason.InvalidRepositoryConfiguration;
import com.braintribe.devrock.model.repository.LocalRepository;
import com.braintribe.devrock.model.repository.Repository;
import com.braintribe.devrock.model.repository.RepositoryConfiguration;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.model.artifact.analysis.AnalysisArtifact;
import com.braintribe.model.artifact.compiled.CompiledArtifactIdentification;
import com.braintribe.model.artifact.consumable.Artifact;
import com.braintribe.model.artifact.consumable.Part;
import com.braintribe.model.artifact.consumable.PartEnrichment;
import com.braintribe.model.artifact.consumable.PartReflection;
import com.braintribe.model.artifact.essential.PartIdentification;
import com.braintribe.model.artifact.essential.VersionedArtifactIdentification;
import com.braintribe.model.resource.FileResource;
import com.braintribe.model.version.Version;
import com.braintribe.utils.lcd.LazyInitialized;
import com.braintribe.wire.api.Wire;
import com.braintribe.wire.api.context.WireContext;
import com.braintribe.wire.api.context.WireContextBuilder;

import devrock.cicd.model.api.BuildArtifacts;
import devrock.cicd.model.api.BuildArtifactsResponse;
import devrock.cicd.model.api.data.BuildResult;
import devrock.cicd.model.api.data.CodebaseAnalysis;
import devrock.cicd.model.api.data.CodebaseDependencyAnalysis;
import devrock.cicd.model.api.data.LocalArtifact;
import devrock.cicd.model.api.reason.SolutionHashResolutionFailed;
import devrock.cicd.steps.processing.SolutionHashResolver;
import devrock.pom.PomTools;

public class BuildArtifactsProcessor extends SpawningServiceProcessor<BuildArtifacts, BuildArtifactsResponse> {
	
	@Override
	protected StatefulServiceProcessor spawn() { 
		return new StatefulServiceProcessor() {
			private RepositoryConfiguration installRepositoryConfiguration;
			private List<Artifact> artifacts;
			
			@Override
			protected Maybe<BuildArtifactsResponse> process() {
				Consumer<LocalArtifact> handler = request.getHandler();
				
				if (handler == null)
					return Reasons.build(InvalidArgument.T).text("Transitive property BuildArtifacts.handler must not be null").toMaybe();
				
				CodebaseAnalysis analysis = request.getCodebaseAnalysis();
				CodebaseDependencyAnalysis dependencyAnalysis = request.getCodebaseDependencyAnalysis();
				Integer threads = request.getThreads();
				boolean skip = request.getSkip();

				Reason error = ParallelBuildSupport.runInParallel(analysis, dependencyAnalysis, analysis.getBuilds(), handler, threads, skip);
				if (error != null)
					return error.asMaybe();
								
				error = loadRepositoryInformation();
				if (error != null)
					return error.asMaybe();
				
				equipBuiltArtifacts();
				
				error = enrichPomsWithCommitAndSolutionHash();
				if (error != null)
					return error.asMaybe();
				
				BuildResult buildResult = BuildResult.T.create();
				buildResult.setArtifacts(artifacts);
				
				BuildArtifactsResponse response = BuildArtifactsResponse.T.create();
				response.setResult(buildResult);
				
				return Maybe.complete(response);
			}
			
			private Reason enrichPomsWithCommitAndSolutionHash() {
				// TODO: add final solution hash determination
				Map<String, Artifact> artifactByArtifactId = new LinkedHashMap<>();
				
				for (Artifact artifact: artifacts) {
					artifactByArtifactId.put(artifact.getArtifactId(), artifact);
				}
				
				List<LocalArtifact> buildArtifacts = request.getCodebaseAnalysis().getBuilds();
				
				
				Maybe<Map<LocalArtifact, String>> solutionHashesMaybe = resolveSolutionHashes(buildArtifacts);
				
				if (solutionHashesMaybe.isUnsatisfied())
					return solutionHashesMaybe.whyUnsatisfied();
				
				Map<LocalArtifact, String> solutionHashes = solutionHashesMaybe.get();
					
				for (LocalArtifact localArtifact: buildArtifacts) {
					String commitHash = localArtifact.getCommitHash();
					
					Map<String, String> properties = new LinkedHashMap<>();
					
					if (commitHash != null) {
						properties.put("commit-hash", commitHash);
					}
					
					if (localArtifact.getBundle()) {
						String solutionHash = solutionHashes.get(localArtifact);
						properties.put("solution-hash", solutionHash);
					}
					
					if (!properties.isEmpty()) {
						Artifact artifact = artifactByArtifactId.get(localArtifact.getArtifactIdentification().getArtifactId());
						
						Part part = artifact.getParts().get(PartIdentifications.pomPartKey);
						
						FileResource resource = (FileResource)part.getResource();
						File pomFile = new File(resource.getPath());

						Reason error = PomTools.addProperties(pomFile, properties);
						if (error != null)
							return error;
					}
				}
				
				return null;
			}

			private Maybe<Map<LocalArtifact, String>> resolveSolutionHashes(List<LocalArtifact> buildArtifacts) {
				Map<LocalArtifact, String> results = new LinkedHashMap<>();
				
				CodebaseAnalysis codebaseAnalysis = request.getCodebaseAnalysis();
				File groupDir = new File(codebaseAnalysis.getBasePath());
				
				List<LocalArtifact> bundleArtifacts = buildArtifacts.stream().filter(LocalArtifact::getBundle).collect(Collectors.toList());

				if (!buildArtifacts.isEmpty()) {
					
					LazyInitialized<Reason> failure = new LazyInitialized<>(() -> Reasons.build(SolutionHashResolutionFailed.T).text("Error while resolving solution hashes for built artifacts that are bundlers").toReason());
					
					try (SolutionHashResolver resolver = new SolutionHashResolver(buildArtifacts, groupDir)) {
						for (LocalArtifact buildArtifact: bundleArtifacts) {
							Maybe<String> maybe = resolver.resolveCurrentSolutionHash(buildArtifact);
							
							if (maybe.isUnsatisfied()) {
								failure.get().getReasons().add(maybe.whyUnsatisfied());
							}
							else {
								results.put(buildArtifact, maybe.get());
							}
						}
					}
					if (failure.isInitialized())
						return failure.get().asMaybe();
				}
				

				return Maybe.complete(results);
			}

			private File hasDevEnvParent(File currentWorkingDirectory) {
				File file = new File(currentWorkingDirectory, "dev-environment.yaml");
				
				if (file.exists()) {
					return currentWorkingDirectory;
				}
				else {
					File parentFolder = currentWorkingDirectory.getParentFile();
					
					if (parentFolder != null) {
						return hasDevEnvParent(parentFolder);
					}
					else {
						return null;
					}
				}
			}
			
			private File getDevEnv() {
				return context.findAttribute(CallerEnvironment.class) //
						.map(CallerEnvironment::currentWorkingDirectory) //
						.map(this::hasDevEnvParent)
						.orElse(null);
			}
			
			private Reason loadRepositoryInformation() {
				WireContextBuilder<ArtifactDataResolverContract> wireContextBuilder = Wire.contextBuilder(ArtifactDataResolverModule.INSTANCE) //
						.bindContract(DevelopmentEnvironmentContract.class, this::getDevEnv);
				
				try (WireContext<ArtifactDataResolverContract> wireContext = wireContextBuilder.build()) {
					RepositoryConfiguration repositoryConfiguration = wireContext.contract().repositoryReflection().getRepositoryConfiguration();
					
					if (repositoryConfiguration.hasFailed())
						return repositoryConfiguration.getFailure();
					
					Repository installRepository = repositoryConfiguration.getInstallRepository();
					
					if (installRepository == null) {
						@SuppressWarnings("deprecation")
						String localRepositoryPath = repositoryConfiguration.getLocalRepositoryPath();
						
						if (localRepositoryPath != null) {
							localRepositoryPath = Path.of(localRepositoryPath).toAbsolutePath().normalize().toString();
							LocalRepository localRepository = LocalRepository.T.create();
							localRepository.setCachable(false);
							localRepository.setName("local");
							localRepository.setRootPath(localRepositoryPath);
							
							installRepository = localRepository;
						}
					}
					
					if (installRepository == null)
						return Reasons.build(InvalidRepositoryConfiguration.T) //
								.text("Could not deduce a repository for artifact installation from repository configuration").toReason();
					
					installRepositoryConfiguration = RepositoryConfiguration.T.create();
					installRepositoryConfiguration.setCachePath(repositoryConfiguration.cachePath());
					installRepositoryConfiguration.getRepositories().add(installRepository);
				}
				
				return null;
			}
			
			private boolean isCandidateInstall() {
				Boolean isCandidateInstall = request.getCandidateInstall();
				
				if (isCandidateInstall != null)
					return isCandidateInstall;
				
				String value = System.getenv("DEVROCK_PIPELINE_CANDIDATE_INSTALL");
				
				if (value != null) {
					return Boolean.TRUE.toString().equals(value);
				}
				
				return true;
			}
			
			private void equipBuiltArtifacts() {
				WireContextBuilder<ArtifactDataResolverContract> wireContextBuilder = Wire.contextBuilder(ArtifactDataResolverModule.INSTANCE) //
						.bindContract(RepositoryConfigurationContract.class, () -> Maybe.complete(installRepositoryConfiguration));
				
				boolean isCandidateInstall = isCandidateInstall();
				
				try (WireContext<ArtifactDataResolverContract> wireContext = wireContextBuilder.build()) {
					List<LocalArtifact> localArtifacts = request.getCodebaseAnalysis().getBuilds();
					
					// determine parts
					ArtifactDataResolverContract artifactDataResolverContract = wireContext.contract();
					PartAvailabilityReflection partAvailabilityReflection = artifactDataResolverContract.partAvailabilityReflection();

					Map<AnalysisArtifact, List<PartEnrichment>> enrichments = new LinkedHashMap<>();
					
					for (LocalArtifact localArtifact: localArtifacts) {
						VersionedArtifactIdentification vai = localArtifact.getArtifactIdentification();
						CompiledArtifactIdentification cai = CompiledArtifactIdentification.from(vai);
						
						Version version = cai.getVersion();
						
						// ensure candidate?
						if (isCandidateInstall && !version.isPreliminary()) {
							version.setQualifier("rc");
						}
						
						// add parts from local installation
						List<PartReflection> partReflections = partAvailabilityReflection.getAvailablePartsOf(cai);
						Map<String, PartEnrichment> parts = new HashMap<>();
						
						for (PartReflection reflection: partReflections) {
							PartEnrichment enrichment = PartEnrichment.T.create();
							enrichment.setClassifier(reflection.getClassifier());
							enrichment.setType(reflection.getType());
							enrichment.setMandatory(true);
							
							parts.put(PartIdentification.asString(enrichment), enrichment);
						}
						
						AnalysisArtifact analysisArtifact = AnalysisArtifact.T.create();
						analysisArtifact.setGroupId(vai.getGroupId());
						analysisArtifact.setArtifactId(vai.getArtifactId());
						analysisArtifact.setVersion(vai.getVersion());
						analysisArtifact.setPackaging(localArtifact.getPackaging());
						
						enrichments.put(analysisArtifact, new ArrayList<>(parts.values()));
					}
					
					PartEnrichingContext enrichingContext = PartEnrichingContext.build() //
					.enrichingExpert(a -> {
						return enrichments.getOrDefault(a, Collections.emptyList());
					}).done(); 
					
					PartEnricher partEnricher = artifactDataResolverContract.partEnricher();
					partEnricher.enrich(enrichingContext, enrichments.keySet());

					artifacts = new ArrayList<>();

					for (AnalysisArtifact analysisArtifact: enrichments.keySet()) {
						Artifact artifact = Artifact.from(analysisArtifact);
						artifact.setParts(analysisArtifact.getParts());
						
						artifacts.add(artifact);
					}
				}
			}
		};
	}
}

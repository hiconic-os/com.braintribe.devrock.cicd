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
package devrock.cicd.steps.processing;

import static com.braintribe.wire.api.util.Lists.list;

import java.io.File;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.braintribe.artifact.declared.marshaller.DeclaredArtifactMarshaller;
import com.braintribe.devrock.mc.api.classpath.ClasspathDependencyResolver;
import com.braintribe.devrock.mc.api.classpath.ClasspathResolutionContext;
import com.braintribe.devrock.mc.api.classpath.ClasspathResolutionScope;
import com.braintribe.devrock.mc.api.commons.PartIdentifications;
import com.braintribe.devrock.mc.api.resolver.ArtifactDataResolver;
import com.braintribe.devrock.mc.api.resolver.DeclaredArtifactCompiler;
import com.braintribe.devrock.mc.core.declared.DeclaredArtifactResolver;
import com.braintribe.devrock.mc.core.resolver.BasicDependencyResolver;
import com.braintribe.devrock.mc.core.wirings.classpath.ClasspathResolverWireModule;
import com.braintribe.devrock.mc.core.wirings.classpath.contract.ClasspathResolverContract;
import com.braintribe.devrock.mc.core.wirings.env.configuration.EnvironmentSensitiveConfigurationWireModule;
import com.braintribe.devrock.mc.core.wirings.resolver.ArtifactDataResolverModule;
import com.braintribe.devrock.mc.core.wirings.resolver.contract.ArtifactDataResolverContract;
import com.braintribe.devrock.mc.core.wirings.workspace.WorkspaceRepositoryModule;
import com.braintribe.devrock.model.mc.reason.UnresolvedDependencyVersion;
import com.braintribe.devrock.model.repository.Repository;
import com.braintribe.devrock.model.repository.WorkspaceRepository;
import com.braintribe.exception.Exceptions;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.model.artifact.analysis.AnalysisArtifact;
import com.braintribe.model.artifact.analysis.AnalysisArtifactResolution;
import com.braintribe.model.artifact.compiled.CompiledArtifact;
import com.braintribe.model.artifact.compiled.CompiledArtifactIdentification;
import com.braintribe.model.artifact.compiled.CompiledDependencyIdentification;
import com.braintribe.model.artifact.compiled.CompiledTerminal;
import com.braintribe.model.artifact.consumable.Artifact;
import com.braintribe.model.artifact.consumable.Part;
import com.braintribe.model.artifact.declared.DeclaredArtifact;
import com.braintribe.model.artifact.essential.VersionedArtifactIdentification;
import com.braintribe.model.resource.Resource;
import com.braintribe.model.version.FuzzyVersion;
import com.braintribe.model.version.Version;
import com.braintribe.utils.StringTools;
import com.braintribe.wire.api.Wire;
import com.braintribe.wire.api.context.WireContext;
import com.braintribe.wire.api.module.WireModule;
import com.braintribe.wire.api.module.WireTerminalModule;

import devrock.cicd.model.api.data.LocalArtifact;
import devrock.cicd.model.api.reason.SolutionHashResolutionFailed;
import devrock.pom.PomTools;

public class SolutionHashResolver implements AutoCloseable {
	private static final String FILENAME_POM = "pom.xml";
	
	private BasicDependencyResolver uploadDependencyResolver;
	private ArtifactDataResolver uploadRepositoryResolver;
	private final WireContext<ClasspathResolverContract> context;
	private final ClasspathDependencyResolver classpathResolver;
	private final DeclaredArtifactCompiler declaredArtifactCompiler;
	private final File groupPath;
	private DeclaredArtifactResolver declaredArtifactResolver;
	
	public SolutionHashResolver(Collection<LocalArtifact> changedArtifacts, File groupPath) {
		this.groupPath = groupPath;
		this.context = Wire.context(new SolutionHashResolverWireModule(groupPath, buildWorkspaceRepository(changedArtifacts)));
		ClasspathResolverContract classpathResolverContract = context.contract();
		ArtifactDataResolverContract dataResolver = classpathResolverContract.transitiveResolverContract().dataResolverContract();
		
		classpathResolver = classpathResolverContract.classpathResolver();
		declaredArtifactCompiler = dataResolver.declaredArtifactCompiler();
		
		Repository uploadRepository = dataResolver.repositoryReflection().getUploadRepository();
		
		if (uploadRepository != null) {
			uploadRepositoryResolver = dataResolver.backendContract().repository(uploadRepository);
			uploadDependencyResolver = new BasicDependencyResolver(uploadRepositoryResolver);
			declaredArtifactResolver = new DeclaredArtifactResolver();
			declaredArtifactResolver.setPartResolver(uploadRepositoryResolver);
			declaredArtifactResolver.setMarshaller(DeclaredArtifactMarshaller.INSTANCE);
		}
	}
	
	private WorkspaceRepository buildWorkspaceRepository(Collection<LocalArtifact> changedArtifacts) {
		WorkspaceRepository workspaceRepository = WorkspaceRepository.T.create();
		workspaceRepository.setCachable(false);
		workspaceRepository.setName("codebase");
		
		// TODO: add all filter for dominance
		// workspaceRepository.
		
		for (LocalArtifact localArtifact: changedArtifacts) {
			VersionedArtifactIdentification vai = localArtifact.getArtifactIdentification();

			Version version = Version.parse(vai.getVersion());
			version.setQualifier(null);
			
			String versionAsStr = version.asString();
			
			Artifact artifact = Artifact.T.create();
			artifact.setGroupId(vai.getGroupId());
			artifact.setArtifactId(vai.getArtifactId());
			artifact.setVersion(versionAsStr);
			
			Part pomPart = Part.T.create();
			pomPart.setClassifier(PartIdentifications.pom.getClassifier());
			pomPart.setType(PartIdentifications.pom.getType());
			pomPart.setRepositoryOrigin(workspaceRepository.getName());
			
			File pomFile = groupPath.toPath().resolve(localArtifact.getFolderName()).resolve(FILENAME_POM).toAbsolutePath().toFile();
			
			// TODO: no longer neccessary
			Maybe<Resource> changedPomMaybe = PomTools.getResourceWithChangeVersioned(pomFile, versionAsStr);

			// TODO: handle reasoning

			Resource pomResource = changedPomMaybe.get();
			
			pomPart.setResource(pomResource);
			
			artifact.getParts().put(PartIdentifications.pom.asString(), pomPart);
			
			workspaceRepository.getArtifacts().add(artifact);
		}
		
		return workspaceRepository;
	}
	
	public Maybe<Boolean> hasSolutionHashChange(LocalArtifact localArtifact) {
		Maybe<String> latestHashMaybe = resolveLatestSolutionHash(localArtifact);
		
		if (latestHashMaybe.isUnsatisfied()) {
			if (latestHashMaybe.isUnsatisfiedBy(NotFound.T))
				return Maybe.complete(true);
			
			return latestHashMaybe.whyUnsatisfied().asMaybe();
		}
		
		// TODO: is this involving pc revisions, and wouldn't that be problematic?
		Maybe<String> currentHashMaybe = resolveCurrentSolutionHash(localArtifact);
		
		if (currentHashMaybe.isUnsatisfied())
			return currentHashMaybe.whyUnsatisfied().asMaybe();
		
		String latestHash = latestHashMaybe.get();
		String currentHash = currentHashMaybe.get();
		
		boolean solutionHashChange = !latestHash.equals(currentHash);
		
		return Maybe.complete(solutionHashChange);
	}

	public Maybe<String> resolveCurrentSolutionHash(LocalArtifact localArtifact) {
		File pomFile = groupPath.toPath().resolve(localArtifact.getFolderName()).resolve("pom.xml").toFile();
		Maybe<CompiledArtifact> caMaybe = declaredArtifactCompiler.compileReasoned(pomFile);
		
		if (caMaybe.isUnsatisfied()) {
			return caMaybe.whyUnsatisfied().asMaybe();
		}
		
		CompiledTerminal terminal = caMaybe.get();
		
		ClasspathResolutionContext context = ClasspathResolutionContext.build().lenient(true).scope(ClasspathResolutionScope.compile).done();
		
		AnalysisArtifactResolution resolution = classpathResolver.resolve(context, terminal);
		
		if (resolution.hasFailed())
			return resolution.getFailure().asMaybe();
		
		List<AnalysisArtifact> solutions = resolution.getSolutions();
		
		String hash = buildHash(solutions);
		
		return Maybe.complete(hash);
	}
	
	private String buildHash(List<AnalysisArtifact> solutions) {
		
		final MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw Exceptions.unchecked(e);
		}
		
		Charset charset = Charset.forName("UTF-8");
		
		solutions.stream() //
			.map(AnalysisArtifact::asString) //
			.sorted()
			.map(n -> n + "\n")
			.map(s -> s.getBytes(charset)) //
			.forEach(digest::update);
		
		return StringTools.toHex(digest.digest());
	}

	public Maybe<String> resolveLatestSolutionHash(LocalArtifact localArtifact) {
		VersionedArtifactIdentification ai = localArtifact.getArtifactIdentification();
		FuzzyVersion range = FuzzyVersion.from(Version.parse(ai.getVersion()));
		CompiledDependencyIdentification cdi = CompiledDependencyIdentification.create(ai.getGroupId(), ai.getArtifactId(), range);
		
		Maybe<CompiledArtifactIdentification> caiMaybe = uploadDependencyResolver.resolveDependency(cdi);
		
		if (caiMaybe.isUnsatisfiedBy(UnresolvedDependencyVersion.T)) {
			return Reasons.build(NotFound.T).text("No existing version found for " + cdi).toMaybe();
		}
		
		if (caiMaybe.isUnsatisfied())
			return Reasons.build(SolutionHashResolutionFailed.T).text("Failed to resolve latest version for " + cdi).cause(caiMaybe.whyUnsatisfied()).toMaybe();
		
		CompiledArtifactIdentification cai = caiMaybe.get();
		
		Maybe<DeclaredArtifact> declaredArtifactMaybe = declaredArtifactResolver.resolve(cai);
		
		if (declaredArtifactMaybe.isUnsatisfied()) {
			if (declaredArtifactMaybe.isUnsatisfiedBy(NotFound.T)) {
				return Reasons.build(NotFound.T).text("No solution hash found").cause(declaredArtifactMaybe.whyUnsatisfied()).toMaybe();
			}
			
			return declaredArtifactMaybe.whyUnsatisfied().asMaybe();
		}
		
		DeclaredArtifact declaredArtifact = declaredArtifactMaybe.get();
		
		String solutionHash = declaredArtifact.getProperties().get("solution-hash");
		
		if (solutionHash == null)
			return Reasons.build(NotFound.T).text("No solution-hash property found in latest artifact: " + cai).toMaybe();
		
		return Maybe.complete(solutionHash);
	}
	
	@Override
	public void close() {
		context.close();
	}

	private static class SolutionHashResolverWireModule implements WireTerminalModule<ClasspathResolverContract> {

		private final File groupPath;
		private final WorkspaceRepositoryModule workspaceRepositoryModule;

		public SolutionHashResolverWireModule(File groupPath, WorkspaceRepository workspaceRepository) {
			super();
			this.groupPath = groupPath;
			this.workspaceRepositoryModule = new WorkspaceRepositoryModule(Collections.singletonList(workspaceRepository));
		}

		@Override
		public List<WireModule> dependencies() {
			return list(ClasspathResolverWireModule.INSTANCE, ArtifactDataResolverModule.INSTANCE,
					EnvironmentSensitiveConfigurationWireModule.INSTANCE, workspaceRepositoryModule, new GroupEnvironmentWireModule(groupPath));

		}

	}
}
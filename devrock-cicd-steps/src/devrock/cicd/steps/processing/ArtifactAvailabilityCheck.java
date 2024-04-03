package devrock.cicd.steps.processing;

import static com.braintribe.wire.api.util.Lists.list;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.braintribe.devrock.mc.api.commons.PartIdentifications;
import com.braintribe.devrock.mc.api.commons.VersionInfo;
import com.braintribe.devrock.mc.api.repository.configuration.RepositoryReflection;
import com.braintribe.devrock.mc.api.resolver.ArtifactDataResolution;
import com.braintribe.devrock.mc.api.resolver.ArtifactResolver;
import com.braintribe.devrock.mc.api.resolver.CompiledArtifactResolver;
import com.braintribe.devrock.mc.api.resolver.DependencyResolver;
import com.braintribe.devrock.mc.core.wirings.configuration.contract.RepositoryConfigurationContract;
import com.braintribe.devrock.mc.core.wirings.resolver.ArtifactDataResolverModule;
import com.braintribe.devrock.mc.core.wirings.resolver.contract.ArtifactDataResolverContract;
import com.braintribe.devrock.mc.core.wirings.transitive.TransitiveResolverWireModule;
import com.braintribe.devrock.mc.core.wirings.transitive.contract.TransitiveResolverContract;
import com.braintribe.devrock.model.mc.reason.UnresolvedDependency;
import com.braintribe.devrock.model.repository.Repository;
import com.braintribe.devrock.model.repository.RepositoryConfiguration;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.InternalError;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.model.artifact.compiled.CompiledArtifact;
import com.braintribe.model.artifact.compiled.CompiledArtifactIdentification;
import com.braintribe.model.artifact.compiled.CompiledDependencyIdentification;
import com.braintribe.model.version.Version;
import com.braintribe.wire.api.Wire;
import com.braintribe.wire.api.context.WireContext;
import com.braintribe.wire.api.context.WireContextBuilder;
import com.braintribe.wire.api.module.WireModule;
import com.braintribe.wire.api.module.WireTerminalModule;

import devrock.cicd.model.api.data.LocalArtifact;
import devrock.cicd.model.api.reason.ArtifactAvailabilityCheckFailed;

public class ArtifactAvailabilityCheck {
	
	public static Maybe<Map<LocalArtifact, Boolean>> resolveArtifactsAvailability(Collection<LocalArtifact> artifacts, File groupPath) {
		
		final String cachePath;
		final Repository uploadRepository;
		
		// determine uploadRepository and cachePath from repository configuration
		try (WireContext<ArtifactDataResolverContract> wireContext = Wire.context(ArtifactDataResolverModule.INSTANCE, new GroupEnvironmentWireModule(groupPath))) {
			ArtifactDataResolverContract dataResolver = wireContext.contract();
			RepositoryReflection repositoryReflection = dataResolver.repositoryReflection();
			
			RepositoryConfiguration repositoryConfiguration = repositoryReflection.getRepositoryConfiguration();
			
			if (repositoryConfiguration.hasFailed()) {
				return Reasons.build(ArtifactAvailabilityCheckFailed.T).text("Failed to check artifact availability").causes(repositoryConfiguration.getFailure()).toMaybe();
			}
			
			cachePath = repositoryConfiguration.cachePath();
			uploadRepository = repositoryReflection.getUploadRepository();
		}
		
		// use a synthetic repository configuration to resolve latest artifacts to determine the commit hashes
		try (WireContext<TransitiveResolverContract> wireContext = Wire.context(new CommitHashResolverWireModule(uploadRepository, cachePath, groupPath))) {
			TransitiveResolverContract resolver = wireContext.contract();
			ArtifactResolver artifactResolver = resolver.dataResolverContract().artifactResolver();
			
			PreviousArtifactsResolver previousArtifactsResolver = new PreviousArtifactsResolver(artifactResolver);
			artifacts.parallelStream().forEach(previousArtifactsResolver::resolvePreviousArtifact);
			
			List<Reason> errors = previousArtifactsResolver.errors;
			
			if (!errors.isEmpty()) {
				return Reasons.build(ArtifactAvailabilityCheckFailed.T).text("Failed to check artifact availability").causes(errors).toMaybe();
			}
			
			return Maybe.complete(previousArtifactsResolver.availablities);
		}
	}
	
	private static class PreviousArtifactsResolver {
		ArtifactResolver artifactResolver;
		
		Map<LocalArtifact, Boolean> availablities = new ConcurrentHashMap<>();
		List<Reason> errors = new ArrayList<>();
		volatile boolean failed;
		
		public PreviousArtifactsResolver(ArtifactResolver artifactResolver) {
			this.artifactResolver = artifactResolver;
		}

		public void resolvePreviousArtifact(LocalArtifact la) {
			if (failed)
				return;
			
			// create artifact identification with a version one prior to the one from the local artifact  
			CompiledArtifactIdentification cai = CompiledArtifactIdentification.from(la.getArtifactIdentification());
			
			Version version = cai.getVersion();
			version.setRevision(version.getRevision() - 1);
			
			try {
				Maybe<List<VersionInfo>> versionsMaybe = artifactResolver.getVersionsReasoned(cai);
				
				if (versionsMaybe.isUnsatisfied()) {
					addError(versionsMaybe.whyUnsatisfied());
					return;
				}
				
				List<VersionInfo> versions = versionsMaybe.get();
				
				boolean versionFound = false;
				
				for (VersionInfo vi: versions) {
					if (vi.version().matches(version)) {
						versionFound = true;
						break;
					}
				}
				
				if (!versionFound) {
					availablities.put(la, false);
					return;
				}
				
				Maybe<ArtifactDataResolution> resolutionMaybe = artifactResolver.resolvePart(cai, PartIdentifications.publishComplete);
				
				if (resolutionMaybe.isUnsatisfied()) {
					if (resolutionMaybe.isUnsatisfiedBy(NotFound.T)) {
						// expected
						availablities.put(la, false);
						return;
					}
					else {
						// unexpected
						addError(resolutionMaybe.whyUnsatisfied());
						return;
					}
				}
				
				ArtifactDataResolution artifactDataResolution = resolutionMaybe.get();
				
				Maybe<Boolean> backedMaybe = artifactDataResolution.backed();
				
				if (backedMaybe.isUnsatisfied()) {
					addError(backedMaybe.whyUnsatisfied());
					return;
				}
				
				availablities.put(la, backedMaybe.get());
			}
			catch (RuntimeException e) {
				addError(InternalError.from(e, "Error while resolving " + cai.asString()));
			}		
		}
		
		private void addError(Reason error) {
			synchronized (errors) {
				errors.add(error);
				failed = true;
			}
		}
	}
	@SuppressWarnings("unused")
	// not used at the moment as we are still using revisions on source repo level
	private static class LatestArtifactsResolver {
		DependencyResolver dependencyResolver;
		CompiledArtifactResolver compiledArtifactResolver;
		Map<CompiledDependencyIdentification, Maybe<CompiledArtifact>> resolutions = new ConcurrentHashMap<>();
		
		public LatestArtifactsResolver(DependencyResolver dependencyResolver,
				CompiledArtifactResolver compiledArtifactResolver) {
			this.dependencyResolver = dependencyResolver;
			this.compiledArtifactResolver = compiledArtifactResolver;
		}

		public void resolveLatestArtifact(CompiledDependencyIdentification cdi) {
			try {
				Maybe<CompiledArtifactIdentification> caiMaybe = dependencyResolver.resolveDependency(cdi);
				
				if (caiMaybe.isUnsatisfied()) {
					if (caiMaybe.isUnsatisfiedBy(UnresolvedDependency.T)) {
						// this reasoning addresses the expected case of no available version in range at all
						resolutions.put(cdi, Reasons.build(NotFound.T).text("artifact not found").cause(caiMaybe.whyUnsatisfied()).toMaybe());
					}
					else {
						resolutions.put(cdi, caiMaybe.whyUnsatisfied().asMaybe());
					}
					return;
				}
				
				CompiledArtifactIdentification cai = caiMaybe.get();
				
				Maybe<CompiledArtifact> caMaybe = compiledArtifactResolver.resolve(cai);
				
				if (caMaybe.isUnsatisfied()) {
					if (caMaybe.isUnsatisfiedBy(NotFound.T)) {
						// this reasoning addresses the expected case of no available artifact (less likely but possible if maven-metadata.xml was inconsistent with actual data)
						resolutions.put(cdi, Reasons.build(NotFound.T).text("artifact not found").cause(caMaybe.whyUnsatisfied()).toMaybe());
					}
					else {
						resolutions.put(cdi, caMaybe.whyUnsatisfied().asMaybe());
					}
					return;
				}
				
				resolutions.put(cdi, caMaybe);
			}
			catch (RuntimeException e) {
				resolutions.put(cdi, InternalError.from(e, "Error while resolving " + cdi.asString()).asMaybe());
			}		
		}
		
		
	}
	
	private static class CommitHashResolverWireModule implements WireTerminalModule<TransitiveResolverContract> {
		private RepositoryConfiguration repositoryConfiguration;
		private File groupPath;
		
		public CommitHashResolverWireModule(Repository uploadRepository, String cachePath, File groupPath) {
			super();
			this.groupPath = groupPath;
			repositoryConfiguration = RepositoryConfiguration.T.create();
			repositoryConfiguration.setCachePath(cachePath);
			repositoryConfiguration.getRepositories().add(uploadRepository);
		}

		@Override
		public List<WireModule> dependencies() {
			return list(TransitiveResolverWireModule.INSTANCE, ArtifactDataResolverModule.INSTANCE, new GroupEnvironmentWireModule(groupPath));
		}

		@Override
		public void configureContext(WireContextBuilder<?> contextBuilder) {
			contextBuilder.bindContract(RepositoryConfigurationContract.class, () -> Maybe.complete(repositoryConfiguration));
		}
		
	}
}
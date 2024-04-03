package devrock.cicd.steps.processor;

import static com.braintribe.console.ConsoleOutputs.println;
import static com.braintribe.console.ConsoleOutputs.sequence;
import static com.braintribe.console.ConsoleOutputs.text;

import java.io.File;
import java.util.List;

import com.braintribe.common.attribute.common.CallerEnvironment;
import com.braintribe.console.ConsoleOutputs;
import com.braintribe.devrock.mc.api.deploy.ArtifactDeployer;
import com.braintribe.devrock.mc.api.resolver.ArtifactDataResolver;
import com.braintribe.devrock.mc.core.commons.McOutputs;
import com.braintribe.devrock.mc.core.wirings.backend.ArtifactDataBackendModule;
import com.braintribe.devrock.mc.core.wirings.backend.contract.ArtifactDataBackendContract;
import com.braintribe.devrock.mc.core.wirings.configuration.contract.DevelopmentEnvironmentContract;
import com.braintribe.devrock.mc.core.wirings.resolver.ArtifactDataResolverModule;
import com.braintribe.devrock.mc.core.wirings.resolver.contract.ArtifactDataResolverContract;
import com.braintribe.devrock.model.mc.reason.InvalidRepositoryConfiguration;
import com.braintribe.devrock.model.repository.Repository;
import com.braintribe.devrock.model.repository.RepositoryConfiguration;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.model.artifact.consumable.Artifact;
import com.braintribe.model.artifact.consumable.ArtifactResolution;
import com.braintribe.model.artifact.consumable.Part;
import com.braintribe.model.resource.FileResource;
import com.braintribe.model.resource.Resource;
import com.braintribe.utils.lcd.LazyInitialized;
import com.braintribe.wire.api.Wire;
import com.braintribe.wire.api.context.WireContext;
import com.braintribe.wire.api.context.WireContextBuilder;

import devrock.cicd.model.api.PublishArtifacts;
import devrock.cicd.model.api.PublishArtifactsResponse;
import devrock.cicd.model.api.reason.UploadArtifactsFailed;
import devrock.cicd.steps.processing.ArtifactIndexUpdate;

public class PublishArtifactsProcessor extends SpawningServiceProcessor<PublishArtifacts, PublishArtifactsResponse> {
	
	@Override
	protected StatefulServiceProcessor spawn() { 
		return new StatefulServiceProcessor() {
			private Repository uploadRepository;
			
			@Override
			protected Maybe<? extends PublishArtifactsResponse> process() {
				if (request.getBuildResult().getArtifacts().isEmpty())
					return Maybe.complete(PublishArtifactsResponse.T.create());
				
				Reason error = loadRepositoryInformation();
				
				if (error != null)
					return error.asMaybe();
				
				for (Artifact artifact: request.getBuildResult().getArtifacts()) {
					ConsoleOutputs.println();
					ConsoleOutputs.println(McOutputs.artifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion()));
					
					for (Part part: artifact.getParts().values()) {
						Resource resource = part.getResource();
						
						final String path;
						if (resource instanceof FileResource) {
							FileResource fileResource = (FileResource)resource;
							path = fileResource.getPath();
						}
						else {
							path = null;
						}
						
						println(sequence( //
							text("  "), //
							McOutputs.partIdentification(part), //
							text(" -> "), //
							text(String.valueOf(path))
						));
					}
				}
				
				PublishArtifactsResponse response = PublishArtifactsResponse.T.create();
				
				error = uploadArtifacts();
				
				if (error != null)
					return error.asMaybe();
				
				return Maybe.complete(response);
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
					
					uploadRepository = repositoryConfiguration.getUploadRepository();
					
					if (uploadRepository == null)
						return Reasons.build(InvalidRepositoryConfiguration.T) //
								.text("Missing upload repository in repository configuration").toReason();
				}
				
				return null;
			}
			
			private Reason uploadArtifacts() {
				try (WireContext<ArtifactDataBackendContract> wireContext = Wire.context(ArtifactDataBackendModule.INSTANCE)) {
					ArtifactDataBackendContract artifactDataBackendContract = wireContext.contract();
					
					ArtifactDeployer artifactDeployer = artifactDataBackendContract.artifactDeployer(uploadRepository);
					ArtifactDataResolver resolver = artifactDataBackendContract.repository(uploadRepository);
					
					ArtifactResolution resolution = artifactDeployer.deploy(request.getBuildResult().getArtifacts());
					
					List<Artifact> publishedArtifacts = resolution.getSolutions().stream().filter(a -> !a.hasFailed()).toList();
					
					LazyInitialized<Reason> collatorReason = new LazyInitialized<>(() -> Reasons.build(UploadArtifactsFailed.T).text("Failure while uploading to repository " + uploadRepository.getName()).toReason());
					
					Reason error = ArtifactIndexUpdate.updateArtifactIndex(uploadRepository, artifactDeployer, resolver, publishedArtifacts);
					
					if (error != null)
						collatorReason.get().getReasons().add(error);
					
					if (resolution.hasFailed())
						collatorReason.get().getReasons().add(resolution.getFailure());
					
					if (collatorReason.isInitialized())
						return collatorReason.get();
					
					return null;
				}
			}
		};
	};
}

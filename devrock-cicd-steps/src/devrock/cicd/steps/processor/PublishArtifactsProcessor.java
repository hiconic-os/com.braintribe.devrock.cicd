package devrock.cicd.steps.processor;

import static com.braintribe.console.ConsoleOutputs.println;
import static com.braintribe.console.ConsoleOutputs.sequence;
import static com.braintribe.console.ConsoleOutputs.text;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.braintribe.common.attribute.common.CallerEnvironment;
import com.braintribe.console.ConsoleOutputs;
import com.braintribe.devrock.mc.api.deploy.ArtifactDeployer;
import com.braintribe.devrock.mc.api.resolver.ArtifactDataResolution;
import com.braintribe.devrock.mc.api.resolver.ArtifactDataResolver;
import com.braintribe.devrock.mc.core.commons.McOutputs;
import com.braintribe.devrock.mc.core.repository.index.ArtifactIndex;
import com.braintribe.devrock.mc.core.resolver.BasicDependencyResolver;
import com.braintribe.devrock.mc.core.wirings.backend.ArtifactDataBackendModule;
import com.braintribe.devrock.mc.core.wirings.backend.contract.ArtifactDataBackendContract;
import com.braintribe.devrock.mc.core.wirings.configuration.contract.DevelopmentEnvironmentContract;
import com.braintribe.devrock.mc.core.wirings.resolver.ArtifactDataResolverModule;
import com.braintribe.devrock.mc.core.wirings.resolver.contract.ArtifactDataResolverContract;
import com.braintribe.devrock.model.mc.reason.InvalidRepositoryConfiguration;
import com.braintribe.devrock.model.mc.reason.UnresolvedDependencyVersion;
import com.braintribe.devrock.model.repository.MavenHttpRepository;
import com.braintribe.devrock.model.repository.Repository;
import com.braintribe.devrock.model.repository.RepositoryConfiguration;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.InternalError;
import com.braintribe.gm.model.reason.essential.IoError;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.model.artifact.compiled.CompiledArtifactIdentification;
import com.braintribe.model.artifact.compiled.CompiledDependencyIdentification;
import com.braintribe.model.artifact.consumable.Artifact;
import com.braintribe.model.artifact.consumable.ArtifactResolution;
import com.braintribe.model.artifact.consumable.Part;
import com.braintribe.model.artifact.essential.PartIdentification;
import com.braintribe.model.resource.FileResource;
import com.braintribe.model.resource.Resource;
import com.braintribe.model.version.Version;
import com.braintribe.utils.stream.api.StreamPipe;
import com.braintribe.utils.stream.api.StreamPipes;
import com.braintribe.wire.api.Wire;
import com.braintribe.wire.api.context.WireContext;
import com.braintribe.wire.api.context.WireContextBuilder;

import devrock.cicd.model.api.PublishArtifacts;
import devrock.cicd.model.api.PublishArtifactsResponse;
import devrock.cicd.model.api.reason.ArtifactIndexUpdateFailed;
import devrock.cicd.steps.processor.locking.DistributedLocking;

public class PublishArtifactsProcessor extends SpawningServiceProcessor<PublishArtifacts, PublishArtifactsResponse> {
	
	@Override
	protected StatefulServiceProcessor spawn() { 
		return new StatefulServiceProcessor() {
			private RepositoryConfiguration installRepositoryConfiguration;
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
					
					updateArtifactIndex(artifactDeployer, resolver, publishedArtifacts);
					
					if (resolution.hasFailed())
						return resolution.getFailure();
					
					return null;
				}
			}
			
			private Lock acquireLockArtifactUpdateLock(MavenHttpRepository httpRepository) {
				Function<String,Lock> lockManager = DistributedLocking.lockManager();
				return lockManager.apply("update-artifact-index:" + httpRepository.getUrl());
			}
			
			private Reason updateArtifactIndex(ArtifactDeployer artifactDeployer, ArtifactDataResolver resolver, List<Artifact> publishedArtifacts) {
				if (!(uploadRepository instanceof MavenHttpRepository))
					return null;
				
				Lock lock = acquireLockArtifactUpdateLock((MavenHttpRepository)uploadRepository);
				
				try {
					if (!lock.tryLock(10, TimeUnit.SECONDS))
						return Reasons.build(ArtifactIndexUpdateFailed.T).text("Could not acquire update lock").toReason();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				
				try {
					CompiledDependencyIdentification indexCdi = CompiledDependencyIdentification.create("meta", "artifact-index", "[1,)");
					
					
					BasicDependencyResolver dependencyResolver = new BasicDependencyResolver(resolver);
					Maybe<CompiledArtifactIdentification> caiMaybe = dependencyResolver.resolveDependency(indexCdi);

					final CompiledArtifactIdentification indexCai; 
					final ArtifactIndex artifactIndex;
					
					if (caiMaybe.isUnsatisfied()) {
						if (caiMaybe.isUnsatisfiedBy(UnresolvedDependencyVersion.T)) {
							indexCai = CompiledArtifactIdentification.create("meta", "artifact-index", "1");
							artifactIndex = new ArtifactIndex(true);
						}
						else {
							return Reasons.build(ArtifactIndexUpdateFailed.T).text("Error while retrieving existing artifact index") //
									.cause(caiMaybe.whyUnsatisfied()).toReason();
						}
					}
					else {
						indexCai = caiMaybe.get();

						Maybe<ArtifactIndex> indexMaybe = downloadIndex(indexCai, resolver);
						
						if (indexMaybe.isUnsatisfied()) {
							if (indexMaybe.isUnsatisfiedBy(NotFound.T)) {
								artifactIndex = new ArtifactIndex(true);
							}
							else {
								return Reasons.build(ArtifactIndexUpdateFailed.T).text("Error while retrieving existing artifact index") //
										.cause(indexMaybe.whyUnsatisfied()).toReason();
							}
						}
						else {
							Version version = indexCai.getVersion();
							version.setMajor(version.getMajor() + 1);
							
							artifactIndex = indexMaybe.get();
						}
					}
					
					for (Artifact artifact: publishedArtifacts) {
						artifactIndex.update(artifact.asString());
					}
					
					Reason error = uploadIndex(indexCai, artifactDeployer, artifactIndex);
					
					if (error != null) {
						return Reasons.build(ArtifactIndexUpdateFailed.T).text("Error while updating artifact index") //
								.cause(error).toReason();
					}
					
					return null;
				}
				finally {
					lock.unlock();
				}
			}
			
			private Reason uploadIndex(CompiledArtifactIdentification indexCai, ArtifactDeployer deployer, ArtifactIndex index) {
				StreamPipe pipe = StreamPipes.fileBackedFactory().newPipe("artifact-index");
				
				try (OutputStream out = new GZIPOutputStream(pipe.openOutputStream())) {
					index.write(out);
				}
				catch (IOException e) {
					return Reasons.build(IoError.T).text("Error while writing " + indexCai.asString()).cause(InternalError.from(e)).toReason();
				}
				
				Resource resource = Resource.createTransient(pipe::openInputStream);
				resource.setName("artifact-index.gzip");
				
				PartIdentification partIdentification = PartIdentification.create("gzip");
				
				Artifact artifact = Artifact.T.create();
				artifact.setGroupId(indexCai.getGroupId());
				artifact.setArtifactId(indexCai.getArtifactId());
				artifact.setVersion(indexCai.getVersion().asString());
				
				Part part = Part.T.create();
				part.setType(partIdentification.getType());
				part.setResource(resource);
				
				artifact.getParts().put(partIdentification.asString(), part);
				
				ArtifactResolution resolution = deployer.deploy(artifact);
				
				if (resolution.hasFailed())
					return resolution.getFailure();
				
				return null;
			}
			
			private Maybe<ArtifactIndex> downloadIndex(CompiledArtifactIdentification indexCai, ArtifactDataResolver resolver) {
				Maybe<ArtifactDataResolution> indexPartMaybe = resolver.resolvePart(indexCai, PartIdentification.create("gzip"));
				
				if (indexPartMaybe.isUnsatisfied()) {
					return indexPartMaybe.whyUnsatisfied().asMaybe();
				}
				
				ArtifactDataResolution partResolution = indexPartMaybe.get();
				Maybe<InputStream> inMaybe = partResolution.openStream();
				
				if (inMaybe.isUnsatisfied()) {
					return inMaybe.whyUnsatisfied().asMaybe();
				}
				
				try (InputStream in = new GZIPInputStream(inMaybe.get())) {
					return Maybe.complete(ArtifactIndex.read(in, false));
				}
				catch (IOException e) {
					return Reasons.build(IoError.T).text("Error while reading " + indexCai.asString()).cause(InternalError.from(e)).toMaybe();
				}
			}
		};
	};
}

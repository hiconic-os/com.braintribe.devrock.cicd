package devrock.cicd.steps.processor;

import static com.braintribe.wire.api.util.Lists.list;

import java.io.File;
import java.util.Collection;
import java.util.List;

import com.braintribe.devrock.mc.api.commons.PartIdentifications;
import com.braintribe.devrock.mc.core.wirings.configuration.contract.RepositoryConfigurationContract;
import com.braintribe.devrock.mc.core.wirings.transitive.TransitiveResolverWireModule;
import com.braintribe.devrock.mc.core.wirings.transitive.contract.TransitiveResolverContract;
import com.braintribe.devrock.model.repository.RepositoryConfiguration;
import com.braintribe.devrock.model.repository.WorkspaceRepository;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.artifact.consumable.Artifact;
import com.braintribe.model.artifact.consumable.Part;
import com.braintribe.model.artifact.essential.VersionedArtifactIdentification;
import com.braintribe.model.resource.FileResource;
import com.braintribe.wire.api.context.WireContextBuilder;
import com.braintribe.wire.api.module.WireModule;
import com.braintribe.wire.api.module.WireTerminalModule;

import devrock.cicd.model.api.data.LocalArtifact;

public class CodebaseDependencyResolverWireModule implements WireTerminalModule<TransitiveResolverContract> {
		
		private static final String FILENAME_POM = "pom.xml";
		private File groupPath;
		private RepositoryConfiguration repositoryConfiguration;
		
		public CodebaseDependencyResolverWireModule(File groupPath, Collection<LocalArtifact> localArtifacts) {
			super();
			this.groupPath = groupPath;
			
			WorkspaceRepository workspaceRepository = WorkspaceRepository.T.create();
			workspaceRepository.setCachable(false);
			workspaceRepository.setName("codebase");
			
			for (LocalArtifact localArtifact: localArtifacts) {
				VersionedArtifactIdentification vai = localArtifact.getArtifactIdentification();

				Artifact artifact = Artifact.T.create();
				artifact.setGroupId(vai.getGroupId());
				artifact.setArtifactId(vai.getArtifactId());
				artifact.setVersion(vai.getVersion());
				
				Part pomPart = Part.T.create();
				pomPart.setClassifier(PartIdentifications.pom.getClassifier());
				pomPart.setType(PartIdentifications.pom.getType());
				pomPart.setRepositoryOrigin("codebase");
				
				FileResource pomResource = FileResource.T.create();
				pomResource.setName(FILENAME_POM);
				String pomPath = groupPath.toPath().resolve(localArtifact.getFolderName()).resolve(FILENAME_POM).toAbsolutePath().toString();
				pomResource.setPath(pomPath);
				
				pomPart.setResource(pomResource);
				
				artifact.getParts().put(PartIdentifications.pom.asString(), pomPart);
				
				workspaceRepository.getArtifacts().add(artifact);
			}
			
			String cachePath = new File(this.groupPath, "cache").getAbsolutePath();
			
			repositoryConfiguration = RepositoryConfiguration.T.create();
			repositoryConfiguration.setCachePath(cachePath);
			repositoryConfiguration.getRepositories().add(workspaceRepository);
		}

		@Override
		public List<WireModule> dependencies() {
			return list(TransitiveResolverWireModule.INSTANCE);
			
		}
		
		@Override
		public void configureContext(WireContextBuilder<?> contextBuilder) {
			contextBuilder.bindContract(RepositoryConfigurationContract.class, () -> Maybe.complete(repositoryConfiguration));
		}
	}
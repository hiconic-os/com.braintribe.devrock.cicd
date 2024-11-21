package labs;

import java.util.Collections;
import java.util.List;

import com.braintribe.devrock.mc.api.deploy.ArtifactDeployer;
import com.braintribe.devrock.mc.api.resolver.ArtifactDataResolver;
import com.braintribe.devrock.mc.core.repository.index.ArtifactIndex;
import com.braintribe.devrock.mc.core.wirings.backend.ArtifactDataBackendModule;
import com.braintribe.devrock.mc.core.wirings.backend.contract.ArtifactDataBackendContract;
import com.braintribe.devrock.model.repository.IndexedMavenHttpRepository;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.model.artifact.compiled.CompiledArtifactIdentification;
import com.braintribe.model.artifact.essential.VersionedArtifactIdentification;
import com.braintribe.wire.api.Wire;
import com.braintribe.wire.api.context.WireContext;

import devrock.cicd.steps.processing.ArtifactIndexUpdate;

public class ArtifactIndexConcurrencyLab {
	public static void main1(String[] args) {
		IndexedMavenHttpRepository repo = IndexedMavenHttpRepository.T.create();
		repo.setName("test");
		repo.setUrl("https://maven.pkg.github.com/tryptamic/maven-test");
		repo.setUser("tryptamic");
		repo.setPassword(System.getenv("GIT_TOKEN"));
		
		try (WireContext<ArtifactDataBackendContract> wireContext = Wire.context(ArtifactDataBackendModule.INSTANCE)) {
			ArtifactDataBackendContract artifactDataBackendContract = wireContext.contract();
			
			ArtifactDeployer artifactDeployer = artifactDataBackendContract.artifactDeployer(repo);
			ArtifactDataResolver resolver = artifactDataBackendContract.repository(repo);
			
			CompiledArtifactIdentification indexCai = CompiledArtifactIdentification.create("meta", "artifact-index", "1");
			
			
			
			ArtifactIndex index = new ArtifactIndex(true);
			index.update("foo:bar#1.0.1");
			
			Reason reason = ArtifactIndexUpdate.uploadIndex(indexCai, artifactDeployer, index);
			
			if (reason != null)
				System.out.println(reason.stringify());
			
			
			// ArtifactIndexUpdate.updateArtifactIndex(repo, artifactDeployer, resolver, null)
			
		}
	}
	
	public static void main(String[] args) {
		IndexedMavenHttpRepository repo = IndexedMavenHttpRepository.T.create();
		repo.setName("test");
		repo.setUrl("https://maven.pkg.github.com/tryptamic/maven-test");
		repo.setUser("tryptamic");
		repo.setPassword(System.getenv("GIT_TOKEN"));
		
		try (WireContext<ArtifactDataBackendContract> wireContext = Wire.context(ArtifactDataBackendModule.INSTANCE)) {
			ArtifactDataBackendContract artifactDataBackendContract = wireContext.contract();
			
			ArtifactDeployer artifactDeployer = artifactDataBackendContract.artifactDeployer(repo);
			ArtifactDataResolver resolver = artifactDataBackendContract.repository(repo);
			
			List<VersionedArtifactIdentification> updates = Collections.singletonList(VersionedArtifactIdentification.create("foo", "bar", "1.0.1"));
			ArtifactIndexUpdate.updateArtifactIndex(repo, artifactDeployer, resolver, updates);
		}
	}
}

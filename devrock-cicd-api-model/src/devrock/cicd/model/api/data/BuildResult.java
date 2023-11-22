package devrock.cicd.model.api.data;

import java.util.List;

import com.braintribe.model.artifact.consumable.Artifact;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface BuildResult extends GenericEntity {
	EntityType<BuildResult> T = EntityTypes.T(BuildResult.class);
	
	String artifacts = "artifacts";
	
	List<Artifact> getArtifacts();
	void setArtifacts(List<Artifact> artifacts);
}

package devrock.cicd.model.api.reason;

import com.braintribe.gm.model.reason.Reason;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface ArtifactsBuildFailed extends Reason {
	EntityType<ArtifactsBuildFailed> T = EntityTypes.T(ArtifactsBuildFailed.class);
	
}

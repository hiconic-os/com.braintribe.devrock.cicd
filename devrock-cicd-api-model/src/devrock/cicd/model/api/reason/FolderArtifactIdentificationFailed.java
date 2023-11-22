package devrock.cicd.model.api.reason;

import com.braintribe.gm.model.reason.Reason;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface FolderArtifactIdentificationFailed extends Reason {
	EntityType<FolderArtifactIdentificationFailed> T = EntityTypes.T(FolderArtifactIdentificationFailed.class);
}

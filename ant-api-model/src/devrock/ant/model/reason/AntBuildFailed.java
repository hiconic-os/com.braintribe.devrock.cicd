package devrock.ant.model.reason;

import com.braintribe.gm.model.reason.Reason;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface AntBuildFailed extends Reason {
	EntityType<AntBuildFailed> T = EntityTypes.T(AntBuildFailed.class);
}

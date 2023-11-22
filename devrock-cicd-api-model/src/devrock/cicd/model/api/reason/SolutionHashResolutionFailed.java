package devrock.cicd.model.api.reason;

import com.braintribe.gm.model.reason.Reason;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface SolutionHashResolutionFailed extends Reason {
	EntityType<SolutionHashResolutionFailed> T = EntityTypes.T(SolutionHashResolutionFailed.class);
}

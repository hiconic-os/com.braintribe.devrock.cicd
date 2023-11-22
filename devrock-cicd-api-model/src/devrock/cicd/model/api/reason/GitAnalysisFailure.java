package devrock.cicd.model.api.reason;

import com.braintribe.gm.model.reason.Reason;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface GitAnalysisFailure extends Reason {
	EntityType<GitAnalysisFailure> T = EntityTypes.T(GitAnalysisFailure.class);
}

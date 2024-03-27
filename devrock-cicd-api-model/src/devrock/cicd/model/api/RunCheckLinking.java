package devrock.cicd.model.api;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface RunCheckLinking extends RunBuild {
	EntityType<RunCheckLinking> T = EntityTypes.T(RunCheckLinking.class);
}

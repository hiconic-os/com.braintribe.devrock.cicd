package devrock.cicd.model.api;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface RunInstall extends RunBuild {
	EntityType<RunInstall> T = EntityTypes.T(RunInstall.class);
}

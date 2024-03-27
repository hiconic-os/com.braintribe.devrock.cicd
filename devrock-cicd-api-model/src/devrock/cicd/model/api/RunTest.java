package devrock.cicd.model.api;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface RunTest extends RunBuild {
	EntityType<RunTest> T = EntityTypes.T(RunTest.class);
}

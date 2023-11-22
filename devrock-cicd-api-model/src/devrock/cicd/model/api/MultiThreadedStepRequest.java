package devrock.cicd.model.api;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

import devrock.step.model.api.StepRequest;

public interface MultiThreadedStepRequest extends StepRequest {

	EntityType<MultiThreadedStepRequest> T = EntityTypes.T(MultiThreadedStepRequest.class);

	String threads = "threads";

	Integer getThreads();
	void setThreads(Integer threads);

}

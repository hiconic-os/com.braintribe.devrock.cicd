package devrock.cicd.model.api.test;

import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

import devrock.step.model.api.StepRequest;

public interface Test1Request extends StepRequest {
	EntityType<Test1Request> T = EntityTypes.T(Test1Request.class);
	
	@Override
	EvalContext<? extends TestResponse> eval(Evaluator<ServiceRequest> evaluator);
}

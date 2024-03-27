package devrock.step.model.api;

import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

public interface RunDefaultStep extends ServiceRequest {
	EntityType<RunDefaultStep> T = EntityTypes.T(RunDefaultStep.class);
	
	@Override
	EvalContext<? extends StepResponse> eval(Evaluator<ServiceRequest> evaluator);
}

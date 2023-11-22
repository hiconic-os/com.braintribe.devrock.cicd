package devrock.step.model.api;

import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

@Abstract
public interface StepRequest extends ServiceRequest {
	EntityType<StepRequest> T = EntityTypes.T(StepRequest.class);
	
	@Override
	EvalContext<? extends StepResponse> eval(Evaluator<ServiceRequest> evaluator);
}

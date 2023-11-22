package devrock.cicd.model.api;

import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

import devrock.step.model.api.StepRequest;

public interface InitializeExchange extends StepRequest {
	EntityType<InitializeExchange> T = EntityTypes.T(InitializeExchange.class);
	
	@Override
	EvalContext<? extends InitializeExchangeResponse> eval(Evaluator<ServiceRequest> evaluator);
}

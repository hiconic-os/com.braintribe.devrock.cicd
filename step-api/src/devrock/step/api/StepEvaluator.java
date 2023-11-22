package devrock.step.api;

import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.ReasonException;
import com.braintribe.model.generic.reflection.EntityType;

import devrock.step.model.api.StepRequest;

public interface StepEvaluator extends StepExchangeContext {
	Reason evaluate(StepRequest request);
	Reason evaluate(EntityType<? extends StepRequest> stepType);
	default void evaluateOrThrow(EntityType<? extends StepRequest> stepType) {
		Reason reason = evaluate(stepType);
		if (reason != null)
			throw new ReasonException(reason);
	};
	
	default void evaluateOrThrow(StepRequest stepRequest) {
		Reason reason = evaluate(stepRequest);
		if (reason != null)
			throw new ReasonException(reason);
	};
}

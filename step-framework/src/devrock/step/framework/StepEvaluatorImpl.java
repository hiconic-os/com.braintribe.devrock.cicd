package devrock.step.framework;

import java.io.File;
import java.util.function.Function;

import com.braintribe.common.attribute.common.CallerEnvironment;
import com.braintribe.common.attribute.common.impl.BasicCallerEnvironment;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.session.api.managed.ModelAccessory;
import com.braintribe.model.service.api.ServiceRequest;

import devrock.step.api.StepEvaluator;
import devrock.step.api.StepExchangeContextAttribute;
import devrock.step.model.api.StepRequest;
import devrock.step.model.api.StepResponse;

public class StepEvaluatorImpl extends StepExchangeContextImpl implements StepEvaluator {

	private final Evaluator<ServiceRequest> evaluator;
	private final File cwd;
	private StepRequest currentRequest;

	public StepEvaluatorImpl(ModelAccessory modelAccessory, File cwd, File configFolder, Evaluator<ServiceRequest> evaluator,
			Function<String, Object> properties) {

		super(cwd, configFolder, modelAccessory, properties);
		this.cwd = cwd;
		this.evaluator = evaluator;
	}

	@Override
	public Reason evaluate(EntityType<? extends StepRequest> stepType) {
		return evaluate(stepType.create());
	}

	@Override
	public Reason evaluate(StepRequest stepRequest) {
		currentRequest = stepRequest;

		try {
			BasicCallerEnvironment callerEnvironment = new BasicCallerEnvironment(true, cwd);
	
			EvalContext<? extends StepResponse> evalContext = stepRequest.eval(evaluator);
			evalContext.setAttribute(StepExchangeContextAttribute.class, this);
			evalContext.setAttribute(CallerEnvironment.class, callerEnvironment);
	
			return evalContext.getReasoned().whyUnsatisfied();

		} finally {
			currentRequest = null;
		}
	}

	@Override
	public StepRequest getCurrentRequest() {
		return currentRequest;
	}

}

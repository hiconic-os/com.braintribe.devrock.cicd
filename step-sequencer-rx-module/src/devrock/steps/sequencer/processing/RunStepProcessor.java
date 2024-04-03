package devrock.steps.sequencer.processing;

import com.braintribe.cfg.Required;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.service.api.ReasonedServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.utils.StringTools;

import devrock.step.model.api.RunStep;
import devrock.step.model.api.StepRequest;
import devrock.step.model.api.StepResponse;
import devrock.step.sequencer.model.configuration.Step;
import devrock.step.sequencer.model.configuration.StepConfiguration;

public class RunStepProcessor implements ReasonedServiceProcessor<RunStep, StepResponse> {
	private StepConfiguration configuration;
	
	@Required
	public void setConfiguration(StepConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public Maybe<? extends StepResponse> processReasoned(ServiceRequestContext context, RunStep request) {
		
		Maybe<Step> stepMaybe = resolveStep(request.getStep());
		
		if (stepMaybe.isUnsatisfied())
			return stepMaybe.emptyCast();
		
		Step step = stepMaybe.get();
		
		if (step == null)
			step = configuration.getDefaultStep();
		
		if (step == null)
			Reasons.build(NotFound.T).text("No default step found in step-configuration").toMaybe();
		
		StepRequest stepRequest = step.getRequest();
		
		return stepRequest.eval(context).getReasoned();
	}

	private Maybe<Step> resolveStep(String stepName) {
		if (stepName == null)
			return Maybe.complete(null);
		
		for (Step step : configuration.getSteps()) {
			StepRequest request = step.getRequest();
			EntityType<GenericEntity> type = request.entityType();
			
			String candidates[] = {
					StringTools.camelCaseToDashSeparated(type.getShortName()),
					type.getShortName(),
					type.getTypeSignature()
			};
			
			for (String candidate: candidates) {
				if (stepName.equals(candidate))
					return Maybe.complete(step);
			}
		}
		
		return Reasons.build(InvalidArgument.T).text("Unknown step: " + stepName).toMaybe();
	}
}

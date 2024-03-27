package devrock.steps.sequencer.processing;

import com.braintribe.cfg.Required;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.model.processing.service.api.ReasonedServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;

import devrock.step.model.api.RunDefaultStep;
import devrock.step.model.api.StepRequest;
import devrock.step.model.api.StepResponse;
import devrock.step.sequencer.model.configuration.Step;
import devrock.step.sequencer.model.configuration.StepConfiguration;

public class RunDefaultStepProcessor implements ReasonedServiceProcessor<RunDefaultStep, StepResponse> {
	private StepConfiguration configuration;
	
	@Required
	public void setConfiguration(StepConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public Maybe<? extends StepResponse> processReasoned(ServiceRequestContext context, RunDefaultStep request) {
		Step defaultStep = configuration.getDefaultStep();
		
		if (defaultStep == null)
			Reasons.build(NotFound.T).text("No default step found in step-configuration").toMaybe();
		
		StepRequest stepRequest = defaultStep.getRequest();
		
		return stepRequest.eval(context).getReasoned();
	}
}

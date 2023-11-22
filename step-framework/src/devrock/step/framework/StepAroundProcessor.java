package devrock.step.framework;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.model.processing.service.api.ProceedContext;
import com.braintribe.model.processing.service.api.ReasonedServiceAroundProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;

import devrock.step.api.StepExchangeContext;
import devrock.step.api.StepExchangeContextAttribute;
import devrock.step.model.api.StepRequest;
import devrock.step.model.api.StepResponse;

public class StepAroundProcessor implements ReasonedServiceAroundProcessor<StepRequest, StepResponse>{

	@Override
	public Maybe<? extends StepResponse> processReasoned(ServiceRequestContext context, StepRequest request,
			ProceedContext proceedContext) {
		StepExchangeContext exchangeContext = context.getAttribute(StepExchangeContextAttribute.class);
		
		if (exchangeContext == null)
			return Reasons.build(NotFound.T).text("Couldn't find a StepExchangeContext").toMaybe();

		Reason error = exchangeContext.loadProperties(request);
		
		if (error != null)
			return error.asMaybe();
		
		Maybe<? extends StepResponse> maybe = proceedContext.proceedReasoned(request);
		
		if (maybe.isSatisfied()) {
			StepResponse response = maybe.get();
			
			if (response != null)
				exchangeContext.storeProperties(response);
		}
		
		return maybe;
	}
}

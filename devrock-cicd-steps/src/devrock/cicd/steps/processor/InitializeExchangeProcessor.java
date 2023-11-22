package devrock.cicd.steps.processor;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.processing.service.api.ReasonedServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;

import devrock.cicd.model.api.InitializeExchange;
import devrock.cicd.model.api.InitializeExchangeResponse;
import devrock.step.api.StepExchangeContext;
import devrock.step.api.StepExchangeContextAttribute;

public class InitializeExchangeProcessor implements ReasonedServiceProcessor<InitializeExchange, InitializeExchangeResponse> {

	@Override
	public Maybe<? extends InitializeExchangeResponse> processReasoned(ServiceRequestContext context,
			InitializeExchange request) {
		StepExchangeContext exchangeContext = context.getAttribute(StepExchangeContextAttribute.class);
		exchangeContext.makeOrCleanExchangeFolder();
		
		InitializeExchangeResponse response = InitializeExchangeResponse.T.create();
		
		return Maybe.complete(response);

	}
}

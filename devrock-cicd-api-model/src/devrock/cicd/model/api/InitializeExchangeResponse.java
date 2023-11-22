package devrock.cicd.model.api;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

import devrock.step.model.api.StepResponse;

public interface InitializeExchangeResponse extends StepResponse {
	EntityType<InitializeExchangeResponse> T = EntityTypes.T(InitializeExchangeResponse.class);
}

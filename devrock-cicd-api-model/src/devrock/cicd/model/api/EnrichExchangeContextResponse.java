package devrock.cicd.model.api;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

import devrock.step.model.api.StepResponse;

public interface EnrichExchangeContextResponse extends StepResponse {
	EntityType<EnrichExchangeContextResponse> T = EntityTypes.T(EnrichExchangeContextResponse.class);
}

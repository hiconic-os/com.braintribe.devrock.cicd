package devrock.cicd.model.api;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

import devrock.step.model.api.StepResponse;

public interface CheckLinkingResponse extends StepResponse {
	EntityType<CheckLinkingResponse> T = EntityTypes.T(CheckLinkingResponse.class);
}
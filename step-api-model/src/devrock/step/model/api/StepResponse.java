package devrock.step.model.api;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface StepResponse extends GenericEntity {
	EntityType<StepResponse> T = EntityTypes.T(StepResponse.class);
}

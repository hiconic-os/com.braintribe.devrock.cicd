package devrock.step.model.api;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

@Abstract
public interface StepEndpointOptions extends GenericEntity {
	EntityType<StepEndpointOptions> T = EntityTypes.T(StepEndpointOptions.class);
}

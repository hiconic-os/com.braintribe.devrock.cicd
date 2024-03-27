package devrock.ant.model.api;

import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

@Abstract
public interface AntRequest extends ServiceRequest {
	EntityType<AntRequest> T = EntityTypes.T(AntRequest.class);
}

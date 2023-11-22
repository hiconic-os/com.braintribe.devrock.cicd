package devrock.cicd.model.api;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

@Abstract
public interface DistributedLocking extends GenericEntity {
	EntityType<DistributedLocking> T = EntityTypes.T(DistributedLocking.class);
}

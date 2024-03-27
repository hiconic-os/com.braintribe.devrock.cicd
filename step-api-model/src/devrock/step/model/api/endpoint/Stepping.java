package devrock.step.model.api.endpoint;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/**
 * @author dirk.scheffler
 *
 */
public interface Stepping extends GenericEntity {
	EntityType<Stepping> T = EntityTypes.T(Stepping.class);
	
	String externallySequenced = "externallySequenced";
	
	Boolean getExternallySequenced();
	void setExternallySequenced(Boolean externallySequenced);
}

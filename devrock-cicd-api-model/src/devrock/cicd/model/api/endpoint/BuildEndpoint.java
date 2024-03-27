package devrock.cicd.model.api.endpoint;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface BuildEndpoint extends GenericEntity {
	EntityType<BuildEndpoint> T = EntityTypes.T(BuildEndpoint.class);

	String range = "range";
	
	String getRange();
	void setRange(String range);
}

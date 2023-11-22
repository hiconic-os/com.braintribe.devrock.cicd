package devrock.step.model.api.meta;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface ExternalArgument extends ArgumentMapping {
	EntityType<ExternalArgument> T = EntityTypes.T(ExternalArgument.class);
	
	String name = "name";
	
	String getName();
	void setName(String name);
}

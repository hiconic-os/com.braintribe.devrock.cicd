package devrock.cicd.model.api;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface EnvironmentAware extends GenericEntity {
	EntityType<EnvironmentAware> T = EntityTypes.T(EnvironmentAware.class);
	
	String ci = "ci";
	
	boolean getCi();
	void setCi(boolean ci);
}

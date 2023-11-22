package devrock.step.model.api;

import java.util.Map;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/**
 * This special entity carries properties that could be applied on any StepRequest entity
 * @author dirk.scheffler
 *
 */
public interface ExchangeProperties extends GenericEntity {
	EntityType<ExchangeProperties> T = EntityTypes.T(ExchangeProperties.class);
	
	String properties = "properties";
	
	Map<String, Object> getProperties();
	void setProperties(Map<String, Object> properties);
}

package devrock.step.model.api.meta;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.meta.data.MetaData;

public interface ExchangeClassifier extends MetaData {
	EntityType<ExchangeClassifier> T = EntityTypes.T(ExchangeClassifier.class);
	
	String value = "value";
	
	String getValue();
	void setValue(String value);
}

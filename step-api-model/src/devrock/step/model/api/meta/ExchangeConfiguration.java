package devrock.step.model.api.meta;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.meta.data.ExplicitPredicate;

public interface ExchangeConfiguration extends ExplicitPredicate {
	EntityType<ExchangeConfiguration> T = EntityTypes.T(ExchangeConfiguration.class);
}

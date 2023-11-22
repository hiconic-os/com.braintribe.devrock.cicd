package devrock.step.model.api.meta;

import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.meta.data.MetaData;

@Abstract
public interface ArgumentMapping extends MetaData {
	EntityType<ArgumentMapping> T = EntityTypes.T(ArgumentMapping.class);
}

package devrock.steps.sequencer.processing;

import java.util.function.Function;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.Property;

public class EntityPropertyLookup implements Function<String, Object> {
	private GenericEntity entity;
	
	public EntityPropertyLookup(GenericEntity entity) {
		super();
		this.entity = entity;
	}

	@Override
	public Object apply(String propertyName) {
		
		Property property = entity.entityType().findProperty(propertyName);
		
		if (property == null)
			return null;
			
		return property.get(entity);
	}
}

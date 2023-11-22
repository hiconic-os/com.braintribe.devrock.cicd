package devrock.cicd.github.notification.model.api;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface RequestLabelMapping extends GenericEntity {
	EntityType<RequestLabelMapping> T = EntityTypes.T(RequestLabelMapping.class);

	String request = "request";
	String label = "label";
	String color = "color";
	String description = "description";
	
	String getRequest();
	void setRequest(String request);

	String getLabel();
	void setLabel(String label);
	
	String getDescription();
	void setDescription(String description);

	String getColor();
	void setColor(String color);
}

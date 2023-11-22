package devrock.cicd.steps.test.model;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface Node extends GenericEntity {
	EntityType<Node> T = EntityTypes.T(Node.class);
	
	Node getOther();
	void setOther(Node node);
}

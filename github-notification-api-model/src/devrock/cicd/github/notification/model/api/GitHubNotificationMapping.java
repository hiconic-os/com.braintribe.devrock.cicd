package devrock.cicd.github.notification.model.api;

import java.util.List;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface GitHubNotificationMapping extends GenericEntity {
	EntityType<GitHubNotificationMapping> T = EntityTypes.T(GitHubNotificationMapping.class);

	String requestLabelMappings = "requestLabelMappings";
	String ensureLabels = "ensureLabels";
	
	List<RequestLabelMapping> getRequestLabelMappings();
	void setRequestLabelMappings(List<RequestLabelMapping> requestLabelMappings);
	
	boolean getEnsureLabels();
	void setEnsureLabels(boolean ensureLabels);
}

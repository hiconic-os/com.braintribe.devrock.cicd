package devrock.cicd.github.notification.model.api.reason;

import com.braintribe.gm.model.reason.essential.ConfigurationError;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface GitHubNotificationConfigurationError extends ConfigurationError {
	EntityType<GitHubNotificationConfigurationError> T = EntityTypes.T(GitHubNotificationConfigurationError.class);
}

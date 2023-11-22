package devrock.cicd.github.notification.model.api.reason;

import com.braintribe.gm.model.reason.essential.ConfigurationError;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface GitHubLabelAttachmentFailed extends ConfigurationError {
	EntityType<GitHubLabelAttachmentFailed> T = EntityTypes.T(GitHubLabelAttachmentFailed.class);
}

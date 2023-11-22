package devrock.cicd.github.notification.model.api;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.meta.Confidential;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface GitHubNotificationConnection extends GenericEntity {
	EntityType<GitHubNotificationConnection> T = EntityTypes.T(GitHubNotificationConnection.class);

	String gitHubToken = "gitHubToken";
	String organization = "organization";
	String repository = "repository";
	String issue = "issue";
	
	@Confidential
	String getGitHubToken();
	void setGitHubToken(String gitHubToken);

	String getOrganization();
	void setOrganization(String organization);

	String getRepository();
	void setRepository(String repository);

	String getIssue();
	void setIssue(String issue);
}

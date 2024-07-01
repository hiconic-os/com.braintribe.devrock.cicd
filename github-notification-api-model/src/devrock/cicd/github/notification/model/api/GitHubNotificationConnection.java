// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
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

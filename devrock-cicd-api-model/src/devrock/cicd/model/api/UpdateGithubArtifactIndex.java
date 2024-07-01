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
package devrock.cicd.model.api;

import com.braintribe.model.generic.annotation.meta.Confidential;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

import devrock.step.model.api.StepRequest;

public interface UpdateGithubArtifactIndex extends StepRequest {
	EntityType<UpdateGithubArtifactIndex> T = EntityTypes.T(UpdateGithubArtifactIndex.class);

	String token = "token";
	String organization = "organization";
	String repository = "repository";
	String group = "group";
	
	@Confidential
	@Mandatory
	@Description("The github authentication token")
	String getToken();
	void setToken(String token);
	
	@Mandatory
	@Description("The github organization")
	String getOrganization();
	void setOrganization(String organization);
	
	@Mandatory
	@Description("The github repository associated with the maven repo")
	String getRepository();
	void setRepository(String repository);
	
	@Description("Filters to update just artifacts from a certain groupId")
	String getGroup();
	void setGroup(String group);
	
	@Override
	EvalContext<? extends UpdateGithubArtifactIndexResponse> eval(Evaluator<ServiceRequest> evaluator);
}

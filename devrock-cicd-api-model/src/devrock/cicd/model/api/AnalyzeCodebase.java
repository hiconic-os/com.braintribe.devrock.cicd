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

import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.annotation.meta.Alias;
import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

import devrock.step.model.api.StepRequest;

@Alias("analyze")
public interface AnalyzeCodebase extends StepRequest, EnvironmentAware {

	EntityType<AnalyzeCodebase> T = EntityTypes.T(AnalyzeCodebase.class);

	String buildArtifacts = "buildArtifacts";
	String path = "path";
	String baseBranch = "baseBranch";
	String baseHash = "baseHash";
	String baseRemote = "baseRemote";
	String detectUnpublishedArtifacts = "detectUnpublishedArtifacts";
	String allowReleaseViewBuilding = "allowReleaseViewBuilding";

	String getBaseBranch();
	void setBaseBranch(String baseBranch);

	String getBaseHash();
	void setBaseHash(String baseHash);

	@Initializer("'origin'")
	String getBaseRemote();
	void setBaseRemote(String baseRemote);

	@Mandatory
	String getPath();
	void setPath(String path);

	String getBuildArtifacts();
	void setBuildArtifacts(String buildArtifacts);

	boolean getDetectUnpublishedArtifacts();
	void setDetectUnpublishedArtifacts(boolean detectUnpublishedArtifacts);

	boolean getAllowReleaseViewBuilding();
	void setAllowReleaseViewBuilding(boolean allowReleaseViewBuilding);
	
	@Override
	EvalContext<? extends AnalyzeCodebaseResponse> eval(Evaluator<ServiceRequest> evaluator);

}

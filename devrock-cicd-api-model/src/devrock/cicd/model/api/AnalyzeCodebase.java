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

import java.util.Set;

import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.annotation.meta.Alias;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.annotation.meta.FolderName;
import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

import devrock.step.model.api.StepRequest;
import devrock.step.model.api.meta.ExternalArgument;

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
	String artifacts = "artifacts";

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

	/** Note that in the 'hc-build app' there is an {@link ExternalArgument} mapping that maps {@link Build#getRange()} to this. */
	@Description("Describes which artifacts to build. Examples: \n" + //
			"xyz - artifact xyz and all its dependencies\n" + //
			"[xyz] - only artifact xyz\n" + //
			"[xyz]+[abc] - only artifacts xyz and abc\n" + //
			". - all artifacts\n")
	@Alias("r")
	String getBuildArtifacts();
	void setBuildArtifacts(String buildArtifacts);

	@Description("Alternative to 'range' to specify exact artifacts to build, better suited for CLI completions.\n" + //
			"Passing '--artifacts xyz/ abc/' is equivalent to '--range [xyz]+[abc]'.")
	@Alias("a")
	@FolderName
	Set<String> getArtifacts();
	void setArtifacts(Set<String> artifacts);

	boolean getDetectUnpublishedArtifacts();
	void setDetectUnpublishedArtifacts(boolean detectUnpublishedArtifacts);

	boolean getAllowReleaseViewBuilding();
	void setAllowReleaseViewBuilding(boolean allowReleaseViewBuilding);

	@Override
	EvalContext<? extends AnalyzeCodebaseResponse> eval(Evaluator<ServiceRequest> evaluator);

}

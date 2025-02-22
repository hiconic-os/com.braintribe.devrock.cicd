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

import com.braintribe.model.generic.annotation.meta.Alias;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.annotation.meta.FolderName;
import com.braintribe.model.generic.annotation.meta.PositionalArguments;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

import devrock.step.model.api.RunStep;
import devrock.step.model.api.StepEndpointOptions;

@PositionalArguments({ "range", "step" })
public interface Build extends RunStep, StepEndpointOptions {

	EntityType<Build> T = EntityTypes.T(Build.class);

	String range = "range";
	String skip = "skip";
	String artifacts = "artifacts";

	/** See also {@link AnalyzeCodebase#getBuildArtifacts()} */
	@Description("Describes which artifacts to build. Examples: \n" + //
			"xyz - artifact xyz and all its dependencies\n" + //
			"[xyz] - only artifact xyz\n" + //
			"[xyz]+[abc] - only artifacts xyz and abc\n" + //
			". - all artifacts\n")
	@Alias("r")
	String getRange();
	void setRange(String range);

	@Description("Alternative to 'range' to specify exact artifacts to build, better suited for CLI completions.\n" + //
			"Passing '--artifacts xyz/ abc/' is equivalent to '--range [xyz]+[abc]'.")
	@Alias("a")
	@FolderName
	Set<String> getArtifacts();
	void setArtifacts(Set<String> artifacts);

	@Alias("s")
	boolean getSkip();
	void setSkip(boolean skip);
}

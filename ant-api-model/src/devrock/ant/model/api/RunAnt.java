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
package devrock.ant.model.api;

import java.util.Map;

import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.annotation.meta.Alias;
import com.braintribe.model.generic.annotation.meta.FileName;
import com.braintribe.model.generic.annotation.meta.FolderName;
import com.braintribe.model.generic.annotation.meta.PositionalArguments;
import com.braintribe.model.generic.annotation.meta.UnsatisfiedBy;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.service.api.result.Neutral;

import devrock.ant.model.reason.AntBuildFailed;

@UnsatisfiedBy(AntBuildFailed.class)
@Alias("ant")
@PositionalArguments("target")
public interface RunAnt extends AntRequest {

	EntityType<RunAnt> T = EntityTypes.T(RunAnt.class);

	String projectDir = "projectDir";
	String target = "target";
	String properties = "properties";
	String bufferOutput = "bufferOutput";
	String ownerInfo = "ownerInfo";

	@Alias("p")
	@FolderName
	@Initializer("'.'")
	String getProjectDir();
	void setProjectDir(String projectDir);

	@Alias("t")
	String getTarget();
	void setTarget(String target);

	@Alias("f")
	@FileName
	@Initializer("'build.xml'")
	String getBuildFile();
	void setBuildFile(String buildFile);

	String getOwnerInfo();
	void setOwnerInfo(String ownerInfo);

	Map<String, String> getProperties();
	void setProperties(Map<String, String> properties);

	boolean getBufferOutput();
	void setBufferOutput(boolean bufferOutput);

	@Override
	EvalContext<Neutral> eval(Evaluator<ServiceRequest> evaluator);

}

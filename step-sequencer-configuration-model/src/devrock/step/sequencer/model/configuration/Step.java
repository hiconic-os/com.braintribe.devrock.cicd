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
// ============================================================================
package devrock.step.sequencer.model.configuration;

import java.util.List;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

import devrock.step.model.api.StepRequest;

public interface Step extends GenericEntity {

	EntityType<Step> T = EntityTypes.T(Step.class);

	String request = "request";
	String optional = "optional";
	String requires = "requires";
	
	StepRequest getRequest();
	void setRequest(StepRequest request);
	
	boolean getOptional();
	void setOptional(boolean optional);
	
	List<Step> getRequires();
	void setRequires(List<Step> requires);
}

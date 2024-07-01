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
package devrock.step.model.api;

import com.braintribe.model.generic.annotation.meta.Alias;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

@Description("Runs the step sequence up to the default step as configured, typically in StepConfiguration or build.gradle")
public interface RunStep extends ServiceRequest {
	EntityType<RunStep> T = EntityTypes.T(RunStep.class);
	
	@Alias("s")
	@Description("The step to be executed or null if the default step should be executed")
	String getStep();
	void setStep(String step);
	
	@Override
	EvalContext<? extends StepResponse> eval(Evaluator<ServiceRequest> evaluator);
}

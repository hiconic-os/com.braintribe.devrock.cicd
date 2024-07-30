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
package devrock.steps.sequencer.processing;

import com.braintribe.cfg.Required;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.service.api.ReasonedServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.utils.StringTools;

import devrock.step.model.api.RunStep;
import devrock.step.model.api.StepRequest;
import devrock.step.sequencer.model.configuration.Step;
import devrock.step.sequencer.model.configuration.StepConfiguration;

public class RunStepProcessor implements ReasonedServiceProcessor<RunStep, Object> {
	private StepConfiguration configuration;
	
	@Required
	public void setConfiguration(StepConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public Maybe<?> processReasoned(ServiceRequestContext context, RunStep request) {
		
		Maybe<Step> stepMaybe = resolveStep(request.getStep());
		
		if (stepMaybe.isUnsatisfied())
			return stepMaybe.emptyCast();
		
		Step step = stepMaybe.get();
		
		if (step == null)
			step = configuration.getDefaultStep();
		
		if (step == null)
			return Reasons.build(NotFound.T).text("No default step found in step-configuration").toMaybe();
		
		StepRequest stepRequest = step.getRequest();
		
		return stepRequest.eval(context).getReasoned();
	}

	private Maybe<Step> resolveStep(String stepName) {
		if (stepName == null)
			return Maybe.complete(null);
		
		for (Step step : configuration.getSteps()) {
			StepRequest request = step.getRequest();
			EntityType<GenericEntity> type = request.entityType();
			
			String candidates[] = {
					StringTools.camelCaseToDashSeparated(type.getShortName()),
					type.getShortName(),
					type.getTypeSignature()
			};
			
			for (String candidate: candidates) {
				if (stepName.equals(candidate))
					return Maybe.complete(step);
			}
		}
		
		return Reasons.build(InvalidArgument.T).text("Unknown step: " + stepName).toMaybe();
	}
}

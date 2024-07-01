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
package devrock.step.api;

import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.ReasonException;
import com.braintribe.model.generic.reflection.EntityType;

import devrock.step.model.api.StepRequest;

public interface StepEvaluator extends StepExchangeContext {

	Reason evaluate(StepRequest request);
	Reason evaluate(EntityType<? extends StepRequest> stepType);

	StepRequest getCurrentRequest();

	default void evaluateOrThrow(EntityType<? extends StepRequest> stepType) {
		Reason reason = evaluate(stepType);
		if (reason != null)
			throw new ReasonException(reason);
	}

	default void evaluateOrThrow(StepRequest stepRequest) {
		Reason reason = evaluate(stepRequest);
		if (reason != null)
			throw new ReasonException(reason);
	}
}

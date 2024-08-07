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
package devrock.step.framework;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.model.processing.service.api.ProceedContext;
import com.braintribe.model.processing.service.api.ReasonedServiceAroundProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;

import devrock.step.api.StepExchangeContext;
import devrock.step.api.StepExchangeContextAttribute;
import devrock.step.model.api.StepRequest;
import devrock.step.model.api.StepResponse;

public class StepAroundProcessor implements ReasonedServiceAroundProcessor<StepRequest, Object> {

	@Override
	public Maybe<?> processReasoned(ServiceRequestContext context, StepRequest request, ProceedContext proceedContext) {
		StepExchangeContext exchangeContext = context.getAttribute(StepExchangeContextAttribute.class);

		Reason error = exchangeContext.loadProperties(request);
		if (error != null)
			return error.asMaybe();

		Maybe<?> maybe = proceedContext.proceedReasoned(request);

		if (maybe.isSatisfied())
			if (maybe.get() instanceof StepResponse stepResponse)
				exchangeContext.storeProperties(stepResponse);

		return maybe;
	}
}

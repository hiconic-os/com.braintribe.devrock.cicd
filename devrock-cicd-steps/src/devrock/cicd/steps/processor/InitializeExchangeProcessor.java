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
package devrock.cicd.steps.processor;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.processing.service.api.ReasonedServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;

import devrock.cicd.model.api.InitializeExchange;
import devrock.cicd.model.api.InitializeExchangeResponse;
import devrock.step.api.StepExchangeContext;
import devrock.step.api.StepExchangeContextAttribute;

public class InitializeExchangeProcessor implements ReasonedServiceProcessor<InitializeExchange, InitializeExchangeResponse> {

	@Override
	public Maybe<? extends InitializeExchangeResponse> processReasoned(ServiceRequestContext context,
			InitializeExchange request) {
		StepExchangeContext exchangeContext = context.getAttribute(StepExchangeContextAttribute.class);
		exchangeContext.makeOrCleanExchangeFolder();
		
		InitializeExchangeResponse response = InitializeExchangeResponse.T.create();
		
		return Maybe.complete(response);

	}
}

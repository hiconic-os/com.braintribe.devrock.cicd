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
import com.braintribe.model.service.api.ServiceRequest;

public abstract class SpawningServiceProcessor<P extends ServiceRequest, R> implements ReasonedServiceProcessor<P, R> {
	
	protected abstract StatefulServiceProcessor spawn();
	
	@Override
	public Maybe<? extends R> processReasoned(ServiceRequestContext context, P request) {
		return spawn().process(context, request);
	}
	
	protected abstract class StatefulServiceProcessor {
		protected ServiceRequestContext context;
		protected P request;
		
		private Maybe<? extends R> process(ServiceRequestContext context, P request) {
			this.context = context;
			this.request = request;
			return process();
		}
		
		protected abstract Maybe<? extends R> process();
	}

}

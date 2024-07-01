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
package devrock.cicd.steps.processor.test;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.processing.service.api.ReasonedServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;

import devrock.cicd.model.api.test.Test1Request;
import devrock.cicd.model.api.test.TestData;
import devrock.cicd.model.api.test.TestResponse;

public class Test1Processor implements ReasonedServiceProcessor<Test1Request, TestResponse> {

	@Override
	public Maybe<? extends TestResponse> processReasoned(ServiceRequestContext context, Test1Request request) {
		TestData testData = TestData.T.create();
		testData.setValue1("value1");
		
		TestResponse response = TestResponse.T.create();
		response.setTestData(testData);
		
		return Maybe.complete(response);
	}

}

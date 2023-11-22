package devrock.cicd.steps.processor.test;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.processing.service.api.ReasonedServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;

import devrock.cicd.model.api.test.Test2Request;
import devrock.cicd.model.api.test.TestData;
import devrock.cicd.model.api.test.TestResponse;

public class Test2Processor implements ReasonedServiceProcessor<Test2Request, TestResponse> {

	@Override
	public Maybe<? extends TestResponse> processReasoned(ServiceRequestContext context, Test2Request request) {
		TestData testData = request.getTestData();
		testData.setValue2("value2");
		
		TestResponse response = TestResponse.T.create();
		response.setTestData(testData);
		
		return Maybe.complete(response);
	}

}

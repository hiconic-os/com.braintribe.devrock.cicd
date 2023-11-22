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

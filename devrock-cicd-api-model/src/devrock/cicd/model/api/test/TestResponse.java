package devrock.cicd.model.api.test;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

import devrock.step.model.api.StepResponse;

public interface TestResponse extends StepResponse {
	EntityType<TestResponse> T = EntityTypes.T(TestResponse.class);

	String testData = "testData";
	
	TestData getTestData();
	void setTestData(TestData testData);
}

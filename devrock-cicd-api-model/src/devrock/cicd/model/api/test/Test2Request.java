package devrock.cicd.model.api.test;

import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

import devrock.step.model.api.StepRequest;

public interface Test2Request extends StepRequest {
	EntityType<Test2Request> T = EntityTypes.T(Test2Request.class);
	
	String testData = "testData";
	
	TestData getTestData();
	void setTestData(TestData testData);

	@Override
	EvalContext<? extends TestResponse> eval(Evaluator<ServiceRequest> evaluator);
}

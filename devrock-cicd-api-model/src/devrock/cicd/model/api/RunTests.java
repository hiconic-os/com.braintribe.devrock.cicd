package devrock.cicd.model.api;

import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

import devrock.cicd.model.api.data.CodebaseAnalysis;
import devrock.step.model.api.StepRequest;

public interface RunTests extends StepRequest, HasArtifactHandler {
	EntityType<RunTests> T = EntityTypes.T(RunTests.class);
	
	String codebaseAnalysis = "codebaseAnalysis";
	
	@Mandatory
	CodebaseAnalysis getCodebaseAnalysis();
	void setCodebaseAnalysis(CodebaseAnalysis codebaseAnalysis);
	
	@Override
	EvalContext<? extends RunTestsResponse> eval(Evaluator<ServiceRequest> evaluator);
}

package devrock.cicd.model.api;

import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

import devrock.cicd.model.api.data.CodebaseAnalysis;
import devrock.cicd.model.api.data.CodebaseDependencyAnalysis;
import devrock.step.model.api.StepRequest;

public interface PreparePublishing extends StepRequest {
	EntityType<PreparePublishing> T = EntityTypes.T(PreparePublishing.class);
	
	String codebaseAnalysis = "codebaseAnalysis";
	String codebaseDependencyAnalysis = "codebaseDependencyAnalysis";
	
	@Mandatory
	CodebaseAnalysis getCodebaseAnalysis();
	void setCodebaseAnalysis(CodebaseAnalysis codebaseAnalysis);
	
	@Mandatory
	CodebaseDependencyAnalysis getCodebaseDependencyAnalysis();
	void setCodebaseDependencyAnalysis(CodebaseDependencyAnalysis codebaseDependencyAnalysis);
	
	@Override
	EvalContext<? extends PreparePublishingResponse> eval(Evaluator<ServiceRequest> evaluator);
}

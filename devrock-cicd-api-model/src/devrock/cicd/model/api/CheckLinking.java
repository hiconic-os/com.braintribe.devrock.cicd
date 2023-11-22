package devrock.cicd.model.api;

import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

import devrock.cicd.model.api.data.CodebaseAnalysis;
import devrock.cicd.model.api.data.CodebaseDependencyAnalysis;

public interface CheckLinking extends MultiThreadedStepRequest, HasArtifactHandler {
	EntityType<CheckLinking> T = EntityTypes.T(CheckLinking.class);
	
	String codebaseAnalysis = "codebaseAnalysis";
	String codebaseDependencyAnalysis = "codebaseDependencyAnalysis";
	
	@Mandatory
	CodebaseAnalysis getCodebaseAnalysis();
	void setCodebaseAnalysis(CodebaseAnalysis codebaseAnalysis);
	
	@Mandatory
	CodebaseDependencyAnalysis getCodebaseDependencyAnalysis();
	void setCodebaseDependencyAnalysis(CodebaseDependencyAnalysis codebaseDependencyAnalysis);
	
	@Override
	EvalContext<? extends CheckLinkingResponse> eval(Evaluator<ServiceRequest> evaluator);
}

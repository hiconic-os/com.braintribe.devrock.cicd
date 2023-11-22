package devrock.cicd.model.api;

import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

import devrock.cicd.model.api.data.CodebaseAnalysis;
import devrock.cicd.model.api.data.CodebaseDependencyAnalysis;
import devrock.cicd.model.api.data.GitContext;
import devrock.step.model.api.StepRequest;

public interface RaiseAndMergeArtifacts extends StepRequest {
	EntityType<RaiseAndMergeArtifacts> T = EntityTypes.T(RaiseAndMergeArtifacts.class);
	
	String codebaseAnalysis = "codebaseAnalysis";
	String codebaseDependencyAnalysis = "codebaseDependencyAnalysis";
	String gitContext = "gitContext";
	
	@Mandatory 
	GitContext getGitContext();
	void setGitContext(GitContext gitContext);
	
	@Mandatory
	CodebaseAnalysis getCodebaseAnalysis();
	void setCodebaseAnalysis(CodebaseAnalysis codebaseAnalysis);
	
	@Mandatory
	CodebaseDependencyAnalysis getCodebaseDependencyAnalysis();
	void setCodebaseDependencyAnalysis(CodebaseDependencyAnalysis codebaseDependencyAnalysis);
	
	@Override
	EvalContext<? extends RaiseAndMergeArtifactsResponse> eval(Evaluator<ServiceRequest> evaluator);
}

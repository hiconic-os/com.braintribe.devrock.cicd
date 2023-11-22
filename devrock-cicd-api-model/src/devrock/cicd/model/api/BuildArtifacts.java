package devrock.cicd.model.api;

import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

import devrock.cicd.model.api.data.CodebaseAnalysis;
import devrock.cicd.model.api.data.CodebaseDependencyAnalysis;

public interface BuildArtifacts extends MultiThreadedStepRequest, HasArtifactHandler {
	EntityType<BuildArtifacts> T = EntityTypes.T(BuildArtifacts.class);
	
	String codebaseAnalysis = "codebaseAnalysis";
	String codebaseDependencyAnalysis = "codebaseDependencyAnalysis";
	String candidateInstall = "candidateInstall";
	String skip = "skip";
	
	Boolean getCandidateInstall();
	void setCandidateInstall(Boolean candidateInstall);
	
	@Mandatory
	CodebaseAnalysis getCodebaseAnalysis();
	void setCodebaseAnalysis(CodebaseAnalysis codebaseAnalysis);
	
	@Mandatory
	CodebaseDependencyAnalysis getCodebaseDependencyAnalysis();
	void setCodebaseDependencyAnalysis(CodebaseDependencyAnalysis codebaseDependencyAnalysis);

	/** If previous build attempt failed and this property is true, artifacts built successfully during previous run won't be built again. */
	boolean getSkip();
	void setSkip(boolean skip);
	
	
	@Override
	EvalContext<? extends BuildArtifactsResponse> eval(Evaluator<ServiceRequest> evaluator);
}

package devrock.cicd.model.api;

import com.braintribe.model.artifact.analysis.AnalysisArtifactResolution;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

import devrock.cicd.model.api.data.CodebaseAnalysis;
import devrock.cicd.model.api.data.CodebaseDependencyAnalysis;
import devrock.cicd.model.api.data.GitContext;
import devrock.step.model.api.StepResponse;

public interface AnalyzeCodebaseResponse extends StepResponse {
	EntityType<AnalyzeCodebaseResponse> T = EntityTypes.T(AnalyzeCodebaseResponse.class);

	String analysis = "analysis";
	String dependencyAnalysis = "dependencyAnalysis";
	String dependencyResolution = "dependencyResolution";
	String gitContext = "gitContext";
	
	CodebaseAnalysis getAnalysis();
	void setAnalysis(CodebaseAnalysis analysis);
	
	CodebaseDependencyAnalysis getDependencyAnalysis();
	void setDependencyAnalysis(CodebaseDependencyAnalysis dependencyAnalysis);
	
	AnalysisArtifactResolution getDependencyResolution();
	void setDependencyResolution(AnalysisArtifactResolution dependencyResolution);
	
	GitContext getGitContext();
	void setGitContext(GitContext gitContext);
}

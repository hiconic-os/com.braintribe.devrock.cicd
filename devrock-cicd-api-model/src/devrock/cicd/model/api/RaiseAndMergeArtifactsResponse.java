package devrock.cicd.model.api;

import com.braintribe.model.artifact.analysis.AnalysisArtifactResolution;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

import devrock.cicd.model.api.data.CodebaseAnalysis;
import devrock.cicd.model.api.data.CodebaseDependencyAnalysis;
import devrock.step.model.api.StepResponse;

public interface RaiseAndMergeArtifactsResponse extends StepResponse {
	EntityType<RaiseAndMergeArtifactsResponse> T = EntityTypes.T(RaiseAndMergeArtifactsResponse.class);
	
	CodebaseAnalysis getAnalysis();
	void setAnalysis(CodebaseAnalysis analysis);
	
	CodebaseDependencyAnalysis getDependencyAnalysis();
	void setDependencyAnalysis(CodebaseDependencyAnalysis dependencyAnalysis);
	
	AnalysisArtifactResolution getDependencyResolution();
	void setDependencyResolution(AnalysisArtifactResolution dependencyResolution);
}
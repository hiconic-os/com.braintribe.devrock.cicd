package devrock.cicd.model.api;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

import devrock.cicd.model.api.data.BuildResult;
import devrock.step.model.api.StepResponse;

public interface BuildArtifactsResponse extends StepResponse {
	EntityType<BuildArtifactsResponse> T = EntityTypes.T(BuildArtifactsResponse.class);
	
	String result = "result";
	
	BuildResult getResult();
	void setResult(BuildResult result);
}
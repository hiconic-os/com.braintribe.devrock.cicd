package devrock.cicd.model.api;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

import devrock.step.model.api.StepResponse;

public interface UpdateGithubArtifactIndexResponse extends StepResponse {
	EntityType<UpdateGithubArtifactIndexResponse> T = EntityTypes.T(UpdateGithubArtifactIndexResponse.class);
}
package devrock.cicd.model.api;

import com.braintribe.model.generic.annotation.meta.Alias;
import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

import devrock.cicd.model.api.data.BuildResult;
import devrock.step.model.api.StepRequest;

@Alias("publish")
public interface PublishArtifacts extends StepRequest {
	EntityType<PublishArtifacts> T = EntityTypes.T(PublishArtifacts.class);
	
	String buildResult = "buildResult";
	
	@Mandatory
	BuildResult getBuildResult();
	void setBuildResult(BuildResult buildResult);
	
	@Override
	EvalContext<? extends PublishArtifactsResponse> eval(Evaluator<ServiceRequest> evaluator);
}

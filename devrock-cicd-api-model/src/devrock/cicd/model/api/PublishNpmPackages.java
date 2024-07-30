package devrock.cicd.model.api;

import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

import devrock.cicd.model.api.data.CodebaseAnalysis;
import devrock.step.model.api.StepRequest;

public interface PublishNpmPackages extends StepRequest {

	EntityType<PublishNpmPackages> T = EntityTypes.T(PublishNpmPackages.class);

	@Mandatory
	CodebaseAnalysis getCodebaseAnalysis();
	void setCodebaseAnalysis(CodebaseAnalysis codebaseAnalysis);

	@Override
	EvalContext<? extends PublishArtifactsResponse> eval(Evaluator<ServiceRequest> evaluator);

}

package devrock.cicd.model.api;

import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

import devrock.step.model.api.StepRequest;

public interface EnrichExchangeContext extends StepRequest, EnvironmentAware {
	EntityType<EnrichExchangeContext> T = EntityTypes.T(EnrichExchangeContext.class);
	
	String gitPath = "gitPath";
	String commentInput = "commentInput";
	
	@Mandatory
	String getGitPath();
	void setGitPath(String gitPath);
	
	@Description("Expects multiline text which could contain sections that hold entities and properties for the exchange context")
	String getCommentInput();
	void setCommentInput(String commentInput);
	
	@Override
	EvalContext<? extends AnalyzeCodebaseResponse> eval(Evaluator<ServiceRequest> evaluator);
}

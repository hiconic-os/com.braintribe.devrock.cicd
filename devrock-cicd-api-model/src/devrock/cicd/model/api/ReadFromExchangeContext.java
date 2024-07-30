package devrock.cicd.model.api;

import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

import devrock.step.model.api.StepRequest;

@Description("Reands and entity or its property from the exchange context.")
public interface ReadFromExchangeContext extends StepRequest, EnvironmentAware {

	EntityType<ReadFromExchangeContext> T = EntityTypes.T(ReadFromExchangeContext.class);

	String gitPath = "gitPath";
	String commentInput = "commentInput";

	@Mandatory
	@Description("Type signature of the type to read.")
	String getType();
	void setType(String typpe);

	@Description("Name of the instance's property to read. If not specified, the entire instance is returned.")
	String getProperty();
	void setProperty(String property);

	@Override
	EvalContext<?> eval(Evaluator<ServiceRequest> evaluator);
}

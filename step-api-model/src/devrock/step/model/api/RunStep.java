package devrock.step.model.api;

import com.braintribe.model.generic.annotation.meta.Alias;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

@Description("Runs the step sequence up to the default step as configured, typically in StepConfiguration or build.gradle")
public interface RunStep extends ServiceRequest {
	EntityType<RunStep> T = EntityTypes.T(RunStep.class);
	
	@Alias("s")
	@Description("The step to be executed or null if the default step should be executed")
	String getStep();
	void setStep(String step);
	
	@Override
	EvalContext<? extends StepResponse> eval(Evaluator<ServiceRequest> evaluator);
}

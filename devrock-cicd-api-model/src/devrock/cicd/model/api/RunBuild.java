package devrock.cicd.model.api;

import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.service.api.result.Neutral;

import devrock.cicd.model.api.data.LocalArtifact;
import devrock.step.model.api.StepRequest;

@Abstract
public interface RunBuild extends ServiceRequest {
	EntityType<RunBuild> T = EntityTypes.T(RunBuild.class);
	
	String artifact = "artifact";
	String caller = "caller";
	
	LocalArtifact getArtifact();
	void setArtifact(LocalArtifact localArtifact);
	
	StepRequest getCaller();
	void setCaller(StepRequest caller);
	
	@Override
	EvalContext<Neutral> eval(Evaluator<ServiceRequest> evaluator);
}

package devrock.cicd.model.api;

import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

import devrock.step.model.api.StepRequest;

public interface AnalyzeCodebase extends StepRequest, EnvironmentAware {
	EntityType<AnalyzeCodebase> T = EntityTypes.T(AnalyzeCodebase.class);
	
	String buildArtifacts = "buildArtifacts";
	String path = "path";
	String baseBranch = "baseBranch";
	String baseHash = "baseHash";
	String baseRemote = "baseRemote";
	String detectUnpublishedArtifacts = "detectUnpublishedArtifacts";
	
	String getBaseBranch();
	void setBaseBranch(String baseBranch);
	
	String getBaseHash();
	void setBaseHash(String baseHash);
	
	@Initializer("'origin'")
	String getBaseRemote();
	void setBaseRemote(String baseRemote);
	
	@Mandatory
	String getPath();
	void setPath(String path);
	
	String getBuildArtifacts();
	void setBuildArtifacts(String buildArtifacts);
	
	boolean getDetectUnpublishedArtifacts();
	void setDetectUnpublishedArtifacts(boolean detectUnpublishedArtifacts);
	
	@Override
	EvalContext<? extends AnalyzeCodebaseResponse> eval(Evaluator<ServiceRequest> evaluator);
}

package devrock.ant.model.api;

import java.util.Map;

import com.braintribe.model.generic.annotation.meta.UnsatisfiedBy;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.service.api.result.Neutral;

import devrock.ant.model.reason.AntBuildFailed;

@UnsatisfiedBy(AntBuildFailed.class)
public interface RunAnt extends AntRequest {
	EntityType<RunAnt> T = EntityTypes.T(RunAnt.class);
	
	String projectDir = "projectDir";
	String target = "target";
	String properties = "properties";
	String bufferOutput = "bufferOutput";
	String ownerInfo = "ownerInfo";
	
	String getProjectDir();
	void setProjectDir(String projectDir);
	
	String getTarget();
	void setTarget(String target);
	
	String getOwnerInfo();
	void setOwnerInfo(String ownerInfo);
	
	Map<String, String> getProperties();
	void setProperties(Map<String, String> properties);
	
	boolean getBufferOutput();
	void setBufferOutput(boolean bufferOutput);
	
	@Override
	EvalContext<Neutral> eval(Evaluator<ServiceRequest> evaluator);
}

// ============================================================================
package devrock.step.sequencer.model.configuration;

import java.util.List;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

import devrock.step.model.api.StepRequest;

public interface Step extends GenericEntity {

	EntityType<Step> T = EntityTypes.T(Step.class);

	String request = "request";
	String optional = "optional";
	String requires = "requires";
	
	StepRequest getRequest();
	void setRequest(StepRequest request);
	
	boolean getOptional();
	void setOptional(boolean optional);
	
	List<Step> getRequires();
	void setRequires(List<Step> requires);
}

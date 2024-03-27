// ============================================================================
package devrock.step.sequencer.model.configuration;

import java.util.List;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface StepConfiguration extends GenericEntity {

	EntityType<StepConfiguration> T = EntityTypes.T(StepConfiguration.class);

	String steps = "steps";
	String defaultStep = "defaultStep";
	
	Step getDefaultStep();
	void setDefaultStep(Step defaultStep);
	
	List<Step> getSteps();
	void setSteps(List<Step> databases);
}

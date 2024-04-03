package devrock.cicd.model.api;

import com.braintribe.model.generic.annotation.meta.Alias;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

import devrock.step.model.api.StepEndpointOptions;

@Alias("bo")
public interface BuildOptions extends StepEndpointOptions {

	EntityType<BuildOptions> T = EntityTypes.T(BuildOptions.class);

	String range = "range";
	
	@Alias("r")
	String getRange();
	void setRange(String range);
}

package devrock.cicd.model.api;

import com.braintribe.model.generic.annotation.meta.Alias;
import com.braintribe.model.generic.annotation.meta.PositionalArguments;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

import devrock.step.model.api.RunStep;
import devrock.step.model.api.StepEndpointOptions;

@PositionalArguments({"range", "step"})
public interface Build extends RunStep, StepEndpointOptions {
	EntityType<Build> T = EntityTypes.T(Build.class);

	String range = "range";
	String skip = "skip";
	
	@Alias("r")
	String getRange();
	void setRange(String range);
	
	@Alias("s")
	boolean getSkip();
	void setSkip(boolean skip);
 }

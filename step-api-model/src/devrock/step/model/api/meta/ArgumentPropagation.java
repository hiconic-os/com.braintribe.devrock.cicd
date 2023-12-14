package devrock.step.model.api.meta;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.meta.data.MetaData;

import devrock.step.model.api.StepRequest;

/**
 * If configured on a property of a {@link StepRequest}, the property is meant to be propagated (as an argument) further downstream to lower-level
 * processors.
 * <p>
 * An example is a build for the entire group that propagates certain properties to individual artifact builds.
 */
public interface ArgumentPropagation extends MetaData {

	EntityType<ArgumentPropagation> T = EntityTypes.T(ArgumentPropagation.class);

	String name = "name";

	/**
	 * Name of the property in the downstream processor.
	 * <p>
	 * If <tt>null</tt> the name of the property annotated with this MD is used.
	 */
	String getName();
	void setName(String name);

}

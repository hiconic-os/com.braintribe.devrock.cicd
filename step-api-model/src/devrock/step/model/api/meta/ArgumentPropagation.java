// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
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

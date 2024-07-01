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
package devrock.step.api;

import java.util.function.Supplier;

import com.braintribe.common.attribute.TypeSafeAttribute;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.processing.meta.cmd.CmdResolver;

/**
 * A StepContext allows to acquire 
 */
public interface StepExchangeContext {

	void makeOrCleanExchangeFolder();
	
	<V> Maybe<V> getProperty(GenericModelType type, String name);
	
	<E extends GenericEntity> Maybe<E> load(EntityType<E> type);
	
	<E extends GenericEntity> void store(EntityType<E> type, E data);
	
	<E extends GenericEntity> Maybe<E> load(EntityType<E> type, String classifier);
	
	<E extends GenericEntity> void store(EntityType<E> type, String classifier, E data);
	
	default <E extends GenericEntity> void store(E data) {
		store(data.entityType(), data);
	}
	
	Reason loadProperties(GenericEntity data);
	
	void storeProperties(GenericEntity data);
	
	<A extends TypeSafeAttribute<V>, V> V getService(Class<A> attribute, Supplier<V> defaultValueSupplier);

	CmdResolver getCmdResolver();
}

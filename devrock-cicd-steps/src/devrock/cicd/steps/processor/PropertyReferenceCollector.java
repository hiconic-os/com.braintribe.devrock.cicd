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
package devrock.cicd.steps.processor;

import java.util.HashSet;
import java.util.Set;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EssentialTypes;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.processing.traverse.EntityCollector;
import com.braintribe.utils.template.Template;

public class PropertyReferenceCollector extends EntityCollector {
	
	private Set<String> propertyReferences = new HashSet<>();
	
	private PropertyReferenceCollector() {
		
	}
	
	public static Set<String> scanPropertyReferences(GenericEntity entity) {
		PropertyReferenceCollector collector = new PropertyReferenceCollector();
		collector.visit(entity.entityType(), entity);
		return collector.propertyReferences;
	}
	
	@Override
	protected boolean include(Property property, GenericEntity entity, EntityType<?> entityType) {
		if (property.getType() == EssentialTypes.TYPE_STRING) {
			String value = property.get(entity);
			
			if (value != null) {
				Template template = Template.parse(value, true);
				
				if (template.containsVariables())
					propertyReferences.add(value);
			}
		}
		return true;
	}
	
}

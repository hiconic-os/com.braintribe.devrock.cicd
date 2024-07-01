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
package devrock.cicd.lock.data.model;

import java.util.Date;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface LockRecord extends GenericEntity {
	EntityType<LockRecord> T = EntityTypes.T(LockRecord.class);
	
	String created = "created";
	String owner = "owner";
	String key = "key";
	String touched = "touched";
	
	String getKey();
	void setKey(String key);
	
	String getOwner();
	void setOwner(String owner);
	
	Date getCreated();
	void setCreated(Date created);
	
	Date getTouched();
	void setTouched(Date touched);
}

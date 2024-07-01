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
package devrock.cicd.model.api.data;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface GitContext extends GenericEntity {
	EntityType<GitContext> T = EntityTypes.T(GitContext.class);
	
	String baseBranch = "baseBranch";
	String baseHash = "baseHash";
	String baseRemote = "baseRemote";
	
	String getBaseBranch();
	void setBaseBranch(String baseBranch);
	
	// head or in case of a feature branch retrieved with: git rev-parse <branch> 
	String getBaseHash();
	void setBaseHash(String baseHash);
	
	String getBaseRemote();
	void setBaseRemote(String baseRemote);
}

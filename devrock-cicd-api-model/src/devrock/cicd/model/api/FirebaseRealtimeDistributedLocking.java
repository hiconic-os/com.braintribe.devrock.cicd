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
package devrock.cicd.model.api;

import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.annotation.meta.Confidential;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface FirebaseRealtimeDistributedLocking extends DistributedLocking {
	EntityType<FirebaseRealtimeDistributedLocking> T = EntityTypes.T(FirebaseRealtimeDistributedLocking.class);
	
	String owner = "owner";
	String tableUri = "tableUri";
	String webApiKey = "webApiKey";
	String user = "user";
	String password = "password";
	
	String touchIntervalInMs = "touchIntervalInMs";
	String touchWorkerIntervalInMs = "touchWorkerIntervalInMs";

	String getOwner();
	void setOwner(String owner);
	
	String getTableUri();
	void setTableUri(String tableUri);
	
	String getWebApiKey();
	void setWebApiKey(String webApiKey);
	
	String getUser();
	void setUser(String user);
	
	@Confidential
	String getPassword();
	void setPassword(String password);
	
	@Initializer("10000L")
	long getTouchIntervalInMs();
	void setTouchIntervalInMs(long touchIntervalInMs);
	
	@Initializer("1000L")
	long getTouchWorkerIntervalInMs();
	void setTouchWorkerIntervalInMs(long touchWorkerIntervalInMs);

}

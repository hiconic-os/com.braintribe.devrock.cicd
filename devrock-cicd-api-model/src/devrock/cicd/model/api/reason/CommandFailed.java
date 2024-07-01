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
package devrock.cicd.model.api.reason;

import com.braintribe.gm.model.reason.Reason;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface CommandFailed extends Reason {
	EntityType<CommandFailed> T = EntityTypes.T(CommandFailed.class);
	
	String errorMessage = "errorMessage";
	String errorCode = "errorCode";
	
	int getErrorCode();
	void setErrorCode(int errorCode);
	
	String getErrorMessage();
	void setErrorMessage(String errorMessage);

}

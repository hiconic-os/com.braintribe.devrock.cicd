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
package devrock.cicd.steps.processing;

import java.io.File;

public class DevEnvLocations {
	static File hasDevEnvParent(File dir) {
		File file = new File(dir, "dev-environment.yaml");
		
		if (file.exists()) {
			return dir;
		}
		else {
			File parentFolder = dir.getParentFile();
			
			if (parentFolder != null) {
				return hasDevEnvParent(parentFolder);
			}
			else {
				return null;
			}
		}
	}
}

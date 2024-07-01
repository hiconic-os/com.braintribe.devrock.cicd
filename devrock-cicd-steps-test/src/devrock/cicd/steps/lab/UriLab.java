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
package devrock.cicd.steps.lab;

import java.net.URI;
import java.net.URISyntaxException;

public class UriLab {
	public static void main(String[] args) {
		//orgs/hiconic-os/packages?package_type=maven
		
		try {
			URI uri = new URI("https", "user", "api.github.com", -1, "/orgs/hiconic-os", "package_type=maven", null);
			System.out.println(uri);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

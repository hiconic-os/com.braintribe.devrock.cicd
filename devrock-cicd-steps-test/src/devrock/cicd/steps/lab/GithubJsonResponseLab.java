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

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.braintribe.codec.marshaller.api.GmSerializationOptions;
import com.braintribe.codec.marshaller.json.JsonStreamMarshaller;
import com.braintribe.model.generic.GMF;
import com.braintribe.model.generic.reflection.EssentialTypes;
import com.braintribe.utils.FileTools;

public class GithubJsonResponseLab {
	private static final JsonStreamMarshaller marshaller = new JsonStreamMarshaller();

	public static void main(String[] args) {
		String prettyBody = FileTools.read(new File("res/github-list-packages-response-pretty.json")).asString();
		String originalBody = FileTools.read(new File("res/github-list-packages-response-original.json")).asString();
		
		List<Map<String,Object>> data = decodeJson(originalBody);
		
		List<String> artifacts = data.stream().map(p -> (String)p.get("name")).collect(Collectors.toList());
		
		System.out.println(artifacts);
		
	}
	
	public static List<Map<String, Object>> decodeJson(String json) {
		GmSerializationOptions options = GmSerializationOptions.deriveDefaults().setInferredRootType(GMF.getTypeReflection().getListType(EssentialTypes.TYPE_OBJECT)).build();
		
		List<Map<String, Object>> data = (List<Map<String, Object>>) marshaller.decode(json);
		
		return data;
	}

}

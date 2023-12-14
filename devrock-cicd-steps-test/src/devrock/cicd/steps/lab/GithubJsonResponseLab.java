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

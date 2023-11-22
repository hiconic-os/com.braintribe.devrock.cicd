package devrock.git;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.braintribe.codec.marshaller.api.GmDeserializationOptions;
import com.braintribe.codec.marshaller.yaml.YamlMarshaller;
import com.braintribe.common.lcd.Pair;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.model.generic.GenericEntity;

import devrock.step.model.api.ExchangeProperties;

public class CommentYamlInjectionParser {
	private static final Pattern pattern = Pattern.compile("^---\\s+#\\s+ci(\\s+[a-z0-9_\\-]+)?\\s*?(\\r?\\n|$)", Pattern.MULTILINE);
	
	public static Maybe<List<Pair<GenericEntity,String>>> extractYamlSections(String text) {
		Matcher matcher = pattern.matcher(text);
		
		List<Pair<GenericEntity, String>> entities = new ArrayList<>();
		int i = 0;
		
		String latestKey = null;
		
		while (matcher.find(i)) {
			String token = matcher.group(1);
			
			if (token != null) {
				token = token.trim();
			}
			
			int e = matcher.end();
			
			if (i > 0) {
				String section = text.substring(i, matcher.start());
				Maybe<GenericEntity> entityMaybe = parseEntity(section);
				
				if (entityMaybe.isUnsatisfied())
					return entityMaybe.whyUnsatisfied().asMaybe();
			
				entities.add(Pair.of(entityMaybe.get(), latestKey));
			}

			latestKey = token;
			
			i = e;
		}
		
		if (i > 0) {
			String section = text.substring(i);
			Maybe<GenericEntity> entityMaybe = parseEntity(section);
			
			if (entityMaybe.isUnsatisfied())
				return entityMaybe.whyUnsatisfied().asMaybe();
		
			entities.add(Pair.of(entityMaybe.get(), latestKey));
		}
		
		return Maybe.complete(entities);
	}
	
	private static Maybe<GenericEntity> parseEntity(String text) {
		YamlMarshaller marshaller = new YamlMarshaller();
		
		try (StringReader reader = new StringReader(text)) {
			Maybe<Object> valueMaybe = marshaller.unmarshallReasoned(reader, GmDeserializationOptions.defaultOptions);
			
			if (valueMaybe.isUnsatisfied())
				return Reasons.build(InvalidArgument.T).text("Could not extract yaml section from commit message").cause(valueMaybe.whyUnsatisfied()).toMaybe();
			
			Object value = valueMaybe.get();
			
			if (value instanceof Map) {
				ExchangeProperties exchangeProperties = ExchangeProperties.T.create();
				exchangeProperties.getProperties().putAll((Map<String, Object>)value);
				return Maybe.complete(exchangeProperties);
			}
			else if (value instanceof GenericEntity) {
				return Maybe.complete((GenericEntity)value);
			}
			else
				return Reasons.build(InvalidArgument.T).text("Unsupported value type in yaml section from commit message").toMaybe();
			
		}
	}
}

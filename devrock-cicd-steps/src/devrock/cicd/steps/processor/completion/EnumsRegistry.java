// ============================================================================
package devrock.cicd.steps.processor.completion;

import static com.braintribe.utils.lcd.CollectionTools2.newMap;
import static com.braintribe.utils.lcd.CollectionTools2.newTreeMap;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.braintribe.model.generic.reflection.EnumType;
import com.braintribe.model.meta.GmEnumConstant;
import com.braintribe.model.meta.GmEnumType;

/**
 * @author peter.gazdik
 */
/* package */ class EnumsRegistry {

	public final Map<String, GmEnumType> shortIdentifierToType = newTreeMap();

	private final Map<GmEnumType, String> typeToShortIdentifier = newMap();
	private final Map<String, Integer> shortNameToCount = newMap();

	public String acquireShortIdentifier(GmEnumType gmType) {
		return typeToShortIdentifier.computeIfAbsent(gmType, this::newShortIdentifier);
	}

	private String newShortIdentifier(GmEnumType gmType) {
		String result = resolveShortIdentifier(gmType);

		shortIdentifierToType.put(result, gmType);

		return result;
	}

	private String resolveShortIdentifier(GmEnumType gmType) {
		String shortName = gmType.<EnumType<?>> reflectionType().getShortName();
		Integer c = shortNameToCount.compute(shortName, (name, count) -> (count == null ? 1 : count + 1));
		return c == 1 ? shortName : shortName + c;
	}

	public static List<String> listConstantsNames(GmEnumType gmEnumType) {
		return gmEnumType.getConstants().stream() //
				.map(GmEnumConstant::getName) //
				.sorted() //
				.collect(Collectors.toList());
	}

}

// ============================================================================
package devrock.cicd.steps.processor.completion;

import static com.braintribe.utils.lcd.CollectionTools2.asMap;
import static com.braintribe.utils.lcd.CollectionTools2.newMap;
import static com.braintribe.utils.lcd.CollectionTools2.newTreeMap;
import static hiconic.rx.cli.processing.help.HelpProcessor.USE_CASE_EXECUTION;
import static hiconic.rx.cli.processing.help.HelpProcessor.USE_CASE_HELP;

import java.io.File;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.braintribe.model.generic.tools.BasicStringifier;
import com.braintribe.model.meta.GmEntityType;
import com.braintribe.model.meta.GmEnumType;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.meta.GmProperty;
import com.braintribe.model.meta.GmType;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.cmd.CmdResolverImpl;
import com.braintribe.model.processing.meta.cmd.builders.ModelMdResolver;
import com.braintribe.model.processing.meta.cmd.builders.PropertyMdResolver;
import com.braintribe.model.processing.meta.oracle.BasicModelOracle;
import com.braintribe.model.processing.meta.oracle.EntityTypeOracle;
import com.braintribe.model.processing.meta.oracle.ModelOracle;
import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.service.api.result.Neutral;
import com.braintribe.utils.FileTools;
import com.braintribe.utils.lcd.NullSafe;
import com.braintribe.utils.lcd.StringTools;

import devrock.cicd.model.api.CliCompletionStrategy;
import devrock.cicd.model.api.GenerateShellCompletionScript;
import hiconic.rx.cli.processing.help.CommandsReflection;
import hiconic.rx.cli.processing.help.CommandsReflection.CommandsOverview;

/**
 * @author peter.gazdik
 */
public class GenerateShellCompletionScriptProcessor implements ServiceProcessor<GenerateShellCompletionScript, Neutral> {

	private CommandsReflection commandsReflection;
	private CommandsOverview commandsOverview;
	private Comparator<GmEntityType> entityComparator_StandardAlias;

	private CommandsReflection helpCommandsReflection;

	@Override
	public Neutral process(ServiceRequestContext requestContext, GenerateShellCompletionScript request) {
		init(request);

		File outputFile = new File(request.getOutputFile());

		FileTools.write(outputFile).usingWriter(writer -> {
			FreemarkerRenderer.loadingViaClassLoader(getClass()) //
					.renderTemplate("shell-completion.sh.ftl", freemarkerParams(request), writer);
		});

		return Neutral.NEUTRAL;
	}

	private void init(GenerateShellCompletionScript request) {
		Map<String, GmMetaModel> domainNameToModel = request.getDomainNameToModel();

		Collection<GmMetaModel> models = domainNameToModel.values();

		GmMetaModel unifiedModel = createUnifiedModel(models);

		ModelOracle modelOracle = new BasicModelOracle(unifiedModel);
		CmdResolver cmdResolver = CmdResolverImpl.create(modelOracle).done();

		ModelMdResolver mdResolver = cmdResolver.getMetaData().useCases(USE_CASE_EXECUTION);

		this.commandsReflection = new CommandsReflection(modelOracle, mdResolver);
		this.entityComparator_StandardAlias = Comparator.comparing(commandsReflection::resolveStandardAlias);
		this.commandsOverview = commandsReflection.getCommandsOverview();

		ModelMdResolver helpMdResolver = cmdResolver.getMetaData().useCases(USE_CASE_EXECUTION, USE_CASE_HELP);
		this.helpCommandsReflection = new CommandsReflection(modelOracle, helpMdResolver);

		// remove non-unique types; they cannot be completed top-level, but only iff service domain is specified
		// this isn't support yet
		Set<GmEntityType> multiDomainRequestTypes = resolveMultiDomainRequestTypes(models);
		this.commandsOverview.requestTypes.removeAll(multiDomainRequestTypes);
		this.commandsOverview.allTypes.removeAll(multiDomainRequestTypes);
	}

	private Set<GmEntityType> resolveMultiDomainRequestTypes(Collection<GmMetaModel> models) {
		Map<GmEntityType, Boolean> typeToUniquenessIndicator = newMap();

		for (GmMetaModel model : models) {
			EntityTypeOracle serviceRequestOracle = new BasicModelOracle(model).findEntityTypeOracle(ServiceRequest.T);

			serviceRequestOracle.getSubTypes() //
					.transitive() //
					.onlyInstantiable() //
					.asGmTypes() //
					.stream() //
					.forEach(gmType -> {
						GmEntityType gmEntityType = (GmEntityType) gmType;
						typeToUniquenessIndicator.merge(gmEntityType, Boolean.TRUE, (v1, v2) -> Boolean.FALSE);
					});
		}

		// remove all unique types from the map
		typeToUniquenessIndicator.entrySet().removeIf(e -> e.getValue());

		return typeToUniquenessIndicator.keySet();
	}

	private GmMetaModel createUnifiedModel(Collection<GmMetaModel> models) {
		GmMetaModel result = GmMetaModel.T.create();
		result.setName("synthetic:all-domains-model");
		result.getDependencies().addAll(models);

		return result;
	}

	private Map<String, Object> freemarkerParams(GenerateShellCompletionScript request) {
		ShellCompletionGenerator generator = new ShellCompletionGenerator(request);

		return asMap(//
				"cliCommand", request.getCliCommand(), //
				"commandsList", generator.suggestedCommands(), //
				"resolveParameterTypeIfRelevant_Case", generator.parameterTypeIfRelevant_Case(), //
				"suggestParameterName_Case", generator.suggestParameterName_Case(), //
				"suggestHelp_CommandsList", generator.suggestHelp_Body(), //
				"suggestParameterValue_CustomCases", generator.suggestParameterValue_CustomCases() //
		);
	}

	private class ShellCompletionGenerator {

		private final GenerateShellCompletionScript request;
		private final EnumsRegistry enumsRegistry;

		private BasicStringifier stringifier;

		public ShellCompletionGenerator(GenerateShellCompletionScript request) {
			this.request = request;
			this.enumsRegistry = new EnumsRegistry();
		}

		private String suggestedCommands() {
			return printSuggestCommandsOverview(commandsOverview);
		}

		/**
		 * <pre>
		 * 	case $commandName in
		 * 		create-model)
		 * 			if [[ "$parameterName" =~ ^(--gwtSupport|--gwt|--overwrite|-o)$ ]]; then valueType="boolean"; return; fi
		 * 			if [[ "$parameterName" =~ ^(--file)$ ]]; then valueType="file"; return; fi
		 * 			if [[ "$parameterName" =~ ^(--dirList)$ ]]; then valueType="folder"; collectionType="list"; return; fi
		 * 			if [[ "$parameterName" =~ ^(--dirSet)$ ]]; then valueType="folder"; collectionType="set"; return; fi
		 * 			if [[ "$parameterName" =~ ^(--dirMap)$ ]]; then keyType="folder"; valueType="boolean"; collectionType="map"; return; fi
		 * 			;;
		 * 		...
		 * 	esac
		 * </pre>
		 */
		private String parameterTypeIfRelevant_Case() {
			Map<GmEntityType, Map<KnownType, List<String>>> commandToKnownTypeParams = resolveKnownTypeParams();

			stringifier = new BasicStringifier(new StringBuilder(), "\t", "\t");

			if (!commandToKnownTypeParams.isEmpty()) {
				stringifier.println("case $commandName in");
				stringifier.levelUp();

				for (Entry<GmEntityType, Map<KnownType, List<java.lang.String>>> e : commandToKnownTypeParams.entrySet()) {
					Map<KnownType, List<String>> typeToParamNames = e.getValue();

					// create-model)
					startCommandCaseWithAllAliases(e.getKey());

					for (Entry<KnownType, List<String>> e2 : typeToParamNames.entrySet())
						printSetParameterTypeForTheseParams(e2.getKey(), e2.getValue());

					endCase();
				}

				stringifier.levelDown();
				stringifier.print("esac");
			}

			return stringifier.appendable().toString();
		}

		private Map<GmEntityType, Map<KnownType, List<String>>> resolveKnownTypeParams() {
			Map<GmEntityType, Map<KnownType, List<String>>> result = newTreeMap(entityComparator_StandardAlias);

			addKnownTypes(commandsOverview.allTypes, result);

			return result;
		}

		private void addKnownTypes(List<GmEntityType> types, Map<GmEntityType, Map<KnownType, List<String>>> result) {
			for (GmEntityType gmType : types) {
				Map<KnownType, List<String>> knownParams = resolveKnownTypeParamsFor(gmType);
				knownParams.remove(KnownType.IGNORED_TYPE);

				if (!knownParams.isEmpty())
					result.put(gmType, knownParams);
			}
		}

		private Map<KnownType, List<String>> resolveKnownTypeParamsFor(GmEntityType requestType) {
			// entityType -> properties -> Map<knownTypeName, List<propertyNameAndAliases>>

			return commandsReflection.getRelevantPropertiesOf(requestType).stream() //
					.collect( //
							Collectors.groupingBy( //
									this::resolveKnownTypeOf, //
									flatMapping( //
											commandsReflection::cliNameAndAliasesOf, //
											Collectors.toList() //
									) //
							) //
					);
		}

		private KnownType resolveKnownTypeOf(GmProperty p) {
			GmType type = p.getType();
			PropertyMdResolver propertyMdResolver = commandsReflection.mdResolver.property(p);

			return KnownType.resolveKnownType(type, propertyMdResolver, enumsRegistry);
		}

		private void printSetParameterTypeForTheseParams(KnownType knownType, List<String> params) {
			String regextMatchingParams = StringTools.join("|", params);
			stringifier.println(
					"if [[ \"$parameterName\" =~ ^(" + regextMatchingParams + ")$ ]]; then" + variableAssignmentsFor(knownType) + " return; fi");
		}

		private String variableAssignmentsFor(KnownType kt) {
			String result = "";

			if (!StringTools.isEmpty(kt.keyType))
				result += " keyType=\"" + kt.keyType + "\";";

			if (!StringTools.isEmpty(kt.valueType))
				result += " valueType=\"" + kt.valueType + "\";";

			if (!StringTools.isEmpty(kt.collectionType))
				result += " collectionType=\"" + kt.collectionType + "\";";

			return result;
		}

		/**
		 * <pre>
		 * __suggestParameterName() {
		 * case $commandName in
		 * create-model)
		 * __suggest "--artifactId --groupId --gwt";;
		 * ...
		 * esac
		 * }
		 * </pre>
		 */
		private String suggestParameterName_Case() {
			stringifier = new BasicStringifier(new StringBuilder(), "\t", "\t");

			stringifier.println("case $commandName in");
			stringifier.levelUp();

			writeParameterNameSuggestionsForStandardCommands();

			stringifier.levelDown();
			stringifier.print("esac");

			return stringifier.appendable().toString();
		}

		private void writeParameterNameSuggestionsForStandardCommands() {
			Map<GmEntityType, List<String>> commandToParamNames = resolveCommandsWithParamNames();

			for (Entry<GmEntityType, List<String>> e : commandToParamNames.entrySet())
				writeParameterNameSuggestionsFor(e);
		}

		private void writeParameterNameSuggestionsFor(Entry<GmEntityType, List<String>> e) {
			List<String> paramNames = e.getValue();

			if (paramNames.isEmpty())
				return;

			startCommandCaseWithAllAliases(e.getKey());
			endCaseWithSuggestValues(paramNames);
		}

		private void startCommandCaseWithAllAliases(GmEntityType gmType) {
			String commandCase = commandsReflection.nameAndAliasesSorted(gmType).collect(Collectors.joining("|"));
			startCase(commandCase);
		}

		private Map<GmEntityType, List<String>> resolveCommandsWithParamNames() {
			Map<GmEntityType, List<String>> result = newTreeMap(entityComparator_StandardAlias);

			for (GmEntityType gmType : commandsOverview.allTypes)
				result.put(gmType, allPropsMaybeAlsoWithAliasesOf(gmType));

			return result;
		}

		private List<String> allPropsMaybeAlsoWithAliasesOf(GmEntityType gmType) {
			return commandsReflection.getRelevantPropertiesOf(gmType).stream() //
					.flatMap(this::nameAndMaybeAlsoAliasesOf) //
					.sorted() //
					.collect(Collectors.toList());
		}

		private Stream<String> nameAndMaybeAlsoAliasesOf(GmProperty gmProperty) {
			CliCompletionStrategy strategy = NullSafe.get(request.getArgumentNameCompletionStrategy(), CliCompletionStrategy.realName);

			switch (strategy) {
				case all:
					return commandsReflection.cliNameAndAliasesOf(gmProperty);
				case shortest:
					return commandsReflection.cliNameAndAliasesOf(gmProperty).sorted(StringTools::compareStringsSizeFirst).limit(1);
				case realName:
				default:
					return Stream.of(commandsReflection.cliNameOf(gmProperty));
			}
		}

		/**
		 * <pre>
		 * __suggestHelp() {
		 * 	__suggest "create-model create-module create-library...";
		 * }
		 * </pre>
		 */
		private String suggestHelp_Body() {
			CommandsOverview helpCo = helpCommandsReflection.getCommandsOverview();
			return printSuggestCommandsOverview(helpCo);
		}

		/**
		 * Renders custom types, e.g. enums (MyColor) or virtual enums.
		 * 
		 * <pre>
		 * __suggestParameterValue() {
		 * 	case $currentWordType in
		 * 		boolean)
		 * 			__suggest "true false";;
		 * 		file)
		 * 			__suggestFile;;
		 *		folder)
		 * 			__suggestFolder;;
		 * 		MyColor)
		 * 			__suggest "red green blue";;
		 * esac
		 * }
		 * </pre>
		 */
		private String suggestParameterValue_CustomCases() {
			stringifier = new BasicStringifier(new StringBuilder(), "\t\t", "\t");

			for (Entry<String, GmEnumType> e : enumsRegistry.shortIdentifierToType.entrySet()) {
				startCase(e.getKey());
				endCaseWithSuggestValues(EnumsRegistry.listConstantsNames(e.getValue()));
			}

			return stringifier.appendable().toString();
		}

		private void startCase(String key) {
			stringifier.println(key + ")");
			stringifier.levelUp();
		}

		private void endCaseWithSuggestValues(List<String> values) {
			stringifier.print("__suggest \"" + StringTools.join(" ", values) + "\"");
			endCase();
		}

		private void endCase() {
			stringifier.println(";;");
			stringifier.levelDown();
		}

	}

	// Helpers

	private String printSuggestCommandsOverview(CommandsOverview commandsOverview) {
		return commandsOverview.allTypes.stream() //
				.map(commandsReflection::resolveStandardAlias) //
				.collect(Collectors.joining(" "));
	}

	// Once we move past Java 8 replace with Collectors.flatMapping(...)
	static <T, U, A, R> Collector<T, ?, R> flatMapping(Function<? super T, ? extends Stream<? extends U>> mapper,
			Collector<? super U, A, R> downstream) {

		BiConsumer<A, ? super U> acc = downstream.accumulator();
		return Collector.of( //
				downstream.supplier(), //
				(a, t) -> {
					try (Stream<? extends U> s = mapper.apply(t)) {
						if (s != null)
							s.forEachOrdered(u -> acc.accept(a, u));
					}
				}, //
				downstream.combiner(), //
				downstream.finisher(), //
				downstream.characteristics().toArray(new Collector.Characteristics[0]));
	}
}

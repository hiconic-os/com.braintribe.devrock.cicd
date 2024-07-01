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
package devrock.step.framework;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import com.braintribe.codec.marshaller.api.EntityVisitorOption;
import com.braintribe.codec.marshaller.api.GmSerializationOptions;
import com.braintribe.codec.marshaller.api.OutputPrettiness;
import com.braintribe.codec.marshaller.api.TypeExplicitness;
import com.braintribe.codec.marshaller.api.TypeExplicitnessOption;
import com.braintribe.codec.marshaller.yaml.YamlMarshaller;
import com.braintribe.common.attribute.TypeSafeAttribute;
import com.braintribe.gm.config.yaml.ConfigVariableResolver;
import com.braintribe.gm.config.yaml.YamlConfigurations;
import com.braintribe.gm.config.yaml.api.ConfigurationReadBuilder;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.gm.model.reason.essential.ParseError;
import com.braintribe.gm.model.reason.essential.UnsupportedOperation;
import com.braintribe.model.generic.GMF;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EssentialTypes;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.generic.reflection.ScalarType;
import com.braintribe.model.meta.data.constraint.Mandatory;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.session.api.managed.ModelAccessory;
import com.braintribe.model.resource.Resource;
import com.braintribe.model.resource.source.TransientSource;
import com.braintribe.utils.FileTools;
import com.braintribe.utils.IOTools;
import com.braintribe.utils.StringTools;
import com.braintribe.utils.lcd.LazyInitialized;
import com.braintribe.utils.lcd.NullSafe;
import com.braintribe.ve.impl.StandardEnvironment;

import devrock.step.api.StepExchangeContext;
import devrock.step.model.api.ExchangeProperties;
import devrock.step.model.api.meta.ArgumentMapping;
import devrock.step.model.api.meta.ExchangeClassifier;
import devrock.step.model.api.meta.ExchangeConfiguration;
import devrock.step.model.api.meta.ExternalArgument;
import devrock.step.model.api.meta.Intricate;
import devrock.step.model.api.meta.ProjectDir;

public class StepExchangeContextImpl implements StepExchangeContext {
	
	private static final ExternalArgument defaultExternalArgument = ExternalArgument.T.create();
	private final File configFolder;
	
	private static record ConfigKey (EntityType<?> type, String classifier) {}
	
	private final Map<ConfigKey, LazyInitialized<Maybe<? extends GenericEntity>>> configs = new ConcurrentHashMap<>();
	private final Function<String, Object> properties;
	private final File projectDir;
	private final Map<Class<? extends TypeSafeAttribute<?>>, Object> services = new ConcurrentHashMap<>();
	private CmdResolver cmdResolver;
	
	@Deprecated
	public StepExchangeContextImpl(File projectDir, File configFolder, ModelAccessory modelAccessory, Function<String, Object> properties) {
		this(projectDir, configFolder, modelAccessory.getCmdResolver(), properties);
	}
	
	public StepExchangeContextImpl(File projectDir, File configFolder, CmdResolver cmdResolver, Function<String, Object> properties) {
		this.projectDir = projectDir;
		this.configFolder = configFolder;
		this.cmdResolver = cmdResolver;
		this.properties = properties;
	}

	@Override
	public <V> Maybe<V> getProperty(GenericModelType type, String name) {
		Maybe<Object> exchangeValueMaybe = resolvePropertyFromExchangeProperties(type, name);

		if (exchangeValueMaybe.isSatisfied())
			return exchangeValueMaybe.cast();
		
		if (exchangeValueMaybe.isUnsatisfiedBy(NotFound.T)) {
			if (properties == null)
				return Reasons.build(NotFound.T).text("No external properties are configured").toMaybe();
			
			Object value = properties.apply(name);
			
			if (value == null)
				return Reasons.build(NotFound.T).text("Property " + name + " not found in external properties").toMaybe();
			
			return cast(type, value);
		}
		
		return exchangeValueMaybe.cast();
	}
	
	private Maybe<? extends GenericEntity> loadConfig(ConfigKey key) {
		EntityType<?> configType = key.type();
		
		String fileName = buildConfigFileName(key);
		File configFile = new File(configFolder, fileName);
		
		// if file does not exist a default instance of the configuration will be created
		if (!configFile.exists()) {
			return Reasons.build(NotFound.T).text("Could not load " + configType.getTypeSignature() + " due to missing exchange file " + configFile.getAbsolutePath()).toMaybe();
		}
		
		Set<TransientSource> transientSources = new HashSet<>();
		
		boolean exchangeConfiguration = cmdResolver.getMetaData().entityType(configType).is(ExchangeConfiguration.T);
		
		ConfigurationReadBuilder<? extends GenericEntity> builder = YamlConfigurations.read(configType) //
				.options(o -> o.set(EntityVisitorOption.class, e -> {
					if (e instanceof TransientSource) {
						transientSources.add((TransientSource)e);
					}
				}));
		
		if (exchangeConfiguration) {
			ConfigVariableResolver resolver = new ConfigVariableResolver(StandardEnvironment.INSTANCE, configFile);
			builder.placeholders(resolver::resolve);
		}
		else {
			builder.noDefaulting();
		}
		
		Maybe<? extends GenericEntity> configMaybe = builder.from(configFile);
		
		if (!transientSources.isEmpty()) {
			File resFolder = new File(configFolder, fileName + ".res");
			
			for (TransientSource source: transientSources) {
				String resFileName = source.getGlobalId();
				
				if (resFileName == null)
					continue;
				
				String suffix = Optional.ofNullable(source.getOwner()).map(Resource::getName).orElse(null);
				
				if (suffix != null)
					resFileName += "-" + suffix;
				
				File resFile = new File(resFolder, resFileName);
				
				if (resFile.exists()) {
					source.setInputStreamProvider(() -> new FileInputStream(resFile));
				}
			}
		}
		
		return configMaybe;
	}
	
	@Override
	public void makeOrCleanExchangeFolder() {
		if (configFolder.exists()) {
			try {
				FileTools.deleteDirectoryRecursively(configFolder);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		
		configFolder.mkdirs();
		File file = new File(configFolder, ".gitignore");
		try {
			IOTools.spit(file, "*", "UTF-8", false);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public <E extends GenericEntity> Maybe<E> load(EntityType<E> configType) {
		return load(configType, null);
	}
	
	@Override
	public <E extends GenericEntity> Maybe<E> load(EntityType<E> type, String classifier) {
		ConfigKey configKey = new ConfigKey(type, classifier);
		return (Maybe<E>) configs.computeIfAbsent(configKey, k -> new LazyInitialized<>(() -> this.loadConfig(k))).get();
	}

	@Override
	public <E extends GenericEntity> void store(EntityType<E> type, E data) {
		store(type, null, data);
	}
	
	@Override
	public <E extends GenericEntity> void store(EntityType<E> type, String classifier, E data) {
		ConfigKey key = new ConfigKey(type, classifier);
		YamlMarshaller yamlMarshaller = new YamlMarshaller();
		
		yamlMarshaller.setWritePooled(isIntricate(data));
		File configFile = new File(configFolder, buildConfigFileName(key));
		
		Set<TransientSource> transientSources = new HashSet<>();
		
		GmSerializationOptions options = GmSerializationOptions.deriveDefaults() //
			.inferredRootType(type)
			.setOutputPrettiness(OutputPrettiness.high)
			.set(TypeExplicitnessOption.class, TypeExplicitness.polymorphic)
			.set(EntityVisitorOption.class, e -> { 
				if (e instanceof TransientSource) { 
					TransientSource transientSource = (TransientSource)e;
					if (transientSource.getInputStreamProvider() != null)
						transientSources.add(transientSource); 
				}
			})
			.build();
		
		try (OutputStream out = new BufferedOutputStream(new FileOutputStream(configFile))) {
			yamlMarshaller.marshall(out, data, options);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		
		File resourceFolder = new File(configFolder, configFile.getName() + ".res");
		
		if (resourceFolder.exists()) {
			try {
				FileTools.deleteDirectoryRecursively(resourceFolder);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		
		if (!transientSources.isEmpty()) {
			resourceFolder.mkdirs();
			
			for (TransientSource source: transientSources) {
				
				String fileName = source.getGlobalId();
				
				String suffix = Optional.ofNullable(source.getOwner()).map(Resource::getName).orElse(null);
				
				if (suffix != null)
					fileName += "-" + suffix;
				
				File resFile = new File(resourceFolder, fileName);
					
				try (InputStream in = source.openStream(); OutputStream out = new FileOutputStream(resFile)) {
					IOTools.transferBytes(in, out);
				}
				catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		}
		
		configs.put(key, new LazyInitialized<>(() -> Maybe.complete(data)));
	}

	private <E extends GenericEntity> boolean isIntricate(E data) {
		if (cmdResolver == null)
			return false;
		
		return cmdResolver.getMetaData().entity(data).is(Intricate.T);
	}
	
	private String buildConfigFileName(ConfigKey key) {
		StringBuilder builder = new StringBuilder();
		builder.append(StringTools.camelCaseToDashSeparated(key.type().getShortName()).toLowerCase());
		
		String classifier = key.classifier();
		
		if (classifier != null) {
			builder.append('.');
			builder.append(classifier);
		}
		
		builder.append(".yaml");
		return  builder.toString();
	}

	@Override
	public Reason loadProperties(GenericEntity containerEntity) {
		EntityType<GenericEntity> containerType = containerEntity.entityType();
		for (Property property: containerType.getProperties()) {
			Maybe<?> maybe = resolveValue(containerEntity, property);

			// if there is no value present from elsewhere
			if (maybe.isUnsatisfiedBy(NotFound.T)) {
				Object value = property.get(containerEntity);

				// if a preset value is present everything is ok
				if (value != null)
					continue;

				// if no preset value is present it depends on Mandatory meta data if this is a problem or not
				if (cmdResolver != null && cmdResolver.getMetaData().entityType(containerType).property(property).is(Mandatory.T))
					return Reasons.build(InvalidArgument.T).text("Property " + containerEntity.entityType().getTypeSignature() + "." + property.getName() + " must not be empty").toReason();
				
				continue;
			}
			
			if (maybe.isUnsatisfied()) {
				return Reasons.build(InvalidArgument.T) //
						.text("Property " + containerEntity.entityType().getTypeSignature() + "." + property.getName() + " could not be resolved") //
						.cause(maybe.whyUnsatisfied()) //
						.toReason();
			}
			
			property.set(containerEntity, maybe.get());
		}
		
		return null;
	}
	
	private Maybe<?> resolveValue(GenericEntity containerEntity, Property property) {
		if (property.getType().isEntity()) {
			return resolveFromExchange(containerEntity, property);
		}
		
		if (property.getType().isScalar()) {
			ArgumentMapping argumentMapping = resolveArgumentMapping(containerEntity, property);
			
			if (argumentMapping instanceof ProjectDir) {
				return Maybe.complete(projectDir.getAbsolutePath());
			}
			else if (argumentMapping instanceof ExternalArgument) {
				return getArgumentValue(containerEntity.entityType(), (ExternalArgument)argumentMapping, property);
			}
			
			return Reasons.build(UnsupportedOperation.T).text("Unsupported ArgumentMapping metadata type " + argumentMapping.type().getTypeSignature()).toMaybe();
		}
		
		return Reasons.build(NotFound.T).text("Property not found").toMaybe();
	}
	
	private Maybe<? extends GenericEntity> resolveFromExchange(GenericEntity containerEntity, Property property) {
		GenericModelType propertyType = property.getType();
		
		if (propertyType.isEntity()) {
			GenericEntity entity = property.get(containerEntity);
			
			if (entity == null) {
				String exchangeClassifier = getExchangeClassifier(containerEntity.entityType(), property);
				
				EntityType<? extends GenericEntity> type = propertyType.cast();
				return load(type, exchangeClassifier);
			}
		}
		
		return Reasons.build(NotFound.T).text("Cound find property in exchange").toMaybe();
	}

	private ArgumentMapping resolveArgumentMapping(GenericEntity containerEntity, Property property) {
		if (cmdResolver == null)
			return defaultExternalArgument;
		
		return NullSafe.get(//
				cmdResolver.getMetaData().entity(containerEntity).property(property).meta(ArgumentMapping.T).exclusive(), //
				defaultExternalArgument //
		);
	}
	
	private Maybe<Object> getArgumentValue(EntityType<?> entityType, ExternalArgument argument, Property property) {
		String name = argument.getName();
		
		if (name == null)
			name = property.getName();
		
		String extendedName = entityType.getShortName() + "." + name;
		
		Maybe<Object> valueMaybe = getProperty(property.getType(), extendedName);
		
		if (valueMaybe.isUnsatisfiedBy(NotFound.T))
			return getProperty(property.getType(), name);
		
		return valueMaybe;
	}
	
	private Maybe<Object> resolvePropertyFromExchangeProperties(GenericModelType type, String name) {
		Maybe<ExchangeProperties> propertiesMaybe = load(ExchangeProperties.T);

		if (propertiesMaybe.isSatisfied()) {
			ExchangeProperties exchangeProperties = propertiesMaybe.get();
			Object value = exchangeProperties.getProperties().get(name);
			
			if (value != null) {
				if (!type.isInstance(value))
					return Reasons.build(InvalidArgument.T).text("Type of " + name + " from exchange properties is not assignable to " + type.getTypeSignature()).toMaybe();
				
				return Maybe.complete(value);
			}
			
			return Reasons.build(NotFound.T).text("Property " + name + " not found in ExchangeProperties").toMaybe();
		}
		
		if (propertiesMaybe.isUnsatisfiedBy(NotFound.T))
			return propertiesMaybe.whyUnsatisfied().asMaybe();
		
		return Reasons.build(InvalidArgument.T).text("Error while resolving property from ExchangeProperties").cause(propertiesMaybe.whyUnsatisfied()).toMaybe();
	}

	private <V> Maybe<V> cast(GenericModelType propertyType, Object value) {
		GenericModelType actualType = GMF.getTypeReflection().getType(value);
		
		if (propertyType.isAssignableFrom(actualType)) {
			@SuppressWarnings("unchecked")
			V castedValue = (V)value;
			return Maybe.complete(castedValue);
		}
		
		if (actualType == EssentialTypes.TYPE_STRING) {
			return parse(propertyType, (String)value);
		}
		
		return Reasons.build(UnsupportedOperation.T).text("Cannot convert from " + actualType.getTypeSignature() + " to " + propertyType.getTypeSignature()).toMaybe();
	}
	
	private <V> Maybe<V> parse(GenericModelType type, String expression) {
		switch (type.getTypeCode()) {
		case booleanType:
			switch (expression) {
			case "true": return Maybe.complete((V)Boolean.TRUE);
			case "false": return Maybe.complete((V)Boolean.FALSE);
			default: 
				return Reasons.build(ParseError.T) //
					.text("Cannot parse [" + expression + "] to type " + type.getTypeSignature()).toMaybe(); 
			}
		case dateType:
		case decimalType:
		case doubleType:
		case enumType:
		case floatType:
		case integerType:
		case longType:
			ScalarType scalarType = (ScalarType) type;
			try {
				return Maybe.complete(scalarType.instanceFromString(expression));
			}
			catch (RuntimeException e) {
				return Reasons.build(ParseError.T).text("Cannot parse [" + expression + "] to type " + type.getTypeSignature() + ": " + e.getMessage()).toMaybe();
			}
		default:
			return Reasons.build(ParseError.T).text("Cannot parse [" + expression + "] to type " + type.getTypeSignature()).toMaybe();
		}
	}

	private String getExchangeClassifier(EntityType<?> type, Property property) {
		if (cmdResolver == null)
			return null;
		
		ExchangeClassifier exchangeClassifier = cmdResolver.getMetaData().entityType(type).property(property).meta(ExchangeClassifier.T).exclusive();
		
		if (exchangeClassifier == null)
			return null;
			
		return exchangeClassifier.getValue();
	}

	@Override
	public void storeProperties(GenericEntity containerEntity) {
		EntityType<GenericEntity> containerType = containerEntity.entityType();
		for (Property property: containerType.getProperties()) {
			GenericModelType propertyType = property.getType();
			
			if (propertyType.isEntity()) {
				GenericEntity entity = property.get(containerEntity);
				
				if (entity != null) {
					String exchangeClassifier = getExchangeClassifier(containerType, property);
					
					EntityType<GenericEntity> type = entity.entityType();
					store(type, exchangeClassifier, entity);
				}
			}
		}
	}
	
	@Override
	public <A extends TypeSafeAttribute<V>, V> V getService(Class<A> attribute, Supplier<V> defaultValueSupplier) {
		return (V) services.computeIfAbsent(attribute, k -> defaultValueSupplier.get());
	}

	@Override
	public CmdResolver getCmdResolver() {
		return cmdResolver;
	}

}

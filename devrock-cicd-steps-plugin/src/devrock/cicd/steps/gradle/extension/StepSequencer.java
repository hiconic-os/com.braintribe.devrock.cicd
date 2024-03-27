package devrock.cicd.steps.gradle.extension;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.tools.ant.BuildException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.UncheckedIOException;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.cmd.builders.EntityMdResolver;
import com.braintribe.utils.lcd.NullSafe;
import com.braintribe.utils.lcd.StringTools;

import devrock.cicd.steps.gradle.common.AntTaskContext;
import devrock.cicd.steps.gradle.common.Closures;
import devrock.cicd.steps.gradle.common.GradleAntContext;
import devrock.step.api.StepEvaluator;
import devrock.step.framework.Steps;
import devrock.step.model.api.StepRequest;
import devrock.step.model.api.meta.ArgumentPropagation;
import groovy.lang.Closure;

public class StepSequencer {
	private static final String ENV_PROPERTY_PREFIX = "DEVROCK_STEPS__";
	private static final String ENV_PROPERTY_PRIO_PREFIX = "DEVROCK_STEPS_OVERRIDE__";
	private Task lastMandatoryStep = null;
	private final List<Task> lastOptionalSteps = new ArrayList<>();
	private final Map<String, Task> stepByName = new LinkedHashMap<>();
	private Boolean externallySequenced;
	private final Project project;
	private final StepEvaluator evaluator;
	private final GradleAntContext gradleAntContext;
	private final boolean useColors;

	public StepSequencer(Project project, GradleAntContext gradleAntContext, boolean useColors) {
		this.project = project;
		this.gradleAntContext = gradleAntContext;
		this.useColors = useColors;
		File exchangeFolder = new File(project.getProjectDir(), ".step-exchange");
		evaluator = Steps.evaluator(project.getProjectDir(), exchangeFolder, this::findProperty);
	}

	private Object findProperty(String name) {
		String envName = convertPropertyNameToEnvName(name, true);

		String envValue = System.getenv(envName);

		if (envValue != null)
			return envValue;

		String separatedName = "." + name;
		Object value = project.findProperty(separatedName);

		if (value != null)
			return value;
		
		/* Start of temporary backwards compatible direct name lookup */
		value = project.findProperty(name);
		
		if (value != null)
			return value;
		/* End of temporary backwards compatible direct name lookup */

		envName = convertPropertyNameToEnvName(name, false);

		return System.getenv(envName);
	}

	private String convertPropertyNameToEnvName(String name, boolean prio) {
		String adaptedName = StringTools.camelCaseToScreamingSnakeCase(name);

		adaptedName = (prio ? ENV_PROPERTY_PRIO_PREFIX : ENV_PROPERTY_PREFIX) + adaptedName.replace(".", "__");

		return adaptedName;
	}

	public void makeOrCleanExchangeFolder() {
		evaluator.makeOrCleanExchangeFolder();
	}

	public <E extends GenericEntity> Maybe<E> load(EntityType<E> type) {
		return evaluator.load(type);
	}

	public <E extends GenericEntity> void store(EntityType<E> type, E data) {
		evaluator.store(type, data);
	}

	public <E extends GenericEntity> void store(E data) {
		evaluator.store(data);
	}

	public boolean isAutoSequenced() {
		return !isExternallySequenced();
	}

	public boolean isExternallySequenced() {
		if (externallySequenced == null) {
			externallySequenced = "true".equals(System.getenv("DEVROCK_PIPELINE_EXTERNAL_SEQUENCING"))
					|| "true".equals(project.findProperty("externallySequenced"));
		}

		return externallySequenced;
	}

	public void step(String name, Runnable runnable) {
		step(name, runnable, (Consumer<RunnableStepConfiguration>) null);
	}

	public void step(String name, Runnable runnable, Closure<?> stepConfigurer) {
		step(name, runnable, c -> Closures.with(stepConfigurer, c));
	}

	public void step(String name, Runnable runnable, Consumer<RunnableStepConfiguration> stepConfigurer) {
		RunnableStepConfiguration stepConf = new RunnableStepConfiguration(name, runnable);

		if (stepConfigurer != null) {
			stepConfigurer.accept(stepConf);
		}

		step(stepConf);
	}

	public <S extends StepRequest> void step(EntityType<S> stepType) {
		step(stepType, (Consumer<RequestStepConfiguration<S>>) null);
	}

	public <S extends StepRequest> void step(EntityType<S> stepType, Closure<?> stepConfigurer) {
		step(stepType, c -> Closures.with(stepConfigurer, c));
	}

	public <S extends StepRequest> void step(EntityType<S> stepType, Consumer<RequestStepConfiguration<S>> stepConfigurer) {
		RequestStepConfiguration<S> stepConf = new RequestStepConfiguration<S>(evaluator, stepType, gradleAntContext);

		if (stepConfigurer != null)
			stepConfigurer.accept(stepConf);

		step(stepConf);
	}

	public void step(StepConfiguration conf) {
		boolean optional = conf.isOptional();
		String name = conf.getName();
		Consumer<Task> configurer = conf.getConfigurer();

		if (isExternallySequenced())
			optional = true;

		Task step = project.task(name);

		step.setGroup("devrock pipeline");

		if (project.hasProperty("dry")) {
			step.doLast(Closures.from(() -> System.out.println(name)));
		} else {
			step.doLast(Closures.from(conf.getRunnable()));
		}

		if (configurer != null)
			configurer.accept(step);

		stepByName.put(name, step);

		if (isAutoSequenced())
			conf.getRequires().forEach(s -> step.dependsOn(s));

		lastOptionalSteps.forEach(t -> step.mustRunAfter(t));

		if (lastMandatoryStep != null) {
			step.dependsOn(lastMandatoryStep);
		}

		if (optional)
			lastOptionalSteps.add(step);
		else {
			lastMandatoryStep = step;
			lastOptionalSteps.clear();
		}
	}

	public void ant(String folderName, String target, String... propertyPropagations) {
		File artifactDir = new File(project.getProjectDir(), folderName);

		try {
			File outputFile = File.createTempFile("antOutput", ".log");

			Map<String, String> properties = new LinkedHashMap<>();
			addPassedPropertyPropagations(properties, propertyPropagations);
			addMdBasedPropertyPropagations(properties);

			properties.put("colors", String.valueOf(useColors));

			AntTaskContext taskCtx = new AntTaskContext(artifactDir, target, outputFile, properties);
			long startMs = System.currentTimeMillis();

			try {
				gradleAntContext.executeAntTask(taskCtx);

			} catch (BuildException e) {
				taskCtx.buildException = e;
				throw e;

			} finally {
				taskCtx.durationMs = System.currentTimeMillis() - startMs;

				gradleAntContext.printReport(taskCtx);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void addPassedPropertyPropagations(Map<String, String> properties, String... propertyPropagations) {
		for (String property : propertyPropagations) {
			Object value = resolvePropertyValue(property);

			if (value != null)
				properties.put(property, value.toString());
		}
	}

	private Object resolvePropertyValue(String propertyName) {
		StepRequest currentRequest = evaluator.getCurrentRequest();

		if (currentRequest == null)
			return project.findProperty(propertyName);

		Property p = currentRequest.entityType().getProperty(propertyName);
		return p.get(currentRequest);
	}

	private void addMdBasedPropertyPropagations(Map<String, String> properties) {
		StepRequest currentRequest = evaluator.getCurrentRequest();
		if (currentRequest == null)
			return;

		CmdResolver cmdResolver = evaluator.getCmdResolver();
		EntityMdResolver entityMdResolver = cmdResolver.getMetaData().entity(currentRequest);

		for (Property p : currentRequest.entityType().getProperties()) {
			ArgumentPropagation ap = entityMdResolver.property(p).meta(ArgumentPropagation.T).exclusive();
			if (ap == null)
				continue;

			Object value = p.get(currentRequest);
			if (value == null)
				continue;

			String downstreamArgumentName = NullSafe.get(ap.getName(), p.getName());
			properties.put(downstreamArgumentName, value.toString());
		}

	}
}

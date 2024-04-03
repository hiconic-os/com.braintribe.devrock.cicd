package devrock.cicd.steps.gradle.plugin;

import java.io.File;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.logging.configuration.ShowStacktrace;

import devrock.cicd.steps.gradle.common.GradleAntContext;
import devrock.cicd.steps.gradle.extension.StepSequencer;

public class DevrockCicdStepsPlugin implements Plugin<Project> {
	
	private GradleAntContext antCtx;

	@Override
	public void apply(Project project) {
		installDevrockAntTasks(project);
		installExtension(project);
	}

	private void installExtension(Project project) {
		project.getExtensions().create("steps", StepSequencer.class, project, antCtx);
	}

	private void installDevrockAntTasks(Project project) {
		String devrockSdkRootVar = System.getenv("DEVROCK_SDK_HOME");
	    File devrockSdkRoot = devrockSdkRootVar != null? //
	        new File(devrockSdkRootVar): // 
	        new File(project.getProjectDir(), "../../../..").toPath().toAbsolutePath().normalize().toFile();
	    
	    File antCustomLibsFolder = new File(devrockSdkRoot, "tools/ant-libs");
	    File antLibsFolder = new File(devrockSdkRoot, "tools/ant/lib");
		if (antCustomLibsFolder.exists()) {
			String classpath = Stream.of(antCustomLibsFolder.listFiles()) // 
				.filter(File::isFile)
				.map(File::getAbsolutePath)
				.filter(e -> e.endsWith(".jar"))
				.collect(Collectors.joining(File.pathSeparator));

			// TODO: set repository configuration variable in order to use SDKs repository-configuration-devrock.yaml
			// env var name -> REPOSITORY_CONFIGURATION_DEVROCK_ANT_TASKS
			
			antCtx = new GradleAntContext(classpath, antLibsFolder, antCustomLibsFolder, shouldShowStacktrace(project));
			//antCtx.removeGradleLoggerFromAnt();
			antCtx.taskdef("antlib:com.braintribe.build.ant.tasks", "com/braintribe/build/ant/tasks/antlib.xml");
			antCtx.taskdef("net/sf/antcontrib/antlib.xml");
			antCtx.taskdef("org/jacoco/ant/antlib.xml");
		}
	}

	private boolean shouldShowStacktrace(Project project) {
		return project.getGradle().getStartParameter().getShowStacktrace() != ShowStacktrace.INTERNAL_EXCEPTIONS;
	}
}

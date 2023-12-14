package devrock.cicd.steps.gradle.common;

import static com.braintribe.console.ConsoleOutputs.brightRed;
import static com.braintribe.console.ConsoleOutputs.brightYellow;
import static com.braintribe.console.ConsoleOutputs.cyan;
import static com.braintribe.console.ConsoleOutputs.println;
import static com.braintribe.console.ConsoleOutputs.sequence;
import static com.braintribe.console.ConsoleOutputs.stackTrace;
import static com.braintribe.console.ConsoleOutputs.text;
import static com.braintribe.console.ConsoleOutputs.white;
import static com.braintribe.console.ConsoleOutputs.yellow;
import static com.braintribe.utils.lcd.CollectionTools2.newConcurrentSet;

import java.io.File;
import java.io.PrintStream;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Ant;
import org.apache.tools.ant.taskdefs.Property;
import org.apache.tools.ant.taskdefs.Taskdef;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

import com.braintribe.console.output.ConsoleOutput;
import com.braintribe.utils.FileTools;
import com.braintribe.utils.lcd.StringTools;

public class GradleAntContext {

	private final String loaderref = "devrock-ant-loader";
	private boolean loaderrefDefined = false;
	private final String classpath;
	private final File antLibsFolder;
	private final File antCustomLibsFolder;
	private final boolean showStacktrace;

	private final Project project;
	private final Logger rootLogger = Logger.getLogger("");

	public GradleAntContext(String classpath, File antLibsFolder, File antCustomLibsFolder, boolean showStacktrace) {
		this.classpath = classpath;
		this.antLibsFolder = antLibsFolder;
		this.antCustomLibsFolder = antCustomLibsFolder;
		this.showStacktrace = showStacktrace;

		this.project = new Project();
		this.project.init();
	}

	public void taskdef(String resource) {
		taskdef(null, resource);
	}

	public void taskdef(String uri, String resource) {
		Taskdef taskdef = new Taskdef();
		taskdef.setProject(project);
		taskdef.setResource(resource);

		if (uri != null)
			taskdef.setURI(uri);

		if (!loaderrefDefined) {
			taskdef.setClasspath(new Path(project, classpath));
			loaderrefDefined = true;
		}

		taskdef.setLoaderRef(new Reference(project, loaderref));

		taskdef.execute();
	}

	public void executeAntTask(AntTaskContext taskCtx) {
		DrAnt ant = createAnt(taskCtx);

		for (Entry<String, String> e : taskCtx.properties.entrySet())
			createPropety(ant, e.getKey(), e.getValue());

		createPropety(ant, "ant.custom.library.dir", antCustomLibsFolder.getAbsolutePath());
		createPropety(ant, "ant.library.dir", antLibsFolder.getAbsolutePath());

		bindSubProjectToCurrentThread(ant.getNewProject());
		try {
			ant.execute();

		} finally {
			unbindSubProjectFromCurrentThread();
		}
	}

	private void createPropety(DrAnt ant, String key, String value) {
		Property property = ant.createProperty();
		property.setName(key);
		property.setValue(value);
	}

	private DrAnt createAnt(AntTaskContext taskCtx) {
		DrAnt antTask = new DrAnt();
		antTask.setProject(project);
		antTask.setTaskType("ant"); // Who knows?
		antTask.init();

		antTask.setDir(taskCtx.artifactDir);
		antTask.setTarget(taskCtx.target);
		antTask.setInheritAll(false);
		antTask.setInheritRefs(false);
		antTask.setOutput(taskCtx.outputFile.getAbsolutePath());

		// offline
		Property offlineProperty = antTask.createProperty();
		offlineProperty.setName("offline");
		offlineProperty.setValue(String.valueOf(false)); // TODO review

		return antTask;
	}

	// @formatter:off
	class DrAnt extends Ant {
		@Override public Project getNewProject() { return super.getNewProject(); }
	}
	// @formatter:on

	// #####################################################
	// ## . . . . Logging and System.out handling . . . . ##
	// #####################################################

	public void onAfterStepEvaluated() {
		cleanupLogHandler();
		cleanupDemuxOutputStream();
		printFailedBuilds();
	}

	private volatile boolean configuredDemuxing = false;

	private DrDemuxOutputStream btDemuxOut, btDemuxErr;
	private PrintStream originalOut, originalErr;
	private Handler originalHandler;

	private void bindSubProjectToCurrentThread(Project project) {
		ensureDemuxing();

		btDemuxOut.bindProjectToCurrentThread(project);
		btDemuxErr.bindProjectToCurrentThread(project);
	}

	private void ensureDemuxing() {
		if (!configuredDemuxing)
			synchronized (this) {
				if (!configuredDemuxing) {
					prepareDrDemuxOutputStream();
					prepareConsoleLogHandler();
					configuredDemuxing = true;
				}
			}
	}

	/**
	 * We install the console log handler to ensure logging from ant-tasks ends up in the configured output file. Otherwise it's going straight to the
	 * console.
	 */
	private void prepareConsoleLogHandler() {
		originalHandler = setLogHandler(new ConsoleHandler());
	}

	private void unbindSubProjectFromCurrentThread() {
		btDemuxOut.unbindCurrentThread();
		btDemuxErr.unbindCurrentThread();
	}

	/** @see DrDemuxOutputStream */
	private void prepareDrDemuxOutputStream() {
		originalOut = System.out;
		originalErr = System.err;

		btDemuxOut = new DrDemuxOutputStream(originalOut, false);
		btDemuxErr = new DrDemuxOutputStream(originalErr, true);

		System.setOut(new PrintStream(btDemuxOut));
		System.setErr(new PrintStream(btDemuxErr));
	}

	private void cleanupDemuxOutputStream() {
		if (configuredDemuxing) {
			System.setOut(originalOut);
			System.setErr(originalErr);
			configuredDemuxing = false;
		}
	}

	/** @see #prepareConsoleLogHandler() */
	private void cleanupLogHandler() {
		if (originalHandler != null) {
			setLogHandler(originalHandler);
			originalHandler = null;
		}
	}

	private Handler setLogHandler(Handler handler) {
		Handler[] handlers = rootLogger.getHandlers();
		if (handlers.length > 1)
			return null;

		Handler result = handlers[0];
		rootLogger.removeHandler(result);
		rootLogger.addHandler(handler);

		return result;
	}

	private final Set<AntTaskContext> failedBuilds = newConcurrentSet();

	public void printReport(AntTaskContext taskCtx) {
		if (taskCtx.buildException != null)
			failedBuilds.add(taskCtx);
		else
			printReportNow(taskCtx);
	}

	private void printFailedBuilds() {
		if (failedBuilds.isEmpty())
			return;

		for (AntTaskContext failedBuild : failedBuilds)
			printReportNow(failedBuild);

		println("");
		println(brightRed("Following builds failed (see details above):"));
		for (AntTaskContext failedBuild : failedBuilds)
			println(cyan("\t" + failedBuild.artifactDir.getName()));

		println("");
		println(sequence( //
				text("\tTO SKIP ALREADY BUILT ARTIFACTS use '"), //
				cyan("-Pskip=true"), //
				text("' PARAMETER after fixing the issue.\n\t"), //
				text("The list of artifacts you've already built will be read from a temp file.") //
		));

		failedBuilds.clear();
	}

	private void printReportNow(AntTaskContext taskCtx) {
		String logOutput = FileTools.read(taskCtx.outputFile).asString();

		String artifactId = taskCtx.artifactDir.getName();

		println(sequence( //
				text("===================================================\n"), //
				white("Executing ant target "), brightYellow(taskCtx.target), text(" for "), cyan(artifactId), text("\n"), //
				text(logOutput), //
				brightRed(errorIfRelevant(taskCtx)), //
				duration(taskCtx.durationMs, artifactId), //
				text("\n") //
		));
		taskCtx.outputFile.delete();
	}

	/* This is here, because in some cases Ant doesn't log the error, but only throws a BuildException (e.g. when instantiating a POJO in build.xml
	 * using an invalid property name.) */
	private ConsoleOutput errorIfRelevant(AntTaskContext taskCtx) {
		Exception e = taskCtx.buildException;
		if (e == null)
			return text("");

		ConsoleOutput errorOutput = showStacktrace ? stackTrace(e) : text(e.getMessage());

		return sequence( //
				text("\nBUILD FAILED WITH:\n"), //
				errorOutput, //
				text("\n\n") //
		);
	}

	private ConsoleOutput duration(long durationMs, String folderName) {
		if (durationMs < 0)
			return text("");

		String duration = StringTools.prettyPrintMilliseconds(durationMs, true);
		return sequence(text("Building "), cyan(folderName), text(" took "), yellow(duration));
	}

}

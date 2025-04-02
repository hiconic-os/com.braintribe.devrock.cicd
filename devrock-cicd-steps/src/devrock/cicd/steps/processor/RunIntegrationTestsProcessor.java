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
package devrock.cicd.steps.processor;

import static com.braintribe.console.ConsoleOutputs.cyan;
import static com.braintribe.console.ConsoleOutputs.println;
import static com.braintribe.console.ConsoleOutputs.sequence;
import static com.braintribe.console.ConsoleOutputs.text;
import static com.braintribe.console.ConsoleOutputs.yellow;
import static com.braintribe.utils.lcd.CollectionTools2.concat;
import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.braintribe.console.ConsoleOutputs;
import com.braintribe.console.output.ConsoleOutput;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.essential.InternalError;
import com.braintribe.logging.Logger;
import com.braintribe.model.processing.service.api.ReasonedServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.utils.FileTools;
import com.braintribe.utils.OsTools;

import devrock.cicd.model.api.RunIntegrationTests;
import devrock.cicd.model.api.RunTest;
import devrock.cicd.model.api.RunTestsResponse;
import devrock.cicd.model.api.data.CodebaseAnalysis;
import devrock.cicd.model.api.data.LocalArtifact;
import devrock.cicd.steps.processing.BuildHandlers;
import devrock.process.execution.ProcessExecution;

public class RunIntegrationTestsProcessor implements ReasonedServiceProcessor<RunIntegrationTests, RunTestsResponse> {

	@Override
	public Maybe<? extends RunTestsResponse> processReasoned(ServiceRequestContext context, RunIntegrationTests request) {
		return new IntegrationTestsRunner(context, request).run();
	}

}

class IntegrationTestsRunner {

	private final Function<LocalArtifact, Maybe<?>> handler;
	private final CodebaseAnalysis codebaseAnalysis;
	private final List<LocalArtifact> tests;
	private final File workDir;
	private final File outLogsDir;

	private Reason error;
	private Reason shutdownError;

	private LocalArtifact currentTest;
	private String currentSetupDep;
	private File currentSetupDirectory;
	private File currentTomcatBinDir;
	private File currentTomcatLogsDir;

	private static final int SERVER_START_TIMEOUT_SEC = 120;
	private static final int SERVER_STOP_TIMEOUT_SEC = 60;

	private static final int PORT = 8080;

	private static final String SHUTDOWN_CMD = "SHUTDOWN";

	private static final Logger log = Logger.getLogger(IntegrationTestsRunner.class);

	public IntegrationTestsRunner(ServiceRequestContext context, RunIntegrationTests request) {
		this.handler = BuildHandlers.getHandler(context, request, RunTest.T);
		this.codebaseAnalysis = request.getCodebaseAnalysis();
		this.tests = codebaseAnalysis.getIntegrationTests();
		this.workDir = new File(request.getWorkingDirectory());
		this.outLogsDir = new File(workDir, "logs");
	}

	public Maybe<? extends RunTestsResponse> run() {
		printInfo();

		try {
			FileTools.ensureDirectoryExists(workDir);
		} catch (Exception e) {
			return InternalError.from(e, "Error while ensuring working directory: " + pathOf(workDir)).asMaybe();
		}

		for (LocalArtifact test : tests) {
			runIntegrationTest(test);
			if (error != null)
				return error.asMaybe();

		}

		return Maybe.complete(RunTestsResponse.T.create());
	}

	private void printInfo() {
		ConsoleOutput prefix = sequence( //
				text("Running integration tests in working dir: "), yellow(pathOf(workDir)), text(":\n\t") //
		);

		ConsoleOutput info = tests.stream() //
				.map(test -> yellow(test.getFolderName())) //
				.collect(ConsoleOutputs.joiningCollector(text("\n\t"), prefix, null));

		println(info);
	}

	private void runIntegrationTest(LocalArtifact test) {
		println(sequence(text("\nRunning: "), yellow(test.getFolderName())));

		boolean result = true && //
				prepareContext(test) && //
				setupServer() && //
				startServer_run_stopServer() //
		;

		copyLogs();

		if (!result)
			println(sequence(text("\nSomething went wrong while running: "), yellow(test.getFolderName())));
	}

	private boolean prepareContext(LocalArtifact test) {
		String setupArtifactId = test.getFolderName() + "-setup";

		currentTest = test;

		currentSetupDep = codebaseAnalysis.getGroupId() + ":" + setupArtifactId + "#" + codebaseAnalysis.getGroupVersion();

		currentSetupDirectory = new File(workDir, setupArtifactId);

		Path hostPath = currentSetupDirectory.toPath().resolve("runtime").resolve("host");

		currentTomcatBinDir = hostPath.resolve("bin").toFile();
		currentTomcatLogsDir = hostPath.resolve("logs").toFile();

		return true;
	}

	private boolean setupServer() {
		println(sequence(text("\nSetting up: "), yellow(currentSetupDep)));

		return runJinni( //
				"setup-local-tomcat-platform", //
				"--setupDependency", currentSetupDep, //
				"--installationPath", currentSetupDirectory.getAbsolutePath(), //
				"--shutdownCommand", SHUTDOWN_CMD //
		);
	}

	private boolean runJinni(String... cmd) {
		Maybe<String> resultMaybe = ProcessExecution.buildCommand(workDir, jinniCmds(cmd))//
				.withInheritIo(true) //
				.runReasoned();

		error = resultMaybe.whyUnsatisfied();

		return error == null;
	}

	private ProcessHandle tomcatProcessHandle;

	private boolean startServer_run_stopServer() {
		println(sequence(text("Starting the "), cyan("Hiconic"), text(" server...")));

		Maybe<String> resultMaybe = ProcessExecution.buildCommand(currentTomcatBinDir, catalinaStartCmds())//
				.withInheritIo(true) //
				.withTask(p -> runTest_And_StopServer(p)) //
				.runReasoned();

		if (error != null)
			return false;

		error = resultMaybe.whyUnsatisfied();

		return error == null;
	}

	private void runTest_And_StopServer(Process p) throws Exception {
		try {
			waitForTomcatToStart();

			noteTomcatProcessHandle(p);

			waitForHiconicAppToStart();

			runTest();

		} finally {
			shutdownTomcat();
			// Maybe<String> stopMaybe = ProcessExecution.buildCommand(currentTomcatBinDir, catalinaStopCmds())//
			// .withInheritIo(true) //
			// .runReasoned();
			//
			// if (stopMaybe.isUnsatisfied()) {
			// error = Reasons.build(TestsFailed.T).cause(stopMaybe.whyUnsatisfied()).toReason();
			// }

			if (shutdownError == null)
				waitForTomcatToStop_Or_DestroyJvmProcess();
			else if (error == null)
				// If we had no error, we fail because of shutdown error
				error = shutdownError;
		}
	}

	private void noteTomcatProcessHandle(Process p) {
		try {
			tomcatProcessHandle = p.descendants() //
					.filter(ph -> ph.info() //
							.command() //
							.map(cmd -> cmd.contains("java")) //
							.orElse(false))//
					.findFirst() //
					.orElse(null);

			if (tomcatProcessHandle != null)
				println(sequence(text("Tomcat running with pid: "), yellow("" + tomcatProcessHandle.pid())));
			else {
				println(sequence(text("Tomcat process not found. Processes:")));
				println(p.descendants() //
						.map(ph -> ph.info().command().orElse("")) //
						.map(s -> text(s)) //
						.collect(ConsoleOutputs.joiningCollector(text("\n"))) //
				);
			}

		} catch (Exception e) {
			log.warn("Error while getting Tomcat process handle", e);
		}
	}

	private static void waitForHiconicAppToStart() throws MalformedURLException, URISyntaxException {
		println(sequence(text("Waiting for "), cyan("Hiconic"), text(" app to start...")));

		URL serverUrl = new URI("http://localhost:8080/tribefire-services").toURL();

		final int TIMEOUT_SECONDS = 5 * 60;

		try {
			HttpURLConnection connection = (HttpURLConnection) serverUrl.openConnection();
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(10_000); // connections should happen quickly
			connection.setReadTimeout(TIMEOUT_SECONDS * 1000); // server should respond within 5 minutes
			connection.connect();

			int responseCode = connection.getResponseCode();
			if (responseCode != 200)
				throw new RuntimeException("Hiconic server started, but did not respond with 200. Response code: " + responseCode);

		} catch (IOException ignored) {
			throw new RuntimeException("Tomcat failed to start the server within " + TIMEOUT_SECONDS + " seconds");
		}
	}

	private void waitForTomcatToStart() throws InterruptedException {
		waitForTomcat(true, SERVER_START_TIMEOUT_SEC);
	}

	private void runTest() {
		println(sequence(text("\nServer started. Running test: "), yellow(currentTest.getFolderName())));

		Maybe<?> testResultMaybe = handler.apply(currentTest);
		error = testResultMaybe.whyUnsatisfied();
	}

	private void shutdownTomcat() {
		println(sequence(text("Stopping the "), cyan("Hiconic"), text(" server...")));

		try (Socket socket = new Socket("localhost", 8005); OutputStream out = socket.getOutputStream()) {
			// Send the shutdown command
			out.write(SHUTDOWN_CMD.getBytes());
			out.flush();
			println(sequence(text("Shutdown command successfully sent to "), cyan("Tomcat")));

		} catch (Exception e) {
			e.printStackTrace();
			shutdownError = InternalError.from(e, "Shutting down Tomcat failed");
		}
	}

	private void waitForTomcatToStop_Or_DestroyJvmProcess() throws InterruptedException {
		waitForTomcat(false, SERVER_STOP_TIMEOUT_SEC);

		if (tomcatProcessHandle != null)
			checkIfTomcatProcessNotAlive();
		else
			/* In GitHub we don't have the tomcatProcessHandle, but locally the server sometimes needed a little extra time until the process was
			 * really terminated, so let's wait just in case */
			sleepOneSecond();

		println(text("Server is stopped."));
	}

	private void waitForTomcat(boolean waitingForStart, int maxAttempts) throws InterruptedException {
		String startOrStop = waitingForStart ? "start" : "stop";
		println(text("Waiting for Server to " + startOrStop + " for up to " + maxAttempts + " seconds."));

		int attempt = 0;

		while (doesServerRun() != waitingForStart) {
			if (attempt > 0 && attempt % 10 == 0)
				println(text(attempt + " seconds and still nothing..."));

			if (attempt++ >= maxAttempts)
				throw new RuntimeException("Tomcat failed to " + startOrStop + " within " + maxAttempts + " seconds");

			sleepOneSecond();
		}
	}

	private boolean doesServerRun() {
		try (Socket socket = new Socket("localhost", PORT)) {
			return true;

		} catch (IOException e) {
			return false;
		}
	}

	private void checkIfTomcatProcessNotAlive() throws InterruptedException {
		println(sequence(text("JVM process is alive: "), yellow("" + tomcatProcessHandle.isAlive())));
		if (!tomcatProcessHandle.isAlive())
			return;

		println(text("Waiting a little longer for the JVM process to finish."));
		waitForTomcatProcessToDie();

		if (!tomcatProcessHandle.isAlive())
			return;

		println(text("JVM process still didn't finish, destroying forcibly now!"));
		try {
			tomcatProcessHandle.destroyForcibly();

		} catch (RuntimeException e) {
			println(text("Error while destroying JVM process forcibly."));
			e.printStackTrace();
			if (!tomcatProcessHandle.isAlive())
				throw e;

			println(text("Despite the error, the JVM process is not alive anymore, so we assume it's fine and continue."));
		}
	}

	private void waitForTomcatProcessToDie() throws InterruptedException {
		// This should be fast, but just in case
		int maxSeconds = SERVER_STOP_TIMEOUT_SEC / 2;
		for (int i = 0; i < maxSeconds; i++) {
			sleepOneSecond();
			if (!tomcatProcessHandle.isAlive())
				return;
		}
	}

	private void copyLogs() {
		if (!currentTomcatLogsDir.isDirectory()) {
			println(sequence(text("Logs directory doesn't exist: "), yellow(pathOf(currentTomcatLogsDir))));
			return;
		}

		File currentOutLogsDir = new File(outLogsDir, currentTest.getFolderName());

		println(sequence(text("Copying logs from\n\t"), yellow(pathOf(currentTomcatLogsDir)), text("\nto\n\t"), yellow(pathOf(currentOutLogsDir))));

		FileTools.copy(currentTomcatLogsDir).as(currentOutLogsDir).please();
	}

	private List<String> catalinaStartCmds() {
		return catalinaCmds("run");
	}

	// private List<String> catalinaStopCmds() {
	// return catalinaCmds("stop", "" + SERVER_STOP_TIMEOUT_SEC);
	// }

	private List<String> catalinaCmds(String... cmds) {
		return concat( //
				catalinaForOs(), //
				Arrays.asList(cmds) //
		);
	}

	private static final List<String> CATALINA_CMDS_WIN = asList("cmd.exe", "/c", "catalina.bat");
	private static final List<String> CATALINA_CMDS_UNIX = asList("sh", "catalina.sh");

	private List<String> catalinaForOs() {
		return OsTools.isUnixSystem() ? CATALINA_CMDS_UNIX : CATALINA_CMDS_WIN;
	}

	private List<String> jinniCmds(String... cmds) {
		return concat( //
				jinniForOs(), //
				Arrays.asList(cmds), //
				jinniOptions() //
		);
	}

	private static final List<String> JINNI_CMDS_WIN = asList("cmd.exe", "/c", "jinni.bat");
	private static final List<String> JINNI_CMDS_UNIX = asList("jinni");

	private List<String> jinniForOs() {
		return OsTools.isUnixSystem() ? JINNI_CMDS_UNIX : JINNI_CMDS_WIN;
	}

	private List<String> jinniOptions() {
		// TODO only if we're in ANSI console, but not GH actions, as the console supports color but not overwriting output (e.g. DL monitor)
		return Arrays.asList(":", "options", "--colored", "false");
	}

	private void sleepOneSecond() throws InterruptedException {
		Thread.sleep(Duration.ofSeconds(1L));
	}

	private String pathOf(File f) {
		try {
			return f.getCanonicalPath();
		} catch (IOException e) {
			return f.getAbsolutePath();
		}
	}

}

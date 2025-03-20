package devrock.cicd.steps.processor;

import static com.braintribe.utils.SysPrint.spOut;
import static com.braintribe.utils.lcd.CollectionTools2.concat;
import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.utils.OsTools;

import devrock.process.execution.ProcessExecution;

/**
 * @author peter.gazdik
 */
public class StartTomcat {

	private static File currentTomcatBinDir = new File(
			"C:\\devrock-sdk\\env\\hiconic\\git\\core\\integration-test-setups\\tribefire-services-integration-test-setup\\runtime\\host\\bin");

	public static void main(String[] args) throws Exception {
		new StartTomcat().run();
	}

	private ProcessHandle tomcatProcessHandle;

	private void run() throws Exception {
		Maybe<String> resultMaybe = ProcessExecution.buildCommand(currentTomcatBinDir, catalinaStartCmds())//
				.withInheritIo(true) //
				.withTask(p -> runTest(p)) //
				.runReasoned();

		spOut("The whole run thing: " + resultMaybe.get());
	}

	private void runTest(Process p) throws Exception {
		waitForTomcatToStart();

		notTomcatProcessHandle(p);

		waitForHiconicAppToStart();

		Maybe<String> resultMaybe = ProcessExecution.buildCommand(currentTomcatBinDir, catalinaStopCmds())//
				.withInheritIo(true) //
				.runReasoned();

		spOut("Stopping server is done: " + resultMaybe.get());

		waitForTomcatToStop();

		spOut("Server runs: " + doesServerRuns());
		spOut("--------");

	}

	private void notTomcatProcessHandle(Process p) {
		try {
			tomcatProcessHandle = p.descendants() //
					.filter(ph -> ph.info() //
							.command() //
							.map(cmd -> cmd.contains("java")) //
							.orElse(false))//
					.peek(ph -> spOut("Tomcat JVM: " + ph.info().command().get())) //
					.findFirst() //
					.orElse(null);

			if (tomcatProcessHandle != null) {
				spOut("Tomcat Process:" + tomcatProcessHandle);
				spOut("Tomcat JVM: " + tomcatProcessHandle.info().command().get());

				// boolean destroy = tomcatProcessHandle.destroyForcibly();
				// spOut("JVM destroy: " + destroy);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void waitForTomcatToStart() throws InterruptedException {
		spOut("Waiting for Tomcat to start");

		int port = 8080;
		int maxAttempts = 20;
		int attempt = 0;

		while (attempt < maxAttempts) {
			try (Socket socket = new Socket("localhost", port)) {
				System.out.println("Tomcat is running on port " + port);
				return;

			} catch (IOException e) {
				if (attempt++ > maxAttempts)
					throw new RuntimeException("Tomcat failed to start within " + maxAttempts + " seconds");

				TimeUnit.SECONDS.sleep(1);
			}
		}
	}

	private static void waitForHiconicAppToStart() throws MalformedURLException, URISyntaxException {
		URL serverUrl = new URI("http://localhost:8080/tribefire-services").toURL();

		final int TIMEOUT_SECONDS = 5 * 60;
		
		try {
			HttpURLConnection connection = (HttpURLConnection) serverUrl.openConnection();
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(10_000); // connections should happen quickly
			connection.setReadTimeout(TIMEOUT_SECONDS * 1000); // server should respond within 5 minutes
			connection.connect();

			int responseCode = connection.getResponseCode();
			if (responseCode == 200) {
				System.out.println("✅ Tomcat is up!");
				return;
			}

		} catch (IOException ignored) {
			throw new RuntimeException("Tomcat failed to start the server within " + TIMEOUT_SECONDS + " seconds");
		}
	}

	private void waitForTomcatToStop() throws InterruptedException {
		spOut("Waiting for Tomcat to stop");

		int maxAttempts = 5 * 60;
		int attempt = 0;

		while (doesServerRuns()) {
			if (attempt++ >= maxAttempts) {
				System.out.println("Tomcat failed to stop within " + maxAttempts + " seconds");
				return;
			}
			Thread.sleep(1000);
		}

		spOut("Tomcat should be stopped.");
		spOut("Tomcat is alive: " + tomcatProcessHandle.isAlive());
	}

	private boolean doesServerRuns() {
		int port = 8080;

		try (Socket socket = new Socket("localhost", port)) {
			return true;

		} catch (IOException e) {
			return false;
		}
	}

	private List<String> catalinaStartCmds() {
		// start -> starts Tomcat in new window
		// run -> starts Tomcat in current window
		return catalinaCmds("run");
		// return catalinaCmds("start");
	}

	private List<String> catalinaStopCmds() {
		return catalinaCmds("stop", "60");
	}

	private List<String> catalinaCmds(String... cmds) {
		return concat( //
				catalinaForOs(), //
				Arrays.asList(cmds) //
		);
	}

	private static final List<String> CATALINA_CMDS_WIN = asList("cmd.exe", "/c", "catalina.bat");
	private static final List<String> CATALINA_CMDS_UNIX = asList("catalina.sh");

	private List<String> catalinaForOs() {
		return OsTools.isUnixSystem() ? CATALINA_CMDS_UNIX : CATALINA_CMDS_WIN;
	}

}

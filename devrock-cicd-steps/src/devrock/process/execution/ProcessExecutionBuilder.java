package devrock.process.execution;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.braintribe.common.lcd.function.XConsumer;
import com.braintribe.exception.Exceptions;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.UnsupportedOperation;
import com.braintribe.utils.IOTools;

import devrock.cicd.model.api.reason.CommandFailed;

public class ProcessExecutionBuilder {
	// Mandatory parameters
	public final List<String> cmd;
	public final File workingDirectory;

	// Optional parameters with defaults
	private String description = "";
	public Map<String, String> environment = null;
	public ProcessNotificationListener monitor = (t, m) -> {/* NO OP */};
	public boolean inheritIo = false;
	public XConsumer<Process> task;
	public Exception taskException;

	public ProcessExecutionBuilder(List<String> cmd, File workingDirectory) {
		this.cmd = cmd;
		this.workingDirectory = workingDirectory;
	}

	public ProcessExecutionBuilder withDescription(String description) {
		this.description = description;
		return this;
	}

	public ProcessExecutionBuilder withEnvironment(Map<String, String> environment) {
		this.environment = environment;
		return this;
	}

	public ProcessExecutionBuilder withMonitor(ProcessNotificationListener monitor) {
		this.monitor = monitor;
		return this;
	}

	public ProcessExecutionBuilder withInheritIo(boolean inheritIo) {
		this.inheritIo = inheritIo;
		return this;
	}

	/**
	 * Task which is executed after the process was started, but before we start waiting for it to terminate.
	 * <p>
	 * This is relevant e.g. for processes that need to be terminated in a special way, e.g. when starting a Tomcat. 
	 * <p>
	 * Note that should this task throw an exception, we destroy the process via {@link Process#destroyForcibly()}.
	 */
	public ProcessExecutionBuilder withTask(XConsumer<Process> task) {
		this.task = task;
		return this;
	}

	public Maybe<String> runReasoned() {
		ProcessResults results;

		try {
			results = run();
		} catch (UnsupportedOperationException e) {
			String command = cmd.stream().collect(Collectors.joining(" "));
			return Reasons.build(UnsupportedOperation.T).text("Command [" + command + "] is not supported: " + e.getMessage()).toMaybe();
		}

		if (results.getRetVal() != 0) {
			String command = cmd.stream().collect(Collectors.joining(" "));
			return Reasons.build(CommandFailed.T) //
					.assign(CommandFailed::setErrorMessage, results.getErrorText()) //
					.assign(CommandFailed::setErrorCode, results.getRetVal()) //
					.text("Command [" + command + "] execution in directory [" + workingDirectory.getAbsolutePath() + "] failed with error code ["
							+ results.getRetVal() + "]: " + results.getErrorText()) //
					.toMaybe();
		}

		return Maybe.complete(results.getNormalText());
	}

	public ProcessResults run() {
		ProcessBuilder processBuilder = new ProcessBuilder(cmd);

		if (workingDirectory != null) {
			processBuilder.directory(workingDirectory);
		}

		if (environment != null) {
			Map<String, String> processBuilderEnvironment = processBuilder.environment();
			processBuilderEnvironment.putAll(environment);
		}

		Process process = start(processBuilder);

		ProcessStreamReader errorReader = inheritIo ? //
				new ProcessStreamReader(process.getErrorStream(), System.err) : //
				new ProcessStreamReader(process.getErrorStream());

		ProcessStreamReader inputReader = inheritIo ? //
				new ProcessStreamReader(process.getInputStream(), System.out) : //
				new ProcessStreamReader(process.getInputStream());

		inputReader.setListener(monitor);
		errorReader.setListener(monitor);

		errorReader.start();
		inputReader.start();

		if (task != null)
			runTask(process);

		waitForOrDestroy(process);

		int retVal = process.exitValue();

		inputReader.cancel();
		errorReader.cancel();

		if (taskException != null)
			throw Exceptions.unchecked(taskException, "Error while running the configured task while executing process: " + description);

		return new ProcessResults(retVal, inputReader.getStreamResults(), errorReader.getStreamResults());
	}

	private Process start(ProcessBuilder processBuilder) {
		try {
			return processBuilder.start();
		} catch (IOException e1) {
			throw new UncheckedIOException(e1);
		}
	}

	private void runTask(Process process) {
		try {
			task.accept(process);
		} catch (Exception e) {
			taskException = e;
		}
	}

	private void waitForOrDestroy(Process process) {
		try {
			if (taskException != null)
				process.destroyForcibly();

			process.waitFor();

		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}
	}

}

class ProcessStreamReader extends Thread {
	private final InputStream in;
	private final StringBuilder buffer = new StringBuilder();
	private ProcessNotificationListener listener;
	private OutputStream redirect;

	public ProcessStreamReader(InputStream in) {
		this.in = in;
	}

	public ProcessStreamReader(InputStream in, OutputStream redirect) {
		this.in = in;
		this.redirect = redirect;
	}

	public void setListener(ProcessNotificationListener listener) {
		this.listener = listener;
	}

	@Override
	public void run() {
		if (redirect != null) {
			IOTools.transferBytes(in, redirect);
			return;
		}

		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, ProcessExecution.getConsoleEncoding()));
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (listener != null) {
					listener.acknowledgeProcessNotification(MessageType.info, line);
				}
				if (buffer.length() > 0)
					buffer.append('\n');
				buffer.append(line);
			}
		} catch (IOException e) {
			// ignored
		}
	}

	public String getStreamResults() {
		return buffer.toString();
	}

	public void cancel() {
		if (isAlive()) {
			interrupt();
			try {
				join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
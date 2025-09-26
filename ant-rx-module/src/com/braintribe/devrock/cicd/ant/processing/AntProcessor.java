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
package com.braintribe.devrock.cicd.ant.processing;

import static com.braintribe.console.ConsoleOutputs.brightRed;
import static com.braintribe.console.ConsoleOutputs.brightYellow;
import static com.braintribe.console.ConsoleOutputs.cyan;
import static com.braintribe.console.ConsoleOutputs.println;
import static com.braintribe.console.ConsoleOutputs.sequence;
import static com.braintribe.console.ConsoleOutputs.text;
import static com.braintribe.console.ConsoleOutputs.white;
import static com.braintribe.console.ConsoleOutputs.yellow;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.time.LocalTime;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.console.output.ConsoleOutput;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.model.processing.service.api.OutputConfig;
import com.braintribe.model.processing.service.api.OutputConfigAspect;
import com.braintribe.model.processing.service.impl.AbstractDispatchingServiceProcessor;
import com.braintribe.model.processing.service.impl.DispatchConfiguration;
import com.braintribe.model.service.api.result.Neutral;
import com.braintribe.utils.IOTools;
import com.braintribe.utils.collection.impl.AttributeContexts;
import com.braintribe.utils.lcd.StringTools;
import com.braintribe.utils.stream.api.StreamPipe;
import com.braintribe.utils.stream.api.StreamPipeFactory;

import devrock.ant.model.api.AntRequest;
import devrock.ant.model.api.RunAnt;
import devrock.ant.model.reason.AntBuildFailed;

public class AntProcessor extends AbstractDispatchingServiceProcessor<AntRequest, Object> {

	private static Lock bufferingLock = new ReentrantLock();
	private StreamPipeFactory streamPipeFactory;
	private File antLibDir;

	@Configurable
	public void setAntLibDir(File antLibDir) {
		this.antLibDir = antLibDir;
	}

	@Required
	public void setStreamPipeFactory(StreamPipeFactory streamPipeFactory) {
		this.streamPipeFactory = streamPipeFactory;
	}

	@Override
	protected void configureDispatching(DispatchConfiguration<AntRequest, Object> dispatching) {
		dispatching.registerReasoned(RunAnt.T, (c,r) -> runAnt(r));
	}

	interface Outputs extends Closeable {
		PrintStream out();
		PrintStream err();

		void notifyBuildException(BuildException e);
		void notifyDuration(long ms);

		@Override
		default void close() throws IOException {
			/* NOOP */
		}
	}

	class DirectOutputs implements Outputs {

		private final RunAnt request;
		private final File projectDir;

		public DirectOutputs(RunAnt request, File projectDir) {
			this.request = request;
			this.projectDir = projectDir;
		}

		// @formatter:off
		@Override public PrintStream err() { return System.err; }
		@Override public PrintStream out() { return System.out; }
		// @formatter:on

		@Override
		public void notifyDuration(long ms) {
			String ownerInfo = request.getOwnerInfo();
			if (ownerInfo == null)
				ownerInfo = projectDir.getName();

			println(duration(ms, ownerInfo));
		}

		@Override
		public void notifyBuildException(BuildException e) {
			println(brightRed(errorIfRelevant(e)));
		}
	}

	class BufferedOutputs implements Outputs {
		private final StreamPipe pipe;
		private final PrintStream ps;
		private final RunAnt request;
		private final File projectDir;
		private BuildException buildException;
		private long duration;

		public BufferedOutputs(RunAnt request, File projectDir) {
			this.request = request;
			this.projectDir = projectDir;
			pipe = streamPipeFactory.newPipe("ant-output-buffer");

			Charset charset = System.out.charset();

			ps = new PrintStream(pipe.openOutputStream(), false, charset);
		}

		// @formatter:off
		@Override public PrintStream err() { return ps; }
		@Override public PrintStream out() { return ps; }
		@Override public void notifyDuration(long ms) { this.duration = ms; }
		@Override public void notifyBuildException(BuildException e) { buildException = e; }
		// @formatter:on

		@Override
		public void close() throws IOException {
			ps.close();

			String ownerInfo = request.getOwnerInfo();
			if (ownerInfo == null)
				ownerInfo = projectDir.getName();

			bufferingLock.lock();

			try {
				println(sequence( //
						text("===================================================\n"), //
						white("Executing ant target "), brightYellow(request.getTarget()), text(" for "), cyan(ownerInfo), text("\n")));

				try (InputStream in = pipe.openInputStream()) {
					IOTools.transferBytes(in, System.out);
				}

				println(sequence( //
						brightRed(errorIfRelevant(buildException)), //
						duration(duration, ownerInfo), //
						text("\n") //
				));
			} finally {
				bufferingLock.unlock();
			}
		}
	}

	private ConsoleOutput duration(long durationMs, String folderName) {
		if (durationMs < 0)
			return text("");

		LocalTime currentTime = LocalTime.now();
		String hour = doubleDigits(currentTime.getHour());
		String minute = doubleDigits(currentTime.getMinute());
		String second = doubleDigits(currentTime.getSecond());

		String duration = StringTools.prettyPrintMilliseconds(durationMs, true);

		return sequence(text("Building "), cyan(folderName), text(" took "), yellow(duration), //
				text("\nFinished at: "), yellow(hour), text(":"), yellow(minute), text(":"), yellow(second));
	}

	private String doubleDigits(int i) {
		return (i < 10 ? "0" : "") + i;
	}

	/* This is here, because in some cases Ant doesn't log the error, but only throws a BuildException (e.g. when instantiating a POJO in build.xml
	 * using an invalid property name.) */
	private ConsoleOutput errorIfRelevant(BuildException e) {
		if (e == null)
			return text("");

		ConsoleOutput errorOutput = text(getErrorText(e));

		return sequence( //
				text("\nBUILD FAILED WITH:\n"), //
				errorOutput, //
				text("\n\n") //
		);
	}
	
	private String getErrorText(BuildException e) {
		OutputConfig oc = AttributeContexts.peek().findOrDefault(OutputConfigAspect.class, OutputConfig.empty);
		if (oc.verbose()) {
			StringWriter stringWriter = new StringWriter();
			PrintWriter printWriter = new PrintWriter(stringWriter);
			printWriter.append(e.getLocation().toString());
			e.printStackTrace(printWriter);
			printWriter.flush();
			
			return stringWriter.toString();
		}
		else
			return e.toString();
	}

	private Outputs openOutputs(RunAnt request, File projectDir) {
		if (!request.getBufferOutput())
			return new DirectOutputs(request, projectDir);

		return new BufferedOutputs(request, projectDir);
	}

	private Maybe<Neutral> runAnt(RunAnt request) {
		String buildXmlFile = request.getBuildFile();
		if (buildXmlFile == null)
			buildXmlFile = "build.xml";

		String p = request.getProjectDir();
		if (p == null)
			p = ".";

		File projectDir = new File(p);

		try (Outputs outputs = openOutputs(request, projectDir)) {
			Project project = new Project();

			DefaultLogger logger = new DefaultLogger();
			logger.setErrorPrintStream(outputs.err());
			logger.setOutputPrintStream(outputs.out());
			logger.setMessageOutputLevel(Project.MSG_INFO);

			project.addBuildListener(logger);
			project.init();

			if (antLibDir != null)
				project.setProperty("ant.library.dir", antLibDir.getAbsolutePath());

			// transfer properties
			for (Map.Entry<String, String> entry : request.getProperties().entrySet()) {
				project.setProperty(entry.getKey(), entry.getValue());
			}

			long start = System.currentTimeMillis();

			Demuxing.bindSubProjectToCurrentThread(project);

			try {
				ProjectHelper.configureProject(project, new File(projectDir, buildXmlFile));

				String target = request.getTarget();

				if (target == null)
					target = project.getDefaultTarget();

				if (target == null)
					return Reasons.build(InvalidArgument.T)
							.text("RunAnt.target must not be null for the project " + projectDir.getName() + " as it has no default target")
							.toMaybe();

				project.executeTarget(target);
			} catch (BuildException e) {
				outputs.notifyBuildException(e);
				return Reasons.build(AntBuildFailed.T).text(e.toString()).toMaybe();
			} finally {
				Demuxing.unbindSubProjectFromCurrentThread();

				long duration = System.currentTimeMillis() - start;
				outputs.notifyDuration(duration);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return Maybe.complete(Neutral.NEUTRAL);
	}
}

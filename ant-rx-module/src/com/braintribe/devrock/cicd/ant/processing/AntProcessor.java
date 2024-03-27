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
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;

import com.braintribe.cfg.Required;
import com.braintribe.console.output.ConsoleOutput;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.service.impl.AbstractDispatchingServiceProcessor;
import com.braintribe.model.processing.service.impl.DispatchConfiguration;
import com.braintribe.model.service.api.result.Neutral;
import com.braintribe.utils.IOTools;
import com.braintribe.utils.lcd.StringTools;
import com.braintribe.utils.stream.api.StreamPipe;
import com.braintribe.utils.stream.api.StreamPipeFactory;

import devrock.ant.model.api.AntRequest;
import devrock.ant.model.api.RunAnt;
import devrock.ant.model.reason.AntBuildFailed;

public class AntProcessor extends AbstractDispatchingServiceProcessor<AntRequest, Object>{
	
	private static Lock bufferingLock = new ReentrantLock();
	private StreamPipeFactory streamPipeFactory;
	
	
	@Required
	public void setStreamPipeFactory(StreamPipeFactory streamPipeFactory) {
		this.streamPipeFactory = streamPipeFactory;
	}
	
	@Override
	protected void configureDispatching(DispatchConfiguration<AntRequest, Object> dispatching) {
		dispatching.registerReasoned(RunAnt.T, this::runAnt);
		
	}
	
	interface Outputs extends Closeable {
		PrintStream out();
		PrintStream err();
		
		void notifyBuildException(BuildException e);
		void notifyDuration(long ms);
		
		@Override
		default void close() throws IOException {
		}
	}
	
	class DirectOutputs implements Outputs {
		
		private RunAnt request;
		private File projectDir;

		public DirectOutputs(RunAnt request, File projectDir) {
			this.request = request;
			this.projectDir = projectDir;
		}
		
		@Override
		public PrintStream err() {
			return System.err;
		}
		
		@Override
		public PrintStream out() {
			return System.out;
		}
		
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
		private StreamPipe pipe;
		private PrintStream ps;
		private RunAnt request;
		private File projectDir;
		private BuildException buildException;
		private long duration;
		
		public BufferedOutputs(RunAnt request, File projectDir) {
			this.request = request;
			this.projectDir = projectDir;
			pipe = streamPipeFactory.newPipe("ant-output-buffer");
			
			Charset charset = System.out.charset();
			
			ps = new PrintStream(pipe.openOutputStream(), false, charset);
		}
		
		@Override
		public PrintStream err() {
			return ps;
		}
		
		@Override
		public PrintStream out() {
			return ps;
		}
		
		@Override
		public void notifyDuration(long ms) {
			this.duration = ms;
		}
		
		@Override
		public void notifyBuildException(BuildException e) {
			buildException = e; 
		}
		
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
			}
			finally {
				bufferingLock.unlock();
			}
		}
	}
	
	private ConsoleOutput duration(long durationMs, String folderName) {
		if (durationMs < 0)
			return text("");

		String duration = StringTools.prettyPrintMilliseconds(durationMs, true);
		return sequence(text("Building "), cyan(folderName), text(" took "), yellow(duration));
	}
	
	/* This is here, because in some cases Ant doesn't log the error, but only throws a BuildException (e.g. when instantiating a POJO in build.xml
	 * using an invalid property name.) */
	private ConsoleOutput errorIfRelevant(BuildException e) {
		if (e == null)
			return text("");

		ConsoleOutput errorOutput = text(e.getMessage());

		return sequence( //
				text("\nBUILD FAILED WITH:\n"), //
				errorOutput, //
				text("\n\n") //
		);
	}

	
	private Outputs openOutputs(RunAnt request, File projectDir) {
		if (!request.getBufferOutput())
			return new DirectOutputs(request, projectDir);
		
		return new BufferedOutputs(request, projectDir);
	}
	
	public Maybe<Neutral> runAnt(ServiceRequestContext requestContext, RunAnt request) {
        Project project = new Project();
        project.init();
        
        System.err.charset();
        
        File projectDir = new File(request.getProjectDir());
        
        try (Outputs outputs = openOutputs(request, projectDir)) {
	        DefaultLogger consoleLogger = new DefaultLogger();
	        consoleLogger.setErrorPrintStream(outputs.err());
	        consoleLogger.setOutputPrintStream(outputs.out());
	        consoleLogger.setMessageOutputLevel(Project.MSG_INFO);
	
	        project.addBuildListener(consoleLogger);
	        
			project.setBaseDir(projectDir);
	
	        ProjectHelper projectHelper = ProjectHelper.getProjectHelper();
	        project.addReference("ant.projectHelper", projectHelper);
	
	        // transfer properties
	        for (Map.Entry<String, String> entry: request.getProperties().entrySet()) {
	        	project.setProperty(entry.getKey(), entry.getValue());
	        }
	        
	        projectHelper.parse(project, new File(projectDir, "build.xml"));
	
	        long start = System.currentTimeMillis();

	        Demuxing.bindSubProjectToCurrentThread(project);
	        
	        try {
	        	project.executeTarget(request.getTarget());
	        }
	        catch (BuildException e) {
	        	outputs.notifyBuildException(e);
	        	return Reasons.build(AntBuildFailed.T).text(e.getMessage()).toMaybe();
	        }
	        finally {
	        	Demuxing.unbindSubProjectFromCurrentThread();
	        	
	        	long duration = System.currentTimeMillis() - start;
	        	outputs.notifyDuration(duration);
	        }
        }
        catch (IOException e) {
        	throw new UncheckedIOException(e);
        }

        return Maybe.complete(Neutral.NEUTRAL);
	}
}

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
// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package devrock.process.execution;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.UnsupportedOperation;
import com.braintribe.utils.IOTools;

import devrock.cicd.model.api.reason.CommandFailed;

public class ProcessExecution {
	
	private static class ProcessStreamReader extends Thread {
		private InputStream in;
		private StringBuilder buffer = new StringBuilder();
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
			}
			else {
				try {
					BufferedReader reader = new BufferedReader(new InputStreamReader(in, getConsoleEncoding()));
					String line = null;				
					while ((line = reader.readLine()) != null) {
						if (listener != null) {
							listener.acknowledgeProcessNotification(MessageType.info, line);
						}
						if (buffer.length() > 0) buffer.append('\n');
						buffer.append(line);
					}
				}
				catch (InterruptedIOException e) {
				}
				catch (IOException e) {
					
				}
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
	
	public static Maybe<String> runScript(File cwd, boolean systemIo, String... scriptArgs) {
		return null;
	}
	
	public static Maybe<String> runCommand(File cwd, String ... cmd) {
		return runCommand(cwd, false, cmd);
	}
	
	public static Maybe<String> runCommand(File cwd, boolean systemIo, String ... cmd) {
		List<String> commands = Arrays.asList(cmd);
		ProcessResults results;
		
		try {
			results = runCommand(commands, cwd, null, (t,m) -> {}, systemIo);
		} catch (UnsupportedOperationException e) {
			String command = commands.stream().collect(Collectors.joining(" "));
			return Reasons.build(UnsupportedOperation.T).text("Command [" + command + "] is not supported: " + e.getMessage()).toMaybe();
		}
		
		if (results.getRetVal() != 0) {
			String command = commands.stream().collect(Collectors.joining(" "));
			return Reasons.build(CommandFailed.T) //
					.assign(CommandFailed::setErrorMessage, results.getErrorText()) //
					.assign(CommandFailed::setErrorCode, results.getRetVal()) //
					.text("Command [" + command + "] execution in directory [" + cwd.getAbsolutePath() + "] failed with error code [" + results.getRetVal() + "]: " + results.getErrorText()) //
					.toMaybe();
		}
		
		return Maybe.complete(results.getNormalText());
	}
	
	public static ProcessResults runCommand( ProcessNotificationListener listener, Map<String, String> environment, String ... cmd) {
		return runCommand(Arrays.asList( cmd), null, environment, listener);
	}
	
	public static ProcessResults runCommand( ProcessNotificationListener listener, File workingCopy, Map<String, String> environment, String ... cmd) {
		return runCommand(Arrays.asList( cmd), workingCopy, environment, listener);
	}
	
	public static ProcessResults runCommand(List<String> cmd, File workingDirectory, Map<String, String> environment, ProcessNotificationListener monitor) {
		return runCommand(cmd, workingDirectory, environment, monitor, false);
	}
	
	public static ProcessResults runCommand(List<String> cmd, File workingDirectory, Map<String, String> environment, ProcessNotificationListener monitor, boolean redirectIo) {
		ProcessBuilder processBuilder = new ProcessBuilder( cmd);
		
		if (workingDirectory != null) {
			processBuilder.directory( workingDirectory);
		}
					
		
		if (environment != null) {
			Map<String, String> processBuilderEnvironment = processBuilder.environment();
			processBuilderEnvironment.putAll(environment);
		}
		
							
		final Process process;
		try {
			process = processBuilder.start();
		} catch (IOException e1) {
			throw new UncheckedIOException(e1);
		}

		
		ProcessStreamReader errorReader = redirectIo? //
				new ProcessStreamReader(process.getErrorStream(), System.err): // 
				new ProcessStreamReader(process.getErrorStream());
		
		ProcessStreamReader inputReader = redirectIo? //
				new ProcessStreamReader(process.getInputStream(), System.out):
				new ProcessStreamReader(process.getInputStream());
		
		inputReader.setListener(monitor);
		errorReader.setListener(monitor);
		
		errorReader.start();
		inputReader.start();
		
		try {
			process.waitFor();
		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}
		
		int retVal = process.exitValue();

		inputReader.cancel();
		errorReader.cancel();

		return new ProcessResults(retVal, inputReader.getStreamResults(), errorReader.getStreamResults());
	}
	
	public static String getConsoleEncoding() {
		return "Cp850";
	}


}

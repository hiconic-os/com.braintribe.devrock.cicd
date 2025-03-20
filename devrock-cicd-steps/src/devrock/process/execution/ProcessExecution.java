// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
//
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
package devrock.process.execution;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.braintribe.gm.model.reason.Maybe;

public class ProcessExecution {

	public static Maybe<String> runCommand(File cwd, String... cmd) {
		return runCommand(cwd, false, cmd);
	}

	public static Maybe<String> runCommand(File cwd, boolean inheritIo, String... cmd) {
		return buildCommand(cwd, cmd) //
				.withInheritIo(inheritIo) //
				.runReasoned();
	}

	public static ProcessResults runCommand(ProcessNotificationListener listener, Map<String, String> environment, String... cmd) {
		return runCommand(Arrays.asList(cmd), null, environment, listener);
	}

	public static ProcessResults runCommand(ProcessNotificationListener listener, File workingCopy, Map<String, String> environment, String... cmd) {
		return runCommand(Arrays.asList(cmd), workingCopy, environment, listener);
	}

	public static ProcessResults runCommand( //
			List<String> cmd, File workingDirectory, Map<String, String> environment, ProcessNotificationListener monitor) {

		return runCommand(cmd, workingDirectory, environment, monitor, false);
	}

	public static ProcessResults runCommand(List<String> cmd, File workingDirectory, //
			Map<String, String> environment, ProcessNotificationListener monitor, boolean inheritIo) {

		return buildCommand(workingDirectory, cmd) //
				.withEnvironment(environment) //
				.withMonitor(monitor) //
				.withInheritIo(inheritIo) //
				.run();
	}

	public static ProcessExecutionBuilder buildCommand(File cwd, String... cmd) {
		return buildCommand(cwd, Arrays.asList(cmd));
	}

	public static ProcessExecutionBuilder buildCommand(File cwd, List<String> cmd) {
		return new ProcessExecutionBuilder(cmd, cwd);
	}

	public static String getConsoleEncoding() {
		return "Cp850";
	}

}

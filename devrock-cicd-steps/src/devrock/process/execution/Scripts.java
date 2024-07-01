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
package devrock.process.execution;

import java.io.File;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.utils.OsTools;

public abstract class Scripts {
	public static Reason run(File cwd, String... args) {
		if (OsTools.isWindowsOperatingSystem()) {
			String cmdArg = "call " + Stream.of(args).map(Scripts::escapeCmdArg).collect(Collectors.joining(" ")) + " && exit /b %errorlevel%";
			
			args = new String[]{
				"cmd", "/C", cmdArg	
			};
		}
		
		Maybe<String> maybe = ProcessExecution.runCommand(cwd, true, args);
		
		if (maybe.isUnsatisfied())
			return maybe.whyUnsatisfied();
		
		return null;
	}
	
	private static String escapeCmdArg(String arg) {
		if (arg.contains("\""))
			return "\"" + arg.replace("\"", "\\\"") + "\"";
		else
			return arg;
	}

}

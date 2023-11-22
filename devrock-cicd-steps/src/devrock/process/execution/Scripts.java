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

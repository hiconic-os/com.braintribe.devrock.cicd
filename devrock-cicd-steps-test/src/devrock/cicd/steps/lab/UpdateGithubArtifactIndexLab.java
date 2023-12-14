package devrock.cicd.steps.lab;

import java.io.File;

import com.braintribe.console.ConsoleConfiguration;
import com.braintribe.console.PrintStreamConsole;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.utils.FileTools;
import com.braintribe.wire.impl.properties.PropertyLookups;

import devrock.cicd.model.api.UpdateGithubArtifactIndex;
import devrock.step.api.StepEvaluator;
import devrock.step.framework.Steps;

public class UpdateGithubArtifactIndexLab {
	private static Secrets secrets = PropertyLookups.create(Secrets.class, System::getenv);
	
	public static void main(String[] args) {
		
		ConsoleConfiguration.install(new PrintStreamConsole(System.out, true));
		
		try {
			File exchangeFolder = new File("out/exchange");
			FileTools.deleteDirectoryRecursively(exchangeFolder);
			ConsoleConfiguration.install(new PrintStreamConsole(System.out, true));
			
			StepEvaluator evaluator = Steps.evaluator(exchangeFolder);
			
			UpdateGithubArtifactIndex request = UpdateGithubArtifactIndex.T.create();
			request.setOrganization("hiconic-os");
			request.setRepository("maven-repo-dev");
			request.setToken(secrets.githubToken());
			//request.setGroup("com.braintribe.gm");
			
			Reason reason = evaluator.evaluate(request);
			
			if (reason != null)
				System.out.println(reason.stringify());
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}

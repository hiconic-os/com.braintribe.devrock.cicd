package devrock.cicd.steps.processor;

import java.io.File;

import com.braintribe.gm.model.reason.Maybe;

import devrock.cicd.model.api.PublishArtifactsResponse;
import devrock.cicd.model.api.PublishNpmPackages;
import devrock.cicd.model.api.data.CodebaseAnalysis;
import devrock.cicd.model.api.data.LocalArtifact;
import devrock.process.execution.ProcessExecution;

public class PublishNpmPackagesProcessor extends SpawningServiceProcessor<PublishNpmPackages, PublishArtifactsResponse> {

	@Override
	protected StatefulServiceProcessor spawn() {
		return new StatefulServiceProcessor() {

			@Override
			protected Maybe<PublishArtifactsResponse> process() {
				CodebaseAnalysis analysis = request.getCodebaseAnalysis();

				File groupDir = new File(analysis.getBasePath());

				for (LocalArtifact localArtifact : analysis.getBuilds())
					if (localArtifact.getNpmPackage()) {
						Maybe<String> publishResult = publishNpmPackage(groupDir, localArtifact);
						if (publishResult.isUnsatisfied())
							return publishResult.whyUnsatisfied().asMaybe();
					}

				return Maybe.complete(PublishArtifactsResponse.T.create());
			}

			private Maybe<String> publishNpmPackage(File groupDir, LocalArtifact localArtifact) {
				File distDir = groupDir.toPath() //
						.resolve(localArtifact.getArtifactIdentification().getArtifactId()) //
						.resolve("dist") //
						.toFile();

				String npmCommand = resolveNpmCommand();

				for (File folder : distDir.listFiles()) {
					if (!folder.isDirectory())
						continue;

					// in case of GWT terminals we have 2 different packages - npm (obfuscated) and npm-debug (pretty)
					if (folder.getName().startsWith("npm")) {
						Maybe<String> resultMaybe = ProcessExecution.runCommand(folder, true, npmCommand, "publish");
						if (resultMaybe.isUnsatisfied())
							return resultMaybe;
					}
				}

				return Maybe.complete(null);
			}

			private String resolveNpmCommand() {
				boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");
				return isWindows ? "npm.cmd" : "npm";
			}

		};
	}
}

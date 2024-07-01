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
package devrock.cicd.steps.processor;

import static com.braintribe.console.ConsoleOutputs.brightBlack;
import static com.braintribe.console.ConsoleOutputs.sequence;
import static com.braintribe.console.ConsoleOutputs.text;

import java.io.File;
import java.util.Map;

import com.braintribe.console.ConsoleOutputs;
import com.braintribe.console.output.ConsoleOutput;
import com.braintribe.devrock.mc.core.commons.McOutputs;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.model.artifact.analysis.AnalysisArtifact;
import com.braintribe.model.artifact.essential.VersionedArtifactIdentification;
import com.braintribe.model.version.Version;

import devrock.cicd.model.api.PreparePublishing;
import devrock.cicd.model.api.PreparePublishingResponse;
import devrock.cicd.model.api.data.CodebaseAnalysis;
import devrock.cicd.model.api.data.CodebaseDependencyAnalysis;
import devrock.cicd.model.api.data.LocalArtifact;
import devrock.pom.PomTools;

public class PreparePublishingProcessor extends SpawningServiceProcessor<PreparePublishing, PreparePublishingResponse> {

	@Override
	protected StatefulServiceProcessor spawn() {
		return new StatefulCodebaseAnalysis();
	}
	
	private class StatefulCodebaseAnalysis extends StatefulServiceProcessor {
		
		private CodebaseAnalysis codebaseAnalysis;
		private CodebaseDependencyAnalysis dependencyAnalysis;

		@Override
		protected Maybe<? extends PreparePublishingResponse> process() {
			
			codebaseAnalysis = request.getCodebaseAnalysis();
			dependencyAnalysis = request.getCodebaseDependencyAnalysis();

			Reason error = adaptVersions();
			
			if (error != null)
				return error.asMaybe();

			PreparePublishingResponse response = PreparePublishingResponse.T.create();
			response.setAnalysis(codebaseAnalysis);
			response.setDependencyAnalysis(dependencyAnalysis);
			response.setDependencyResolution(dependencyAnalysis.getResolution());
			
			return Maybe.complete(response);
		}

		// raises the versions of all build artifacts in codebase, codebase-analysis and dependency-analysis
		private Reason adaptVersions() {
			Map<String, AnalysisArtifact> artifactIndex = dependencyAnalysis.getArtifactIndex();
			File groupDir = new File(codebaseAnalysis.getBasePath());
			
			for (LocalArtifact artifact: codebaseAnalysis.getBuilds()) {
				// read version from local artifact
				VersionedArtifactIdentification ai = artifact.getArtifactIdentification();
				Version version = Version.parse(ai.getVersion());
				
				ConsoleOutput versionAsBefore = McOutputs.version(version);
				
				version.setQualifier(null);
				String adaptedVersion = version.asString();

				// adapt local artifact
				ai.setVersion(adaptedVersion);
				artifact.setIdentification(ai.asString());
				
				// adapt analysis artifact
				AnalysisArtifact analysisArtifact = artifactIndex.get(ai.getArtifactId());
				analysisArtifact.setVersion(adaptedVersion);
				
				// pom
				File pomFile = new File(groupDir, "pom.xml");
				Reason error = PomTools.changeVersion(pomFile, artifact.getArtifactIdentification().getVersion());
				
				if (error != null)
					return error;
				
				ConsoleOutput versionAsAfter = McOutputs.version(version);
				
				ConsoleOutputs.println(sequence( //
					brightBlack("Raised "), //
					text(ai.getArtifactId()), //
					brightBlack(" from "), //
					versionAsBefore,
					brightBlack(" to "), //
					versionAsAfter
				));
			}
			
			return null;
		}
	}
}

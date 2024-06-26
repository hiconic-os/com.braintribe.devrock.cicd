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
package devrock.cicd.steps.test;

import java.io.File;
import java.util.List;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.utils.FileTools;

import devrock.cicd.model.api.AnalyzeCodebase;
import devrock.cicd.model.api.data.CodebaseAnalysis;
import devrock.cicd.model.api.data.LocalArtifact;
import devrock.step.api.StepEvaluator;
import devrock.step.framework.Steps;

public class DetectUnpublishedLab {
	
	
	public static void main(String[] args) {
		System.out.println(false | false);
		System.out.println(false | true);
		System.out.println(true | false);
		System.out.println(true | true);
	}
	
	public static void mainO(String[] args) {
		try {
			File groupDir = new File("C:\\devrock-sdk\\env\\public-github-test\\git\\com.braintribe.testing");
			File exchangeFolder = new File(groupDir, ".step-exchange");
			FileTools.deleteDirectoryRecursively(exchangeFolder);
			exchangeFolder.mkdirs();
			
			StepEvaluator evaluator = Steps.evaluator(groupDir, exchangeFolder, n -> null);
			
			AnalyzeCodebase request = AnalyzeCodebase.T.create();
			request.setDetectUnpublishedArtifacts(true);
			evaluator.evaluateOrThrow(request);
			Maybe<CodebaseAnalysis> changedArtifactsMaybe = evaluator.load(CodebaseAnalysis.T);
			CodebaseAnalysis changedArtifacts = changedArtifactsMaybe.get();
			
			List<LocalArtifact> artifacts = changedArtifacts.getArtifacts();
			
			System.out.println("=== All ===");
			for (LocalArtifact artifact: artifacts) {
				System.out.println(artifact.getIdentification() + " -> " + artifact.getBuildReason());
			}
			
			
			System.out.println("=== Builds ===");
			for (LocalArtifact artifact: changedArtifacts.getBuilds()) {
				System.out.println(artifact.getIdentification() + " -> " + artifact.getBuildReason());
			}
			
			
			List<LocalArtifact> linkChecks = changedArtifacts.getBuildLinkingChecks();
			
			System.out.println("=== Link Checks ===");
			for (LocalArtifact artifact: linkChecks) {
				System.out.println(artifact.getIdentification() + " -> " + artifact.getBuildReason());
			}
			
			List<LocalArtifact> tests = changedArtifacts.getBuildLinkingChecks();
			
			System.out.println("=== Link Tests ===");
			for (LocalArtifact artifact: tests) {
				System.out.println(artifact.getIdentification() + " -> " + artifact.getBuildReason());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}
}

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
package devrock.cicd.model.api;

import com.braintribe.model.artifact.analysis.AnalysisArtifactResolution;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

import devrock.cicd.model.api.data.CodebaseAnalysis;
import devrock.cicd.model.api.data.CodebaseDependencyAnalysis;
import devrock.cicd.model.api.data.GitContext;
import devrock.step.model.api.StepResponse;

public interface AnalyzeCodebaseResponse extends StepResponse {
	EntityType<AnalyzeCodebaseResponse> T = EntityTypes.T(AnalyzeCodebaseResponse.class);

	String analysis = "analysis";
	String dependencyAnalysis = "dependencyAnalysis";
	String dependencyResolution = "dependencyResolution";
	String gitContext = "gitContext";
	
	CodebaseAnalysis getAnalysis();
	void setAnalysis(CodebaseAnalysis analysis);
	
	CodebaseDependencyAnalysis getDependencyAnalysis();
	void setDependencyAnalysis(CodebaseDependencyAnalysis dependencyAnalysis);
	
	AnalysisArtifactResolution getDependencyResolution();
	void setDependencyResolution(AnalysisArtifactResolution dependencyResolution);
	
	GitContext getGitContext();
	void setGitContext(GitContext gitContext);
}

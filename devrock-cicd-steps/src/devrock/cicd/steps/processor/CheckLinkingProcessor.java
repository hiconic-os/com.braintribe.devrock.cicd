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

import java.util.function.Consumer;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.model.processing.service.api.ReasonedServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;

import devrock.cicd.model.api.CheckLinking;
import devrock.cicd.model.api.CheckLinkingResponse;
import devrock.cicd.model.api.RunCheckLinking;
import devrock.cicd.model.api.data.CodebaseAnalysis;
import devrock.cicd.model.api.data.CodebaseDependencyAnalysis;
import devrock.cicd.model.api.data.LocalArtifact;
import devrock.cicd.steps.processing.BuildHandlers;

public class CheckLinkingProcessor implements ReasonedServiceProcessor<CheckLinking, CheckLinkingResponse> {
	@Override
	public Maybe<? extends CheckLinkingResponse> processReasoned(ServiceRequestContext context,
			CheckLinking request) {
		Consumer<LocalArtifact> handler = BuildHandlers.getHandler(context, request, RunCheckLinking.T);
		
		if (handler == null)
			return Reasons.build(InvalidArgument.T).text("Transitive property BuildArtifact.handler must not be null").toMaybe();
		
		CodebaseAnalysis analysis = request.getCodebaseAnalysis();
		CodebaseDependencyAnalysis dependencyAnalysis = request.getCodebaseDependencyAnalysis();

		Integer threads = request.getThreads();

		Reason error = ParallelBuildSupport.runInParallel(analysis, dependencyAnalysis, analysis.getBuildLinkingChecks(), handler, threads, null);
		if (error != null)
			return error.asMaybe();
		
		CheckLinkingResponse response = CheckLinkingResponse.T.create();
		
		return Maybe.complete(response);
	}
}

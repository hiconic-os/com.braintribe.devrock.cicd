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
import devrock.cicd.model.api.data.CodebaseAnalysis;
import devrock.cicd.model.api.data.CodebaseDependencyAnalysis;
import devrock.cicd.model.api.data.LocalArtifact;

public class CheckLinkingProcessor implements ReasonedServiceProcessor<CheckLinking, CheckLinkingResponse> {
	@Override
	public Maybe<? extends CheckLinkingResponse> processReasoned(ServiceRequestContext context,
			CheckLinking request) {
		Consumer<LocalArtifact> handler = request.getHandler();
		
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

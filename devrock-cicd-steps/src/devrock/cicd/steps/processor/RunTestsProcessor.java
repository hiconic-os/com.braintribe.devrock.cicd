package devrock.cicd.steps.processor;

import java.util.List;
import java.util.function.Consumer;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.model.processing.service.api.ReasonedServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;

import devrock.cicd.model.api.RunTest;
import devrock.cicd.model.api.RunTests;
import devrock.cicd.model.api.RunTestsResponse;
import devrock.cicd.model.api.data.CodebaseAnalysis;
import devrock.cicd.model.api.data.LocalArtifact;
import devrock.cicd.steps.processing.BuildHandlers;

public class RunTestsProcessor implements ReasonedServiceProcessor<RunTests, RunTestsResponse> {
	@Override
	public Maybe<? extends RunTestsResponse> processReasoned(ServiceRequestContext context,
			RunTests request) {
		Consumer<LocalArtifact> handler = BuildHandlers.getHandler(context, request, RunTest.T);
		
		if (handler == null)
			return Reasons.build(InvalidArgument.T).text("Transitive property RunTests.handler must not be null").toMaybe();
		
		CodebaseAnalysis analysis = request.getCodebaseAnalysis();

		
		List<LocalArtifact> sequence = analysis.getBuildTests();

		sequence.forEach(handler);
		
		RunTestsResponse response = RunTestsResponse.T.create();
		
		return Maybe.complete(response);
	}
}

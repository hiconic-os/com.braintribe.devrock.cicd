package devrock.cicd.steps.processor;

import java.io.File;
import java.util.List;

import com.braintribe.common.lcd.Pair;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.processing.service.api.ReasonedServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;

import devrock.cicd.model.api.EnrichExchangeContext;
import devrock.cicd.model.api.EnrichExchangeContextResponse;
import devrock.git.CommentYamlInjectionParser;
import devrock.git.GitTools;
import devrock.step.api.StepExchangeContext;
import devrock.step.api.StepExchangeContextAttribute;

public class EnrichExchangeContextProcessor implements ReasonedServiceProcessor<EnrichExchangeContext, EnrichExchangeContextResponse> {

	@Override
	public Maybe<? extends EnrichExchangeContextResponse> processReasoned(ServiceRequestContext context,
			EnrichExchangeContext request) {
		
		Reason error = enhanceFromGitCommitMessage(context, request);
		
		if (error != null)
			return error.asMaybe();
		
		error = enhanceFromCommentInput(context, request);
		
		if (error != null)
			return error.asMaybe();
		
		EnrichExchangeContextResponse response = EnrichExchangeContextResponse.T.create();
		
		return Maybe.complete(response);
	}
	
	private Reason enhanceFromGitCommitMessage(ServiceRequestContext context, EnrichExchangeContext request) {
		File dir = new File(request.getGitPath());

		if (!GitTools.isGitCheckoutRoot(dir))
			return null;

		Maybe<String> commentMaybe = GitTools.getLatestCommitComment(dir);

		// We already know this is a git dir. Error here most likely means there is no commit yet in current branch, so we ignore it.
		if (commentMaybe.isUnsatisfied())
			return null;

		return enhance(context, commentMaybe.get());
	}
	
	private Reason enhanceFromCommentInput(ServiceRequestContext context, EnrichExchangeContext request) {
		String commentInput = request.getCommentInput();
		
		if (commentInput == null || commentInput.isBlank())
			return null;
		
		return enhance(context, commentInput);
	}

	private Reason enhance(ServiceRequestContext context, String text) {
		Maybe<List<Pair<GenericEntity, String>>> entitiesMaybe = CommentYamlInjectionParser.extractYamlSections(text);
		
		if (entitiesMaybe.isUnsatisfied())
			return entitiesMaybe.whyUnsatisfied();
		
		List<Pair<GenericEntity, String>> exchangeEntities = entitiesMaybe.get();
		
		StepExchangeContext exchangeContext = context.findOrNull(StepExchangeContextAttribute.class);
		
		for (Pair<GenericEntity, String> entry: exchangeEntities) {
			GenericEntity entity = entry.first();
			exchangeContext.store(entity.entityType(), entry.second(), entity);	
		}
		
		return null;
	}

}

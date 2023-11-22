package devrock.cicd.steps.processor;

import java.io.File;
import java.util.List;

import com.braintribe.common.lcd.Pair;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.processing.service.api.ReasonedServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;

import devrock.cicd.model.api.EnrichExchangeContext;
import devrock.cicd.model.api.EnrichExchangeContextResponse;
import devrock.cicd.model.api.reason.GitAnalysisFailure;
import devrock.git.CommentYamlInjectionParser;
import devrock.git.GitTools;
import devrock.step.api.StepExchangeContext;
import devrock.step.api.StepExchangeContextAttribute;

public class EnrichExchangeContextProcessor implements ReasonedServiceProcessor<EnrichExchangeContext, EnrichExchangeContextResponse> {

	@Override
	public Maybe<? extends EnrichExchangeContextResponse> processReasoned(ServiceRequestContext context,
			EnrichExchangeContext request) {
		
		Reason error = enhancePropertiesFromGitCommitMessage(context, request);
		
		if (error != null)
			return error.asMaybe();
		
		EnrichExchangeContextResponse response = EnrichExchangeContextResponse.T.create();
		
		return Maybe.complete(response);

	}
	
	private Reason enhancePropertiesFromGitCommitMessage(ServiceRequestContext context, EnrichExchangeContext request) {
		File gitPath = new File(request.getGitPath());
		
		// Not being associated with git is an expected state as you initially might start just with a unassociated local folder
		if (!GitTools.isGitCheckoutRoot(gitPath))
			return null;
		
		Maybe<String> commentMaybe = GitTools.getLatestCommitComment(gitPath);
		
		if (commentMaybe.isUnsatisfied()) {
			return Reasons.build(GitAnalysisFailure.T).text("Could not determine comment of latest commit for request customization").toReason();
		}
		
		Maybe<List<Pair<GenericEntity, String>>> entitiesMaybe = CommentYamlInjectionParser.extractYamlSections(commentMaybe.get());
		
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

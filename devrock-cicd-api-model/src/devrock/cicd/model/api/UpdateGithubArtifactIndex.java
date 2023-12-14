package devrock.cicd.model.api;

import com.braintribe.model.generic.annotation.meta.Confidential;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

import devrock.step.model.api.StepRequest;

public interface UpdateGithubArtifactIndex extends StepRequest {
	EntityType<UpdateGithubArtifactIndex> T = EntityTypes.T(UpdateGithubArtifactIndex.class);

	String token = "token";
	String organization = "organization";
	String repository = "repository";
	String group = "group";
	
	@Confidential
	@Mandatory
	@Description("The github authentication token")
	String getToken();
	void setToken(String token);
	
	@Mandatory
	@Description("The github organization")
	String getOrganization();
	void setOrganization(String organization);
	
	@Mandatory
	@Description("The github repository associated with the maven repo")
	String getRepository();
	void setRepository(String repository);
	
	@Description("Filters to update just artifacts from a certain groupId")
	String getGroup();
	void setGroup(String group);
	
	@Override
	EvalContext<? extends UpdateGithubArtifactIndexResponse> eval(Evaluator<ServiceRequest> evaluator);
}

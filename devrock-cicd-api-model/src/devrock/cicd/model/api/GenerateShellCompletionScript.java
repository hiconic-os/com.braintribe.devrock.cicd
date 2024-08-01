// ============================================================================
package devrock.cicd.model.api;

import java.util.Map;

import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.annotation.meta.FileName;
import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.annotation.meta.PositionalArguments;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.service.api.PlatformRequest;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.service.api.result.Neutral;

@PositionalArguments({ "file" })
@Description("Generates completion script for Shell that you need to \"source\" to use it.")
public interface GenerateShellCompletionScript extends PlatformRequest {

	EntityType<GenerateShellCompletionScript> T = EntityTypes.T(GenerateShellCompletionScript.class);

	@Description("The file to which the script should be written")
	@Mandatory
	Map<String, GmMetaModel> getDomainNameToModel();
	void setDomainNameToModel(Map<String, GmMetaModel> domainNameToModel);

	@Description("The name of the CLI command that should be completed.")
	@Mandatory
	String getCliCommand();
	void setCliCommand(String cliCommand);

	@Description("The file to which the script should be written")
	@Mandatory
	@FileName
	String getOutputFile();
	void setOutputFile(String outputFile);

	@Description("Specifies which of the argument's real name and aliases should be included as suggestions for completion.")
	@Initializer("realName")
	CliCompletionStrategy getArgumentNameCompletionStrategy();
	void setArgumentNameCompletionStrategy(CliCompletionStrategy argumentNameCompletionStrategy);

	@Override
	EvalContext<Neutral> eval(Evaluator<ServiceRequest> evaluator);

}

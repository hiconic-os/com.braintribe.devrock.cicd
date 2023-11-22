package devrock.cicd.github.notification.wire.space;

import com.braintribe.devrock.cicd._GithubNotificationApiModel_;
import com.braintribe.model.processing.meta.configured.ConfigurationModelBuilder;
import com.braintribe.model.processing.meta.editor.ModelMetaDataEditor;
import com.braintribe.model.processing.service.common.ConfigurableDispatchingServiceProcessor;
import com.braintribe.wire.api.annotation.Managed;

import devrock.cicd.github.notification.model.api.GitHubNotificationConnection;
import devrock.cicd.github.notification.model.api.GitHubNotificationMapping;
import devrock.cicd.github.notification.processing.GitHubLabelNotifier;
import devrock.step.api.module.wire.StepModuleContract;
import devrock.step.model.api.StepRequest;
import devrock.step.model.api.meta.ExchangeConfiguration;

@Managed
public class DevrockCicdGitHubNotificationSpace implements StepModuleContract {

	@Override
	public void addApiModels(ConfigurationModelBuilder builder) {
		builder.addDependency(_GithubNotificationApiModel_.reflection);
	}
	
	@Override
	public void configureApiModel(ModelMetaDataEditor editor) {
		ExchangeConfiguration evaluatePlaceholders = ExchangeConfiguration.T.create();
		editor.onEntityType(GitHubNotificationConnection.T).addMetaData(evaluatePlaceholders);
		editor.onEntityType(GitHubNotificationMapping.T).addMetaData(evaluatePlaceholders);
	}
	
	@Override
	public void registerProcessors(ConfigurableDispatchingServiceProcessor dispatching) {
		dispatching.registerInterceptor("github-label-notifier").registerForType(StepRequest.T, gitHubLabelNotifier());
	}
	
	@Managed
	private GitHubLabelNotifier gitHubLabelNotifier() {
		return new GitHubLabelNotifier();
	}
}

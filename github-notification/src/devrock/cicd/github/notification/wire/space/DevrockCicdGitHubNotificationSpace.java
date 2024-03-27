package devrock.cicd.github.notification.wire.space;

import com.braintribe.model.processing.meta.editor.ModelMetaDataEditor;
import com.braintribe.wire.api.annotation.Managed;

import devrock.cicd.github.notification.model.api.GitHubNotificationConnection;
import devrock.cicd.github.notification.model.api.GitHubNotificationMapping;
import devrock.cicd.github.notification.processing.GitHubLabelNotifier;
import devrock.step.model.api.StepRequest;
import devrock.step.model.api.meta.ExchangeConfiguration;
import hiconic.rx.module.api.service.ServiceDomainConfiguration;
import hiconic.rx.module.api.wire.RxModuleContract;

@Managed
public class DevrockCicdGitHubNotificationSpace implements RxModuleContract {
	
	@Override
	public void configureMainServiceDomain(ServiceDomainConfiguration configuration) {
		configuration.registerInterceptor("github-label-notifier").registerForType(StepRequest.T, gitHubLabelNotifier());
		configuration.configureModel(this::configureApiModel);
	}

	public void configureApiModel(ModelMetaDataEditor editor) {
		ExchangeConfiguration evaluatePlaceholders = ExchangeConfiguration.T.create();
		editor.onEntityType(GitHubNotificationConnection.T).addMetaData(evaluatePlaceholders);
		editor.onEntityType(GitHubNotificationMapping.T).addMetaData(evaluatePlaceholders);
	}
	
	@Managed
	private GitHubLabelNotifier gitHubLabelNotifier() {
		return new GitHubLabelNotifier();
	}
}

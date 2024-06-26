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
package devrock.cicd.github.notification.wire.space;

import com.braintribe.devrock.cicd._GithubNotificationApiModel_;
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
		configuration.bindInterceptor("github-label-notifier").forType(StepRequest.T).bind(this::gitHubLabelNotifier);
		configuration.addModel(_GithubNotificationApiModel_.reflection);
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

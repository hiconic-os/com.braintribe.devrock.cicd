package devrock.cicd.github.notification.wire;

import com.braintribe.wire.api.context.WireContextBuilder;
import com.braintribe.wire.api.module.WireTerminalModule;

import devrock.cicd.github.notification.wire.space.DevrockCicdGitHubNotificationSpace;
import devrock.step.api.module.wire.StepModuleContract;

public enum DevrockCicdGitHubNotificationWireModule implements WireTerminalModule<StepModuleContract> {
	INSTANCE;

	@Override
	public void configureContext(WireContextBuilder<?> contextBuilder) {
		WireTerminalModule.super.configureContext(contextBuilder);
		contextBuilder.bindContract(StepModuleContract.class, DevrockCicdGitHubNotificationSpace.class);
	}
}

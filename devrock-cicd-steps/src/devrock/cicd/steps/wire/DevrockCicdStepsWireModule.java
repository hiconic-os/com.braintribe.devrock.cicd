package devrock.cicd.steps.wire;

import com.braintribe.wire.api.context.WireContextBuilder;
import com.braintribe.wire.api.module.WireTerminalModule;

import devrock.cicd.steps.wire.space.DevrockCicdStepsSpace;
import devrock.step.api.module.wire.StepModuleContract;

public enum DevrockCicdStepsWireModule implements WireTerminalModule<StepModuleContract> {
	INSTANCE;

	@Override
	public void configureContext(WireContextBuilder<?> contextBuilder) {
		WireTerminalModule.super.configureContext(contextBuilder);
		contextBuilder.bindContract(StepModuleContract.class, DevrockCicdStepsSpace.class);
	}
}

package devrock.step.framework.wire;

import devrock.step.api.wire.StepFrameworkContract;
import devrock.step.framework.wire.space.StepFrameworkRxModuleSpace;
import hiconic.rx.module.api.wire.Exports;
import hiconic.rx.module.api.wire.RxModule;

public enum StepFrameworkRxModule implements RxModule<StepFrameworkRxModuleSpace> {
	
	INSTANCE;
	
	@Override
	public void bindExports(Exports exports) {
		exports.bind(StepFrameworkContract.class, moduleSpaceClass());
	}
	
}

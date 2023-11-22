package devrock.step.framework.wire;

import java.util.List;

import com.braintribe.gm.service.wire.common.CommonServiceProcessingWireModule;
import com.braintribe.wire.api.module.WireModule;
import com.braintribe.wire.api.module.WireTerminalModule;
import com.braintribe.wire.api.util.Lists;

import devrock.step.framework.wire.contract.StepFrameworkContract;

public enum StepFrameworkWireModule implements WireTerminalModule<StepFrameworkContract> {
	
	INSTANCE;
	
	@Override
	public List<WireModule> dependencies() {
		return Lists.list(CommonServiceProcessingWireModule.INSTANCE);
	}
	
}

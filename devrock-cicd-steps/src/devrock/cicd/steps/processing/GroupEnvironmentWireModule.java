package devrock.cicd.steps.processing;

import static com.braintribe.wire.api.util.Lists.list;

import java.io.File;
import java.util.List;

import com.braintribe.devrock.mc.core.wirings.configuration.contract.DevelopmentEnvironmentContract;
import com.braintribe.devrock.mc.core.wirings.env.configuration.EnvironmentSensitiveConfigurationWireModule;
import com.braintribe.wire.api.context.WireContextBuilder;
import com.braintribe.wire.api.module.WireModule;

public class GroupEnvironmentWireModule implements WireModule {
	private File groupPath;
	
	public GroupEnvironmentWireModule(File groupPath) {
		super();
		this.groupPath = groupPath;
	}

	@Override
	public List<WireModule> dependencies() {
		return list(EnvironmentSensitiveConfigurationWireModule.INSTANCE);
	}

	@Override
	public void configureContext(WireContextBuilder<?> contextBuilder) {
		contextBuilder.bindContract(DevelopmentEnvironmentContract.class, () -> DevEnvLocations.hasDevEnvParent(groupPath));
	}
}
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
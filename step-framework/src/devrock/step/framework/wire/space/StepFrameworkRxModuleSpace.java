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
package devrock.step.framework.wire.space;

import java.io.File;
import java.util.function.Function;

import com.braintribe.model.processing.service.api.InterceptorRegistry;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import devrock.step.api.StepExchangeContext;
import devrock.step.api.wire.StepFrameworkContract;
import devrock.step.framework.StepAroundProcessor;
import devrock.step.framework.StepExchangeContextImpl;
import devrock.step.model.api.StepRequest;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;

@Managed
public class StepFrameworkRxModuleSpace implements RxModuleContract, StepFrameworkContract {

	@Import
	private RxPlatformContract platform;
	
	@Override
	public void registerCrossDomainInterceptors(InterceptorRegistry interceptorRegistry) {
		interceptorRegistry.registerInterceptor("step-around").registerForType(StepRequest.T, stepAroundProcessor());
	}
	
	@Managed
	private StepAroundProcessor stepAroundProcessor() {
		StepAroundProcessor bean = new StepAroundProcessor();
		return bean;
	}
	
	@Override
	public StepExchangeContext newStepExchangeContext(File projectDir, File configDir,
			Function<String, Object> propertyLookup) {
		return new StepExchangeContextImpl(projectDir, configDir, platform.serviceDomains().main().systemCmdResolver(), propertyLookup);
	}
}

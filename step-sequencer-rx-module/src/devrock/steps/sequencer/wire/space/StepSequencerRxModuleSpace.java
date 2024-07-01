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
package devrock.steps.sequencer.wire.space;

import com.braintribe.model.processing.service.api.InterceptorRegistry;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import devrock.cicd.model.api.RunBuild;
import devrock.step.api.wire.StepFrameworkContract;
import devrock.step.model.api.RunStep;
import devrock.step.model.api.StepRequest;
import devrock.step.sequencer.model.configuration.StepConfiguration;
import devrock.steps.sequencer.processing.AntBuildProcessor;
import devrock.steps.sequencer.processing.RunStepProcessor;
import devrock.steps.sequencer.processing.StepSequencer;
import devrock.steps.sequencer.wire.contract.SequencerEnvironmentContract;
import hiconic.rx.module.api.service.ServiceDomainConfiguration;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;

@Managed
public class StepSequencerRxModuleSpace implements RxModuleContract {

	@Import
	private RxPlatformContract platform;
	
	@Import
	private StepFrameworkContract stepFramework;
	
	@Import
	private SequencerEnvironmentContract sequencerEnvironment;
	
	@Override
	public void configureMainServiceDomain(ServiceDomainConfiguration configuration) {
		configuration.bindRequest(RunBuild.T, this::buildProcessor);
		configuration.bindRequest(RunStep.T, this::runDefaultStepProcessor);
		configuration.addDefaultRequestSupplier(StepSequencer::getDefaultRequest);
	}
	
	@Override
	public void registerCrossDomainInterceptors(InterceptorRegistry interceptorRegistry) {
		interceptorRegistry.registerInterceptor("stepping").before("step-around").registerForType(StepRequest.T, sequencer());
	}
	
	@Managed
	private StepSequencer sequencer() {
		StepSequencer bean = new StepSequencer();
		bean.setConfiguration(configuration());
		bean.setExternallySequenced(sequencerEnvironment.DEVROCK_PIPELINE_EXTERNAL_SEQUENCING());
		bean.setStepExchangeContextFactory(stepFramework);
		return bean;
	}

	@Managed
	private StepConfiguration configuration() {
		return platform.readConfig(StepConfiguration.T).get();
	}
	
	@Managed
	private AntBuildProcessor buildProcessor() {
		AntBuildProcessor bean = new AntBuildProcessor();
		bean.setServiceDomain(platform.serviceDomains().main());
		return bean;
	}
	
	@Managed
	private RunStepProcessor runDefaultStepProcessor() {
		RunStepProcessor bean = new RunStepProcessor();
		bean.setConfiguration(configuration());
		return bean;
	}
}
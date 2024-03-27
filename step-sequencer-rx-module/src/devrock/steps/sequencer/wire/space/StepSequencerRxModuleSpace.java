package devrock.steps.sequencer.wire.space;

import com.braintribe.model.processing.service.api.InterceptorRegistry;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import devrock.cicd.model.api.RunBuild;
import devrock.step.api.wire.StepFrameworkContract;
import devrock.step.model.api.RunDefaultStep;
import devrock.step.model.api.StepRequest;
import devrock.step.sequencer.model.configuration.StepConfiguration;
import devrock.steps.sequencer.processing.AntBuildProcessor;
import devrock.steps.sequencer.processing.RunDefaultStepProcessor;
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
		configuration.register(RunBuild.T, buildProcessor());
		configuration.register(RunDefaultStep.T, runDefaultStepProcessor());
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
	private RunDefaultStepProcessor runDefaultStepProcessor() {
		RunDefaultStepProcessor bean = new RunDefaultStepProcessor();
		bean.setConfiguration(configuration());
		return bean;
	}
}
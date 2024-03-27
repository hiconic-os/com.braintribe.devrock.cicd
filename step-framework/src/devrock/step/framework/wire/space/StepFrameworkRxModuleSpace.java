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
		return new StepExchangeContextImpl(projectDir, configDir, platform.serviceDomains().main().cmdResolver(), propertyLookup);
	}
}

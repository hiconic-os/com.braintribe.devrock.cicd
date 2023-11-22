package devrock.step.framework.wire.space;

import java.io.File;
import java.util.function.Function;

import com.braintribe.devrock.cicd._StepApiModel_;
import com.braintribe.gm.service.wire.common.contract.CommonServiceProcessingContract;
import com.braintribe.gm.service.wire.common.contract.ServiceProcessingConfigurationContract;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.processing.meta.configuration.ConfigurationModels;
import com.braintribe.model.processing.meta.configured.ConfigurationModelBuilder;
import com.braintribe.model.processing.meta.editor.BasicModelMetaDataEditor;
import com.braintribe.model.processing.service.common.ConfigurableDispatchingServiceProcessor;
import com.braintribe.model.processing.session.impl.managed.StaticAccessModelAccessory;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.annotation.Scope;
import com.braintribe.wire.api.context.WireContext;
import com.braintribe.wire.api.context.WireContextConfiguration;

import devrock.step.api.StepEvaluator;
import devrock.step.api.module.wire.StepModuleContract;
import devrock.step.framework.StepAroundProcessor;
import devrock.step.framework.StepEvaluatorImpl;
import devrock.step.framework.StepModuleLoader;
import devrock.step.framework.wire.contract.StepFrameworkContract;
import devrock.step.model.api.StepRequest;

@Managed
public class StepFrameworkSpace implements StepFrameworkContract {

	@Import
	private ServiceProcessingConfigurationContract serviceProcessingConfiguration;
	
	@Import
	private CommonServiceProcessingContract commonServiceProcessing;
	
	@Import
	WireContext<?> wireContext;
	
	@Override
	public void onLoaded(WireContextConfiguration configuration) {
		serviceProcessingConfiguration.registerServiceConfigurer(this::configureServices);
	}
	
	private void configureServices(ConfigurableDispatchingServiceProcessor config) {
		config.registerInterceptor("step-around").registerForType(StepRequest.T, stepAroundProcessor());
		
		for (StepModuleContract moduleContract: stepModuleLoader().getStepModuleContracts()) {
			moduleContract.registerProcessors(config);
		}
	}
	
	@Managed
	private StepAroundProcessor stepAroundProcessor() {
		return new StepAroundProcessor();
	}
	
	@Managed
	private StepModuleLoader stepModuleLoader() {
		StepModuleLoader bean = new StepModuleLoader();
		bean.setParentContext(wireContext);
		return bean;
	}
	
	@Managed(Scope.prototype)
	@Override
	public StepEvaluator stepEvaluator(File cwd, File exchangeFolder, Function<String, Object> properties) {
		return new StepEvaluatorImpl(modelAccessory(), cwd, exchangeFolder, commonServiceProcessing.evaluator(), properties);
	}
	
	@Managed
	private StaticAccessModelAccessory modelAccessory() {
		StaticAccessModelAccessory bean = new StaticAccessModelAccessory(configurationModel(), "cicd"); 
		return bean;		
	}
	
	@Managed
	public GmMetaModel configurationModel() {
		ConfigurationModelBuilder configurationModelBuilder = ConfigurationModels.create("configured-" + _StepApiModel_.reflection.name());
		
		for (StepModuleContract moduleContract: stepModuleLoader().getStepModuleContracts()) {
			moduleContract.addApiModels(configurationModelBuilder);
		}
		
		GmMetaModel bean =  configurationModelBuilder.get();
		
		BasicModelMetaDataEditor editor = new BasicModelMetaDataEditor(bean);
		
		for (StepModuleContract moduleContract: stepModuleLoader().getStepModuleContracts()) {
			moduleContract.configureApiModel(editor);
		}
		
		return bean;
	}
}

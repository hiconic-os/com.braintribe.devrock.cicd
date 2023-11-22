package devrock.step.api.module.wire;

import com.braintribe.model.processing.meta.configured.ConfigurationModelBuilder;
import com.braintribe.model.processing.meta.editor.ModelMetaDataEditor;
import com.braintribe.model.processing.service.common.ConfigurableDispatchingServiceProcessor;
import com.braintribe.wire.api.space.WireSpace;

public interface StepModuleContract extends WireSpace {
	void registerProcessors(ConfigurableDispatchingServiceProcessor dispatching);
	void addApiModels(ConfigurationModelBuilder builder);
	void configureApiModel(ModelMetaDataEditor editor);
}

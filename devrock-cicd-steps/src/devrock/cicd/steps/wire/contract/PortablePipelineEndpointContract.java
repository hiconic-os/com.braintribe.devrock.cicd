package devrock.cicd.steps.wire.contract;

import java.io.File;

import com.braintribe.wire.api.space.WireSpace;

import devrock.step.api.StepEvaluator;

public interface PortablePipelineEndpointContract extends WireSpace {
	StepEvaluator stepEvaluator(File exchangeFolder);
}

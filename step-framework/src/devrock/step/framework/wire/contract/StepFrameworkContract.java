package devrock.step.framework.wire.contract;

import java.io.File;
import java.util.function.Function;

import com.braintribe.wire.api.space.WireSpace;

import devrock.step.api.StepEvaluator;

public interface StepFrameworkContract extends WireSpace {
	StepEvaluator stepEvaluator(File cwd, File exchangeFolder, Function<String, Object> properties);
}

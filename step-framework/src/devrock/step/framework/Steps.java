package devrock.step.framework;


import java.io.File;
import java.util.function.Function;

import com.braintribe.wire.api.Wire;

import devrock.step.api.StepEvaluator;
import devrock.step.framework.wire.StepFrameworkWireModule;
import devrock.step.framework.wire.contract.StepFrameworkContract;

public abstract class Steps {
	private static StepFrameworkContract stepFrameworkContract;
	
	static {
		stepFrameworkContract = Wire.context(StepFrameworkWireModule.INSTANCE).contract();
	}
	
	public static StepEvaluator evaluator(File exchangeFolder) {
		return evaluator(exchangeFolder, exchangeFolder);
	}
	
	public static StepEvaluator evaluator(File cwd, File exchangeFolder) {
		return stepFrameworkContract.stepEvaluator(cwd, exchangeFolder, null);
	}
	
	public static StepEvaluator evaluator(File cwd, File exchangeFolder, Function<String, Object> properties) {
		return stepFrameworkContract.stepEvaluator(cwd, exchangeFolder, properties);
	}
}

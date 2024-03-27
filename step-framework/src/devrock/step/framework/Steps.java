package devrock.step.framework;


import java.io.File;
import java.util.function.Function;

import devrock.step.api.StepEvaluator;
import hiconic.rx.module.api.service.ServiceDomain;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.platform.RxPlatform;

public abstract class Steps {
	private static RxPlatformContract platform = new RxPlatform().getContract();
	
	public static StepEvaluator evaluator(File exchangeFolder) {
		return evaluator(exchangeFolder, exchangeFolder);
	}
	
	public static StepEvaluator evaluator(File cwd, File exchangeFolder) {
		return evaluator(cwd, exchangeFolder, null);
	}
	
	public static StepEvaluator evaluator(File cwd, File exchangeFolder, Function<String, Object> properties) {
		ServiceDomain mainDomain = platform.serviceDomains().main();
		return new StepEvaluatorImpl(mainDomain.cmdResolver(), cwd, exchangeFolder, platform.evaluator(), properties);
	}
}

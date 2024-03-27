package devrock.step.api;

import java.io.File;
import java.util.function.Function;

public interface StepExchangeContextFactory {
	StepExchangeContext newStepExchangeContext(File projectDir, File configDir, Function<String, Object> propertyLookup);
}

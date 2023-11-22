package devrock.cicd.steps.gradle.extension;

import java.util.function.Consumer;

import org.gradle.api.GradleException;

import com.braintribe.gm.model.reason.Reason;
import com.braintribe.logging.Logger;
import com.braintribe.model.generic.reflection.EntityType;

import devrock.cicd.steps.gradle.common.Closures;
import devrock.cicd.steps.gradle.common.GradleAntContext;
import devrock.step.api.StepEvaluator;
import devrock.step.model.api.StepRequest;
import groovy.lang.Closure;

public class RequestStepConfiguration<S extends StepRequest> extends StepConfiguration {
	
	private static final Logger log = Logger.getLogger(RequestStepConfiguration.class);

	private final EntityType<S> type;
    private Consumer<? super S> initializer;
    private final StepEvaluator evaluator;
	private final GradleAntContext gradleAntContext;

    public RequestStepConfiguration(StepEvaluator evaluator, EntityType<S> type, GradleAntContext gradleAntContext) {
        super(com.braintribe.utils.StringTools.camelCaseToDashSeparated(type.getShortName()));
        this.type = type;
        this.evaluator = evaluator;
		this.gradleAntContext = gradleAntContext;
    }

    public void equip(Closure<?> initializer) {
    	equip(s -> Closures.withConcurrently(initializer, s));
    }
    
    public void equip(Consumer<? super S> initializer) {
        this.initializer = initializer;
    }

    @Override
	@SuppressWarnings("serial")
	public Closure<?> getRunnable() {
        return new Closure<Object>(null) {
        	@Override
        	public Object call() {
                S step = type.create();

                if (initializer != null) {
                	initializer.accept(step);
                }

                Reason reason = evaluateAndDoCleanup(step);
                if (reason != null) {
                    throw new GradleException(reason.stringify());
                }

                return null;
        	}

			private Reason evaluateAndDoCleanup(S step) {
				try {
                	return evaluator.evaluate(step);
                } finally {
                	try {
                		gradleAntContext.onAfterStepEvaluated();
                	} catch (Exception e) {
                		log.error("Error while cleaning up ant task", e);
                	}
                }
			}
        };
    }
}


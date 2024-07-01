// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package devrock.cicd.steps.gradle.extension;

import java.util.function.Consumer;

import org.gradle.api.GradleException;

import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.essential.InternalError;
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

                if (initializer != null)
                	initializer.accept(step);

                Reason reason = evaluateAndDoCleanup(step);
                if (reason == null)
                	return null;
                else
                	throw toException(reason);
        	}

			private GradleException toException(Reason reason) {
                Throwable cause = extractFirstCause(reason);

                if (cause != null)
                	return new GradleException(reason.stringify(), cause);
                else
                	return new GradleException(reason.stringify());
			}

			private Throwable extractFirstCause(Reason reason) {
				if (reason instanceof InternalError ie)
					return ie.getJavaException();

				for (Reason causeReason : reason.getReasons()) {
					Throwable cause = extractFirstCause(causeReason);
					if (cause != null)
						return cause;
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


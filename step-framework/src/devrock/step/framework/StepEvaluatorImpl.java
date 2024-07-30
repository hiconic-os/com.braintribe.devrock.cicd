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
package devrock.step.framework;

import java.io.File;
import java.util.function.Function;

import com.braintribe.common.attribute.common.CallerEnvironment;
import com.braintribe.common.attribute.common.impl.BasicCallerEnvironment;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.service.api.ServiceRequest;

import devrock.step.api.StepEvaluator;
import devrock.step.api.StepExchangeContextAttribute;
import devrock.step.model.api.StepRequest;

public class StepEvaluatorImpl extends StepExchangeContextImpl implements StepEvaluator {

	private final Evaluator<ServiceRequest> evaluator;
	private final File cwd;
	private StepRequest currentRequest;

	public StepEvaluatorImpl(CmdResolver cmdResolver, File cwd, File configFolder, Evaluator<ServiceRequest> evaluator,
			Function<String, Object> properties) {

		super(cwd, configFolder, cmdResolver, properties);
		this.cwd = cwd;
		this.evaluator = evaluator;
	}

	@Override
	public Reason evaluate(EntityType<? extends StepRequest> stepType) {
		return evaluate(stepType.create());
	}

	@Override
	public Reason evaluate(StepRequest stepRequest) {
		currentRequest = stepRequest;

		try {
			BasicCallerEnvironment callerEnvironment = new BasicCallerEnvironment(true, cwd);
	
			EvalContext<?> evalContext = stepRequest.eval(evaluator);
			evalContext.setAttribute(StepExchangeContextAttribute.class, this);
			evalContext.setAttribute(CallerEnvironment.class, callerEnvironment);
	
			return evalContext.getReasoned().whyUnsatisfied();

		} finally {
			currentRequest = null;
		}
	}

	@Override
	public StepRequest getCurrentRequest() {
		return currentRequest;
	}

}

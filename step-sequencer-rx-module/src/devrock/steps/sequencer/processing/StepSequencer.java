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
package devrock.steps.sequencer.processing;

import static com.braintribe.console.ConsoleOutputs.println;
import static com.braintribe.console.ConsoleOutputs.white;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.common.attribute.common.CallerEnvironment;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.service.api.ProceedContext;
import com.braintribe.model.processing.service.api.ReasonedServiceAroundProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.service.api.ServiceRequestContextBuilder;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.utils.StringTools;

import devrock.step.api.StepExchangeContext;
import devrock.step.api.StepExchangeContextAttribute;
import devrock.step.api.StepExchangeContextFactory;
import devrock.step.model.api.RunStep;
import devrock.step.model.api.StepEndpointOptions;
import devrock.step.model.api.StepRequest;
import devrock.step.model.api.endpoint.Stepping;
import devrock.step.sequencer.model.configuration.Step;
import devrock.step.sequencer.model.configuration.StepConfiguration;
import hiconic.rx.module.api.endpoint.EndpointInput;

public class StepSequencer implements ReasonedServiceAroundProcessor<StepRequest, Object> {
	private StepConfiguration configuration;
	private boolean externallySequenced;
	private StepExchangeContextFactory stepExchangeContextFactory;
	
	@Required
	public void setConfiguration(StepConfiguration configuration) {
		this.configuration = configuration;
	}
	
	@Required
	public void setStepExchangeContextFactory(StepExchangeContextFactory stepExchangeContextFactory) {
		this.stepExchangeContextFactory = stepExchangeContextFactory;
	}
	
	@Configurable
	public void setExternallySequenced(boolean externallySequenced) {
		this.externallySequenced = externallySequenced;
	}
	
	@Override
	public Maybe<?> processReasoned(ServiceRequestContext context, StepRequest triggerRequest,
			ProceedContext proceedContext) {

		EndpointInput input = EndpointInput.get();
		
		StepExchangeContext exchangeContext = buildExchangeContext(input);
		
		ServiceRequestContextBuilder builder = context.derive();
		builder.setAttribute(StepExchangeContextAttribute.class, exchangeContext);
		
		ServiceRequestContext enrichedRequestContext = builder.build();
		return processSequence(enrichedRequestContext, input, exchangeContext, triggerRequest, proceedContext);
	}
	
	private StepExchangeContext buildExchangeContext(EndpointInput input) {
		
		File projectDir = CallerEnvironment.getCurrentWorkingDirectory();
		File configFolder = new File(projectDir, ".step-exchange");
		
		StepEndpointOptions options = input.findInput(StepEndpointOptions.T);
		
		Function<String, Object> propertyLookup = null;
		
		if (options != null) {
			propertyLookup = new EntityPropertyLookup(options);
		}
		
		return stepExchangeContextFactory.newStepExchangeContext(projectDir, configFolder, propertyLookup);
	}
	
	public Maybe<?> processSequence(ServiceRequestContext enrichedRequestContext, EndpointInput input, StepExchangeContext exchangeContext, StepRequest triggerRequest,
			ProceedContext proceedContext) {
		
		Stepping stepping = input.findInput(Stepping.T);
		
		if (isExternallySequenced(stepping)) 
			return proceedContext.proceedReasoned(enrichedRequestContext, triggerRequest);
		
		List<StepRequest> predecessorSequence = determineSequence(input, triggerRequest);
		
		Maybe<?> maybe = null;
		
		for (StepRequest request: predecessorSequence) {
			String stepName = StringTools.camelCaseToDashSeparated(request.entityType().getShortName());
			
			println(white("\n> Step: " + stepName));
			
			maybe = proceedContext.proceedReasoned(enrichedRequestContext, request);
			
			if (maybe.isUnsatisfied())
				return maybe;
		}
		
		return maybe;
	}
	
	private List<StepRequest> determineSequence(EndpointInput input, StepRequest triggerRequest) {
		List<StepRequest> explicitStepRequests = input.findInputs(StepRequest.T);
		
		if (explicitStepRequests.isEmpty())
			explicitStepRequests = List.of(triggerRequest);
		
		List<StepRequest> sequence = mergeRequests(explicitStepRequests);
		
		if (sequence.isEmpty())
			return List.of(triggerRequest);
		
		return sequence;
	}
	
	private List<StepRequest> mergeRequests(List<StepRequest> explicitStepRequests) {
		List<Step> steps = configuration.getSteps();
		List<StepRequest> sequence = new LinkedList<>();
		
		boolean found = false;
		
		for (int i = steps.size() - 1; i >= 0; i--) {
			Step step = steps.get(i);
			StepRequest request = step.getRequest();
			EntityType<?> stepType = request.entityType();
			
			for (StepRequest explicitRequest: explicitStepRequests) {
				if (explicitRequest.entityType() == stepType) {
					request = explicitRequest;
					found = true;
					break;
				}
			}
			
			if (found)
				sequence.addFirst(request);
		}
		
		return sequence;
	}
	
	public boolean isExternallySequenced(Stepping stepping) {
		Boolean externallySequenced = stepping != null? stepping.getExternallySequenced(): null;
		
		if (externallySequenced != null)
			return externallySequenced;
		
		return this.externallySequenced;
	}
	
	public static ServiceRequest getDefaultRequest() {
		File cwd = CallerEnvironment.getCurrentWorkingDirectory();
		
		File parent = new File(cwd, "parent");
		File parentPom = new File(parent, "pom.xml");
		
		if (parentPom.exists())
			return RunStep.T.create();
		
		return null;
	}
}

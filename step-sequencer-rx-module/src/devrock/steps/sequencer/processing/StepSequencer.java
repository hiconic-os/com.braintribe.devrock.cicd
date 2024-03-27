package devrock.steps.sequencer.processing;

import static com.braintribe.console.ConsoleOutputs.println;
import static com.braintribe.console.ConsoleOutputs.white;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.common.attribute.common.CallerEnvironment;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.proxy.DynamicEntityType;
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
import devrock.step.model.api.RunDefaultStep;
import devrock.step.model.api.StepRequest;
import devrock.step.model.api.StepResponse;
import devrock.step.model.api.endpoint.Stepping;
import devrock.step.sequencer.model.configuration.Step;
import devrock.step.sequencer.model.configuration.StepConfiguration;
import hiconic.rx.module.api.endpoint.EndpointInput;

public class StepSequencer implements ReasonedServiceAroundProcessor<StepRequest, StepResponse> {
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
	public Maybe<? extends StepResponse> processReasoned(ServiceRequestContext context, StepRequest triggerRequest,
			ProceedContext proceedContext) {

		StepExchangeContext exchangeContext = buildExchangeContext(context);
		
		ServiceRequestContextBuilder builder = context.derive();
		builder.setAttribute(StepExchangeContextAttribute.class, exchangeContext);
		
		ServiceRequestContext enrichedRequestContext = builder.build();
		return processSequence(enrichedRequestContext, exchangeContext, triggerRequest, proceedContext);
	}
	
	private StepExchangeContext buildExchangeContext(ServiceRequestContext context) {
		
		File projectDir = CallerEnvironment.getCurrentWorkingDirectory();
		File configFolder = new File(projectDir, ".step-exchange");
		
		return stepExchangeContextFactory.newStepExchangeContext(projectDir, configFolder, System::getProperty);
	}
	
	public Maybe<? extends StepResponse> processSequence(ServiceRequestContext enrichedRequestContext, StepExchangeContext exchangeContext, StepRequest triggerRequest,
			ProceedContext proceedContext) {
		EndpointInput input = EndpointInput.get();
		Stepping stepping = input.findInput(Stepping.T);
		
		if (isExternallySequenced(stepping)) 
			return proceedContext.proceedReasoned(enrichedRequestContext, triggerRequest);
		
		List<StepRequest> predecessorSequence = determineSequence(triggerRequest);
		
		Maybe<? extends StepResponse> maybe = null;
		
		for (StepRequest request: predecessorSequence) {
			String stepName = StringTools.camelCaseToDashSeparated(request.entityType().getShortName());
			
			println(white("\n> Step: " + stepName));
			
			maybe = proceedContext.proceedReasoned(enrichedRequestContext, request);
			
			if (maybe.isUnsatisfied())
				return maybe;
		}
		
		return maybe;
	}
	
	private List<StepRequest> determineSequence(StepRequest request) {
		int stepIndex = indexOf(request);
		
		if (stepIndex == -1)
			return List.of(request);
		
		return Stream.concat( //
				configuration.getSteps().subList(0, stepIndex).stream().map(Step::getRequest), //
				Stream.of(request) //
				).toList();
	}
	
	private int indexOf(StepRequest request) {
		int i = 0;
		EntityType<GenericEntity> entityType = request.entityType();
		for (Step step: configuration.getSteps()) {
			if (step.getRequest().entityType() == entityType)
				return i;
			i++;
		}
		
		return -1;
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
			return RunDefaultStep.T.create();
		
		return null;
	}
	
	
	private EntityType<?> buildCoalescingType(StepRequest triggerRequest) {
		List<StepRequest> sequence = determineSequence(triggerRequest);
		
		DynamicEntityType et = new DynamicEntityType("VirtualStepRequest");
		
		for (StepRequest request: sequence.reversed()) {
			request.entityType();
			et.addProperty(null, et)
		}
		
	}



}

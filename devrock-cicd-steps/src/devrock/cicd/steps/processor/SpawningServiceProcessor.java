package devrock.cicd.steps.processor;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.processing.service.api.ReasonedServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.service.api.ServiceRequest;

public abstract class SpawningServiceProcessor<P extends ServiceRequest, R> implements ReasonedServiceProcessor<P, R> {
	
	protected abstract StatefulServiceProcessor spawn();
	
	@Override
	public Maybe<? extends R> processReasoned(ServiceRequestContext context, P request) {
		return spawn().process(context, request);
	}
	
	protected abstract class StatefulServiceProcessor {
		protected ServiceRequestContext context;
		protected P request;
		
		private Maybe<? extends R> process(ServiceRequestContext context, P request) {
			this.context = context;
			this.request = request;
			return process();
		}
		
		protected abstract Maybe<? extends R> process();
	}

}

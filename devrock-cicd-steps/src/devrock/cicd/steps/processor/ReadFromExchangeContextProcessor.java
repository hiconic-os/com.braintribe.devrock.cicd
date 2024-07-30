package devrock.cicd.steps.processor;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.model.generic.GMF;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.processing.service.api.ReasonedServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;

import devrock.cicd.model.api.ReadFromExchangeContext;
import devrock.step.api.StepExchangeContext;
import devrock.step.api.StepExchangeContextAttribute;

public class ReadFromExchangeContextProcessor implements ReasonedServiceProcessor<ReadFromExchangeContext, Object> {

	@Override
	public Maybe<?> processReasoned(ServiceRequestContext context, ReadFromExchangeContext request) {
		String typeSignature = request.getType();

		EntityType<GenericEntity> entityType = GMF.getTypeReflection().findEntityType(typeSignature);
		if (entityType == null)
			return Reasons.build(InvalidArgument.T) //
					.text("Type not found: " + typeSignature) //
					.toMaybe();

		StepExchangeContext exchangeContext = context.getAttribute(StepExchangeContextAttribute.class);

		Maybe<GenericEntity> maybeInstance = exchangeContext.load(entityType);
		if (maybeInstance.isIncomplete())
			return maybeInstance;

		GenericEntity genericEntity = maybeInstance.get();

		String property = request.getProperty();
		if (property == null)
			return Maybe.complete(genericEntity);

		if (genericEntity == null)
			return Reasons.build(InvalidArgument.T) //
					.text("Property '" + property + "' cannot be resolved, value is null for type: " + typeSignature) //
					.toMaybe();

		Property p = entityType.findProperty(property);
		if (p == null)
			return Reasons.build(InvalidArgument.T) //
					.text("Property '" + property + "' not found for type: " + typeSignature) //
					.toMaybe();

		return Maybe.complete(p.get(genericEntity));
	}

}

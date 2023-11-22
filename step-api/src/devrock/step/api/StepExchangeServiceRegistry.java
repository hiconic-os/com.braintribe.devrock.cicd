package devrock.step.api;

import java.util.function.Function;

import com.braintribe.common.attribute.TypeSafeAttribute;

public interface StepExchangeServiceRegistry {
	<A extends TypeSafeAttribute<? super V>, V> void register(Class<A> attribute, Function<StepExchangeContext, V> factory);
}

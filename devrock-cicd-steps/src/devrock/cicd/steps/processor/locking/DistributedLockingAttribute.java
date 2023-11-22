package devrock.cicd.steps.processor.locking;

import java.util.concurrent.locks.Lock;
import java.util.function.Function;

import com.braintribe.common.attribute.TypeSafeAttribute;

public interface DistributedLockingAttribute extends TypeSafeAttribute<Function<String, Lock>> {
	// empty
}

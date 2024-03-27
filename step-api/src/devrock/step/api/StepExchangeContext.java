package devrock.step.api;

import java.util.function.Supplier;

import com.braintribe.common.attribute.TypeSafeAttribute;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.processing.meta.cmd.CmdResolver;

/**
 * A StepContext allows to acquire 
 */
public interface StepExchangeContext {

	void makeOrCleanExchangeFolder();
	
	<V> Maybe<V> getProperty(GenericModelType type, String name);
	
	<E extends GenericEntity> Maybe<E> load(EntityType<E> type);
	
	<E extends GenericEntity> void store(EntityType<E> type, E data);
	
	<E extends GenericEntity> Maybe<E> load(EntityType<E> type, String classifier);
	
	<E extends GenericEntity> void store(EntityType<E> type, String classifier, E data);
	
	default <E extends GenericEntity> void store(E data) {
		store(data.entityType(), data);
	}
	
	Reason loadProperties(GenericEntity data);
	
	void storeProperties(GenericEntity data);
	
	<A extends TypeSafeAttribute<V>, V> V getService(Class<A> attribute, Supplier<V> defaultValueSupplier);

	CmdResolver getCmdResolver();
}

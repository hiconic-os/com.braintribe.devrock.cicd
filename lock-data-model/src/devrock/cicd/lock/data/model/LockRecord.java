package devrock.cicd.lock.data.model;

import java.util.Date;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface LockRecord extends GenericEntity {
	EntityType<LockRecord> T = EntityTypes.T(LockRecord.class);
	
	String created = "created";
	String owner = "owner";
	String key = "key";
	String touched = "touched";
	
	String getKey();
	void setKey(String key);
	
	String getOwner();
	void setOwner(String owner);
	
	Date getCreated();
	void setCreated(Date created);
	
	Date getTouched();
	void setTouched(Date touched);
}

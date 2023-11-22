package devrock.cicd.model.api;

import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.annotation.meta.Confidential;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface FirebaseRealtimeDistributedLocking extends DistributedLocking {
	EntityType<FirebaseRealtimeDistributedLocking> T = EntityTypes.T(FirebaseRealtimeDistributedLocking.class);
	
	String owner = "owner";
	String tableUri = "tableUri";
	String webApiKey = "webApiKey";
	String user = "user";
	String password = "password";
	
	String touchIntervalInMs = "touchIntervalInMs";
	String touchWorkerIntervalInMs = "touchWorkerIntervalInMs";

	String getOwner();
	void setOwner(String owner);
	
	String getTableUri();
	void setTableUri(String tableUri);
	
	String getWebApiKey();
	void setWebApiKey(String webApiKey);
	
	String getUser();
	void setUser(String user);
	
	@Confidential
	String getPassword();
	void setPassword(String password);
	
	@Initializer("10000L")
	long getTouchIntervalInMs();
	void setTouchIntervalInMs(long touchIntervalInMs);
	
	@Initializer("1000L")
	long getTouchWorkerIntervalInMs();
	void setTouchWorkerIntervalInMs(long touchWorkerIntervalInMs);

}

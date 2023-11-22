package devrock.cicd.locking.firebase.test;

import com.braintribe.cfg.Required;
import com.braintribe.wire.api.annotation.Name;

public interface EnvironmentProperties {
	@Name("FIREBASE_LOCKING_TEST__WEB_API_KEY")
	@Required
	String webApiKey();
	
	@Name("FIREBASE_LOCKING_TEST__USER")
	@Required
	String user();
	
	@Name("FIREBASE_LOCKING_TEST__PASSWORD")
	@Required
	String password();
}

package devrock.cicd.model.api.reason;

import com.braintribe.gm.model.reason.Reason;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface CommandFailed extends Reason {
	EntityType<CommandFailed> T = EntityTypes.T(CommandFailed.class);
	
	String errorMessage = "errorMessage";
	String errorCode = "errorCode";
	
	int getErrorCode();
	void setErrorCode(int errorCode);
	
	String getErrorMessage();
	void setErrorMessage(String errorMessage);

}

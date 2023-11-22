package devrock.cicd.model.api.data;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface GitContext extends GenericEntity {
	EntityType<GitContext> T = EntityTypes.T(GitContext.class);
	
	String baseBranch = "baseBranch";
	String baseHash = "baseHash";
	String baseRemote = "baseRemote";
	
	String getBaseBranch();
	void setBaseBranch(String baseBranch);
	
	// head or in case of a feature branch retrieved with: git rev-parse <branch> 
	String getBaseHash();
	void setBaseHash(String baseHash);
	
	String getBaseRemote();
	void setBaseRemote(String baseRemote);
}

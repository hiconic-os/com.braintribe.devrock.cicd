package devrock.cicd.model.api;

import java.util.function.Consumer;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Transient;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

import devrock.cicd.model.api.data.LocalArtifact;

public interface HasArtifactHandler extends GenericEntity {
	EntityType<HasArtifactHandler> T = EntityTypes.T(HasArtifactHandler.class);
	
	@Transient
	Consumer<LocalArtifact> getHandler();
	void setHandler(Consumer<LocalArtifact> handler);
}

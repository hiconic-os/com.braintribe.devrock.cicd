package devrock.step.model.api.meta;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface ProjectDir extends ArgumentMapping {
	EntityType<ProjectDir> T = EntityTypes.T(ProjectDir.class);
}

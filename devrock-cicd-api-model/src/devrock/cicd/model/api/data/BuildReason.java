package devrock.cicd.model.api.data;

import com.braintribe.model.generic.base.EnumBase;
import com.braintribe.model.generic.reflection.EnumType;
import com.braintribe.model.generic.reflection.EnumTypes;

public enum BuildReason implements EnumBase {
	NONE, UNPUBLISHED, ARTIFACT_CHANGED, DEPENDENCY_RESOLUTION_CHANGED, ARTIFACT_UNTRACKED, PARENT_CHANGED, EXPLICIT;
	
	public static EnumType T = EnumTypes.T(BuildReason.class);

	@Override
	public EnumType type() {
		return T;
	}
}

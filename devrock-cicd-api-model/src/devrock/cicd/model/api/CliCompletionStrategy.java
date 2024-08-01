// ============================================================================
package devrock.cicd.model.api;

import com.braintribe.model.generic.base.EnumBase;
import com.braintribe.model.generic.reflection.EnumType;
import com.braintribe.model.generic.reflection.EnumTypes;

/**
 * Specifies which identifiers should be suggested for completion for elements which have a name and possibly multiple
 * aliases.
 * 
 * @author peter.gazdik
 */
public enum CliCompletionStrategy implements EnumBase<CliCompletionStrategy> {
	/** suggest real name and all aliases */
	all,

	/** suggest real name only */
	realName,

	/** suggest the shortest name or alias */
	shortest;

	public static final EnumType<CliCompletionStrategy> T = EnumTypes.T(CliCompletionStrategy.class);

	@Override
	public EnumType<CliCompletionStrategy> type() {
		return T;
	}
}

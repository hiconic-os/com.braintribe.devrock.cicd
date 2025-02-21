package devrock.cicd.steps.processing;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.version.Version;

public interface Versions {
	
	static Maybe<Version> raise(Version version) {
		String nonConform = version.getNonConform();
		
		if (nonConform != null)
			return Reasons.build(InvalidArgument.T) //
				.text("Given version " + version.asString() + " cannot be raised as it has a non-conformity: " + nonConform) //
				.toMaybe();

		Property property = getMostDetailedCounter(version);
		
		Integer num = property.get(version);
		
		if (num == null)
			num = 0;
		
		num++;
		
		property.set(version, num);
		
		return Maybe.complete(version);
	}
	
	static Property getMostDetailedCounter(Version version) {
		
		if (version.getQualifier() != null)
			return Version.T.getProperty(Version.buildNumber);
		
		if (version.getRevision() != null)
			return Version.T.getProperty(Version.revision);
		
		if (version.getMinor() != null)
			return Version.T.getProperty(Version.minor);
		
		return Version.T.getProperty(Version.major);
	}
}

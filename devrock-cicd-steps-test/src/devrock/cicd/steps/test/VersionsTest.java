package devrock.cicd.steps.test;


import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.model.version.Version;

import devrock.cicd.steps.processing.Versions;

public class VersionsTest {
	@Test
	public void testVersionRaising() {
		Assertions.assertThat(Versions.raise(Version.create(1)).get().compareTo(Version.create(2))).isEqualTo(0);
		Assertions.assertThat(Versions.raise(Version.create(1,1)).get().compareTo(Version.create(1,2))).isEqualTo(0);
		Assertions.assertThat(Versions.raise(Version.create(1,1,1)).get().compareTo(Version.create(1,1,2))).isEqualTo(0);
		Assertions.assertThat(Versions.raise(Version.create(1).qualifier("alpha")).get().compareTo(Version.create(1).qualifier("alpha").buildNumber(1))).isEqualTo(0);
		Assertions.assertThat(Versions.raise(Version.create(1,1).qualifier("alpha")).get().compareTo(Version.create(1,1).qualifier("alpha").buildNumber(1))).isEqualTo(0);
		Assertions.assertThat(Versions.raise(Version.create(1,1,1).qualifier("alpha")).get().compareTo(Version.create(1,1,1).qualifier("alpha").buildNumber(1))).isEqualTo(0);
		Assertions.assertThat(Versions.raise(Version.create(1,1,1).qualifier("alpha").buildNumber(0)).get().compareTo(Version.create(1,1,1).qualifier("alpha").buildNumber(1))).isEqualTo(0);
		Assertions.assertThat(Versions.raise(Version.create(1,1,1).qualifier("alpha").buildNumber(1)).get().compareTo(Version.create(1,1,1).qualifier("alpha").buildNumber(2))).isEqualTo(0);
		Assertions.assertThat(Versions.raise(Version.parse("1.1.1.1")).isUnsatisfiedBy(InvalidArgument.T)).isTrue();
	}
}

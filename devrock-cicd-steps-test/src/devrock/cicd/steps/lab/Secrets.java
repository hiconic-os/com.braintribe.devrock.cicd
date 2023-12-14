package devrock.cicd.steps.lab;

import com.braintribe.wire.api.annotation.Name;

public interface Secrets {
	@Name("DEVROCK_CICD_STEPS_TEST_GITHUB_TOKEN")
	String githubToken();
}

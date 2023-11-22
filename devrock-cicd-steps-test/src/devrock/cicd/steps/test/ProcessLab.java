package devrock.cicd.steps.test;

import java.io.File;

import com.braintribe.gm.model.reason.Reason;

import devrock.process.execution.Scripts;

public class ProcessLab {
	public static void main(String[] args) {
		
		Reason reason = Scripts.run(new File("C:/devrock-sdk/env/mc-ng/git/com.braintribe.devrock"), "dr", "-Drange=[mc-api]");
		
		if (reason != null)
			System.out.println(reason.stringify());
	}

}

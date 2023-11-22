package devrock.cicd.steps.processing;

import java.io.File;

public class DevEnvLocations {
	static File hasDevEnvParent(File dir) {
		File file = new File(dir, "dev-environment.yaml");
		
		if (file.exists()) {
			return dir;
		}
		else {
			File parentFolder = dir.getParentFile();
			
			if (parentFolder != null) {
				return hasDevEnvParent(parentFolder);
			}
			else {
				return null;
			}
		}
	}
}

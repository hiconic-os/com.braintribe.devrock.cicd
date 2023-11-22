package devrock.cicd.steps.gradle.common;

import java.io.File;
import java.util.Map;

/**
 * @author peter.gazdik
 */
public class AntTaskContext {

	public final File artifactDir;
	public final String target;
	public final File outputFile;
	public final Map<String, String> properties;

	public long durationMs;
	public boolean failed;

	public AntTaskContext(File artifactDir, String target, File outputFile, Map<String, String> properties) {
		this.artifactDir = artifactDir;
		this.target = target;
		this.outputFile = outputFile;
		this.properties = properties;
	}

}

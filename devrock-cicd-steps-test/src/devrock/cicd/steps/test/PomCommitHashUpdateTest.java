package devrock.cicd.steps.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import devrock.pom.PomTools;

public class PomCommitHashUpdateTest {
	@Test
	public void commitHashUpdate() throws IOException {
		process("pom1.xml");
		process("pom2.xml");
		process("pom3.xml");
		process("pom4.xml");
	}

	private void process(String filename) throws IOException {
		String modifiedFilename = "modified-" + filename;
		File outDir = new File("out");
		outDir.mkdir();
		Path pomOriginal = Paths.get("res", filename);
		Path pomCopy = Paths.get("out", modifiedFilename);
		Path pomExpected = Paths.get("res", modifiedFilename);
		Files.copy(pomOriginal, pomCopy, StandardCopyOption.REPLACE_EXISTING);
		
		PomTools.addCommitHash(pomCopy.toFile(), "abc123");
		
		Assertions.assertThat(pomCopy.toFile()).hasSameBinaryContentAs(pomExpected.toFile());
	}
}

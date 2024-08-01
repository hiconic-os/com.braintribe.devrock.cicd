// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
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

		Assertions.assertThat(pomCopy.toFile()).hasSameTextualContentAs(pomExpected.toFile());
	}
}

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
package devrock.cicd.steps.gradle.common;

import java.io.File;
import java.util.Map;

import org.apache.tools.ant.BuildException;

/**
 * @author peter.gazdik
 */
public class AntTaskContext {

	public final File artifactDir;
	public final String target;
	public final File outputFile;
	public final Map<String, String> properties;

	public long durationMs;
	public BuildException buildException;

	public AntTaskContext(File artifactDir, String target, File outputFile, Map<String, String> properties) {
		this.artifactDir = artifactDir;
		this.target = target;
		this.outputFile = outputFile;
		this.properties = properties;
	}

}

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
package devrock.cicd.model.api.data;

import com.braintribe.model.artifact.essential.VersionedArtifactIdentification;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface LocalArtifact extends GenericEntity {

	EntityType<LocalArtifact> T = EntityTypes.T(LocalArtifact.class);

	String folderName = "folderName";
	String identification = "identification";
	String artifactIdentification = "artifactIdentification";
	String buildReason = "buildReason";
	String packaging = "packaging";
	String integrationTest = "integrationTest";
	String releaseView = "releaseView";
	String test = "test";
	String bundle = "bundle";
	String commitHash = "commitHash";

	String getFolderName();
	void setFolderName(String folderName);

	/**
	 * Full name of the artifact such as: group-id:artifact-id#version
	 */
	String getIdentification();
	void setIdentification(String identification);

	VersionedArtifactIdentification getArtifactIdentification();
	void setArtifactIdentification(VersionedArtifactIdentification artifactIdentification);

	String getPackaging();
	void setPackaging(String packaging);

	BuildReason getBuildReason();
	void setBuildReason(BuildReason buildReason);

	/** If this is <code>true</code> then {@link #getIntegrationTest()} is <code>false</code>. */
	boolean getTest();
	void setTest(boolean test);

	String getCommitHash();
	void setCommitHash(String commitHash);

	boolean getBundle();
	void setBundle(boolean bundle);

	/** If this is <code>true</code> then #getTest() is <code>false</code>. */
	boolean getIntegrationTest();
	void setIntegrationTest(boolean integrationTest);

	boolean getReleaseView();
	void setReleaseView(boolean releaseView);

	boolean getNpmPackage();
	void setNpmPackage(boolean npmPackage);

	@Override
	default String asString() {
		return getIdentification();
	}

}

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

import java.util.List;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface CodebaseAnalysis extends GenericEntity {
	EntityType<CodebaseAnalysis> T = EntityTypes.T(CodebaseAnalysis.class);
	
	String artifacts = "artifacts";
	String builds = "builds";
	String buildLinkingChecks = "buildLinkingChecks";
	String buildTests = "buildTests";
	String basePath = "basePath";
	String groupId = "groupId";
	String groupVersion = "groupVersion";
	
	String getGroupId();
	void setGroupId(String groupId);
	
	String getGroupVersion();
	void setGroupVersion(String groupVersion);
	
	List<LocalArtifact> getArtifacts();
	void setArtifacts(List<LocalArtifact> artifacts);
	
	// artifacts to be build because of BuildReason in build order
	List<LocalArtifact> getBuilds();
	void setBuilds(List<LocalArtifact> builds);
	
	// currently contains the direct dependers of the builds that are to be compiled error free for breaking changes validation
	// build-compile-check artifacts are not to be found in the builds collection as they are derived
	// the filler of this needs to ensure the exclusiveness of the builds and build-compile-checks
	List<LocalArtifact> getBuildLinkingChecks();
	void setBuildLinkingChecks(List<LocalArtifact> buildLinkingChecks);
	
	// Unit-Test artifacts to be executed after building and compile checking (no need to be compiled again as either building or compile-checking achieved that already)
	// A build-test artifact must either be mirrored in the builds collection or in the build-compile-checks collection
	List<LocalArtifact> getBuildTests();
	void setBuildTests(List<LocalArtifact> buildTests);
	
	String getBasePath();
	void setBasePath(String basePath);
}

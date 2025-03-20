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
package devrock.cicd.steps.processing;

import java.util.function.Function;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.service.api.ServiceRequestContext;

import devrock.cicd.model.api.RunBuild;
import devrock.cicd.model.api.data.LocalArtifact;
import devrock.step.model.api.StepRequest;

public interface BuildHandlers {

	static Function<LocalArtifact, Maybe<?>> getHandler(ServiceRequestContext context, StepRequest request, EntityType<? extends RunBuild> runBuildType) {
		return artifact -> {
			RunBuild runBuild = runBuildType.create();
			runBuild.setArtifact(artifact);
			runBuild.setCaller(request);
			return runBuild.eval(context).getReasoned();
		};
	}

}

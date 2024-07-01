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

import static com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling.getOrTunnel;

import java.util.function.Consumer;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.service.api.ServiceRequestContext;

import devrock.cicd.model.api.HasArtifactHandler;
import devrock.cicd.model.api.RunBuild;
import devrock.cicd.model.api.data.LocalArtifact;
import devrock.step.model.api.StepRequest;

public interface BuildHandlers {
	static <R extends StepRequest & HasArtifactHandler> Consumer<LocalArtifact> getHandler(ServiceRequestContext context, R request, EntityType<? extends RunBuild> runBuildType) {
		Consumer<LocalArtifact> handler = request.getHandler();
		
		if (handler != null)
			return handler;
		
		return artifact -> {
			RunBuild runBuild = runBuildType.create();
			runBuild.setArtifact(artifact);
			runBuild.setCaller(request);
			getOrTunnel(runBuild.eval(context).getReasoned());
		};
	}
}

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
package devrock.steps.sequencer.processing;

import java.io.File;
import java.util.Map;

import com.braintribe.cfg.Required;
import com.braintribe.common.attribute.common.CallerEnvironment;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.processing.meta.cmd.builders.EntityMdResolver;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.service.impl.AbstractDispatchingServiceProcessor;
import com.braintribe.model.processing.service.impl.DispatchConfiguration;
import com.braintribe.model.service.api.result.Neutral;
import com.braintribe.utils.lcd.NullSafe;

import devrock.ant.model.api.RunAnt;
import devrock.cicd.model.api.RunBuild;
import devrock.cicd.model.api.RunCheckLinking;
import devrock.cicd.model.api.RunInstall;
import devrock.cicd.model.api.RunTest;
import devrock.step.model.api.StepRequest;
import devrock.step.model.api.meta.ArgumentPropagation;
import hiconic.rx.module.api.service.ServiceDomain;

public class AntBuildProcessor extends AbstractDispatchingServiceProcessor<RunBuild, Neutral> {
	
	private ServiceDomain serviceDomain;
	
	@Required
	public void setServiceDomain(ServiceDomain serviceDomain) {
		this.serviceDomain = serviceDomain;
	}
	
	@Override
	protected void configureDispatching(DispatchConfiguration<RunBuild, Neutral> dispatching) {
		dispatching.registerReasoned(RunInstall.T, this::runInstall);
		dispatching.registerReasoned(RunCheckLinking.T, this::runCheckLinking);
		dispatching.registerReasoned(RunTest.T, this::runTest);
		
	}
	
	private Maybe<? extends Neutral> runInstall(ServiceRequestContext context, RunInstall request) {
		return runBuild(context, request, "install");
	}
	
	private Maybe<? extends Neutral> runCheckLinking(ServiceRequestContext context, RunCheckLinking request) {
		return runBuild(context, request, "check-linking");
	}
	
	private Maybe<? extends Neutral> runTest(ServiceRequestContext context, RunTest request) {
		return runBuild(context, request, "test");
	}
	
	private Maybe<? extends Neutral> runBuild(ServiceRequestContext context, RunBuild request, String target) {
		File groupDir = CallerEnvironment.getCurrentWorkingDirectory();
		File projectDir = new File(groupDir, request.getArtifact().getFolderName());
		
		StepRequest caller = request.getCaller();
		RunAnt runAnt = RunAnt.T.create();
		runAnt.setTarget(target);
		runAnt.setOwnerInfo(request.getArtifact().getArtifactIdentification().getArtifactId());
		runAnt.setProjectDir(projectDir.getAbsolutePath());
		runAnt.setBufferOutput(true);
		addMdBasedPropertyPropagations(caller, runAnt.getProperties());
		
		return runAnt.eval(context).getReasoned();
	}
	
	private void addMdBasedPropertyPropagations(StepRequest stepRequest, Map<String, String> properties) {
		EntityMdResolver entityMdResolver = serviceDomain.systemCmdResolver().getMetaData().entity(stepRequest);

		for (Property p : stepRequest.entityType().getProperties()) {
			ArgumentPropagation ap = entityMdResolver.property(p).meta(ArgumentPropagation.T).exclusive();
			if (ap == null)
				continue;

			Object value = p.get(stepRequest);
			if (value == null)
				continue;

			String downstreamArgumentName = NullSafe.get(ap.getName(), p.getName());
			properties.put(downstreamArgumentName, value.toString());
		}

	}


}

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
package com.braintribe.devrock.cicd.ant.wire.space;

import java.io.File;

import com.braintribe.devrock.cicd.ant.processing.AntProcessor;
import com.braintribe.utils.stream.api.StreamPipes;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import devrock.ant.model.api.RunAnt;
import hiconic.rx.module.api.service.ServiceDomainConfiguration;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;

@Managed
public class AntRxModuleSpace implements RxModuleContract {

	@Import
	private RxPlatformContract platform;
	
	@Override
	public void configureMainServiceDomain(ServiceDomainConfiguration configuration) {
		configuration.bindRequest(RunAnt.T, this::antProcessor);
	}
	
	@Managed
	private AntProcessor antProcessor() {
		AntProcessor bean = new AntProcessor();
		bean.setStreamPipeFactory(StreamPipes.simpleFactory());
		String reflexAppDir = System.getProperty("reflex.app.dir");
		if (reflexAppDir != null)
			bean.setAntLibDir(new File(reflexAppDir, "lib"));
		return bean;
	}

}
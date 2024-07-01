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
package devrock.step.framework;


import java.io.File;
import java.util.function.Function;

import devrock.step.api.StepEvaluator;
import hiconic.rx.module.api.service.ServiceDomain;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.platform.RxPlatform;

public abstract class Steps {
	private static RxPlatformContract platform = new RxPlatform().getContract();
	
	public static StepEvaluator evaluator(File exchangeFolder) {
		return evaluator(exchangeFolder, exchangeFolder);
	}
	
	public static StepEvaluator evaluator(File cwd, File exchangeFolder) {
		return evaluator(cwd, exchangeFolder, null);
	}
	
	public static StepEvaluator evaluator(File cwd, File exchangeFolder, Function<String, Object> properties) {
		ServiceDomain mainDomain = platform.serviceDomains().main();
		return new StepEvaluatorImpl(mainDomain.systemCmdResolver(), cwd, exchangeFolder, platform.evaluator(), properties);
	}
}

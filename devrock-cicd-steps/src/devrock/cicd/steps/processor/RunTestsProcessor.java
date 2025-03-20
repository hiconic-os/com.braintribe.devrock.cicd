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
package devrock.cicd.steps.processor;

import static com.braintribe.console.ConsoleOutputs.print;
import static com.braintribe.console.ConsoleOutputs.text;
import static com.braintribe.console.ConsoleOutputs.yellow;

import java.util.List;
import java.util.function.Function;

import com.braintribe.console.ConsoleOutputs;
import com.braintribe.console.output.ConsoleOutput;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.processing.service.api.ReasonedServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;

import devrock.cicd.model.api.RunTest;
import devrock.cicd.model.api.RunTests;
import devrock.cicd.model.api.RunTestsResponse;
import devrock.cicd.model.api.data.LocalArtifact;
import devrock.cicd.model.api.reason.TestsFailed;
import devrock.cicd.steps.processing.BuildHandlers;

public class RunTestsProcessor implements ReasonedServiceProcessor<RunTests, RunTestsResponse> {

	@Override
	public Maybe<? extends RunTestsResponse> processReasoned(ServiceRequestContext context, RunTests request) {
		Function<LocalArtifact, Maybe<?>> handler = BuildHandlers.getHandler(context, request, RunTest.T);

		List<LocalArtifact> tests = request.getCodebaseAnalysis().getBuildTests();

		printInfo(tests);

		TestsFailed testsFailed = TestsFailed.T.create();

		for (LocalArtifact localArtifact : tests) {
			Maybe<?> testResultMaybe = handler.apply(localArtifact);

			if (testResultMaybe.isSatisfied())
				continue;

			if (request.getHaltOnFailure())
				return testResultMaybe.propagateReason();
			else
				testsFailed.causedBy(testResultMaybe.whyUnsatisfied());
		}

		if (!testsFailed.getReasons().isEmpty())
			return testsFailed.asMaybe();

		return Maybe.complete(RunTestsResponse.T.create());

	}

	private void printInfo(List<LocalArtifact> tests) {
		ConsoleOutput info = tests.stream() //
				.map(test -> yellow(test.getFolderName())) //
				.collect(ConsoleOutputs.joiningCollector(text("\n\t"), text("Running tests: "), null));

		print(info);
	}

}

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
package devrock.cicd.steps.wire.space;

import com.braintribe.model.artifact.analysis.AnalysisArtifactResolution;
import com.braintribe.model.processing.meta.editor.ModelMetaDataEditor;
import com.braintribe.wire.api.annotation.Managed;

import devrock.cicd.model.api.AnalyzeCodebase;
import devrock.cicd.model.api.AnalyzeCodebaseResponse;
import devrock.cicd.model.api.BuildArtifacts;
import devrock.cicd.model.api.CheckLinking;
import devrock.cicd.model.api.DistributedLocking;
import devrock.cicd.model.api.EnrichExchangeContext;
import devrock.cicd.model.api.EnvironmentAware;
import devrock.cicd.model.api.InitializeExchange;
import devrock.cicd.model.api.MultiThreadedStepRequest;
import devrock.cicd.model.api.PreparePublishing;
import devrock.cicd.model.api.PreparePublishingResponse;
import devrock.cicd.model.api.PublishArtifacts;
import devrock.cicd.model.api.PublishNpmPackages;
import devrock.cicd.model.api.RaiseAndMergeArtifacts;
import devrock.cicd.model.api.ReadFromExchangeContext;
import devrock.cicd.model.api.RunTests;
import devrock.cicd.model.api.UpdateGithubArtifactIndex;
import devrock.cicd.model.api.data.CodebaseDependencyAnalysis;
import devrock.cicd.model.api.test.Test1Request;
import devrock.cicd.model.api.test.Test2Request;
import devrock.cicd.steps.processor.AnalyzeCodebaseProcessor;
import devrock.cicd.steps.processor.BuildArtifactsProcessor;
import devrock.cicd.steps.processor.CheckLinkingProcessor;
import devrock.cicd.steps.processor.EnrichExchangeContextProcessor;
import devrock.cicd.steps.processor.InitializeExchangeProcessor;
import devrock.cicd.steps.processor.PreparePublishingProcessor;
import devrock.cicd.steps.processor.PublishArtifactsProcessor;
import devrock.cicd.steps.processor.PublishNpmPackagesProcessor;
import devrock.cicd.steps.processor.RaiseAndMergeArtifactsProcessor;
import devrock.cicd.steps.processor.ReadFromExchangeContextProcessor;
import devrock.cicd.steps.processor.RunTestsProcessor;
import devrock.cicd.steps.processor.UpdateGithubArtifactIndexProcessor;
import devrock.cicd.steps.processor.test.Test1Processor;
import devrock.cicd.steps.processor.test.Test2Processor;
import devrock.step.model.api.meta.ArgumentPropagation;
import devrock.step.model.api.meta.ExchangeClassifier;
import devrock.step.model.api.meta.ExchangeConfiguration;
import devrock.step.model.api.meta.ExternalArgument;
import devrock.step.model.api.meta.Intricate;
import devrock.step.model.api.meta.ProjectDir;
import hiconic.rx.module.api.service.ServiceDomainConfiguration;
import hiconic.rx.module.api.wire.RxModuleContract;

@Managed
public class DevrockCicdStepsRxModuleSpace implements RxModuleContract {
	@Override
	public void configureMainServiceDomain(ServiceDomainConfiguration configuration) {
		configuration.bindRequest(Test1Request.T, this::test1Processor);
		configuration.bindRequest(Test2Request.T, this::test2Processor);
		configuration.bindRequest(InitializeExchange.T, this::initializeExchangeProcessor);
		configuration.bindRequest(EnrichExchangeContext.T, this::enrichExchangeContextProcessor);
		configuration.bindRequest(ReadFromExchangeContext.T, this::readFromExchangeContextProcessor);
		configuration.bindRequest(AnalyzeCodebase.T, this::analyzeCodebaseProcessor);
		configuration.bindRequest(PreparePublishing.T, this::prepareCodebaseForPublishingProcessor);
		configuration.bindRequest(BuildArtifacts.T, this::buildArtifactsProcessor);
		configuration.bindRequest(CheckLinking.T, this::checkBuildLinkingProcessor);
		configuration.bindRequest(RunTests.T, this::runTestsProcessor);
		configuration.bindRequest(RaiseAndMergeArtifacts.T, this::raiseAndMergeArtifactsProcessor);
		configuration.bindRequest(PublishArtifacts.T, this::publishArtifactsProcessor);
		configuration.bindRequest(PublishNpmPackages.T, this::publishNpmPackagesProcessor);
		configuration.bindRequest(UpdateGithubArtifactIndex.T, this::updateGithubArtifactIndexProcessor);
		
		configuration.configureModel(this::configureApiModel);
	}

	private void configureApiModel(ModelMetaDataEditor editor) {
		Intricate intricate = Intricate.T.create();
		ArgumentPropagation defaultArgumentPropagation = ArgumentPropagation.T.create();
		
		editor.onEntityType(CodebaseDependencyAnalysis.T).addMetaData(intricate);
		editor.onEntityType(AnalysisArtifactResolution.T).addMetaData(intricate);
		
		ExchangeClassifier exchangeClassifier = ExchangeClassifier.T .create();
		exchangeClassifier.setValue("codebase");

		editor.onEntityType(AnalyzeCodebaseResponse.T).addPropertyMetaData(AnalyzeCodebaseResponse.dependencyResolution, exchangeClassifier);
		editor.onEntityType(PreparePublishingResponse.T).addPropertyMetaData(PreparePublishingResponse.dependencyResolution, exchangeClassifier);

		ExternalArgument defaultExternalArgument = ExternalArgument.T.create();

		editor.onEntityType(MultiThreadedStepRequest.T) //
				.addPropertyMetaData(MultiThreadedStepRequest.threads, defaultExternalArgument);

		editor.onEntityType(BuildArtifacts.T) //
				.addPropertyMetaData(BuildArtifacts.candidateInstall, defaultExternalArgument, defaultArgumentPropagation) //
				.addPropertyMetaData(BuildArtifacts.generateOptionals, defaultExternalArgument, defaultArgumentPropagation) //
				.addPropertyMetaData(BuildArtifacts.skip, defaultExternalArgument);

		ExternalArgument rangeExternalArgument = ExternalArgument.T.create();
		rangeExternalArgument.setName("range");
		editor.onEntityType(AnalyzeCodebase.T).addPropertyMetaData(AnalyzeCodebase.buildArtifacts, rangeExternalArgument);
		
		ProjectDir projectDir = ProjectDir.T.create();
		editor.onEntityType(AnalyzeCodebase.T).addPropertyMetaData(AnalyzeCodebase.path, projectDir);
		editor.onEntityType(EnrichExchangeContext.T).addPropertyMetaData(EnrichExchangeContext.gitPath, projectDir);
		editor.onEntityType(AnalyzeCodebase.T).addPropertyMetaData(AnalyzeCodebase.baseBranch, defaultExternalArgument);
		editor.onEntityType(AnalyzeCodebase.T).addPropertyMetaData(AnalyzeCodebase.baseHash, defaultExternalArgument);
		editor.onEntityType(AnalyzeCodebase.T).addPropertyMetaData(AnalyzeCodebase.baseRemote, defaultExternalArgument);
		editor.onEntityType(AnalyzeCodebase.T).addPropertyMetaData(AnalyzeCodebase.detectUnpublishedArtifacts, defaultExternalArgument);
		editor.onEntityType(EnvironmentAware.T).addPropertyMetaData(EnvironmentAware.ci, defaultExternalArgument);
		
		ExchangeConfiguration exchangeConfiguration = ExchangeConfiguration.T.create();
		
		editor.onEntityType(DistributedLocking.T).addMetaData(exchangeConfiguration);
	}

	
	@Managed
	private Test1Processor test1Processor() {
		return new Test1Processor();
	}
	
	@Managed
	private Test2Processor test2Processor() {
		return new Test2Processor();
	}
	
	@Managed
	private InitializeExchangeProcessor initializeExchangeProcessor() {
		return new InitializeExchangeProcessor();
	}
	
	@Managed
	private EnrichExchangeContextProcessor enrichExchangeContextProcessor() {
		return new EnrichExchangeContextProcessor();
	}

	@Managed
	private ReadFromExchangeContextProcessor readFromExchangeContextProcessor() {
		return new ReadFromExchangeContextProcessor();
	}

	@Managed
	private AnalyzeCodebaseProcessor analyzeCodebaseProcessor() {
		return new AnalyzeCodebaseProcessor();
	}
	
	@Managed
	private BuildArtifactsProcessor buildArtifactsProcessor() {
		return new BuildArtifactsProcessor();
	}
	
	@Managed
	private CheckLinkingProcessor checkBuildLinkingProcessor() {
		return new CheckLinkingProcessor();
	}
	
	@Managed
	private RunTestsProcessor runTestsProcessor() {
		return new RunTestsProcessor();
	}
	
	@Managed
	private RaiseAndMergeArtifactsProcessor raiseAndMergeArtifactsProcessor() {
		return new RaiseAndMergeArtifactsProcessor();
	}
	
	@Managed
	private PublishArtifactsProcessor publishArtifactsProcessor() {
		return new PublishArtifactsProcessor();
	}
	
	@Managed
	private PublishNpmPackagesProcessor publishNpmPackagesProcessor() {
		return new PublishNpmPackagesProcessor();
	}
	
	@Managed
	private PreparePublishingProcessor prepareCodebaseForPublishingProcessor() {
		return new PreparePublishingProcessor();
	}
	
	@Managed
	private UpdateGithubArtifactIndexProcessor updateGithubArtifactIndexProcessor() {
		return new UpdateGithubArtifactIndexProcessor();
	}
}

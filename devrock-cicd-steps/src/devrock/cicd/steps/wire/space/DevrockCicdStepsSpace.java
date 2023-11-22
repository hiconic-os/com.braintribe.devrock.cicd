package devrock.cicd.steps.wire.space;

import com.braintribe.devrock.cicd._DevrockCicdApiModel_;
import com.braintribe.model.artifact.analysis.AnalysisArtifactResolution;
import com.braintribe.model.processing.meta.configured.ConfigurationModelBuilder;
import com.braintribe.model.processing.meta.editor.ModelMetaDataEditor;
import com.braintribe.model.processing.service.common.ConfigurableDispatchingServiceProcessor;
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
import devrock.cicd.model.api.RaiseAndMergeArtifacts;
import devrock.cicd.model.api.RunTests;
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
import devrock.cicd.steps.processor.RaiseAndMergeArtifactsProcessor;
import devrock.cicd.steps.processor.RunTestsProcessor;
import devrock.cicd.steps.processor.test.Test1Processor;
import devrock.cicd.steps.processor.test.Test2Processor;
import devrock.step.api.module.wire.StepModuleContract;
import devrock.step.model.api.meta.ExchangeClassifier;
import devrock.step.model.api.meta.ExchangeConfiguration;
import devrock.step.model.api.meta.ExternalArgument;
import devrock.step.model.api.meta.Intricate;
import devrock.step.model.api.meta.ProjectDir;

@Managed
public class DevrockCicdStepsSpace implements StepModuleContract {

	@Override
	public void addApiModels(ConfigurationModelBuilder builder) {
		builder.addDependency(_DevrockCicdApiModel_.reflection);
	}
	
	@Override
	public void configureApiModel(ModelMetaDataEditor editor) {
		Intricate intricate = Intricate.T.create();
		
		editor.onEntityType(CodebaseDependencyAnalysis.T).addMetaData(intricate);
		editor.onEntityType(AnalysisArtifactResolution.T).addMetaData(intricate);
		
		ExchangeClassifier exchangeClassifier = ExchangeClassifier.T.create();
		exchangeClassifier.setValue("codebase");

		editor.onEntityType(AnalyzeCodebaseResponse.T).addPropertyMetaData(AnalyzeCodebaseResponse.dependencyResolution, exchangeClassifier);
		editor.onEntityType(PreparePublishingResponse.T).addPropertyMetaData(PreparePublishingResponse.dependencyResolution, exchangeClassifier);

		ExternalArgument genericExternalArgument = ExternalArgument.T.create();

		editor.onEntityType(MultiThreadedStepRequest.T) //
				.addPropertyMetaData(MultiThreadedStepRequest.threads, genericExternalArgument);

		editor.onEntityType(BuildArtifacts.T) //
				.addPropertyMetaData(BuildArtifacts.candidateInstall, genericExternalArgument) //
				.addPropertyMetaData(BuildArtifacts.skip, genericExternalArgument);

		ExternalArgument rangeExternalArgument = ExternalArgument.T.create();
		rangeExternalArgument.setName("range");
		editor.onEntityType(AnalyzeCodebase.T).addPropertyMetaData(AnalyzeCodebase.buildArtifacts, rangeExternalArgument);
		
		ProjectDir projectDir = ProjectDir.T.create();
		editor.onEntityType(AnalyzeCodebase.T).addPropertyMetaData(AnalyzeCodebase.path, projectDir);
		editor.onEntityType(EnrichExchangeContext.T).addPropertyMetaData(EnrichExchangeContext.gitPath, projectDir);
		editor.onEntityType(AnalyzeCodebase.T).addPropertyMetaData(AnalyzeCodebase.baseBranch, genericExternalArgument);
		editor.onEntityType(AnalyzeCodebase.T).addPropertyMetaData(AnalyzeCodebase.baseHash, genericExternalArgument);
		editor.onEntityType(AnalyzeCodebase.T).addPropertyMetaData(AnalyzeCodebase.baseRemote, genericExternalArgument);
		editor.onEntityType(AnalyzeCodebase.T).addPropertyMetaData(AnalyzeCodebase.detectUnpublishedArtifacts, genericExternalArgument);
		editor.onEntityType(EnvironmentAware.T).addPropertyMetaData(EnvironmentAware.ci, genericExternalArgument);
		
		ExchangeConfiguration exchangeConfiguration = ExchangeConfiguration.T.create();
		
		editor.onEntityType(DistributedLocking.T).addMetaData(exchangeConfiguration);
	}
	
	@Override
	public void registerProcessors(ConfigurableDispatchingServiceProcessor dispatching) {
		dispatching.register(Test1Request.T, test1Processor());
		dispatching.register(Test2Request.T, test2Processor());
		dispatching.register(InitializeExchange.T, initializeExchangeProcessor());
		dispatching.register(EnrichExchangeContext.T, enrichExchangeContextProcessor());
		dispatching.register(AnalyzeCodebase.T, analyzeCodebaseProcessor());
		dispatching.register(PreparePublishing.T, prepareCodebaseForPublishingProcessor());
		dispatching.register(BuildArtifacts.T, buildArtifactsProcessor());
		dispatching.register(CheckLinking.T, checkBuildLinkingProcessor());
		dispatching.register(RunTests.T, runTestsProcessor());
		dispatching.register(RaiseAndMergeArtifacts.T, raiseAndMergeArtifactsProcessor());
		dispatching.register(PublishArtifacts.T, publishArtifactsProcessor());
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
	private PreparePublishingProcessor prepareCodebaseForPublishingProcessor() {
		return new PreparePublishingProcessor();
	}
}

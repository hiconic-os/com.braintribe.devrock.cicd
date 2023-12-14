package devrock.cicd.steps.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import com.braintribe.console.ConsoleConfiguration;
import com.braintribe.console.PrintStreamConsole;
import com.braintribe.gm.config.yaml.ModeledYamlConfiguration;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.artifact.consumable.Artifact;
import com.braintribe.model.artifact.consumable.Part;
import com.braintribe.model.artifact.essential.PartIdentification;
import com.braintribe.model.resource.Resource;
import com.braintribe.utils.FileTools;
import com.braintribe.utils.IOTools;
import com.braintribe.utils.stream.api.StreamPipe;
import com.braintribe.utils.stream.api.StreamPipes;

import devrock.cicd.model.api.AnalyzeCodebase;
import devrock.cicd.model.api.data.CodebaseAnalysis;
import devrock.cicd.model.api.data.GitContext;
import devrock.cicd.model.api.test.Test1Request;
import devrock.cicd.model.api.test.Test2Request;
import devrock.cicd.model.api.test.TestData;
import devrock.step.api.StepEvaluator;
import devrock.step.framework.Steps;

public class CicdStepsTest {
	@Before
	public void before() throws IOException {
		File exchangeFolder = new File("out/exchange");
		FileTools.deleteDirectoryRecursively(exchangeFolder);
		ConsoleConfiguration.install(new PrintStreamConsole(System.out, true));
	}
	
	@Test
	public void testTransientResources() throws IOException {
		File exchangeFolder = new File("out/exchange");
		exchangeFolder.mkdirs();
		
		StepEvaluator evaluator = Steps.evaluator(exchangeFolder);

		Artifact artifact = Artifact.T.create();
		
		artifact.setGroupId("foo");
		artifact.setArtifactId("bar");
		artifact.setVersion("1.0");
		
		StreamPipe pipe = StreamPipes.simpleFactory().newPipe("test-part");
		String expectedContent = "Hello World!";
		
		try (Writer writer = new OutputStreamWriter(pipe.openOutputStream(), "UTF-8")) {
			writer.write(expectedContent);
		}
		
		Part part = Part.T.create();
		part.setType("txt");
		part.setResource(createTransientTextResource("greets.txt", expectedContent));
		
		String partIdent = PartIdentification.asString(part);
		artifact.getParts().put(partIdent, part);
		
		evaluator.store(artifact);
		
		Artifact artifact1 = evaluator.load(Artifact.T).get();
		
		Part readPart = artifact1.getParts().get(partIdent);
		Resource resource = readPart.getResource();
		
		assertResourceTextContent(expectedContent, resource);
		
		expectedContent = "Thanks";
		
		readPart.setResource(createTransientTextResource("greets.txt", expectedContent));
		
		evaluator.store(artifact);
		Artifact artifact2 = evaluator.load(Artifact.T).get();
		
		readPart = artifact2.getParts().get(partIdent);
		resource = readPart.getResource();
		
		assertResourceTextContent(expectedContent, resource);
	}

	private void assertResourceTextContent(String expectedContent, Resource resource) throws IOException {
		try (InputStream in = resource.openStream()) {
			String content = IOTools.slurp(in, "UTF-8");
			
			Assertions.assertThat(content).isEqualTo(expectedContent);
		}
	}
	
	private Resource createTransientTextResource(String name, String text) throws IOException {
		StreamPipe pipe = StreamPipes.simpleFactory().newPipe("test-part");
		
		try (Writer writer = new OutputStreamWriter(pipe.openOutputStream(), "UTF-8")) {
			writer.write(text);
		}

		Resource resource = Resource.createTransient(pipe::openInputStream);
		resource.setName(name);

		return resource;
	}
	
	@Test
	public void testExchange() {
		File exchangeFolder = new File("out/exchange");
		exchangeFolder.mkdirs();
		StepEvaluator evaluator = Steps.evaluator(exchangeFolder);
		
		evaluator.evaluate(Test1Request.T);
		evaluator.evaluate(Test2Request.T);
		
		ModeledYamlConfiguration exchange = new ModeledYamlConfiguration();
		exchange.setConfigFolder(exchangeFolder);
		TestData config = exchange.config(TestData.T);
		
		Assertions.assertThat(config.getValue1()).isEqualTo("value1");
		Assertions.assertThat(config.getValue2()).isEqualTo("value2");
	}
	
	@Test
	public void testAnalyzeCodebase() {
		File exchangeFolder = new File("out/exchange");
		exchangeFolder.mkdirs();
		
		
		File groupDir = new File("res/codebase");
		
		String customOrigin = "origin-custom";
		
		Map<String, Object> props = Map.of("baseRemote", customOrigin);
		StepEvaluator evaluator = Steps.evaluator(groupDir, exchangeFolder, props::get);
		
		AnalyzeCodebase request = AnalyzeCodebase.T.create();
		evaluator.evaluateOrThrow(request);
		Maybe<CodebaseAnalysis> changedArtifactsMaybe = evaluator.load(CodebaseAnalysis.T);
		CodebaseAnalysis changedArtifacts = changedArtifactsMaybe.get();
		
		Maybe<GitContext> gitContextMaybe = evaluator.load(GitContext.T);
		
		GitContext gitContext = gitContextMaybe.get();
		
		Assertions.assertThat(gitContext.getBaseRemote()).isEqualTo(customOrigin);
	}
}

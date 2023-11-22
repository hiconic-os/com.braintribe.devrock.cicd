package devrock.cicd.steps.test;

import java.io.File;

import com.braintribe.gm.config.yaml.YamlConfigurations;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.processing.vde.clone.async.EntityCollector;

import devrock.cicd.model.api.data.CodebaseDependencyAnalysis;
import devrock.cicd.steps.test.model.Node;

public class YamlConfigurationLab {
	public static void main(String[] args) {
//		Maybe<GitContext> maybe1 = YamlConfigurations.read(GitContext.T) //
//				.placeholders(k -> null)
//				.from(new File("out/gradle-test/git-context.yaml"));
//		
//		
//		if (maybe1.isSatisfied()) {
//			GitContext gitContext = maybe1.get();
//			System.out.println(gitContext);
//		}

//		Maybe<Node> maybe = YamlConfigurations.read(Node.T) //
//				.placeholders(k -> null)
//				.from(new File("res/test.yaml"));
//
//		for (EntityCollector ec: EntityCollector.collectors) {
//			if (ec.getOutstandingCallbacks() != 0) {
//				System.out.println(ec.getAccumulator().entityType() + " -> " + ec.getOutstandingCallbacks());
//			}
//		}
//		
//		if (maybe.isSatisfied()) {
//			Node node = maybe.get();
//			System.out.println(node);
//		}
		
//		Maybe<CodebaseDependencyAnalysis> maybe = YamlConfigurations.read(CodebaseDependencyAnalysis.T) //
//				.placeholders(k -> null)
//				.from(new File("out/gradle-test/codebase-dependency-analysis.yaml"));
//		
//		for (EntityCollector ec: EntityCollector.collectors) {
//			if (ec.getOutstandingCallbacks() != 0) {
//				System.out.println(ec.getAccumulator().entityType() + " -> " + ec.getOutstandingCallbacks());
//			}
//		}
//		
//		if (maybe.isSatisfied()) {
//			CodebaseDependencyAnalysis codebaseDependencyAnalysis = maybe.get();
//			System.out.println(codebaseDependencyAnalysis);
//		}
		
	}
}

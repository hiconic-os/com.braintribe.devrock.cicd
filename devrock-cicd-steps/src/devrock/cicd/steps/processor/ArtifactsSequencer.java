package devrock.cicd.steps.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.braintribe.model.artifact.analysis.AnalysisArtifact;
import com.braintribe.model.artifact.analysis.AnalysisArtifactResolution;
import com.braintribe.model.artifact.analysis.AnalysisDependency;
import com.braintribe.model.artifact.analysis.AnalysisTerminal;

import devrock.cicd.model.api.data.LocalArtifact;

public abstract class ArtifactsSequencer {
	public static List<LocalArtifact> orderSequential(AnalysisArtifactResolution resolution, Collection<LocalArtifact> artifacts) {
		List<LocalArtifact> sequence = new ArrayList<>(artifacts.size());
		Set<AnalysisArtifact> visited = new HashSet<>();
		
		// index local artifacts
		Map<String, LocalArtifact> localArtifactIndex = new HashMap<>();
		
		for (LocalArtifact artifact: artifacts) {
			localArtifactIndex.put(artifact.getArtifactIdentification().getArtifactId(), artifact);
		}
		
		// traverse graph and sequence artifacts in dependency order 
		resolution.getTerminals().stream() //
			.sorted(Comparator.comparing(AnalysisTerminal::getArtifactId))
			.forEach(t -> collect(t, localArtifactIndex, sequence, visited));
		
		return sequence;
	}
	
	private static void collect(AnalysisTerminal terminal, Map<String, LocalArtifact> localArtifactIndex, List<LocalArtifact> sequence, Set<AnalysisArtifact> visited) {
		if (terminal instanceof AnalysisDependency)
			collect((AnalysisDependency)terminal, localArtifactIndex, sequence, visited);
		else if (terminal instanceof AnalysisArtifact)
			collect((AnalysisArtifact)terminal, localArtifactIndex, sequence, visited);
	}
	
	private static void collect(AnalysisArtifact artifact, Map<String, LocalArtifact> localArtifactIndex, List<LocalArtifact> sequence, Set<AnalysisArtifact> visited) {
		if (!visited.add(artifact))
			return;
		
		artifact.getDependencies().stream() //
			.sorted(Comparator.comparing(AnalysisDependency::getArtifactId))
			.forEach(d -> collect(d, localArtifactIndex, sequence, visited));
		
		LocalArtifact localArtifact = localArtifactIndex.get(artifact.getArtifactId());
		
		if (localArtifact != null)
			sequence.add(localArtifact);
	}
	
	private static void collect(AnalysisDependency dependency, Map<String, LocalArtifact> localArtifactIndex, List<LocalArtifact> sequence, Set<AnalysisArtifact> visited) {
		AnalysisArtifact solution = dependency.getSolution();
		if (solution != null)
			collect(solution, localArtifactIndex, sequence, visited);
	}
	
}

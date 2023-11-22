package devrock.cicd.model.api.data;

import java.util.Map;

import com.braintribe.model.artifact.analysis.AnalysisArtifact;
import com.braintribe.model.artifact.analysis.AnalysisArtifactResolution;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface CodebaseDependencyAnalysis extends GenericEntity {
	EntityType<CodebaseDependencyAnalysis> T = EntityTypes.T(CodebaseDependencyAnalysis.class);
	
	String resolution = "resolution";
	String artifactIndex = "artifactIndex";
	
	AnalysisArtifactResolution getResolution();
	void setResolution(AnalysisArtifactResolution resolution);
	
	/**
	 * Maps from artifactId (= folderName) to AnalysisArtifact from the {@link #getResolution()} substructure
	 */
	Map<String, AnalysisArtifact> getArtifactIndex();
	void setArtifactIndex(Map<String, AnalysisArtifact> artifactIndex);
}

package devrock.cicd.model.api.data;

import com.braintribe.model.artifact.essential.VersionedArtifactIdentification;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface LocalArtifact extends GenericEntity {
	EntityType<LocalArtifact> T = EntityTypes.T(LocalArtifact.class);
	
	String folderName = "folderName";
	String identification = "identification";
	String artifactIdentification = "artifactIdentification";
	String buildReason = "buildReason";
	String packaging = "packaging";
	String integrationTest = "integrationTest";
	String test = "test";
	String bundle = "bundle";
	String commitHash = "commitHash";
	
	String getFolderName();
	void setFolderName(String folderName);

	/**
	 * Full name of the artifact such as: group-id:artifact-id#version
	 */
	String getIdentification();
	void setIdentification(String identification);

	VersionedArtifactIdentification getArtifactIdentification();
	void setArtifactIdentification(VersionedArtifactIdentification artifactIdentification);
	
	String getPackaging();
	void setPackaging(String packaging);
	
	BuildReason getBuildReason();
	void setBuildReason(BuildReason buildReason);
	
	boolean getTest();
	void setTest(boolean test);
	
	String getCommitHash();
	void setCommitHash(String commitHash);
	
	boolean getBundle();
	void setBundle(boolean bundle);
	
	boolean getIntegrationTest();
	void setIntegrationTest(boolean integrationTest);
	
	@Override
	default String asString() {
		return getIdentification();
	}
}

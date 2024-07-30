package devrock.cicd.model.api.data;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/**
 * Additional information alongside {@link CodebaseAnalysis}, so that certain information can be easily retrieved
 * without parsing all the {@link LocalArtifact} data.
 */
public interface CodebaseSummary extends GenericEntity {

	EntityType<CodebaseSummary> T = EntityTypes.T(CodebaseSummary.class);

	/**
	 * Specifies whether or not the {@link CodebaseAnalysis#getBuilds()} contains a {@link LocalArtifact} marked as
	 * {@link LocalArtifact#getNpmPackage() npm package}.
	 */
	boolean getHasNpmBuild();
	void setHasNpmBuild(boolean hasNpmBuild);

}

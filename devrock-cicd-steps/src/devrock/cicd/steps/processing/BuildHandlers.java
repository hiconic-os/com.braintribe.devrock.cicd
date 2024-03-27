package devrock.cicd.steps.processing;

import static com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling.getOrTunnel;

import java.util.function.Consumer;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.service.api.ServiceRequestContext;

import devrock.cicd.model.api.HasArtifactHandler;
import devrock.cicd.model.api.RunBuild;
import devrock.cicd.model.api.data.LocalArtifact;
import devrock.step.model.api.StepRequest;

public interface BuildHandlers {
	static <R extends StepRequest & HasArtifactHandler> Consumer<LocalArtifact> getHandler(ServiceRequestContext context, R request, EntityType<? extends RunBuild> runBuildType) {
		Consumer<LocalArtifact> handler = request.getHandler();
		
		if (handler != null)
			return handler;
		
		return artifact -> {
			RunBuild runBuild = runBuildType.create();
			runBuild.setArtifact(artifact);
			runBuild.setCaller(request);
			getOrTunnel(runBuild.eval(context).getReasoned());
		};
	}
}

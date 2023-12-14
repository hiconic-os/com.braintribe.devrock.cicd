package devrock.cicd.steps.processing;

import static com.braintribe.console.ConsoleOutputs.println;
import static com.braintribe.console.ConsoleOutputs.sequence;
import static com.braintribe.console.ConsoleOutputs.text;
import static com.braintribe.console.ConsoleOutputs.yellow;
import static com.braintribe.devrock.mc.core.commons.McOutputs.versionedArtifactIdentification;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.braintribe.console.ConsoleOutputs;
import com.braintribe.devrock.mc.api.deploy.ArtifactDeployer;
import com.braintribe.devrock.mc.api.resolver.ArtifactDataResolution;
import com.braintribe.devrock.mc.api.resolver.ArtifactDataResolver;
import com.braintribe.devrock.mc.core.commons.McOutputs;
import com.braintribe.devrock.mc.core.repository.index.ArtifactIndex;
import com.braintribe.devrock.mc.core.resolver.BasicDependencyResolver;
import com.braintribe.devrock.model.mc.reason.UnresolvedDependencyVersion;
import com.braintribe.devrock.model.repository.MavenHttpRepository;
import com.braintribe.devrock.model.repository.Repository;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.InternalError;
import com.braintribe.gm.model.reason.essential.IoError;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.model.artifact.compiled.CompiledArtifactIdentification;
import com.braintribe.model.artifact.compiled.CompiledDependencyIdentification;
import com.braintribe.model.artifact.consumable.Artifact;
import com.braintribe.model.artifact.consumable.ArtifactResolution;
import com.braintribe.model.artifact.consumable.Part;
import com.braintribe.model.artifact.essential.PartIdentification;
import com.braintribe.model.artifact.essential.VersionedArtifactIdentification;
import com.braintribe.model.resource.Resource;
import com.braintribe.model.version.Version;
import com.braintribe.utils.stream.api.StreamPipe;
import com.braintribe.utils.stream.api.StreamPipes;

import devrock.cicd.model.api.reason.ArtifactIndexUpdateFailed;
import devrock.cicd.steps.processor.locking.DistributedLocking;

public class ArtifactIndexUpdate {

	private static Lock acquireLockArtifactUpdateLock(MavenHttpRepository httpRepository) {
		Function<String,Lock> lockManager = DistributedLocking.lockManager();
		return lockManager.apply("update-artifact-index:" + httpRepository.getUrl());
	}
	
	public static Reason updateArtifactIndex(Repository repository, ArtifactDeployer artifactDeployer, ArtifactDataResolver resolver, List<? extends VersionedArtifactIdentification> publishedArtifacts) {
		return updateArtifactIndex(repository, artifactDeployer, resolver, publishedArtifacts, null, false);
	}
	
	/**
	 * @param cleanup can be different values
	 * 	<ul>
	 * 		<li>a groupId for the group in which a cleanup should take place
	 * 		<li>a * to to make a full cleanup
	 * 		<li>null to skip cleanup
	 * 	</ul>either a specific groupId toor *
	 * @return
	 */
	public static Reason updateArtifactIndex(Repository repository, ArtifactDeployer artifactDeployer, ArtifactDataResolver resolver, List<? extends VersionedArtifactIdentification> publishedArtifacts, String cleanup, boolean updateOnlyIfUnknown) {
		if (!(repository instanceof MavenHttpRepository))
			return null;
		
		Lock lock = acquireLockArtifactUpdateLock((MavenHttpRepository)repository);
		
		try {
			ConsoleOutputs.println("Trying to lock for artifact index update for repo " + repository.getName());
			if (!lock.tryLock(3, TimeUnit.MINUTES)) {
				return Reasons.build(ArtifactIndexUpdateFailed.T).text("Could not acquire update lock").toReason();
			}
			
			ConsoleOutputs.println("Locked for artifact index update for repo " + repository.getName());
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		try {
			ConsoleOutputs.println("Updating artifact index for repo " + repository.getName());
			
			CompiledDependencyIdentification indexCdi = CompiledDependencyIdentification.create("meta", "artifact-index", "[1,)");
			
			
			BasicDependencyResolver dependencyResolver = new BasicDependencyResolver(resolver);
			Maybe<CompiledArtifactIdentification> caiMaybe = dependencyResolver.resolveDependency(indexCdi);

			final CompiledArtifactIdentification indexCai; 
			final ArtifactIndex artifactIndex;
			
			if (caiMaybe.isUnsatisfied()) {
				if (caiMaybe.isUnsatisfiedBy(UnresolvedDependencyVersion.T)) {
					indexCai = CompiledArtifactIdentification.create("meta", "artifact-index", "1");
					artifactIndex = new ArtifactIndex(true);
				}
				else {
					return Reasons.build(ArtifactIndexUpdateFailed.T).text("Error while retrieving existing artifact index") //
							.cause(caiMaybe.whyUnsatisfied()).toReason();
				}
			}
			else {
				indexCai = caiMaybe.get();

				Maybe<ArtifactIndex> indexMaybe = downloadIndex(indexCai, resolver);
				
				if (indexMaybe.isUnsatisfied()) {
					if (indexMaybe.isUnsatisfiedBy(NotFound.T)) {
						artifactIndex = new ArtifactIndex(true);
					}
					else {
						return Reasons.build(ArtifactIndexUpdateFailed.T).text("Error while retrieving existing artifact index") //
								.cause(indexMaybe.whyUnsatisfied()).toReason();
					}
				}
				else {
					Version version = indexCai.getVersion();
					version.setMajor(version.getMajor() + 1);
					
					artifactIndex = indexMaybe.get();
				}
			}
			
			for (VersionedArtifactIdentification artifact: publishedArtifacts) {
				String artifactAsString = artifact.asString();
				
				if (updateOnlyIfUnknown && artifactIndex.get(artifactAsString) != null)
					continue;
				
				artifactIndex.update(artifactAsString);
			}
			
			if (cleanup != null)
				deleteObsoletesFromIndex(artifactIndex, cleanup, publishedArtifacts);
			
			Reason error = uploadIndex(indexCai, artifactDeployer, artifactIndex);
			
			if (error != null) {
				return Reasons.build(ArtifactIndexUpdateFailed.T).text("Error while updating artifact index") //
						.cause(error).toReason();
			}
			
			return null;
		}
		finally {
			lock.unlock();
			ConsoleOutputs.println("Unlocked for artifact index update for repo " + repository.getName());
		}
	}
	
	private static void deleteObsoletesFromIndex(ArtifactIndex artifactIndex, String cleanup, List<? extends VersionedArtifactIdentification> publishedArtifacts) {
		Map<String, Set<String>> existingArtifactsByGroupId = new HashMap<>();
		
		for (VersionedArtifactIdentification vai: publishedArtifacts) {
			existingArtifactsByGroupId.computeIfAbsent(vai.getGroupId(), k -> new HashSet<>()).add(vai.asString());
		}
		
		println(yellow("Deleting non existent artifacts from artifact index:"));
		
		Predicate<String> cleanupPredicate = cleanup.equals("*")? g -> true : Predicate.isEqual(cleanup);
		
		for (String artifact: artifactIndex.getArtifacts()) {
			VersionedArtifactIdentification vai = VersionedArtifactIdentification.parse(artifact);
			String groupId = vai.getGroupId();
			
			Set<String> existingArtifacts = existingArtifactsByGroupId.getOrDefault(groupId, Collections.emptySet());
			
			// restrict the cleanup to mentioned groups 
			if (cleanupPredicate.test(artifact) && !existingArtifacts.contains(artifact)) {
				println(
					sequence(
						text("  "),
						versionedArtifactIdentification(vai)
					)
				);
				
				artifactIndex.delete(artifact);
			}
		}
	}

	private static Reason uploadIndex(CompiledArtifactIdentification indexCai, ArtifactDeployer deployer, ArtifactIndex index) {
		StreamPipe pipe = StreamPipes.fileBackedFactory().newPipe("artifact-index");
		
		try (GZIPOutputStream out = new GZIPOutputStream(pipe.openOutputStream())) {
			index.write(out);
		}
		catch (IOException e) {
			return Reasons.build(IoError.T).text("Error while writing " + indexCai.asString()).cause(InternalError.from(e)).toReason();
		}
		
		Resource resource = Resource.createTransient(pipe::openInputStream);
		resource.setName("artifact-index.gz");
		
		PartIdentification partIdentification = PartIdentification.create("gz");
		
		Artifact artifact = Artifact.T.create();
		artifact.setGroupId(indexCai.getGroupId());
		artifact.setArtifactId(indexCai.getArtifactId());
		artifact.setVersion(indexCai.getVersion().asString());
		
		Part part = Part.T.create();
		part.setType(partIdentification.getType());
		part.setResource(resource);
		
		artifact.getParts().put(partIdentification.asString(), part);
		
		ArtifactResolution resolution = deployer.deploy(artifact);
		
		if (resolution.hasFailed())
			return resolution.getFailure();
		
		return null;
	}
	
	private static Maybe<ArtifactIndex> downloadIndex(CompiledArtifactIdentification indexCai, ArtifactDataResolver resolver) {
		Maybe<ArtifactDataResolution> indexPartMaybe = resolver.resolvePart(indexCai, PartIdentification.create("gz"));
		
		if (indexPartMaybe.isUnsatisfied()) {
			return indexPartMaybe.whyUnsatisfied().asMaybe();
		}
		
		ArtifactDataResolution partResolution = indexPartMaybe.get();
		Maybe<InputStream> inMaybe = partResolution.openStream();
		
		if (inMaybe.isUnsatisfied()) {
			return inMaybe.whyUnsatisfied().asMaybe();
		}
		
		try (InputStream in = new GZIPInputStream(inMaybe.get())) {
			return Maybe.complete(ArtifactIndex.read(in, true));
		}
		catch (IOException e) {
			return Reasons.build(IoError.T).text("Error while reading " + indexCai.asString()).cause(InternalError.from(e)).toMaybe();
		}
	}
}

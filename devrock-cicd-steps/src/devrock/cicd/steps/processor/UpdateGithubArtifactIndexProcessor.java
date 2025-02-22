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
package devrock.cicd.steps.processor;

import static com.braintribe.console.ConsoleOutputs.println;
import static com.braintribe.console.ConsoleOutputs.sequence;
import static com.braintribe.console.ConsoleOutputs.text;
import static com.braintribe.devrock.mc.core.commons.McOutputs.versionedArtifactIdentification;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.braintribe.codec.marshaller.json.JsonStreamMarshaller;
import com.braintribe.console.ConsoleOutputs;
import com.braintribe.devrock.mc.api.commons.VersionInfo;
import com.braintribe.devrock.mc.api.deploy.ArtifactDeployer;
import com.braintribe.devrock.mc.api.resolver.ArtifactDataResolver;
import com.braintribe.devrock.mc.core.wirings.backend.ArtifactDataBackendModule;
import com.braintribe.devrock.mc.core.wirings.backend.contract.ArtifactDataBackendContract;
import com.braintribe.devrock.model.repository.IndexedMavenHttpRepository;
import com.braintribe.devrock.model.repository.MavenHttpRepository;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling;
import com.braintribe.model.artifact.essential.ArtifactIdentification;
import com.braintribe.model.artifact.essential.VersionedArtifactIdentification;
import com.braintribe.model.version.Version;
import com.braintribe.utils.Base64;
import com.braintribe.wire.api.Wire;
import com.braintribe.wire.api.context.WireContext;

import devrock.cicd.model.api.UpdateGithubArtifactIndex;
import devrock.cicd.model.api.UpdateGithubArtifactIndexResponse;
import devrock.cicd.model.api.reason.ArtifactIndexUpdateFailed;
import devrock.cicd.steps.processing.ArtifactIndexUpdate;

public class UpdateGithubArtifactIndexProcessor extends SpawningServiceProcessor<UpdateGithubArtifactIndex, UpdateGithubArtifactIndexResponse> {
	private static final JsonStreamMarshaller marshaller = new JsonStreamMarshaller();
	
	@Override
	protected StatefulServiceProcessor spawn() {
		return new StatefulUpdater();
	}
	
	private class StatefulUpdater extends StatefulServiceProcessor {

		private String repositoryUrl;
		private MavenHttpRepository repository;
		private ArtifactDeployer artifactDeployer;
		private ArtifactDataResolver resolver;
		private HttpClient httpClient;

		@Override
		protected Maybe<? extends UpdateGithubArtifactIndexResponse> process() {
            initialize();
            
            try (WireContext<ArtifactDataBackendContract> wireContext = Wire.context(ArtifactDataBackendModule.INSTANCE)) {
				ArtifactDataBackendContract artifactDataBackendContract = wireContext.contract();
				
				artifactDeployer = artifactDataBackendContract.artifactDeployer(repository);
				resolver = artifactDataBackendContract.repository(repository);
				return processWithWireContext();
			}
		}
		
		private Maybe<? extends UpdateGithubArtifactIndexResponse> processWithWireContext() {
			if (request.getGroup() != null)
				ConsoleOutputs.println("Updating Artifact Index for artifacts in group " + request.getGroup() + " in repository " + repositoryUrl);
			else 
				ConsoleOutputs.println("Updating Artifact Index for all artifacts in repository: " + repositoryUrl);
			
			// make reasoned
            Set<ArtifactIdentification> artifacts = findArtifacts();
            artifacts = filterArtifacts(artifacts);
            System.out.println("filtered artifacts: " + artifacts);
         
            Maybe<Set<VersionedArtifactIdentification>> versionedArtifactsMaybe = resolveVersions(artifacts);
            
            if (versionedArtifactsMaybe.isUnsatisfied()) {
            	return versionedArtifactsMaybe.emptyCast();
            }
            
            Set<VersionedArtifactIdentification> versionedArtifacts = versionedArtifactsMaybe.get();
            
            Comparator<ArtifactIdentification> comparator = Comparator //
            	.comparing(ArtifactIdentification::getGroupId) //
            	.thenComparing(ArtifactIdentification::getArtifactId);
            
            List<VersionedArtifactIdentification> sortedArtifacts = versionedArtifacts.stream() //
            	.sorted(comparator) //
            	.collect(Collectors.toList());
            
            ConsoleOutputs.println("Found " + sortedArtifacts.size() + " artifacts in repository " + repositoryUrl);
            
            for (VersionedArtifactIdentification artifact: sortedArtifacts) {
            	println(sequence(
            			text("  "),
            			versionedArtifactIdentification(artifact)
            	));
            }
            
            Reason error = ArtifactIndexUpdate.updateArtifactIndex(repository, artifactDeployer, resolver, sortedArtifacts, request.getGroup(), true);
            
            if (error != null)
            	return error.asMaybe();
            
			UpdateGithubArtifactIndexResponse response = UpdateGithubArtifactIndexResponse.T.create();
			
			return Maybe.complete(response);
		}

		private Set<ArtifactIdentification> filterArtifacts(Set<ArtifactIdentification> artifacts) {
			String groupId = request.getGroup();
			
			Predicate<ArtifactIdentification> filter = a -> !a.getGroupId().equals("meta");
			
			if (groupId != null && !groupId.isEmpty())
				filter = filter.and(a -> a.getGroupId().equals(groupId));
			
			return artifacts.stream().filter(filter).collect(Collectors.toSet());
		}

		private void initialize() {
			try {
				repositoryUrl = new URI("https", request.getToken(), "maven.pkg.github.com", -1, "/" + URLEncoder.encode(request.getOrganization(), StandardCharsets.UTF_8) + "/" + URLEncoder.encode(request.getRepository(), StandardCharsets.UTF_8), null, null).toString();
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
			
			repository = buildGithubMavenRepository();
			
            httpClient = HttpClient.newBuilder() //
            		.build();
		}

		private Maybe<Set<VersionedArtifactIdentification>> resolveVersions(Set<ArtifactIdentification> identifications) {
			Set<VersionedArtifactIdentification> result = ConcurrentHashMap.newKeySet();
			
			try {
				identifications.parallelStream().forEach(identifiation -> {
					Maybe<List<VersionInfo>> versionsMaybe = resolver.getVersionsReasoned(identifiation);
					List<VersionInfo> versions = UnsatisfiedMaybeTunneling.getOrTunnel(versionsMaybe);
					
					for (VersionInfo versionInfo: versions) {
						Version version = versionInfo.version();
						VersionedArtifactIdentification vai = VersionedArtifactIdentification.create(identifiation.getGroupId(), identifiation.getArtifactId(), version.asString());
						result.add(vai);
					}
				});
			}
			catch (UnsatisfiedMaybeTunneling e) {
				return Reasons.build(ArtifactIndexUpdateFailed.T) //
					.text("Error while resolving packages versions via maven repo from " + repository.getUrl()) //
					.cause(e.getMaybe().whyUnsatisfied()) //
					.toMaybe();
			}
			
			return Maybe.complete(result);
		}
		
		private MavenHttpRepository buildGithubMavenRepository() {
			IndexedMavenHttpRepository githubRepository = IndexedMavenHttpRepository.T.create();
			githubRepository.setName(request.getRepository());
			githubRepository.setUrl(repositoryUrl);
			return githubRepository;
		}

		private Set<ArtifactIdentification> findArtifacts() {
			Set<ArtifactIdentification> allArtifacts = new LinkedHashSet<>();
			for (int i = 1; ; i++) {
				URI uri = buildUri(i);
				List<String> artifacts = readArtifacts(uri, httpClient);
				if (artifacts.isEmpty())
					break;
				
				System.out.println("Found artifacts: " + artifacts);
				
				for (String artifact: artifacts) {
					int index = artifact.lastIndexOf('.');
					
					if (index == -1)
						continue;

					
					String groupId = artifact.substring(0, index);
					String artifactId = artifact.substring(index + 1);
					ArtifactIdentification artifactIdentification = ArtifactIdentification.create(groupId, artifactId);
					allArtifacts.add(artifactIdentification);
					System.out.println("Extracted artifact: " + artifactIdentification);
				}
			}
			
			return allArtifacts;
		}

		private List<String> readArtifacts(URI uri, HttpClient httpClient) {
			HttpRequest httpRequest = HttpRequest.newBuilder() //
					.uri(uri) //
					.header("Authorization", "Basic " + Base64.encodeString(request.getToken()))
					.GET() //
					.header("Accept", "application/vnd.github.v3+json") //
					.build();
			
			BodyHandler<String> bodyHandler = BodyHandlers.ofString();
			
			final HttpResponse<String> httpResponse;
			
			try {
				httpResponse = httpClient.send(httpRequest, bodyHandler);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			} catch (InterruptedException e) {
				throw new IllegalStateException(e);
			}
			
			int statusCode = httpResponse.statusCode();
			
			if (statusCode >= 200 && statusCode < 300) {
				
				List<Map<String, Object>> packages = decodeJson(httpResponse.body());
				
				List<String> artifacts = packages.stream().map(p -> (String)p.get("name")).collect(Collectors.toList());
				return artifacts;
			}
			
			throw new IllegalStateException("Unexpected HTTP result with status code " + statusCode + ": " + httpResponse.body());
		}
		
		URI buildUri(int page) {
			try {
				return new URI("https", request.getToken(), "api.github.com", -1, "/orgs/" + request.getOrganization() + "/packages", "package_type=maven&per_page=100&page=" + page, null);
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
		}
		
		List<Map<String, Object>> decodeJson(String json) {
			List<Map<String, Object>> data = (List<Map<String, Object>>) marshaller.decode(json);
			
			return data;
		}
	}
}

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
package devrock.cicd.github.notification.processing;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Map;

import com.braintribe.codec.marshaller.api.GmSerializationOptions;
import com.braintribe.codec.marshaller.json.JsonStreamMarshaller;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.CommunicationError;
import com.braintribe.gm.model.reason.essential.InternalError;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.gm.model.security.reason.AuthenticationFailure;
import com.braintribe.gm.model.security.reason.Forbidden;
import com.braintribe.logging.Logger;
import com.braintribe.model.generic.GMF;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EssentialTypes;
import com.braintribe.model.processing.service.api.ProceedContext;
import com.braintribe.model.processing.service.api.ReasonedServiceAroundProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.utils.lcd.LazyInitialized;

import devrock.cicd.github.notification.model.api.GitHubNotificationConnection;
import devrock.cicd.github.notification.model.api.GitHubNotificationMapping;
import devrock.cicd.github.notification.model.api.RequestLabelMapping;
import devrock.cicd.github.notification.model.api.reason.GitHubLabelAttachmentFailed;
import devrock.cicd.github.notification.model.api.reason.GitHubNotificationConfigurationError;
import devrock.step.api.StepExchangeContext;
import devrock.step.api.StepExchangeContextAttribute;
import devrock.step.model.api.StepRequest;

public class GitHubLabelNotifier implements ReasonedServiceAroundProcessor<StepRequest, Object> {
	private static final Logger logger = Logger.getLogger(GitHubLabelNotifier.class);
	private static JsonStreamMarshaller marshaller = new JsonStreamMarshaller();
	
	private LazyInitialized<HttpClient> httpClient = new LazyInitialized<>(HttpClient::newHttpClient);
	@Override
	public Maybe<? extends Object> processReasoned(ServiceRequestContext context, StepRequest request,
			ProceedContext proceedContext) {
		
		
		Maybe<Object> maybe = proceedContext.proceedReasoned(request);
		
		if (maybe.isSatisfied()) {
			StepExchangeContext exchangeContext = context.getAttribute(StepExchangeContextAttribute.class);

			Maybe<GitHubNotificationConnection> connectionMaybe = exchangeContext.load(GitHubNotificationConnection.T);
			Maybe<GitHubNotificationMapping> mappingMaybe = exchangeContext.load(GitHubNotificationMapping.T);

			if (connectionMaybe.isUnsatisfiedBy(NotFound.T) && mappingMaybe.isUnsatisfiedBy(NotFound.T))
				return maybe;
			
			LazyInitialized<Reason> error = new LazyInitialized<>(() -> Reasons.build(GitHubNotificationConfigurationError.T).text("Error while reading github configuration from step exchange").toReason());
			
			connectionMaybe.ifUnsatisfied(r -> error.get().causedBy(r));
			mappingMaybe.ifUnsatisfied(r -> error.get().causedBy(r));
			
			if (error.isInitialized())
				return error.get().asMaybe();
		
			GitHubNotificationMapping mapping = mappingMaybe.get();
			
			RequestLabelMapping requestLabel = findRequestLabel(request.entityType(), mapping);
			
			if (requestLabel != null) {
				Reason labelError = addLabel(connectionMaybe.get(), requestLabel);
				if (labelError != null)
					return Reasons.build(GitHubLabelAttachmentFailed.T) //
							.text("Error while attaching label to PR: " + connectionMaybe.get().getIssue()) //
							.cause(labelError)
							.toMaybe();
			}
		}
		
		return maybe;
	}
	
	private Reason addLabel(GitHubNotificationConnection connection, RequestLabelMapping requestLabel) {
		
		URI uri = URI.create("https://api.github.com/repos/" //
				+ connection.getOrganization() //
				+ "/" //
				+ connection.getRepository() //
				+ "/issues/" //
				+ connection.getIssue() //
				+ "/labels");
				
		String json = "{\"labels\":[\"" + requestLabel.getLabel() + "\"]}";
		
		HttpRequest request = HttpRequest.newBuilder() //
			.uri(uri) //
			.header("Authorization", "Bearer " + connection.getGitHubToken()) //
			.header("Accept", "application/vnd.github.v3+json") //
			.header("Content-Type", "application/json") //
			.POST(BodyPublishers.ofString(json)) //
			.build();
		
		try {
			HttpResponse<String> response = httpClient.get().send(request, BodyHandlers.ofString());
			int statusCode = response.statusCode();
			
			switch (statusCode) {
			case 401:
				return Reasons.build(AuthenticationFailure.T).text("Authentication failed for request to: " + uri).toReason();
			case 403:
				return Reasons.build(Forbidden.T).text("Authorization failed for request to: " + uri).toReason();
			}
			
			if (!(statusCode >= 200 && statusCode < 300)) {
				return Reasons.build(CommunicationError.T).text("HTTP status code error " + statusCode + ": " + response.body()).toReason();
			}
			
			return null;
			
//			String body = response.body();
//			
//			boolean isLabelConfigured = isLabelConfigured(body, requestLabel.getLabel());
//			
//			if (isLabelConfigured)
//				return null;
//
//			return configureLabel(connection, requestLabel);
			
		} catch (Exception e) {
			logger.error("Exception while adding label via " + uri, e);
			return InternalError.from(e);
		}
	}
	
	private Reason configureLabel(GitHubNotificationConnection connection, RequestLabelMapping requestLabel) {
		URI uri = URI.create("https://api.github.com/repos/" //
				+ connection.getOrganization() //
				+ "/" //
				+ connection.getRepository() //
				+ "/issues/" //
				+ connection.getIssue() //
				+ "/labels");
				
		String json = "{\"labels\":[\"" + requestLabel.getLabel() + "\"]}";
		
		HttpRequest request = HttpRequest.newBuilder() //
			.uri(uri) //
			.header("Authorization", connection.getGitHubToken()) //
			.header("Accept", "application/vnd.github.v3+json") //
			.header("Content-Type", "application/json") //
			.POST(BodyPublishers.ofString(json)) //
			.build();
		
		try {
			HttpResponse<String> response = httpClient.get().send(request, BodyHandlers.ofString());
			int statusCode = response.statusCode();
			
			switch (statusCode) {
			case 401:
				return Reasons.build(AuthenticationFailure.T).text("Authentication failed for request to: " + uri).toReason();
			case 403:
				return Reasons.build(Forbidden.T).text("Authorization failed for request to: " + uri).toReason();
			}
			
			if (!(statusCode >= 200 && statusCode < 300)) {
				return Reasons.build(CommunicationError.T).text("HTTP status code error " + statusCode + ": " + response.body()).toReason();
			}

			return null;
			
		} catch (Exception e) {
			return InternalError.from(e);
		}
	}

	private boolean isLabelConfigured(String json, String labelName) {
		List<Object> labels = readListFromJson(json);
		
		for (Object value: labels) {
			Map<String, Object> label = (Map<String, Object>)value;
			String name = (String)label.get("name");
			
			if (labelName.equals(name)) {
				return ((String)label.get("description")) != null;
			}
		}
		
		return false;
	}
	
	private List<Object> readListFromJson(String json) {
		
		GmSerializationOptions options = GmSerializationOptions.deriveDefaults().setInferredRootType(GMF.getTypeReflection().getListType(EssentialTypes.TYPE_OBJECT)).build();
		
		List<Object> data = (List<Object>) marshaller.decode(json);
		
		return data;
	}

	private RequestLabelMapping findRequestLabel(EntityType<? extends StepRequest> requestType, GitHubNotificationMapping mapping) {
		String shorthand = requestType.getShortName();
		String signature = requestType.getTypeSignature();
		
		for (RequestLabelMapping requestLabel: mapping.getRequestLabelMappings()) {
			String request = requestLabel.getRequest();
			if (shorthand.equals(request) || signature.equals(request))
				return requestLabel;
		}
		return null;
	}
}

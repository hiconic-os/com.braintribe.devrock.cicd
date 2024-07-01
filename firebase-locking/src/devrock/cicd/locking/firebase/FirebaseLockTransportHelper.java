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
package devrock.cicd.locking.firebase;

import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.util.Base64;
import java.util.Base64.Encoder;

import com.braintribe.codec.marshaller.api.EntityRecurrenceDepth;
import com.braintribe.codec.marshaller.api.GmDeserializationOptions;
import com.braintribe.codec.marshaller.api.GmSerializationOptions;
import com.braintribe.codec.marshaller.api.TypeExplicitness;
import com.braintribe.codec.marshaller.api.TypeExplicitnessOption;
import com.braintribe.codec.marshaller.json.JsonStreamMarshaller;

import devrock.cicd.lock.data.model.LockRecord;

public class FirebaseLockTransportHelper {
	private static final Encoder KEY_ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final JsonStreamMarshaller marshaller = new JsonStreamMarshaller();
	private static GmSerializationOptions marshallerSerializationOptions = GmSerializationOptions.deriveDefaults() //
			.setInferredRootType(LockRecord.T) //
			.set(TypeExplicitnessOption.class, TypeExplicitness.never) //
			.set(EntityRecurrenceDepth.class, 0) //
			.build();
	
	private static GmDeserializationOptions marshallerDeserializationOptions = GmDeserializationOptions.deriveDefaults() //
			.setInferredRootType(LockRecord.T) //
			.build();
	
	private String tableUri;
	private String accessToken;
	
	public FirebaseLockTransportHelper(String tableUri, String accessToken) {
		super();
		this.tableUri = tableUri;
		this.accessToken = accessToken;
	}

	public String encode(LockRecord lockRecord) {
		return marshaller.encode(lockRecord, marshallerSerializationOptions);
	}
	
	public LockRecord decode(String encoded) {
		return (LockRecord) marshaller.decode(encoded, marshallerDeserializationOptions);
	}
	
	private URI buildItemUri(String key) {
		try {
			String encodedKey = KEY_ENCODER.encodeToString(key.getBytes("UTF-8"));
			return URI.create(tableUri + encodedKey + ".json");
		} catch (UnsupportedEncodingException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	public HttpRequest.Builder buildRequest(String key) {
		URI uri = buildItemUri(key);
		
		if (accessToken != null)
			uri = URI.create(uri.toString() + "?auth="+accessToken);
		
		Builder builder = HttpRequest.newBuilder() //
				.uri(uri);
		
		return builder;
	}
}

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
package devrock.pom;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.Date;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.model.resource.Resource;
import com.braintribe.utils.DOMTools;
import com.braintribe.utils.stream.api.StreamPipe;
import com.braintribe.utils.stream.api.StreamPipes;

import devrock.cicd.model.api.reason.PomCommitHashUpdateFailed;
import devrock.cicd.model.api.reason.PomVersionUpdateFailed;
import devrock.xml.XmlUtils;

public class PomTools {
	
	public static Maybe<Document> getDocumentWithChangeVersion(File pomFile, String version) {
		Maybe<Document> documentMaybe = XmlUtils.readDocument(pomFile);
		
		if (documentMaybe.isUnsatisfied())
			return Reasons.build(PomVersionUpdateFailed.T) //
					.text("could not update version in pom [" + pomFile.getAbsolutePath() + "]") //
					.cause(documentMaybe.whyUnsatisfied()) //
					.toMaybe();
		
		// retrieve current revision of working copy
		Document document = documentMaybe.get();
		
		// modify revision to target version
		Element documentElement = document.getDocumentElement();
		Element versionElement = DOMTools.getFirstElement(documentElement, "version");
		
		if (versionElement == null) {
			return Reasons.build(PomVersionUpdateFailed.T) //
					.text("could not find version element in pom [" + pomFile.getAbsolutePath() + "]") //
					.toMaybe();
		}
		
		versionElement.setTextContent(version);
		
		return documentMaybe;
	}

	public static Reason addCommitHash(File pomFile, String commitHash) {
		Maybe<Document> documentMaybe = XmlUtils.readDocument(pomFile);
		
		if (documentMaybe.isUnsatisfied())
			return Reasons.build(PomCommitHashUpdateFailed.T) //
					.text("could not update commit hash in pom [" + pomFile.getAbsolutePath() + "]") //
					.cause(documentMaybe.whyUnsatisfied()) //
					.toReason();
		
		Document document = documentMaybe.get();
		
		addProperty(document, "commit-hash", commitHash);
		
		Reason error = XmlUtils.writeXml(pomFile, document);
		
		if (error != null)
			Reasons.build(PomCommitHashUpdateFailed.T) //
				.text("could not update commit hash in pom [" + pomFile.getAbsolutePath() + "]") //
				.cause(error) //
				.toReason();
		
		return null;
	}
	
	public static Reason addProperties(File pomFile, Map<String, String> properties) {
		Maybe<Document> documentMaybe = XmlUtils.readDocument(pomFile);
		
		if (documentMaybe.isUnsatisfied())
			return Reasons.build(PomCommitHashUpdateFailed.T) //
					.text("could not update properties " + properties + " in pom [" + pomFile.getAbsolutePath() + "]") //
					.cause(documentMaybe.whyUnsatisfied()) //
					.toReason();
		
		Document document = documentMaybe.get();
		
		for (Map.Entry<String, String> entry: properties.entrySet()) {
			String name = entry.getKey();
			String value = entry.getValue();
			addProperty(document, name, value);
		}
		
		Reason error = XmlUtils.writeXml(pomFile, document);
		
		if (error != null)
			Reasons.build(PomCommitHashUpdateFailed.T) //
			.text("could not update properties " + properties + " in pom [" + pomFile.getAbsolutePath() + "]") //
			.cause(error) //
			.toReason();
		
		return null;
	}
	
	private static void addProperty(Document document, String name, String value) {
		Element documentElement = document.getDocumentElement();
		Element propertiesElement = DOMTools.getFirstElement(documentElement, "properties");
		
		if (propertiesElement == null) {
			Element versionElement = DOMTools.getFirstElement(documentElement, "version");
			propertiesElement = document.createElement("properties");
			documentElement.insertBefore(propertiesElement, versionElement.getNextSibling());
			documentElement.insertBefore(document.createTextNode("\n    "), versionElement.getNextSibling());
		}
		
		if (propertiesElement.getFirstChild() == null) {
			propertiesElement.appendChild(document.createTextNode("\n    "));
		}
		
		Element propertyElement = document.createElement(name);
		propertyElement.setTextContent(value);
		propertiesElement.insertBefore(document.createTextNode("    "), null);
		propertiesElement.insertBefore(propertyElement, null);
		propertiesElement.insertBefore(document.createTextNode("\n    "), null);
	}
	
	public static Reason changeVersion(File pomFile, String version) {
		Maybe<Document> documentMaybe = getDocumentWithChangeVersion(pomFile, version);
		
		if (documentMaybe.isUnsatisfied())
			return documentMaybe.whyUnsatisfied();

		Document document = documentMaybe.get();
		
		Reason error = XmlUtils.writeXml(pomFile, document);
		
		if (error != null)
			Reasons.build(PomVersionUpdateFailed.T) //
				.text("could not update version in pom [" + pomFile.getAbsolutePath() + "]") //
				.cause(error) //
				.toReason();
		
		return null;
	}
	
	public static Maybe<Resource> getResourceWithChangeVersioned(File pomFile, String version) {
		Maybe<Document> documentMaybe = getDocumentWithChangeVersion(pomFile, version);
		
		if (documentMaybe.isUnsatisfied())
			return documentMaybe.whyUnsatisfied().asMaybe();

		// retrieve current revision of working copy
		Document document = documentMaybe.get();
		
		StreamPipe pipe = StreamPipes.simpleFactory().newPipe("pom-version-rewrite");
		
		try (Writer writer = new OutputStreamWriter(pipe.openOutputStream(), "UTF-8")) {
			XmlUtils.writeXml(document, writer);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		
		Resource resource = Resource.createTransient(pipe::openInputStream);
		resource.setName("pom.xml");
		resource.setCreated(new Date());
		resource.setMimeType("application/xml");
		
		return Maybe.complete(resource);
	}
}

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
package devrock.xml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.IoError;
import com.braintribe.gm.model.reason.essential.ParseError;

public class XmlUtils {
	public static void writeXml(final Node node, Writer writer) throws IOException {
		try {
			final DOMImplementationLS impl = (DOMImplementationLS) DOMImplementationRegistry.newInstance()
					.getDOMImplementation("LS");

			final LSSerializer lsSerializer = impl.createLSSerializer();
			// see
			// https://xerces.apache.org/xerces2-j/javadocs/api/org/w3c/dom/ls/LSSerializer.html#getDomConfig()
			// lsSerializer.getDomConfig().setParameter("format-pretty-print",
			// Boolean.TRUE);
			lsSerializer.getDomConfig().setParameter("xml-declaration", Boolean.FALSE);
			lsSerializer.getDomConfig().setParameter("comments", Boolean.TRUE);

			final LSOutput destination = impl.createLSOutput();
			destination.setEncoding("UTF-8");
			destination.setCharacterStream(writer);

			lsSerializer.write(node, destination);
			writer.write("\n");
		} catch (IOException e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
		
	public static Reason writeXml(File file, Document document) {
		try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
			writeXml(document, writer);
			return null;
		} catch (IOException e) {
			return Reasons.build(IoError.T).text(e.getMessage()).toReason();
		}
	}
	
	private static DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance(); 
	
	static {
		builderFactory.setNamespaceAware(true);
	}
	
	public static Maybe<Document> readDocument(File file) {
		try { 
			return Maybe.complete(builderFactory.newDocumentBuilder().parse(file));
		}
		catch (SAXException e) {
			return Reasons.build(ParseError.T).text(e.getMessage()).toMaybe();
		}
		catch (IOException e) {
			return Reasons.build(IoError.T).text(e.getMessage()).toMaybe();
		} catch (ParserConfigurationException e) {
			throw new IllegalStateException(e);
		}
	}
	
}

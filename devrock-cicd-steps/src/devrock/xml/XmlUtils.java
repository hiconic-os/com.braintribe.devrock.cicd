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

package groovyx.net.http.optional;

import groovy.util.XmlSlurper;
import groovy.util.slurpersupport.GPathResult;
import groovyx.net.http.*;
import java.io.IOException;
import java.io.InputStreamReader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import static groovyx.net.http.NativeHandlers.Encoders.stringToStream;

public class Html {

    public static GPathResult neckoParse(final FromServer fromServer) {
        try {
            final XMLReader p = new org.cyberneko.html.parsers.SAXParser();
            p.setEntityResolver(NativeHandlers.Parsers.catalogResolver);
            return new XmlSlurper(p).parse(new InputStreamReader(fromServer.getInputStream(), fromServer.getCharset()));
        }
        catch(IOException | SAXException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Document jsoupParse(final FromServer fromServer) {
        try {
            return Jsoup.parse(fromServer.getInputStream(),
                               fromServer.getCharset().name(),
                               fromServer.getUri().toString());
        }
        catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void jsoupEncode(final ChainedHttpConfig.ChainedRequest request, final ToServer ts) {
        final Document document = (Document) request.actualBody();
        ts.toServer(stringToStream(document.text(), request.actualCharset()));
    }
}

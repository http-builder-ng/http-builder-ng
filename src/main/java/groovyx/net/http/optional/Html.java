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
import java.util.function.Supplier;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class Html {

    public static final Supplier<Function<FromServer,Object>> neckoParserSupplier = () -> Html::neckoParse;
    public static final Supplier<BiConsumer<ChainedHttpConfig.ChainedRequest,ToServer>> jsoupEncoderSupplier = () -> Html::jsoupEncode;
    public static final Supplier<Function<FromServer,Object>> jsoupParserSupplier = () -> Html::jsoupParse;
    
    public static Object neckoParse(final FromServer fromServer) {
        try {
            final XMLReader p = new org.cyberneko.html.parsers.SAXParser();
            p.setEntityResolver(NativeHandlers.Parsers.catalogResolver);
            return new XmlSlurper(p).parse(new InputStreamReader(fromServer.getInputStream(), fromServer.getCharset()));
        }
        catch(IOException | SAXException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static Object jsoupParse(final FromServer fromServer) {
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

package groovyx.net.http.optional;

import groovy.util.XmlSlurper;
import groovy.util.slurpersupport.GPathResult;
import groovyx.net.http.*;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import static groovyx.net.http.NativeHandlers.Encoders.stringToStream;
import static groovyx.net.http.NativeHandlers.Encoders.handleRawUpload;

public class Html {

    public static final Supplier<BiFunction<ChainedHttpConfig,FromServer,Object>> neckoParserSupplier = () -> Html::neckoParse;
    public static final Supplier<BiConsumer<ChainedHttpConfig,ToServer>> jsoupEncoderSupplier = () -> Html::jsoupEncode;
    public static final Supplier<BiFunction<ChainedHttpConfig,FromServer,Object>> jsoupParserSupplier = () -> Html::jsoupParse;
    
    public static Object neckoParse(final ChainedHttpConfig config, final FromServer fromServer) {
        try {
            final XMLReader p = new org.cyberneko.html.parsers.SAXParser();
            p.setEntityResolver(NativeHandlers.Parsers.catalogResolver);
            return new XmlSlurper(p).parse(new InputStreamReader(fromServer.getInputStream(), fromServer.getCharset()));
        }
        catch(IOException | SAXException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static Object jsoupParse(final ChainedHttpConfig config, final FromServer fromServer) {
        try {
            return Jsoup.parse(fromServer.getInputStream(),
                               fromServer.getCharset().name(),
                               fromServer.getUri().toString());
        }
        catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void jsoupEncode(final ChainedHttpConfig config, final ToServer ts) {
        final ChainedHttpConfig.ChainedRequest request = config.getChainedRequest();
        if(handleRawUpload(config, ts)) {
            return;
        }
        
        final Document document = (Document) request.actualBody();
        ts.toServer(stringToStream(document.text(), request.actualCharset()));
    }
}

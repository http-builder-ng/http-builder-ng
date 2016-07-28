package groovyx.net.http;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import groovy.json.JsonBuilder;
import groovy.json.JsonSlurper;
import groovy.lang.Closure;
import groovy.lang.GString;
import groovy.lang.Writable;
import groovy.util.XmlSlurper;
import groovy.util.slurpersupport.GPathResult;
import groovy.xml.StreamingMarkupBuilder;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.xml.resolver.Catalog;
import org.apache.xml.resolver.CatalogManager;
import org.apache.xml.resolver.tools.CatalogResolver;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.apache.http.client.HttpResponseException;

public class NativeHandlers {

    private static final Logger log = LoggerFactory.getLogger(NativeHandlers.class);
    
    public static Object success(final FromServer fromServer, final Object data) {
        return data;
    }

    public static Object failure(final FromServer fromServer, final Object data) {
        throw new HttpException(fromServer, data);
    }

    protected static class Expanding {
        CharBuffer charBuffer = CharBuffer.allocate(2048);
        final char[] charAry = new char[2048];
        
        private void resize(final int toWrite) {
            final int byAtLeast = toWrite - charBuffer.remaining();
            int next = charBuffer.capacity() << 1;
            while((next - charBuffer.capacity()) + charBuffer.remaining() < byAtLeast) {
                next = next << 1;
            }
            
            CharBuffer tmp = CharBuffer.allocate(next);
            charBuffer.flip();
            tmp.put(charBuffer);
            charBuffer = tmp;
        }
        
        public void append(final int total) {
            if(charBuffer.remaining() < total) {
                resize(total);
            }
            
            charBuffer.put(charAry, 0, total);
        }
    }
    
    protected static final ThreadLocal<Expanding> tlExpanding = new ThreadLocal<Expanding>() {
            @Override protected Expanding initialValue() {
                return new Expanding();
            }
        };

    
    public static class Encoders {

        private static Object checkNull(final Object body) {
            if(body == null) {
                throw new NullPointerException("Effective body cannot be null");
            }

            return body;
        }

        private static void checkTypes(final Object body, final Class[] allowedTypes) {
            final Class type = body.getClass();
            for(Class allowed : allowedTypes) {
                if(allowed.isAssignableFrom(type)) {
                    return;
                }
            }

            final String msg = String.format("Cannot encode bodies of type %s, only bodies of: %s",
                                             type.getName(),
                                             Arrays.stream(allowedTypes).map(Class::getName).collect(Collectors.joining(", ")));

            throw new IllegalArgumentException(msg);
        }

        private static final Class[] BINARY_TYPES = new Class[] { ByteArrayInputStream.class, InputStream.class, Closure.class };
        
        public static void binary(final ChainedHttpConfig.ChainedRequest request, final ToServer ts) {
            final Object body = checkNull(request.actualBody());
            checkTypes(body, BINARY_TYPES);
            
            if(body instanceof ByteArrayInputStream) {
                ts.toServer((ByteArrayInputStream) body);
            }
            else if(body instanceof InputStream) {
                ts.toServer((InputStream) body);
            }
            else if(body instanceof byte[]) {
                ts.toServer(new ByteArrayInputStream((byte[]) body));
            }
            else {
                throw new UnsupportedOperationException();
            }
        }

        private static final Class[] TEXT_TYPES = new Class[] { Closure.class, Writable.class, Reader.class, String.class };
        
        private static InputStream readerToStream(final Reader r, final Charset cs) throws IOException {
            final Expanding e = tlExpanding.get();
            e.charBuffer.clear();
            int total;
            while((total = r.read(e.charAry)) != -1) {
                e.append(total);
            }

            e.charBuffer.flip();
            return new ByteArrayInputStream(cs.encode(e.charBuffer).array());
        }

        private static InputStream stringToStream(final String s, final Charset cs) {
            final ByteBuffer buf = cs.encode(s);
            return new ByteArrayInputStream(buf.array(), 0, buf.limit());
        }

        public static void text(final ChainedHttpConfig.ChainedRequest request, final ToServer ts) throws IOException {
            final Object body = checkNull(request.actualBody());
            checkTypes(body, TEXT_TYPES);
            String text = null;
            
            if(body instanceof Reader) {
                ts.toServer(readerToStream((Reader) body, request.actualCharset()));
            }
            else {
                ts.toServer(stringToStream(body.toString(), request.actualCharset()));
            }
        }

        private static final Class[] FORM_TYPES = { Map.class, String.class };

        public static void form(final ChainedHttpConfig.ChainedRequest request, final ToServer ts) {
            final Object body = checkNull(request.actualBody());
            checkTypes(body, FORM_TYPES);

            if(body instanceof String) {
                ts.toServer(stringToStream((String) body, request.actualCharset()));
            }
            else if(body instanceof Map) {
                final Map<?,?> params = (Map) body;
                final String encoded = Form.encode(params, request.actualCharset());
                ts.toServer(stringToStream(encoded, request.actualCharset()));
            }
            else {
                throw new UnsupportedOperationException();
            }
        }

        private static final Class[] XML_TYPES = new Class[] { String.class, StreamingMarkupBuilder.class };
        
        public static void xml(final ChainedHttpConfig.ChainedRequest request, final ToServer ts) {
            final Object body = checkNull(request.actualBody());
            checkTypes(body, XML_TYPES);

            if(body instanceof String) {
                ts.toServer(stringToStream((String) body, request.actualCharset()));
            }
            else if(body instanceof Closure) {
                final StreamingMarkupBuilder smb = new StreamingMarkupBuilder();
                ts.toServer(stringToStream(smb.bind(body).toString(), request.actualCharset()));
            }
            else {
                throw new UnsupportedOperationException();
            }
        }

        public static void json(final ChainedHttpConfig.ChainedRequest request, final ToServer ts) {
            final Object body = checkNull(request.actualBody());
            final String json = ((body instanceof String || body instanceof GString)
                                 ? body.toString()
                                 : new JsonBuilder(body).toString());
            ts.toServer(stringToStream(json, request.actualCharset()));
        }
    }

    public static class Parsers {

        public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
        private static final Logger log = LoggerFactory.getLogger(Parsers.class);
        /**
         * This CatalogResolver is static to avoid the overhead of re-parsing
         * the catalog definition file every time.  Unfortunately, there's no
         * way to share a single Catalog instance between resolvers.  The
         * {@link Catalog} class is technically not thread-safe, but as long as you
         * do not parse catalog files while using the resolver, it should be fine.
         */
        protected static CatalogResolver catalogResolver;
        
        static {
            CatalogManager catalogManager = new CatalogManager();
            catalogManager.setIgnoreMissingProperties( true );
            catalogManager.setUseStaticCatalog( false );
            catalogManager.setRelativeCatalogs( true );
            
            try {
                catalogResolver = new CatalogResolver( catalogManager );
                catalogResolver.getCatalog().parseCatalog(NativeHandlers.class.getResource("/catalog/html.xml"));
            }
            catch(IOException ex) {
                if(log.isWarnEnabled()) {
                    log.warn("Could not resolve default XML catalog", ex);
                }
            }
        }
        
        public static byte[] streamToBytes(final FromServer fromServer) {
            try {
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DefaultGroovyMethods.leftShift(baos, fromServer.getInputStream());
                return baos.toByteArray();
            }
            catch(IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }

        public static String textToString(final FromServer fromServer) {
            try(final Reader reader = new InputStreamReader(fromServer.getInputStream(), fromServer.getCharset())) {
                final Expanding e = tlExpanding.get();
                e.charBuffer.clear();
                int total;
                while((total = reader.read(e.charAry)) != -1) {
                    e.append(total);
                }
                
                e.charBuffer.flip();
                return e.charBuffer.toString();
            }
            catch(IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }

        public static Map<String,List<String>> form(final FromServer fromServer) {
            return Form.decode(fromServer.getInputStream(), fromServer.getCharset());
        }

        public static GPathResult html(final FromServer fromServer) {
            try {
                final XMLReader p = new org.cyberneko.html.parsers.SAXParser();
                p.setEntityResolver(catalogResolver);
                return new XmlSlurper(p).parse(new InputStreamReader(fromServer.getInputStream(), fromServer.getCharset()));
            }
            catch(IOException | SAXException ex) {
                throw new RuntimeException(ex);
            }
        }

        public static GPathResult xml(final FromServer fromServer) {
            try {
                final XmlSlurper xml = new XmlSlurper();
                xml.setEntityResolver(catalogResolver);
                xml.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
                xml.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
                return xml.parse(new InputStreamReader(fromServer.getInputStream(), fromServer.getCharset()));
            }
            catch(IOException | SAXException | ParserConfigurationException ex) {
                throw new RuntimeException(ex);
            }
        }
        
        public static Object json(final FromServer fromServer) {
            return new JsonSlurper().parse(new InputStreamReader(fromServer.getInputStream(), fromServer.getCharset()));
        }
    }
}

/**
 * Copyright (C) 2017 HttpBuilder-NG Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package groovyx.net.http;

import groovy.json.JsonBuilder;
import groovy.json.JsonSlurper;
import groovy.lang.Closure;
import groovy.lang.GString;
import groovy.lang.Writable;
import groovy.util.XmlSlurper;
import groovy.util.slurpersupport.GPathResult;
import groovy.xml.StreamingMarkupBuilder;
import groovyx.net.http.util.IoUtils;
import org.apache.xml.resolver.Catalog;
import org.apache.xml.resolver.CatalogManager;
import org.apache.xml.resolver.tools.CatalogResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NativeHandlers {

    /**
     * Default success handler, just returns the passed data, which is the data
     * returned by the invoked parser.
     *
     * @param fromServer Backend independent representation of what the server returned
     * @param data       The parsed data
     * @return The data object.
     */
    public static Object success(final FromServer fromServer, final Object data) {
        return data;
    }

    /**
     * Default failure handler. Throws an HttpException.
     *
     * @param fromServer Backend independent representation of what the server returned
     * @param data       If parsing was possible, this will be the parsed data, otherwise null
     * @return Nothing will be returned, the return type is Object for interface consistency
     * @throws HttpException
     */
    public static Object failure(final FromServer fromServer, final Object data) {
        throw new HttpException(fromServer, data);
    }

    /**
     * Default exception handler. Throws a RuntimeException.
     *
     * @param thrown The original thrown exception
     * @return Nothing will be returned, the return type is Object for interface consistency
     * @throws RuntimeException
     */
    public static Object exception(final Throwable thrown) {
        final RuntimeException rethrow = ((thrown instanceof RuntimeException) ?
            (RuntimeException) thrown :
            new RuntimeException(thrown));
        throw rethrow;
    }

    protected static class Expanding {
        CharBuffer charBuffer = CharBuffer.allocate(2048);
        final char[] charAry = new char[2048];

        private void resize(final int toWrite) {
            final int byAtLeast = toWrite - charBuffer.remaining();
            int next = charBuffer.capacity() << 1;
            while ((next - charBuffer.capacity()) + charBuffer.remaining() < byAtLeast) {
                next = next << 1;
            }

            CharBuffer tmp = CharBuffer.allocate(next);
            charBuffer.flip();
            tmp.put(charBuffer);
            charBuffer = tmp;
        }

        public void append(final int total) {
            if (charBuffer.remaining() < total) {
                resize(total);
            }

            charBuffer.put(charAry, 0, total);
        }
    }

    protected static final ThreadLocal<Expanding> tlExpanding = new ThreadLocal<Expanding>() {
        @Override
        protected Expanding initialValue() {
            return new Expanding();
        }
    };

    /**
     * The set of available content encoders.
     */
    public static class Encoders {

        // TODO: better testing around encoders

        public static Object checkNull(final Object body) {
            if (body == null) {
                throw new NullPointerException("Effective body cannot be null");
            }

            return body;
        }

        public static void checkTypes(final Object body, final Class<?>[] allowedTypes) {
            final Class<?> type = body.getClass();
            for (Class<?> allowed : allowedTypes) {
                if (allowed.isAssignableFrom(type)) {
                    return;
                }
            }

            final String msg = String.format("Cannot encode bodies of type %s, only bodies of: %s",
                type.getName(),
                Arrays.stream(allowedTypes).map(Class::getName).collect(Collectors.joining(", ")));

            throw new IllegalArgumentException(msg);
        }

        public static InputStream readerToStream(final Reader r, final Charset cs) throws IOException {
            return new ReaderInputStream(r, cs);
        }

        public static InputStream stringToStream(final String s, final Charset cs) {
            return new CharSequenceInputStream(s, cs);
        }

        public static boolean handleRawUpload(final ChainedHttpConfig config, final ToServer ts) {
            final ChainedHttpConfig.ChainedRequest request = config.getChainedRequest();
            final Object body = request.actualBody();
            final Charset charset = request.actualCharset();

            try {
                if (body instanceof File) {
                    ts.toServer(new FileInputStream((File) body));
                    return true;
                } else if (body instanceof Path) {
                    ts.toServer(Files.newInputStream((Path) body));
                    return true;
                } else if (body instanceof byte[]) {
                    ts.toServer(new ByteArrayInputStream((byte[]) body));
                    return true;
                } else if (body instanceof InputStream) {
                    ts.toServer((InputStream) body);
                    return true;
                } else if (body instanceof Reader) {
                    ts.toServer(new ReaderInputStream((Reader) body, charset));
                    return true;
                } else {
                    return false;
                }
            } catch (IOException e) {
                throw new TransportingException(e);
            }
        }

        private static final Class[] BINARY_TYPES = new Class[]{ByteArrayInputStream.class, InputStream.class, byte[].class, Closure.class};

        /**
         * Standard encoder for binary types. Accepts ByteArrayInputStream, InputStream, and byte[] types.
         *
         * @param config Fully configured chained request
         * @param ts     Formatted http body is passed to the ToServer argument
         */
        public static void binary(final ChainedHttpConfig config, final ToServer ts) {
            final ChainedHttpConfig.ChainedRequest request = config.getChainedRequest();
            final Object body = checkNull(request.actualBody());
            if (handleRawUpload(config, ts)) {
                return;
            }

            checkTypes(body, BINARY_TYPES);

            if (body instanceof byte[]) {
                ts.toServer(new ByteArrayInputStream((byte[]) body));
            } else {
                throw new UnsupportedOperationException();
            }
        }

        private static final Class[] TEXT_TYPES = new Class[]{Closure.class, Writable.class, Reader.class, String.class};

        /**
         * Standard encoder for text types. Accepts String and Reader types
         *
         * @param config Fully configured chained request
         * @param ts     Formatted http body is passed to the ToServer argument
         */
        public static void text(final ChainedHttpConfig config, final ToServer ts) throws IOException {
            final ChainedHttpConfig.ChainedRequest request = config.getChainedRequest();
            if (handleRawUpload(config, ts)) {
                return;
            }

            final Object body = checkNull(request.actualBody());
            checkTypes(body, TEXT_TYPES);

            ts.toServer(stringToStream(body.toString(), request.actualCharset()));
        }

        private static final Class[] FORM_TYPES = {Map.class, String.class};

        /**
         * Standard encoder for requests with content type 'application/x-www-form-urlencoded'.
         * Accepts String and Map types. If the body is a String type the method assumes it is properly
         * url encoded and is passed to the ToServer parameter as is. If the body is a Map type then
         * the output is generated by the {@link Form} class.
         *
         * @param config Fully configured chained request
         * @param ts     Formatted http body is passed to the ToServer argument
         */
        public static void form(final ChainedHttpConfig config, final ToServer ts) {
            final ChainedHttpConfig.ChainedRequest request = config.getChainedRequest();
            if (handleRawUpload(config, ts)) {
                return;
            }

            final Object body = checkNull(request.actualBody());
            checkTypes(body, FORM_TYPES);

            if (body instanceof String) {
                ts.toServer(stringToStream((String) body, request.actualCharset()));
            } else if (body instanceof Map) {
                final Map<?, ?> params = (Map) body;
                final String encoded = Form.encode(params, request.actualCharset());
                ts.toServer(stringToStream(encoded, request.actualCharset()));
            } else {
                throw new UnsupportedOperationException();
            }
        }

        private static final Class[] XML_TYPES = new Class[]{String.class, StreamingMarkupBuilder.class};

        /**
         * Standard encoder for requests with an xml body.
         * <p>
         * Accepts String and {@link Closure} types. If the body is a String type the method passes the body
         * to the ToServer parameter as is. If the body is a {@link Closure} then the closure is converted
         * to xml using Groovy's {@link StreamingMarkupBuilder}.
         *
         * @param config Fully configured chained request
         * @param ts     Formatted http body is passed to the ToServer argument
         */
        public static void xml(final ChainedHttpConfig config, final ToServer ts) {
            final ChainedHttpConfig.ChainedRequest request = config.getChainedRequest();
            if (handleRawUpload(config, ts)) {
                return;
            }

            final Object body = checkNull(request.actualBody());
            checkTypes(body, XML_TYPES);

            if (body instanceof String) {
                ts.toServer(stringToStream((String) body, request.actualCharset()));
            } else if (body instanceof Closure) {
                final StreamingMarkupBuilder smb = new StreamingMarkupBuilder();
                ts.toServer(stringToStream(smb.bind(body).toString(), request.actualCharset()));
            } else {
                throw new UnsupportedOperationException();
            }
        }

        /**
         * Standard encoder for requests with a json body.
         * <p>
         * Accepts String, {@link GString} and {@link Closure} types. If the body is a String type the method passes the body
         * to the ToServer parameter as is. If the body is a {@link Closure} then the closure is converted
         * to json using Groovy's {@link JsonBuilder}.
         *
         * @param config Fully configured chained request
         * @param ts     Formatted http body is passed to the ToServer argument
         */
        public static void json(final ChainedHttpConfig config, final ToServer ts) {
            final ChainedHttpConfig.ChainedRequest request = config.getChainedRequest();
            if (handleRawUpload(config, ts)) {
                return;
            }

            final Object body = checkNull(request.actualBody());
            final String json = ((body instanceof String || body instanceof GString)
                ? body.toString()
                : new JsonBuilder(body).toString());
            ts.toServer(stringToStream(json, request.actualCharset()));
        }
    }

    /**
     * The default collection of response content parsers.
     */
    public static class Parsers {

        // TODO: better testing around parsers

        public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
        private static final Logger log = LoggerFactory.getLogger(Parsers.class);

        /**
         * This CatalogResolver is static to avoid the overhead of re-parsing the catalog definition file every time.  Unfortunately, there's no
         * way to share a single Catalog instance between resolvers.  The {@link Catalog} class is technically not thread-safe, but as long as you
         * do not parse catalog files while using the resolver, it should be fine.
         */
        public static CatalogResolver catalogResolver;

        static {
            CatalogManager catalogManager = new CatalogManager();
            catalogManager.setIgnoreMissingProperties(true);
            catalogManager.setUseStaticCatalog(false);
            catalogManager.setRelativeCatalogs(true);

            try {
                catalogResolver = new CatalogResolver(catalogManager);
                catalogResolver.getCatalog().parseCatalog(NativeHandlers.class.getResource("/catalog/html.xml"));
            } catch (IOException ex) {
                if (log.isWarnEnabled()) {
                    log.warn("Could not resolve default XML catalog", ex);
                }
            }
        }

        /**
         * Standard parser for raw bytes.
         *
         * @param fromServer Backend indenpendent representation of data returned from http server
         * @return Raw bytes of body returned from http server
         */
        public static byte[] streamToBytes(final ChainedHttpConfig config, final FromServer fromServer) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IoUtils.transfer(fromServer.getInputStream(), baos, true);
            return baos.toByteArray();
        }

        /**
         * Standard parser for text response content.
         *
         * @param config     the http client configuration
         * @param fromServer Backend independent representation of data returned from http server
         * @return Body of response
         */
        public static String textToString(final ChainedHttpConfig config, final FromServer fromServer) {
            try {
                final Reader reader = new InputStreamReader(fromServer.getInputStream(), fromServer.getCharset());
                final Expanding e = tlExpanding.get();
                e.charBuffer.clear();
                int total;
                while ((total = reader.read(e.charAry)) != -1) {
                    e.append(total);
                }

                e.charBuffer.flip();
                return e.charBuffer.toString();
            } catch (IOException ioe) {
                throw new TransportingException(ioe);
            }
        }

        /**
         * Standard parser for responses with content type 'application/x-www-form-urlencoded'.
         *
         * @param fromServer Backend indenpendent representation of data returned from http server
         * @return Form data
         */
        public static Map<String, List<String>> form(final ChainedHttpConfig config, final FromServer fromServer) {
            return Form.decode(fromServer.getInputStream(), fromServer.getCharset());
        }

        /**
         * Standard parser for xml responses.
         *
         * @param fromServer Backend indenpendent representation of data returned from http server
         * @return Body of response
         */
        public static GPathResult xml(final ChainedHttpConfig config, final FromServer fromServer) {
            try {
                final XmlSlurper xml = new XmlSlurper();
                xml.setEntityResolver(catalogResolver);
                xml.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
                xml.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
                return xml.parse(new InputStreamReader(fromServer.getInputStream(), fromServer.getCharset()));
            } catch (IOException | SAXException | ParserConfigurationException ex) {
                throw new TransportingException(ex);
            }
        }

        /**
         * Standard parser for json responses.
         *
         * @param fromServer Backend indenpendent representation of data returned from http server
         * @return Body of response
         */
        public static Object json(final ChainedHttpConfig config, final FromServer fromServer) {
            return new JsonSlurper().parse(new InputStreamReader(fromServer.getInputStream(), fromServer.getCharset()));
        }

        /**
         * Transfers the contents of the {@link InputStream} into the {@link OutputStream}, optionally closing the stream.
         *
         * @param istream the input stream
         * @param ostream the output stream
         * @param close   whether or not to close the output stream
         * @deprecated Use the version in {@link IoUtils} instead - this one just delegates to it
         */
        @Deprecated
        public static void transfer(final InputStream istream, final OutputStream ostream, final boolean close) {
            IoUtils.transfer(istream, ostream, close);
        }
    }
}

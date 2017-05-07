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
package groovyx.net.http.optional;

import groovy.util.XmlSlurper;
import groovyx.net.http.ChainedHttpConfig;
import groovyx.net.http.FromServer;
import groovyx.net.http.NativeHandlers;
import groovyx.net.http.ToServer;
import groovyx.net.http.TransportingException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static groovyx.net.http.NativeHandlers.Encoders.handleRawUpload;
import static groovyx.net.http.NativeHandlers.Encoders.stringToStream;

/**
 * Parser and Encoder methods for handling HTML content using the https://jsoup.org/[JSoup] HTML library.
 */
public class Html {

    public static final Supplier<BiFunction<ChainedHttpConfig, FromServer, Object>> neckoParserSupplier = () -> Html::neckoParse;

    public static final Supplier<BiConsumer<ChainedHttpConfig, ToServer>> jsoupEncoderSupplier = () -> Html::jsoupEncode;

    public static final Supplier<BiFunction<ChainedHttpConfig, FromServer, Object>> jsoupParserSupplier = () -> Html::jsoupParse;

    /**
     * Method that provides an HTML parser for response configuration (uses necko parser).
     *
     * @param config the chained configuration
     * @param fromServer the server response adapter
     * @return the parsed HTML content (a {@link groovy.util.slurpersupport.GPathResult} object)
     */
    public static Object neckoParse(final ChainedHttpConfig config, final FromServer fromServer) {
        try {
            final XMLReader p = new org.cyberneko.html.parsers.SAXParser();
            p.setEntityResolver(NativeHandlers.Parsers.catalogResolver);
            return new XmlSlurper(p).parse(new InputStreamReader(fromServer.getInputStream(), fromServer.getCharset()));
        } catch (IOException | SAXException ex) {
            throw new TransportingException(ex);
        }
    }

    /**
     * Method that provides an HTML parser for response configuration (uses JSoup).
     *
     * @param config the chained configuration
     * @param fromServer the server response adapter
     * @return the parsed HTML content (a {@link Document} object)
     */
    public static Object jsoupParse(final ChainedHttpConfig config, final FromServer fromServer) {
        try {
            return Jsoup.parse(fromServer.getInputStream(), fromServer.getCharset().name(), fromServer.getUri().toString());
        } catch (IOException e) {
            throw new TransportingException(e);
        }
    }

    public static void jsoupEncode(final ChainedHttpConfig config, final ToServer ts) {
        final ChainedHttpConfig.ChainedRequest request = config.getChainedRequest();
        if (handleRawUpload(config, ts)) {
            return;
        }

        final Document document = (Document) request.actualBody();
        ts.toServer(stringToStream(document.text(), request.actualCharset()));
    }
}

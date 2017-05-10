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

import com.fasterxml.jackson.databind.ObjectMapper;
import groovyx.net.http.*;

import java.io.IOException;
import java.io.StringWriter;

import static groovyx.net.http.NativeHandlers.Encoders.handleRawUpload;

/**
 * Parser and Encoder methods for handling JSON content using the https://github.com/FasterXML/jackson[Jackson] JSON library.
 */
public class Jackson {

    static final String OBJECT_MAPPER_ID = "0w4XJJnlTNK8dvISuCDTlsusPQE=";

    /**
     * Used to parse the server response content using the Jackson JSON parser.
     *
     * @param config the configuration
     * @param fromServer the server content accessor
     * @return the parsed object
     */
    @SuppressWarnings("WeakerAccess")
    public static Object parse(final ChainedHttpConfig config, final FromServer fromServer) {
        try {
            final ObjectMapper mapper = (ObjectMapper) config.actualContext(fromServer.getContentType(), OBJECT_MAPPER_ID);
            return mapper.readValue(fromServer.getReader(), config.getChainedResponse().getType());
        } catch (IOException e) {
            throw new TransportingException(e);
        }
    }

    /**
     * Used to encode the request content using the Jackson JSON encoder.
     *
     * @param config the configuration
     * @param ts the server request content accessor
     */
    @SuppressWarnings("WeakerAccess")
    public static void encode(final ChainedHttpConfig config, final ToServer ts) {
        try {
            if (handleRawUpload(config, ts)) {
                return;
            }

            final ChainedHttpConfig.ChainedRequest request = config.getChainedRequest();
            final ObjectMapper mapper = (ObjectMapper) config.actualContext(request.actualContentType(), OBJECT_MAPPER_ID);
            final StringWriter writer = new StringWriter();
            mapper.writeValue(writer, request.actualBody());
            ts.toServer(new CharSequenceInputStream(writer.toString(), request.actualCharset()));
        } catch (IOException e) {
            throw new TransportingException(e);
        }
    }

    /**
     * Used to configure the provided Jackson `ObjectMapper` in the configuration context for the default JSON content type.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = "${serverRule.serverUrl}/jackson"
     *     request.contentType = JSON[0]
     *     Jackson.mapper(delegate, objectMapper)
     * }
     * ----
     *
     * @param config the configuration
     * @param mapper the `ObjectMapper` to be used.
     */
    public static void mapper(final HttpConfig config, final ObjectMapper mapper) {
        mapper(config, mapper, ContentTypes.JSON);
    }

    /**
     * Used to configure the provided Jackson `ObjectMapper` in the configuration context for the specified content type.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = "${serverRule.serverUrl}/jackson"
     *     request.contentType = OTHER_TYPE
     *     Jackson.mapper(delegate, objectMapper, [OTHER_TYPE])
     * }
     * ----
     *
     * @param config the configuration
     * @param mapper the `ObjectMapper` to be used.
     * @param contentTypes the content types to be configured with the mapper
     */
    public static void mapper(final HttpConfig config, final ObjectMapper mapper, final Iterable<String> contentTypes) {
        config.context(contentTypes, OBJECT_MAPPER_ID, mapper);
    }

    /**
     * Configures the client to use the Jackson encoder/decoder for JSON handling, with the default JSON content type.
     *
     * @param config the configuration
     */
    public static void use(final HttpConfig config) {
        use(config, ContentTypes.JSON);
    }

    /**
     * Configures the client to use the Jackson encoder/decoder for JSON handling, with the specified JSON content type.
     *
     * [source,groovy]
     * ----
     * def result = http.get {
     *     Jackson.use(delegate)
     * }
     * ----
     *
     * @param config the configuration
     * @param contentTypes the content types to be configured
     */
    public static void use(final HttpConfig config, final Iterable<String> contentTypes) {
        config.getRequest().encoder(contentTypes, Jackson::encode);
        config.getResponse().parser(contentTypes, Jackson::parse);
    }
}

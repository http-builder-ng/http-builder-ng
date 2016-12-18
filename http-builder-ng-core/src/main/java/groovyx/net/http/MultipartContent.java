/**
 * Copyright (C) 2016 David Clark
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

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovyx.net.http.util.IoUtils;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.util.Collections.unmodifiableList;

/**
 * Multipart request content object used to define the multipart data. An example would be:
 *
 * [source,groovy]
 * ----
 * request.contentType = 'multipart/form-data'
 * request.body = multipart {
 *     field 'userid','someuser'
 *     part 'icon', 'user-icon.jpg', 'image/jpeg', imageFile
 * }
 * ----
 *
 * which would define a `multipart/form-data` request with a field part and a file part with the specified properties.
 */
public class MultipartContent implements ToServer {

    private static final String BOUNDARY_MARK = "--";
    private static final String CRLF = "\r\n";
    private final List<MultipartPart> entries = new LinkedList<>();
    private final String boundary = RandomStringUtils.randomAlphanumeric(16);

    /**
     * Configures multipart request content using a Groovy closure (delegated to {@link MultipartContent}).
     *
     * @param closure the configuration closure
     * @return a configured instance of {@link MultipartContent}
     */
    public static MultipartContent multipart(@DelegatesTo(MultipartContent.class) Closure closure) {
        MultipartContent content = new MultipartContent();
        closure.setDelegate(content);
        closure.call();
        return content;
    }

    /**
     * Configures multipart request content using a {@link Consumer} which will have an instance of {@link MultipartContent} passed into it for
     * configuring the multipart content data.
     *
     * @param config the configuration {@link Consumer}
     * @return a configured instance of {@link MultipartContent}
     */
    public static MultipartContent multipart(final Consumer<MultipartContent> config) {
        MultipartContent content = new MultipartContent();
        config.accept(content);
        return content;
    }

    public MultipartContent field(String fieldName, String value) {
        return part(fieldName, value);
    }

    public MultipartContent field(String fieldName, String contentType, String value) {
        return part(fieldName, contentType, value);
    }

    public MultipartContent part(String fieldName, String value) {
        return part(fieldName, null, ContentTypes.TEXT.getAt(0), value);
    }

    public MultipartContent part(String fieldName, String contentType, String value) {
        return part(fieldName, null, contentType, value);
    }

    // FIXME: should this be renamed MultipartRequestContent or is this the same for response ?
    // FIXME: document
    // FIXME: update guide documentation (and javadocs)
    // FIXME: add interface over multipart to keep the DSL clean

    public MultipartContent part(String fieldName, String fileName, String contentType, Object content) {
        entries.add(new MultipartPart(fieldName, fileName, contentType, content));
        return this;
    }

    Iterable<MultipartPart> parts() {
        return unmodifiableList(entries);
    }

    public String boundary() {
        return boundary;
    }

    public InputStream toInputStream(final ChainedHttpConfig config) {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream(1024);

        try {
            for (final MultipartPart part : entries) {
                buffer.write(string(BOUNDARY_MARK, boundary(), CRLF));
                buffer.write(string("Content-Type: ", part.getContentType(), CRLF));

                if (part.getFileName() != null) {
                    buffer.write(string("Content-Disposition: form-data; name=\"", part.getFieldName(), "\"; filename=\"", part.getFileName(), "\"", CRLF));

                } else {
                    buffer.write(string("Content-Disposition: form-data; name=\"", part.getFieldName(), "\"", CRLF));
                }

                //    BASE64('base64'), QUOTED_PRINTABLE('quoted-printable')
//                buffer.write(string("Content-Transfer-Encoding: ", , CRLF));

                buffer.write(string(CRLF));

                final NestedToServer toServer = new NestedToServer();
                BiConsumer<ChainedHttpConfig, ToServer> encoder = config.findEncoder(part.getContentType());
                encoder.accept(new PartConfig(config, part), toServer);
                buffer.write(toServer.getBytes());

                buffer.write(string(CRLF));
            }

            buffer.write(string(BOUNDARY_MARK, boundary(), BOUNDARY_MARK));

        } catch (IOException e) {
            // FIXME: do better?
            e.printStackTrace();
        }

        return new ByteArrayInputStream(buffer.toByteArray());
    }

    private static byte[] string(final String... str) {
        final StringJoiner joiner = new StringJoiner("");
        for (final String s : str) {
            joiner.add(s);
        }
        return joiner.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void toServer(final InputStream inputStream) {

    }

    /**
     * Represents a single multipart part.
     */
    static class MultipartPart {

        private final String fieldName;
        private final String fileName;
        private final String contentType;
        private final Object content;

        private MultipartPart(String fieldName, String fileName, String contentType, Object content) {
            this.fieldName = fieldName;
            this.fileName = fileName;
            this.contentType = contentType != null ? contentType : ContentTypes.TEXT.getAt(0);
            this.content = content;
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getFileName() {
            return fileName;
        }

        public String getContentType() {
            return contentType;
        }

        public Object getContent() {
            return content;
        }
    }
}

class NestedToServer implements ToServer {

    private byte[] bytes;

    @Override
    public void toServer(final InputStream inputStream) {
        try {
            bytes = IoUtils.streamToBytes(inputStream);
        } catch (IOException e) {
            // FIXME: something better?
            e.printStackTrace();
        }
    }

    public byte[] getBytes() {
        return bytes;
    }
}

class PartConfig implements ChainedHttpConfig {

    private final ChainedHttpConfig config;
    private final MultipartContent.MultipartPart part;

    PartConfig(final ChainedHttpConfig config, final MultipartContent.MultipartPart part) {
        this.config = config;
        this.part = part;
    }

    @Override
    public Map<Map.Entry<String, Object>, Object> getContextMap() {
        return config.getContextMap();
    }

    @Override
    public ChainedResponse getChainedResponse() {
        return config.getChainedResponse();
    }

    @Override
    public ChainedRequest getChainedRequest() {
        return (ChainedRequest) getRequest();
    }

    @Override
    public ChainedHttpConfig getParent() {
        return null;
    }

    @Override
    public void context(String contentType, Object id, Object obj) {
        config.context(contentType, id, obj);
    }

    @Override
    public Request getRequest() {
        final HttpConfigs.BasicRequest request = new HttpConfigs.BasicRequest(null);
        request.setContentType(part.getContentType());
        request.setCharset(StandardCharsets.UTF_8); // TODO: ok?
        request.setBody(part.getContent());
        return request;
    }

    @Override
    public Response getResponse() {
        return config.getResponse();
    }
}
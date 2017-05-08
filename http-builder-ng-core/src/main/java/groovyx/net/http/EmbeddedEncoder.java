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

import groovyx.net.http.util.IoUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Used to apply standard encoders in an embedded request context, primarily for multipart request content encoding.
 *
 * This is not really a public interface class.
 */
class EmbeddedEncoder {

    public static byte[] encode(final ChainedHttpConfig config, final String contentType, final Object content) {
        final InMemoryToServer toServer = new InMemoryToServer();
        BiConsumer<ChainedHttpConfig, ToServer> encoder = config.findEncoder(contentType);
        encoder.accept(new ConfigFragment(config, contentType, content), toServer);
        return toServer.getBytes();
    }

    private static class InMemoryToServer implements ToServer {

        private byte[] bytes;

        @Override
        public void toServer(final InputStream inputStream) {
            try {
                bytes = IoUtils.streamToBytes(inputStream);
            }
            catch(IOException e) {
                throw new TransportingException("Unable to perform embedded encoding", e);
            }
        }

        public byte[] getBytes() {
            return bytes;
        }
    }

    private static class ConfigFragment implements ChainedHttpConfig {

        private final ChainedHttpConfig config;
        private final String contentType;
        private final Object content;

        ConfigFragment(final ChainedHttpConfig config, final String contentType, final Object content) {
            this.config = config;
            this.contentType = contentType;
            this.content = content;
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

            final Charset charset = config.getChainedRequest().getCharset();
            request.setCharset(charset != null ? charset : StandardCharsets.UTF_8);

            request.setContentType(contentType);
            request.setBody(content);
            return request;
        }

        @Override
        public Response getResponse() {
            return config.getResponse();
        }
    }
}

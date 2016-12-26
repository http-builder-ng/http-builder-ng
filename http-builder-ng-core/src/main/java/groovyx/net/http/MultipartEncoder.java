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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;
import java.util.function.BiConsumer;

/**
 * Standard encoder for multipart requests. This is _not_ a full multipart implementation, but should account for most use cases. If a
 * full implementation is required, consider using one of the alternate client implementations and its client-specific multipart content
 * encoder instead.
 */
public class MultipartEncoder implements BiConsumer<ChainedHttpConfig, ToServer> {

    private static final Class[] MULTIPART_TYPES = {MultipartContent.class};
    private static final String BOUNDARY_MARK = "--";
    private static final String CRLF = "\r\n";

    @Override
    public void accept(final ChainedHttpConfig config, final ToServer ts) {
        final ChainedHttpConfig.ChainedRequest request = config.getChainedRequest();

        final Object body = request.actualBody();
        NativeHandlers.Encoders.checkTypes(body, MULTIPART_TYPES);

        MultipartContent mp = (MultipartContent) request.actualBody();
        request.setContentType("multipart/form-data; boundary=" + mp.boundary());
        ts.toServer(stream(config, mp));
    }

    private static InputStream stream(final ChainedHttpConfig config, final MultipartContent content) {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream(1024);

        try {
            for (final MultipartContent.MultipartPart part : content.parts()) {
                buffer.write(string(BOUNDARY_MARK, content.boundary(), CRLF));
                buffer.write(string("Content-Type: ", part.getContentType(), CRLF));

                if (part.getFileName() != null) {
                    buffer.write(string("Content-Disposition: form-data; name=\"", part.getFieldName(), "\"; filename=\"", part.getFileName(), "\"", CRLF));

                } else {
                    buffer.write(string("Content-Disposition: form-data; name=\"", part.getFieldName(), "\"", CRLF));
                }

                // TODO: do we need content-transfer-encoding?
                // buffer.write(string("Content-Transfer-Encoding: ", , CRLF));

                buffer.write(string(CRLF));

                buffer.write(EmbeddedEncoder.encode(config, part.getContentType(), part.getContent()));

                buffer.write(string(CRLF));
            }

            buffer.write(string(BOUNDARY_MARK, content.boundary(), BOUNDARY_MARK));

        } catch (IOException e) {
            throw new RuntimeException("Problem while encoding multipart content: " + e.getMessage());
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
}

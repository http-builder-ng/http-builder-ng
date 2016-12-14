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

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static org.apache.http.entity.ContentType.parse;

/**
 * Request content encoders specific to the Apache client implementation.
 */
public class ApacheEncoders {

    // TODO: there is duplicate code in the multipart encoders - refactor to reduce duplication during parser/encoder work.

    /**
     *  Encodes multipart/form-data where the body content must be an instance of the {@link MultipartContent} class.
     *
     * @param config the chained configuration object
     * @param ts the server adapter
     */
    public static void multipart(final ChainedHttpConfig config, final ToServer ts) {
        try {
            final ChainedHttpConfig.ChainedRequest request = config.getChainedRequest();

            final Object body = request.actualBody();
            if (!(body instanceof MultipartContent)) {
                throw new IllegalArgumentException("Multipart body content must be MultipartContent.");
            }

            final String contentType = request.actualContentType();
            if (!contentType.equals(ContentTypes.MULTIPART_FORMDATA.getAt(0))) {
                throw new IllegalArgumentException("Multipart body content must be multipart/form-data.");
            }

            final String boundary = RandomStringUtils.randomAlphanumeric(10);
            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create().setBoundary(boundary);

            entityBuilder.setContentType(ContentType.parse("multipart/mixed; boundary=" + boundary));

            for (final MultipartContent.MultipartEntry mpe : ((MultipartContent) body).entries()) {
                if (mpe.isField()) {
                    entityBuilder.addTextBody(mpe.getFieldName(), (String) mpe.getContent());

                } else {
                    final ContentType partContentType = parse(mpe.getContentType());

                    if (mpe.getContent() instanceof String) {
                        entityBuilder.addBinaryBody(mpe.getFieldName(), ((String) mpe.getContent()).getBytes(), partContentType, mpe.getFileName());

                    } else if (mpe.getContent() instanceof Path) {
                        entityBuilder.addBinaryBody(mpe.getFieldName(), ((Path) mpe.getContent()).toFile(), partContentType, mpe.getFileName());

                    } else if (mpe.getContent() instanceof byte[]) {
                        entityBuilder.addBinaryBody(mpe.getFieldName(), (byte[]) mpe.getContent(), partContentType, mpe.getFileName());

                    } else {
                        throw new IllegalArgumentException("Unsupported multipart content object type: " + mpe.getContent().getClass());
                    }
                }
            }

            ts.toServer(entityBuilder.build().getContent(), "multipart/mixed; boundary=" + boundary);

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
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

import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import java.io.IOException;

import static groovyx.net.http.ContentTypes.MULTIPART_FORMDATA;
import static groovyx.net.http.ContentTypes.MULTIPART_MIXED;
import static org.apache.http.entity.ContentType.parse;
import static groovyx.net.http.util.Misc.randomString;
/**
 * Request content encoders specific to the Apache client implementation.
 *
 * See the {@link MultipartContent} class documentation for more configuration details.
 */
public class ApacheEncoders {

    /**
     *  Encodes multipart/form-data where the body content must be an instance of the {@link MultipartContent} class. Individual parts will be
     *  encoded using the encoders available to the {@link ChainedHttpConfig} object.
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
            if (!(contentType.equals(MULTIPART_FORMDATA.getAt(0)) || contentType.equals(MULTIPART_MIXED.getAt(0)))) {
                throw new IllegalArgumentException("Multipart body content must be multipart/form-data.");
            }

            final String boundary = randomString(10);
            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create().setBoundary(boundary);

            final String boundaryContentType = "multipart/form-data; boundary=" + boundary;

            entityBuilder.setContentType(ContentType.parse(boundaryContentType));

            for (final MultipartContent.MultipartPart mpe : ((MultipartContent) body).parts()) {
                if (mpe.getFileName() == null) {
                    entityBuilder.addTextBody(mpe.getFieldName(), (String) mpe.getContent());

                } else {
                    final byte[] encodedBytes = EmbeddedEncoder.encode(config, mpe.getContentType(), mpe.getContent());
                    entityBuilder.addBinaryBody(mpe.getFieldName(), encodedBytes, parse(mpe.getContentType()), mpe.getFileName());
                }
            }

            request.setContentType(boundaryContentType);

            ts.toServer(entityBuilder.build().getContent());

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}

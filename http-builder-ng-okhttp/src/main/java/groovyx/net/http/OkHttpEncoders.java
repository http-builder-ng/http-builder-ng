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

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okio.Buffer;

import java.io.IOException;

import static groovyx.net.http.ContentTypes.MULTIPART_FORMDATA;
import static groovyx.net.http.ContentTypes.MULTIPART_MIXED;
import static groovyx.net.http.EmbeddedEncoder.encode;
import static okhttp3.MediaType.parse;
import static okhttp3.RequestBody.create;

/**
 * Request content encoders specific to the OkHttp client implementation.
 *
 * See the {@link MultipartContent} class documentation for more configuration details.
 */
public class OkHttpEncoders {

    /**
     * Encodes multipart/form-data where the body content must be an instance of the {@link MultipartContent} class. Individual parts will be
     *  encoded using the encoders available to the {@link ChainedHttpConfig} object.
     *
     * @param config the chained configuration object
     * @param ts     the server adapter
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

            final MultipartBody.Builder builder = new MultipartBody.Builder();

            for (final MultipartContent.MultipartPart mpe : ((MultipartContent) body).parts()) {
                if (mpe.getFileName() == null) {
                    builder.addFormDataPart(mpe.getFieldName(), (String) mpe.getContent());
                } else {
                    builder.addFormDataPart(
                        mpe.getFieldName(),
                        mpe.getFileName(),
                        create(parse(mpe.getContentType()), encode(config, mpe.getContentType(), mpe.getContent()))
                    );
                }
            }

            final Buffer buffer = new Buffer();
            MultipartBody multipartBody = builder.build();
            multipartBody.writeTo(buffer);

            request.setContentType("multipart/form-data; boundary=" + multipartBody.boundary());

            ts.toServer(buffer.inputStream());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

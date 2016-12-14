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

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okio.Buffer;

import java.io.IOException;
import java.nio.file.Path;

import static okhttp3.MediaType.parse;
import static okhttp3.RequestBody.create;

/**
 * Request content encoders specific to the Apache client implementation.
 */
public class OkHttpEncoders {

    // TODO: there is duplicate code in the multipart encoders - refactor to reduce duplication during parser/encoder work.

    // FIXME: testing the encoders (check coverage)
    // FIXME: go through TODO/FIXME tags

    /**
     * Encodes multipart/form-data where the body content must be an instance of the {@link MultipartContent} class.
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
            if (!contentType.equals(ContentTypes.MULTIPART_FORMDATA.getAt(0))) {
                throw new IllegalArgumentException("Multipart body content must be multipart/form-data.");
            }

            final MultipartBody.Builder builder = new MultipartBody.Builder();

            for (final MultipartContent.MultipartEntry mpe : ((MultipartContent) body).entries()) {
                if (mpe.isField()) {
                    builder.addFormDataPart(mpe.getFieldName(), (String) mpe.getContent());
                } else {
                    RequestBody requestBody = null;

                    if (mpe.getContent() instanceof String) {
                        requestBody = create(parse(mpe.getContentType()), (String) mpe.getContent());

                    } else if (mpe.getContent() instanceof Path) {
                        requestBody = create(parse(mpe.getContentType()), ((Path) mpe.getContent()).toFile());

                    } else if (mpe.getContent() instanceof byte[]) {
                        requestBody = create(parse(mpe.getContentType()), (byte[]) mpe.getContent());

                    } else {
                        throw new IllegalArgumentException("Unsupported multipart content object type: " + mpe.getContent().getClass());
                    }

                    builder.addFormDataPart(mpe.getFieldName(), mpe.getFileName(), requestBody);
                }
            }

            final Buffer buffer = new Buffer();
            MultipartBody multipartBody = builder.build();
            multipartBody.writeTo(buffer);

            ts.toServer(buffer.inputStream(), "multipart/mixed; boundary=" + multipartBody.boundary());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

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

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import java.io.*;

import static groovyx.net.http.ContentTypes.MULTIPART_FORMDATA;
import static groovyx.net.http.ContentTypes.MULTIPART_MIXED;
import static java.lang.String.format;

/**
 * Generic content encoders for use with all client implementations. Note that there may be client-specific implementations for some of these (see
 * {@link groovyx.net.http.ApacheEncoders} and {@link groovyx.net.http.OkHttpEncoders} for more information).
 * 
 * See the {@link MultipartContent} class documentation for more configuration details.
 */
public class CoreEncoders {

    /**
     * Encodes multipart/form-data where the body content must be an instance of the {@link MultipartContent} class. Individual parts will be
     * encoded using the encoders available to the {@link ChainedHttpConfig} object.
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

            final MimeMultipart mimeMultipart = new MimeMultipart();

            for (final MultipartContent.MultipartPart mpe : ((MultipartContent) body).parts()) {
                mimeMultipart.addBodyPart(part(config, mpe));
            }

            request.setContentType(mimeMultipart.getContentType());

            final ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
            mimeMultipart.writeTo(bytesOut);

            ts.toServer(new ByteArrayInputStream(bytesOut.toByteArray()));

        } catch (IOException | MessagingException ioe) {
            ioe.printStackTrace();
        }
    }

    private static MimeBodyPart part(final ChainedHttpConfig config, final MultipartContent.MultipartPart multipartPart) throws MessagingException {
        final MimeBodyPart bodyPart = new MimeBodyPart();
        bodyPart.setDisposition("form-data");

        if (multipartPart.getFileName() != null) {
            bodyPart.setFileName(multipartPart.getFileName());
            bodyPart.setHeader("Content-Disposition", format("form-data; name=\"%s\"; filename=\"%s\"", multipartPart.getFieldName(), multipartPart.getFileName()));

        } else {
            bodyPart.setHeader("Content-Disposition", format("form-data; name=\"%s\"", multipartPart.getFieldName()));
        }

        bodyPart.setDataHandler(new DataHandler(new EncodedDataSource(config, multipartPart.getContentType(), multipartPart.getContent())));
        bodyPart.setHeader("Content-Type", multipartPart.getContentType());

        return bodyPart;
    }

    private static class EncodedDataSource implements DataSource {

        private final ChainedHttpConfig config;
        private final String contentType;
        private final Object value;

        private EncodedDataSource(final ChainedHttpConfig config, final String contentType, final Object value) {
            this.config = config;
            this.contentType = contentType != null ? contentType : "text/plain";
            this.value = value;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(EmbeddedEncoder.encode(config, contentType, value));
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            throw new UnsupportedOperationException("Writing is not supported.");
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public String getName() {
            return null;
        }
    }
}

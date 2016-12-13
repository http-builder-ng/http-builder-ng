package groovyx.net.http;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okio.Buffer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static okhttp3.MediaType.parse;
import static okhttp3.RequestBody.create;

/**
 * FIXME: document
 */
public class OkHttpEncoders {

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

//            final Charset charset = request.actualCharset();

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

                    } else if (mpe.getContent() instanceof InputStream) {
                        // FIXME: finish supporting this
//                        requestBody = create(parse(mpe.getContentType()), );

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

            ts.setContentType("multipart/mixed; boundary=" + multipartBody.boundary());
            ts.toServer(buffer.inputStream());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

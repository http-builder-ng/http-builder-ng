package groovyx.net.http.libspecific;

import groovyx.net.http.ToServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.message.BasicHeader;

public class ApacheToServer implements ToServer, HttpEntity {

    private String contentType;
    private InputStream inputStream;

    public void toServer(final String contentType, final InputStream inputStream) {
        this.contentType = contentType;
        this.inputStream = inputStream;
    }
    
    public boolean isRepeatable() {
        return false;
    }

    public boolean isChunked() {
        return false;
    }

    public long getContentLength() {
        return -1L;
    }

    public Header getContentType() {
        return new BasicHeader("Content-Type", contentType);
    }

    public Header getContentEncoding() {
        return null;
    }

    public InputStream getContent() {
        return inputStream;
    }

    public void writeTo(final OutputStream outputStream) throws IOException {
        final byte[] buffer = new byte[1024];
        int total = 0;
        while((total = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, total);
        }
    }

    public boolean isStreaming() {
        return true;
    }

    public void consumeContent() throws IOException {
        inputStream.close();
    }
}

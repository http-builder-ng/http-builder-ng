package groovyx.net.http;

import java.io.InputStream;

public interface ToServer {
    void toServer(String contentType, InputStream inputStream);
}

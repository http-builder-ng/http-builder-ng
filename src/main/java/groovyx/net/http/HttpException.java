package groovyx.net.http;

import java.util.List;

public class HttpException extends RuntimeException {

    private final FromServer fromServer;
    private final Object body;
    
    public HttpException(final FromServer fromServer, final Object body) {
        super(fromServer.getMessage());
        this.fromServer = fromServer;
        this.body = body;
    }

    public int getStatusCode() {
        return fromServer.getStatusCode();
    }

    public List<FromServer.Header> getHeaders() {
        return fromServer.getHeaders();
    }

    public Object getBody() {
        return body;
    }
}

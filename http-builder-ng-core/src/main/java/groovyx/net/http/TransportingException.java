package groovyx.net.http;

public class TransportingException extends RuntimeException {

    public TransportingException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public TransportingException(final Throwable cause) {
        super(cause);
    }
}

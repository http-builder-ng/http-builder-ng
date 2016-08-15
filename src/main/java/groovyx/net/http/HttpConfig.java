package groovyx.net.http;

import groovy.lang.Closure;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public interface HttpConfig {

    enum Status { SUCCESS, FAILURE };
    enum AuthType { BASIC, DIGEST };

    interface Auth {
        AuthType getAuthType();
        String getUser();
        String getPassword();

        default void basic(String user, String password) {
            basic(user, password, false);
        }
        
        void basic(String user, String password, boolean preemptive);

        default void digest(String user, String password) {
            digest(user, password, false);
        }
        
        void digest(String user, String password, boolean preemptive);
    }

    interface Request {
        Auth getAuth();
        void setContentType(String val);
        void setCharset(String val);
        void setCharset(Charset val);
        
        UriBuilder getUri();
        void setUri(String val) throws URISyntaxException;
        void setUri(URI val);
        void setUri(URL val) throws URISyntaxException;

        Map<String,String> getHeaders();
        void setHeaders(Map<String,String> toAdd);

        void setAccept(String[] values);
        void setAccept(List<String> values);
        void setBody(Object val);

        default void cookie(String name, String value) {
            cookie(name, value, null);
        }
        
        void cookie(String name, String value, Date expires);

        void encoder(String contentType, BiConsumer<ChainedHttpConfig,ToServer> val);
        void encoder(List<String> contentTypes, BiConsumer<ChainedHttpConfig,ToServer> val);
        BiConsumer<ChainedHttpConfig,ToServer> encoder(String contentType);
    }
    
    interface Response {
        void when(Status status, Closure<Object> closure);
        void when(Integer code, Closure<Object> closure);
        void when(String code, Closure<Object> closure);
        Closure<Object> when(Integer code);

        void success(Closure<Object> closure);
        void failure(Closure<Object> closure);

        void parser(String contentType, BiFunction<ChainedHttpConfig,FromServer,Object> val);
        void parser(List<String> contentTypes, BiFunction<ChainedHttpConfig,FromServer,Object> val);
        BiFunction<ChainedHttpConfig,FromServer,Object> parser(String contentType);
    }

    void context(String contentType, Object id, Object obj);
    
    default void context(final List<String> contentTypes, final Object id, final Object obj) {
        for(String contentType : contentTypes) {
            context(contentType, id, obj);
        }
    }
    
    Request getRequest();
    Response getResponse();
}

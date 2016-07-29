package groovyx.net.http.libspecific;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovyx.net.http.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

public class JavaHttpBuilder implements HttpBuilder {

    protected class Action {

        private final HttpURLConnection connection;
        private final ChainedHttpConfig requestConfig;
        private final String verb;
        
        public Action(final ChainedHttpConfig requestConfig, final String verb) {
            try {
                this.requestConfig = requestConfig;
                final ChainedHttpConfig.ChainedRequest cr = requestConfig.getChainedRequest();
                this.connection = (HttpURLConnection) cr.getUri().toURI().toURL().openConnection();
                this.verb = verb;
                this.connection.setRequestMethod(verb);

                if(cr.actualBody() != null) {
                    this.connection.setDoOutput(true);
                }
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void addHeaders() {
            final ChainedHttpConfig.ChainedRequest cr = requestConfig.getChainedRequest();
            for(Map.Entry<String,String> entry : cr.actualHeaders(new LinkedHashMap<>()).entrySet()) {
                connection.addRequestProperty(entry.getKey(), entry.getValue());
            }

            final String contentType = cr.actualContentType();
            if(contentType != null) {
                connection.addRequestProperty("Content-Type", contentType);
            }

            connection.addRequestProperty("Accept-Encoding", "gzip");
            
            final URI uri = cr.getUri().toURI();
            final List<Cookie> cookies = cr.actualCookies(new ArrayList());
            for(Cookie cookie : cookies) {
                final HttpCookie httpCookie = new HttpCookie(cookie.getName(), cookie.getValue());
                httpCookie.setDomain(uri.getHost());
                httpCookie.setPath(uri.getPath());
                if(cookie.getExpires() != null) {
                    final long diff = cookie.getExpires().getTime() - System.currentTimeMillis();
                    httpCookie.setMaxAge(diff / 1_000L);
                }
                else {
                    httpCookie.setMaxAge(3_600L);
                }
                
                globalCookieManager.getCookieStore().add(uri, httpCookie);
            }
        }

        private PasswordAuthentication getAuthInfo() {
            final HttpConfig.Auth auth = requestConfig.getChainedRequest().actualAuth();
            if(auth == null) {
                return null;
            }

            if(auth.getAuthType() == HttpConfig.AuthType.BASIC || auth.getAuthType() == HttpConfig.AuthType.DIGEST) {
                return new PasswordAuthentication(auth.getUser(), auth.getPassword().toCharArray());
            }
            else {
                throw new UnsupportedOperationException("HttpURLConnection does not support " + auth.getAuthType() + " authentication");
            }
        }

        private void handleToServer() {
            final ChainedHttpConfig.ChainedRequest cr = requestConfig.getChainedRequest();
            final Object body = cr.actualBody();
            if(body != null) {
                requestConfig.findEncoder().accept(cr, new JavaToServer());
            }
        }

        private Object handleFromServer() {
            final JavaFromServer fromServer = new JavaFromServer();
            try {
                final Function<FromServer,Object> parser = requestConfig.findParser(fromServer.getContentType());
                final Closure<Object> action = requestConfig.getChainedResponse().actualAction(fromServer.getStatusCode());
                if(fromServer.getHasBody()) {
                    final Object o = parser.apply(fromServer);
                    return action.call(ChainedHttpConfig.closureArgs(action, fromServer, o));
                }
                else {
                    return action.call(ChainedHttpConfig.closureArgs(action, fromServer, null));
                }
            }
            finally {
                fromServer.finish();
            }
        }
        
        public Object execute() {
            try {
                addHeaders();
                return ThreadLocalAuth.with(getAuthInfo(), () -> {
                        if(sslContext != null) {
                            HttpsURLConnection https = (HttpsURLConnection) connection;
                            https.setSSLSocketFactory(sslContext.getSocketFactory());
                        }
                        
                        connection.connect();
                        handleToServer();
                        return handleFromServer();
                    });
            }
            catch(Exception e) {
                throw new RuntimeException(e);
            }
        }

        protected class JavaToServer implements ToServer {
            
            public void toServer(final InputStream inputStream) {
                try {
                    final OutputStream os = connection.getOutputStream();
                    final byte[] buffer = new byte[4_096];
                    int read = 0;
                    while((read = inputStream.read(buffer)) != -1) {
                        os.write(buffer, 0, read);
                    }
                }
                catch(IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        
        protected class JavaFromServer implements FromServer {

            private InputStream is;
            private boolean hasBody;
            private List<Header> headers;
            
            public JavaFromServer() {
                //TODO: detect non success and read from error stream instead
                try {
                    headers = populateHeaders();
                    BufferedInputStream bis = new BufferedInputStream(connection.getInputStream());
                    bis.mark(0);
                    hasBody = bis.read() != -1;
                    bis.reset();
                    is = handleEncoding(bis);
                }
                catch(IOException e) {
                    //swallow, no body is present?
                    is = null;
                    hasBody = false;
                }
            }

            private InputStream handleEncoding(final InputStream is) throws IOException {
                Header encodingHeader = Header.find(headers, "Content-Encoding");
                if(encodingHeader != null) {
                    if(encodingHeader.getValue().equals("gzip")) {
                        return new GZIPInputStream(is);
                    }
                    else if(encodingHeader.getValue().equals("deflate")) {
                        return new InflaterInputStream(is);
                    }
                }

                return is;
            }

            private List<Header> populateHeaders() {
                final List<Header> ret = new ArrayList<>();
                for(int i = 0; i < Integer.MAX_VALUE; ++i) {
                    final String key = connection.getHeaderFieldKey(i);
                    final String value = connection.getHeaderField(i);
                    if(key == null && value == null) {
                        break;
                    }

                    if(key != null && value != null) {
                        ret.add(Header.keyValue(key, value));
                    }
                }

                return Collections.unmodifiableList(ret);
            }
            
            public InputStream getInputStream() {
                return is;
            }
            
            public int getStatusCode() {
                try {
                    return connection.getResponseCode();
                }
                catch(IOException e) {
                    throw new RuntimeException(e);
                }
            }
            
            public String getMessage() {
                try {
                    return connection.getResponseMessage();
                }
                catch(IOException e) {
                    throw new RuntimeException(e);
                }
            }
            
            public List<Header> getHeaders() {
                return headers;
            }
            
            public boolean getHasBody() {
                return hasBody;
            }
            
            public void finish() {
                //do nothing, should auto cleanup
            }
        }
    }

    protected static class ThreadLocalAuth extends Authenticator {
        private static final ThreadLocal<PasswordAuthentication> tlAuth = new ThreadLocal<PasswordAuthentication>();

        public PasswordAuthentication getPasswordAuthentication() {
            return tlAuth.get();
        }
        
        public static final <V> V with(final PasswordAuthentication pa, final Callable<V> callable) throws Exception {
            tlAuth.set(pa);
            try {
                return callable.call();
            }
            finally {
                tlAuth.set(null);
            }
        }
    }

    private final static CookieManager globalCookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    
    static {
        Authenticator.setDefault(new ThreadLocalAuth());
        CookieHandler.setDefault(globalCookieManager);
    }
    
    final private ChainedHttpConfig config;
    final private Executor executor;
    final private SSLContext sslContext;
    
    public JavaHttpBuilder(final HttpObjectConfig config) {
        this.config = new HttpConfigs.ThreadSafeHttpConfig(config.getChainedConfig());
        this.executor = config.getExecution().getExecutor();
        this.sslContext = config.getExecution().getSslContext();
    }

    private ChainedHttpConfig configureRequest(final Closure closure) {
        final ChainedHttpConfig myConfig = HttpConfigs.requestLevel(config);
        closure.setDelegate(myConfig);
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        closure.call();
        return myConfig;
    }

    public Object get(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return new Action(configureRequest(closure), "GET").execute();
    }
    
    public Object head(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return new Action(configureRequest(closure), "HEAD").execute();
    }
    
    public Object post(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return new Action(configureRequest(closure), "POST").execute();
    }
    
    public Object put(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return new Action(configureRequest(closure), "PUT").execute();
    }
    
    public Object delete(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return new Action(configureRequest(closure), "DELETE").execute();
    }
    
    public Executor getExecutor() {
        return executor;
    }

    public void close() {
        throw new UnsupportedOperationException();
    }
}

package groovyx.net.http.libspecific;

import groovyx.net.http.*;
import java.util.function.BiConsumer;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import javax.net.ssl.SSLContext;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.protocol.ResponseProcessCookies;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;
import org.codehaus.groovy.runtime.MethodClosure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApacheHttpBuilder implements HttpBuilder {

    private static final Logger log = LoggerFactory.getLogger(HttpBuilder.class);
    
    private static class Handler implements ResponseHandler<Object> {

        private final ChainedHttpConfig config;
        
        public Handler(final ChainedHttpConfig config) {
            this.config = config;
        }
        
        private Object[] closureArgs(final Closure<Object> closure, final FromServer fromServer, final Object o) {
            final int size = closure.getMaximumNumberOfParameters();
            final Object[] args = new Object[size];
            if(size >= 1) {
                args[0] = fromServer;
            }

            if(size >= 2) {
                args[1] = o;
            }

            return args;
        }

        public Object handleResponse(final HttpResponse response) {
            final ApacheFromServer fromServer = new ApacheFromServer(response);
            try {
                final Function<FromServer,Object> parser = config.findParser(fromServer.getContentType());
                final Closure<Object> action = config.getChainedResponse().actualAction(fromServer.getStatusCode());
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
    }

    final private CookieStore cookieStore;
    final private CloseableHttpClient client;
    final private ChainedHttpConfig config;
    final private Executor executor;

    public ApacheHttpBuilder(final HttpObjectConfig config) {
        this.config = new HttpConfigs.ThreadSafeHttpConfig(config.getChainedConfig());
        this.executor = config.getExecution().getExecutor();
        this.cookieStore = new BasicCookieStore();
        HttpClientBuilder myBuilder = HttpClients.custom().setDefaultCookieStore(cookieStore);
        
        if(config.getExecution().getMaxThreads() > 1) {
            final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
            cm.setMaxTotal(config.getExecution().getMaxThreads());
            cm.setDefaultMaxPerRoute(config.getExecution().getMaxThreads());
            
            myBuilder.setConnectionManager(cm);
        }
        
        if(config.getExecution().getSslContext() != null) {
            myBuilder.setSSLContext(config.getExecution().getSslContext());
        }

        this.client = myBuilder.build();
    }
    
    public Executor getExecutor() {
        return executor;
    }

    public void close() {
        try {
            client.close();
        }
        catch(IOException ioe) {
            if(log.isWarnEnabled()) {
                log.warn("Error in closing http client", ioe);
            }
        }
    }

    private ChainedHttpConfig configureRequest(final Closure closure) {
        final ChainedHttpConfig myConfig = HttpConfigs.requestLevel(config);
        closure.setDelegate(myConfig);
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        closure.call();
        return myConfig;
    }

    private int port(final URI uri) {
        if(uri.getPort() != -1) {
            return uri.getPort();
        }

        if(uri.getScheme().startsWith("https")) {
            return 443;
        }

        return 80;
    }

    private void basicAuth(final HttpClientContext c, final HttpConfig.Auth auth, final URI uri) {
        CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(new AuthScope(uri.getHost(), port(uri)),
                                new UsernamePasswordCredentials(auth.getUser(), auth.getPassword()));
        c.setCredentialsProvider(provider);
    }

    private void digestAuth(final HttpClientContext c, final HttpConfig.Auth auth, final URI uri) {
        basicAuth(c, auth, uri);
    }

    private HttpClientContext context(final ChainedHttpConfig requestConfig) {
        final HttpClientContext c = HttpClientContext.create();
        final HttpConfig.Auth auth = requestConfig.getChainedRequest().actualAuth();
        
        if(auth != null) {
            final URI uri = requestConfig.getRequest().getUri().toURI();
            if(auth.getAuthType() == HttpConfig.AuthType.BASIC) {
                basicAuth(c, auth, uri);
            }
            else if(auth.getAuthType() == HttpConfig.AuthType.DIGEST) {
                digestAuth(c, auth, uri);
            }
        }
        
        return c;
    }

    private Object exec(final HttpUriRequest request, final ChainedHttpConfig requestConfig) {
        try {
            return client.execute(request, new Handler(requestConfig), context(requestConfig));
        }
        catch(IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private HttpEntity entity(final ChainedHttpConfig config) {
        final ApacheToServer ats = new ApacheToServer();
        config.findEncoder().accept(config.getChainedRequest(), ats);
        return ats;
    }

    private <T extends HttpUriRequest> T addHeaders(final ChainedHttpConfig.ChainedRequest cr, final T message) {
        for(Map.Entry<String,String> entry : cr.actualHeaders(new LinkedHashMap<>()).entrySet()) {
            message.addHeader(entry.getKey(), entry.getValue());
        }

        final String contentType = cr.actualContentType();
        if(contentType != null) {
            message.addHeader("Content-Type", contentType);
        }

        //technically cookies are headers, so add them here
        final URI uri = cr.getUri().toURI();
        List<Cookie> cookies = cr.actualCookies(new ArrayList());
        for(Cookie cookie : cookies) {
            final BasicClientCookie apacheCookie = new BasicClientCookie(cookie.getName(), cookie.getValue());
            apacheCookie.setDomain(uri.getHost());
            apacheCookie.setPath(uri.getPath());
            if(cookie.getExpires() != null) {
                apacheCookie.setExpiryDate(cookie.getExpires());
            }
            
            cookieStore.addCookie(apacheCookie);
        }

        return message;
    }

    public Object get(@DelegatesTo(HttpConfig.class) final Closure closure) {
        final ChainedHttpConfig requestConfig = configureRequest(closure);
        final ChainedHttpConfig.ChainedRequest cr = requestConfig.getChainedRequest();
        return exec(addHeaders(cr, new HttpGet(cr.getUri().toURI())), requestConfig);
    }

    public Object head(@DelegatesTo(HttpConfig.class) final Closure closure) {
        final ChainedHttpConfig requestConfig = configureRequest(closure);
        final ChainedHttpConfig.ChainedRequest cr = requestConfig.getChainedRequest();
        return exec(addHeaders(cr, new HttpHead(cr.getUri().toURI())), requestConfig);
    }

    public Object post(@DelegatesTo(HttpConfig.class) final Closure closure) {
        final ChainedHttpConfig requestConfig = configureRequest(closure);
        final ChainedHttpConfig.ChainedRequest cr = requestConfig.getChainedRequest();
        final HttpPost post = addHeaders(cr, new HttpPost(cr.getUri().toURI()));
        if(cr.actualBody() != null) {
            post.setEntity(entity(requestConfig));
        }
        
        return exec(post, requestConfig);
    }

    public Object put(@DelegatesTo(HttpConfig.class) final Closure closure) {
        final ChainedHttpConfig requestConfig = configureRequest(closure);
        final ChainedHttpConfig.ChainedRequest cr = requestConfig.getChainedRequest();
        final HttpPut put = addHeaders(cr, new HttpPut(cr.getUri().toURI()));
        if(cr.actualBody() != null) {
            put.setEntity(entity(requestConfig));
        }
        
        return exec(put, requestConfig);
    }

    public Object delete(@DelegatesTo(HttpConfig.class) final Closure closure) {
        final ChainedHttpConfig requestConfig = configureRequest(closure);
        final ChainedHttpConfig.ChainedRequest cr = requestConfig.getChainedRequest();
        final HttpDelete del = addHeaders(cr, new HttpDelete(cr.getUri().toURI()));
        return exec(del, requestConfig);
    }
}

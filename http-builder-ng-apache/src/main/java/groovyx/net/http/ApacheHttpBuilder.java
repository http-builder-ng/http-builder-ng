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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static groovyx.net.http.HttpBuilder.ResponseHandlerFunction.HANDLER_FUNCTION;

/**
 * `HttpBuilder` implementation based on the https://hc.apache.org/httpcomponents-client-ga/[Apache HttpClient library].
 *
 * Generally, this class should not be used directly, the preferred method of instantiation is via the
 * `groovyx.net.http.HttpBuilder.configure(java.util.function.Function)` or
 * `groovyx.net.http.HttpBuilder.configure(java.util.function.Function, groovy.lang.Closure)` methods.
 */
public class ApacheHttpBuilder extends HttpBuilder {

    private static final Logger log = LoggerFactory.getLogger(ApacheHttpBuilder.class);

    private static class ApacheFromServer implements FromServer {
        
        private final HttpResponse response;
        private final HttpEntity entity;
        private final List<Header<?>> headers;
        private final InputStream inputStream;
        private final URI uri;
    
        public ApacheFromServer(final URI originalUri, final HttpResponse response) {
            this.uri = originalUri;
            this.response = response;
            this.entity = response.getEntity();

            if(entity != null) {
                try {
                    this.inputStream = entity.getContent();
                }
                catch(IOException e) {
                    throw new RuntimeException("Could not get input stream from apache http client", e);
                }
            }
            else {
                this.inputStream = null;
            }
        
            this.headers = new ArrayList<>(response.getAllHeaders().length);
            for(org.apache.http.Header header : response.getAllHeaders()) {
                headers.add(Header.keyValue(header.getName(), header.getValue()));
            }
        }

        public InputStream getInputStream() {
            return inputStream;
        }

        public boolean getHasBody() {
            return entity != null;
        }

        public int getStatusCode() {
            return response.getStatusLine().getStatusCode();
        }

        public String getMessage() {
            return response.getStatusLine().getReasonPhrase();
        }

        public List<Header<?>> getHeaders() {
            return headers;
        }

        public URI getUri() {
            return uri;
        }

        public void finish() {
            EntityUtils.consumeQuietly(response.getEntity());
        }
    }

    public static class ApacheToServer implements ToServer, HttpEntity {

        private final String contentType;
        private InputStream inputStream;

        public ApacheToServer(final String contentType) {
            this.contentType = contentType;
        }

        public void toServer(final InputStream inputStream) {
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

        public org.apache.http.Header getContentType() {
            return new BasicHeader("Content-Type", contentType);
        }
        
        public org.apache.http.Header getContentEncoding() {
            return null;
        }

        public InputStream getContent() {
            return inputStream;
        }

        public void writeTo(final OutputStream outputStream) {
            NativeHandlers.Parsers.transfer(inputStream, outputStream, false);
        }

        public boolean isStreaming() {
            return true;
        }

        @SuppressWarnings("deprecation") //apache httpentity requires method
        public void consumeContent() throws IOException {
            inputStream.close();
        }
    }
    
    private static class Handler implements ResponseHandler<Object> {

        private final ChainedHttpConfig requestConfig;
        
        public Handler(final ChainedHttpConfig config) {
            this.requestConfig = config;
        }

        public Object handleResponse(final HttpResponse response) {
            return HANDLER_FUNCTION.apply(requestConfig, new ApacheFromServer(requestConfig.getChainedRequest().getUri().toURI(), response));
        }
    }

    final private CloseableHttpClient client;
    final private ChainedHttpConfig config;
    final private Executor executor;
    final private HttpObjectConfig.Client clientConfig;
    final private CookieStore cookieStore;

    public ApacheHttpBuilder(final HttpObjectConfig config) {
        super(config);
        this.config = new HttpConfigs.ThreadSafeHttpConfig(config.getChainedConfig());
        this.executor = config.getExecution().getExecutor();
        this.clientConfig = config.getClient();
        HttpClientBuilder myBuilder = HttpClients.custom();

        if(config.getExecution().getMaxThreads() > 1) {
            final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
            cm.setMaxTotal(config.getExecution().getMaxThreads());
            cm.setDefaultMaxPerRoute(config.getExecution().getMaxThreads());
            
            myBuilder.setConnectionManager(cm);
            cookieStore = null;
        }
        else {
            cookieStore = new BasicCookieStore();
        }
        
        if(config.getExecution().getSslContext() != null) {
            myBuilder.setSSLContext(config.getExecution().getSslContext());
        }

        this.client = myBuilder.build();
    }

    protected ChainedHttpConfig getObjectConfig() {
        return config;
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

    private void basicAuth(final HttpClientContext c, final HttpConfig.Auth auth, final URI uri) {
        CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(AuthScope.ANY, //new AuthScope(uri.getHost(), port(uri)),
                                new UsernamePasswordCredentials(auth.getUser(), auth.getPassword()));
        c.setCredentialsProvider(provider);
    }

    private void digestAuth(final HttpClientContext c, final HttpConfig.Auth auth, final URI uri) {
        basicAuth(c, auth, uri);
    }

    private void cookies(final HttpClientContext c, final ChainedHttpConfig.ChainedRequest cr) {
        //if the client was configured for single thread use, then we can safely
        //use the class based cookie store, otherwise we need to create a per request store
        CookieStore cookieStore = this.cookieStore == null ? new BasicCookieStore() : this.cookieStore;
        final URI uri = cr.getUri().toURI();
        List<Cookie> cookies = cr.actualCookies(new ArrayList<>());
        for(Cookie cookie : cookies) {
            final BasicClientCookie apacheCookie = new BasicClientCookie(cookie.getName(), cookie.getValue());
            apacheCookie.setVersion(clientConfig.getCookieVersion());
            apacheCookie.setDomain(uri.getHost());
            apacheCookie.setPath(uri.getPath());
            if(cookie.getExpires() != null) {
                apacheCookie.setExpiryDate(cookie.getExpires());
            }
            
            cookieStore.addCookie(apacheCookie);
        }

        c.setCookieStore(cookieStore);
    }

    private HttpClientContext context(final ChainedHttpConfig requestConfig) {
        final HttpClientContext c = HttpClientContext.create();
        final ChainedHttpConfig.ChainedRequest cr = requestConfig.getChainedRequest();
        final HttpConfig.Auth auth = cr.actualAuth();
        
        if(auth != null) {
            final URI uri = requestConfig.getRequest().getUri().toURI();
            if(auth.getAuthType() == HttpConfig.AuthType.BASIC) {
                basicAuth(c, auth, uri);
            }
            else if(auth.getAuthType() == HttpConfig.AuthType.DIGEST) {
                digestAuth(c, auth, uri);
            }
        }

        cookies(c, cr);
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
        final ApacheToServer ats = new ApacheToServer(config.findContentType());
        config.findEncoder().accept(config, ats);
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

        return message;
    }

    protected Object doGet(final ChainedHttpConfig requestConfig) {
        final ChainedHttpConfig.ChainedRequest cr = requestConfig.getChainedRequest();
        return exec(addHeaders(cr, new HttpGet(cr.getUri().toURI())), requestConfig);
    }

    protected Object doHead(final ChainedHttpConfig requestConfig) {
        final ChainedHttpConfig.ChainedRequest cr = requestConfig.getChainedRequest();
        return exec(addHeaders(cr, new HttpHead(cr.getUri().toURI())), requestConfig);
    }

    protected Object doPost(final ChainedHttpConfig requestConfig) {
        final ChainedHttpConfig.ChainedRequest cr = requestConfig.getChainedRequest();
        final HttpPost post = addHeaders(cr, new HttpPost(cr.getUri().toURI()));
        if(cr.actualBody() != null) {
            post.setEntity(entity(requestConfig));
        }
        
        return exec(post, requestConfig);
    }

    protected Object doPut(final ChainedHttpConfig requestConfig) {
        final ChainedHttpConfig.ChainedRequest cr = requestConfig.getChainedRequest();
        final HttpPut put = addHeaders(cr, new HttpPut(cr.getUri().toURI()));
        if(cr.actualBody() != null) {
            put.setEntity(entity(requestConfig));
        }
        
        return exec(put, requestConfig);
    }

    protected Object doDelete(final ChainedHttpConfig requestConfig) {
        final ChainedHttpConfig.ChainedRequest cr = requestConfig.getChainedRequest();
        return exec(addHeaders(cr, new HttpDelete(cr.getUri().toURI())), requestConfig);
    }
}

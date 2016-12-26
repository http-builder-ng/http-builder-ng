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

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
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
import java.util.function.Consumer;
import java.util.function.Function;

import static groovyx.net.http.HttpBuilder.ResponseHandlerFunction.HANDLER_FUNCTION;
import static groovyx.net.http.NativeHandlers.Parsers.transfer;

/**
 * `HttpBuilder` implementation based on the https://hc.apache.org/httpcomponents-client-ga/[Apache HttpClient library].
 *
 * Generally, this class should not be used directly, the preferred method of instantiation is via one of the two static `configure()` methods of this
 * class or using one of the `configure` methods of `HttpBuilder` with a factory function for this builder.
 */
public class ApacheHttpBuilder extends HttpBuilder {

    private static final Function<HttpObjectConfig, ? extends HttpBuilder> apacheFactory = ApacheHttpBuilder::new;
    private static final Logger log = LoggerFactory.getLogger(ApacheHttpBuilder.class);

    /**
     * Creates an `HttpBuilder` using the `ApacheHttpBuilder` factory instance configured with the provided configuration closure.
     *
     * The configuration closure delegates to the {@link HttpObjectConfig} interface, which is an extension of the {@link HttpConfig} interface -
     * configuration properties from either may be applied to the global client configuration here. See the documentation for those interfaces for
     * configuration property details.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101'
     * }
     * ----
     *
     * @param closure the configuration closure (delegated to {@link HttpObjectConfig})
     * @return the configured `HttpBuilder`
     */
    public static HttpBuilder configure(@DelegatesTo(HttpObjectConfig.class) final Closure closure) {
        return configure(apacheFactory, closure);
    }

    /**
     * Creates an `HttpBuilder` using the `ApacheHttpBuilder` factory instance configured with the provided configuration function.
     *
     * The configuration {@link Consumer} function accepts an instance of the {@link HttpObjectConfig} interface, which is an extension of the {@link HttpConfig}
     * interface - configuration properties from either may be applied to the global client configuration here. See the documentation for those interfaces for
     * configuration property details.
     *
     * This configuration method is generally meant for use with standard Java.
     *
     * [source,java]
     * ----
     * HttpBuilder.configure(new Consumer<HttpObjectConfig>() {
     *     public void accept(HttpObjectConfig config) {
     *         config.getRequest().setUri(format("http://localhost:%d", serverRule.getPort()));
     *     }
     * });
     * ----
     *
     * Or, using lambda expressions:
     *
     * [source,java]
     * ----
     * HttpBuilder.configure(config -> {
     *     config.getRequest().setUri(format("http://localhost:%d", serverRule.getPort()));
     * });
     * ----
     *
     * @param configuration the configuration function (accepting {@link HttpObjectConfig})
     * @return the configured `HttpBuilder`
     */
    public static HttpBuilder configure(final Consumer<HttpObjectConfig> configuration) {
        return configure(apacheFactory, configuration);
    }

    private class ApacheFromServer implements FromServer {

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
            addCookieStore(uri, headers);
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

        private ChainedHttpConfig config;
        private InputStream inputStream;

        public ApacheToServer(final ChainedHttpConfig config) {
            this.config = config;
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
            return new BasicHeader("Content-Type", config.findContentType());
        }

        public org.apache.http.Header getContentEncoding() {
            return null;
        }

        public InputStream getContent() {
            return inputStream;
        }

        public void writeTo(final OutputStream outputStream) {
            transfer(inputStream, outputStream, false);
        }

        public boolean isStreaming() {
            return true;
        }

        @SuppressWarnings("deprecation") //apache httpentity requires method
        public void consumeContent() throws IOException {
            inputStream.close();
        }
    }

    private class Handler implements ResponseHandler<Object> {

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
        final ApacheToServer ats = new ApacheToServer(config);
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

        for(Map.Entry<String,String> e : cookiesToAdd(clientConfig, cr).entrySet()) {
            message.addHeader(e.getKey(), e.getValue());
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
            final HttpEntity entity = entity(requestConfig);
            post.setEntity(entity);
            post.setHeader(entity.getContentType());
        }

        return exec(post, requestConfig);
    }

    protected Object doPut(final ChainedHttpConfig requestConfig) {
        final ChainedHttpConfig.ChainedRequest cr = requestConfig.getChainedRequest();
        final HttpPut put = addHeaders(cr, new HttpPut(cr.getUri().toURI()));
        if(cr.actualBody() != null) {
            final HttpEntity entity = entity(requestConfig);
            put.setEntity(entity);
            put.setHeader(entity.getContentType());
        }

        return exec(put, requestConfig);
    }

    protected Object doDelete(final ChainedHttpConfig requestConfig) {
        final ChainedHttpConfig.ChainedRequest cr = requestConfig.getChainedRequest();
        return exec(addHeaders(cr, new HttpDelete(cr.getUri().toURI())), requestConfig);
    }
}

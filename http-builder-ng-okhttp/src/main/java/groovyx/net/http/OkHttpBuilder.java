/**
 * Copyright (C) 2016 David Clark
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package groovyx.net.http;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import okhttp3.*;
import okio.BufferedSink;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

import static groovyx.net.http.FromServer.Header.keyValue;
import static groovyx.net.http.HttpBuilder.ResponseHandlerFunction.HANDLER_FUNCTION;
import static groovyx.net.http.util.SslIssueIgnoring.*;
import static java.util.stream.Collectors.toList;

/**
 * `HttpBuilder` implementation based on the http://square.github.io/okhttp/[OkHttp] client library.
 * <p>
 * Generally, this class should not be used directly, the preferred method of instantiation is via one of the two static `configure()` methods of this
 * class or using one of the `configure` methods of `HttpBuilder` with a factory function for this builder.
 */
public class OkHttpBuilder extends HttpBuilder {

    private static final Function<HttpObjectConfig, ? extends HttpBuilder> okFactory = OkHttpBuilder::new;
    private final ChainedHttpConfig config;
    private final HttpObjectConfig.Client clientConfig;
    private final Executor executor;
    private final OkHttpClient client;

    protected OkHttpBuilder(final HttpObjectConfig config) {
        super(config);

        this.config = new HttpConfigs.ThreadSafeHttpConfig(config.getChainedConfig());
        this.clientConfig = config.getClient();
        this.executor = config.getExecution().getExecutor();

        final OkHttpClient.Builder builder = new OkHttpClient.Builder();

        final SSLContext sslContext = config.getExecution().getSslContext();
        if (sslContext != null) {
            builder.sslSocketFactory(sslContext.getSocketFactory()/*, (X509TrustManager) TRUST_MANAGERS[0]*/);
            builder.hostnameVerifier(config.getExecution().getHostnameVerifier());
        }

        this.client = builder.build();
    }

    /**
     * Creates an `HttpBuilder` using the `OkHttpBuilder` factory instance configured with the provided configuration closure.
     * <p>
     * The configuration closure delegates to the {@link HttpObjectConfig} interface, which is an extension of the {@link HttpConfig} interface -
     * configuration properties from either may be applied to the global client configuration here. See the documentation for those interfaces for
     * configuration property details.
     * <p>
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     * request.uri = 'http://localhost:10101'
     * }
     * ----
     *
     * @param closure the configuration closure (delegated to {@link HttpObjectConfig})
     * @return the configured `HttpBuilder`
     */
    public static HttpBuilder configure(@DelegatesTo(HttpObjectConfig.class) final Closure closure) {
        return configure(okFactory, closure);
    }

    /**
     * Creates an `HttpBuilder` using the `OkHttpBuilder` factory instance configured with the provided configuration function.
     * <p>
     * The configuration {@link Consumer} function accepts an instance of the {@link HttpObjectConfig} interface, which is an extension of the {@link HttpConfig}
     * interface - configuration properties from either may be applied to the global client configuration here. See the documentation for those interfaces for
     * configuration property details.
     * <p>
     * This configuration method is generally meant for use with standard Java.
     * <p>
     * [source,java]
     * ----
     * HttpBuilder.configure(new Consumer<HttpObjectConfig>() {
     * public void accept(HttpObjectConfig config) {
     * config.getRequest().setUri(format("http://localhost:%d", serverRule.getPort()));
     * }
     * });
     * ----
     * <p>
     * Or, using lambda expressions:
     * <p>
     * [source,java]
     * ----
     * HttpBuilder.configure(config -> {
     * config.getRequest().setUri(format("http://localhost:%d", serverRule.getPort()));
     * });
     * ----
     *
     * @param configuration the configuration function (accepting {@link HttpObjectConfig})
     * @return the configured `HttpBuilder`
     */
    public static HttpBuilder configure(final Consumer<HttpObjectConfig> configuration) {
        return configure(okFactory, configuration);
    }


    @Override
    protected ChainedHttpConfig getObjectConfig() {
        return config;
    }

    @Override
    public Executor getExecutor() {
        return executor;
    }

    @Override
    protected Object doGet(final ChainedHttpConfig chainedConfig) {
        final ChainedHttpConfig.ChainedRequest cr = chainedConfig.getChainedRequest();
        final Request.Builder requestBuilder = new Request.Builder().get().url(HttpUrl.get(cr.getUri().toURI()));

        applyHeaders(requestBuilder, cr);
        applyAuth(requestBuilder, chainedConfig);

        return execute(requestBuilder, chainedConfig);
    }

    @Override
    protected Object doHead(final ChainedHttpConfig chainedConfig) {
        final ChainedHttpConfig.ChainedRequest cr = chainedConfig.getChainedRequest();
        final Request.Builder requestBuilder = new Request.Builder().head().url(HttpUrl.get(cr.getUri().toURI()));

        applyHeaders(requestBuilder, cr);
        applyAuth(requestBuilder, chainedConfig);

        return execute(requestBuilder, chainedConfig);
    }

    @Override
    protected Object doPost(final ChainedHttpConfig chainedConfig) {
        final ChainedHttpConfig.ChainedRequest cr = chainedConfig.getChainedRequest();
        final Request.Builder requestBuilder = new Request.Builder();

        requestBuilder.post(resolveRequestBody(chainedConfig, cr)).url(HttpUrl.get(cr.getUri().toURI()));

        applyHeaders(requestBuilder, cr);
        applyAuth(requestBuilder, chainedConfig);

        return execute(requestBuilder, chainedConfig);
    }

    @Override
    protected Object doPut(final ChainedHttpConfig chainedConfig) {
        final ChainedHttpConfig.ChainedRequest cr = chainedConfig.getChainedRequest();
        final Request.Builder requestBuilder = new Request.Builder();

        requestBuilder.put(resolveRequestBody(chainedConfig, cr)).url(HttpUrl.get(cr.getUri().toURI()));

        applyHeaders(requestBuilder, cr);
        applyAuth(requestBuilder, chainedConfig);

        return execute(requestBuilder, chainedConfig);
    }

    @Override
    protected Object doDelete(final ChainedHttpConfig chainedConfig) {
        final ChainedHttpConfig.ChainedRequest cr = chainedConfig.getChainedRequest();
        final Request.Builder requestBuilder = new Request.Builder().delete().url(HttpUrl.get(cr.getUri().toURI()));

        applyHeaders(requestBuilder, cr);
        applyAuth(requestBuilder, chainedConfig);

        return execute(requestBuilder, chainedConfig);
    }

    @Override
    public void close() throws IOException {
        // does nothing
    }

    private RequestBody resolveRequestBody(ChainedHttpConfig chainedConfig, ChainedHttpConfig.ChainedRequest cr) {
        RequestBody body = RequestBody.create(MediaType.parse("text/html"), "");
        if (cr.actualBody() != null) {
            final OkHttpToServer toServer = new OkHttpToServer(chainedConfig);
            chainedConfig.findEncoder().accept(chainedConfig, toServer);

            body = toServer;
        }
        return body;
    }

    private void applyHeaders(final Request.Builder requestBuilder, final ChainedHttpConfig.ChainedRequest cr) {
        for (Map.Entry<String, String> entry : cr.actualHeaders(new LinkedHashMap<>()).entrySet()) {
            requestBuilder.addHeader(entry.getKey(), entry.getValue());
        }

        final String contentType = cr.actualContentType();
        if (contentType != null) {
            requestBuilder.addHeader("Content-Type", contentType);
        }

        for (Map.Entry<String, String> e : cookiesToAdd(clientConfig, cr).entrySet()) {
            requestBuilder.addHeader(e.getKey(), e.getValue());
        }
    }

    private static void applyAuth(final Request.Builder requestBuilder, final ChainedHttpConfig chainedConfig) {
        final HttpConfig.Auth auth = chainedConfig.getChainedRequest().actualAuth();
        if (auth != null) {
            requestBuilder.addHeader("Authorization", Credentials.basic(auth.getUser(), auth.getPassword()));
        }
    }

    private Object execute(final Request.Builder requestBuilder, final ChainedHttpConfig chainedConfig) {
        try (Response response = client.newCall(requestBuilder.build()).execute()) {
            return HANDLER_FUNCTION.apply(
                chainedConfig,
                new OkHttpFromServer(chainedConfig.getChainedRequest().getUri().toURI(), response)
            );
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private class OkHttpFromServer implements FromServer {

        private final URI uri;
        private final Response response;
        private List<Header<?>> headers;

        private OkHttpFromServer(final URI uri, final Response response) {
            this.uri = uri;
            this.response = response;
            this.headers = populateHeaders();
            addCookieStore(uri, headers);
        }

        private List<Header<?>> populateHeaders() {
            final Headers headers = response.headers();
            return headers.names().stream().map((Function<String, Header<?>>) name -> keyValue(name, headers.get(name))).collect(toList());
        }

        @Override
        public InputStream getInputStream() {
            return response.body().byteStream();
        }

        @Override
        public int getStatusCode() {
            return response.code();
        }

        @Override
        public String getMessage() {
            return response.message();
        }

        @Override
        public List<Header<?>> getHeaders() {
            return headers;
        }

        @Override
        public boolean getHasBody() {
            try {
                return !response.body().source().exhausted();
            } catch (IOException e) {
                return false;
            }
        }

        @Override
        public URI getUri() {
            return uri;
        }

        @Override
        public void finish() {
            response.close();
        }
    }

    private static class OkHttpToServer extends RequestBody implements ToServer {

        private ChainedHttpConfig config;
        private InputStream inputStream;

        private OkHttpToServer(final ChainedHttpConfig config) {
            this.config = config;
        }

        @Override
        public void toServer(final InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public MediaType contentType() {
            return MediaType.parse(config.findContentType());
        }

        @Override
        public void writeTo(final BufferedSink sink) throws IOException {
            try {
                int b = inputStream.read();
                while (b != -1) {
                    sink.writeByte(b);
                    b = inputStream.read();
                }
            } finally {
                inputStream.close();
            }
        }
    }
}

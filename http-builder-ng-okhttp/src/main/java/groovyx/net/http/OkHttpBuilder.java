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

import okhttp3.Cookie;
import okhttp3.*;
import okio.BufferedSink;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.function.Function;

import static groovyx.net.http.FromServer.Header.keyValue;
import static groovyx.net.http.HttpBuilder.ResponseHandlerFunction.HANDLER_FUNCTION;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static okhttp3.internal.http.HttpDate.MAX_DATE;

/**
 * `HttpBuilder` implementation based on the http://square.github.io/okhttp/[OkHttp] client library.
 *
 * Generally, this class should not be used directly, the preferred method of instantiation is via the
 * `groovyx.net.http.HttpBuilder.configure(java.util.function.Function)` or
 * `groovyx.net.http.HttpBuilder.configure(java.util.function.Function, groovy.lang.Closure)` methods.
 */
public class OkHttpBuilder extends HttpBuilder {

    private final ChainedHttpConfig config;
    private final Executor executor;
    private final OkHttpClient client;

    protected OkHttpBuilder(final HttpObjectConfig config) {
        super(config);

        this.config = new HttpConfigs.ThreadSafeHttpConfig(config.getChainedConfig());
        this.executor = config.getExecution().getExecutor();
        this.client = new OkHttpClient.Builder().cookieJar(new NonPersistingCookieJar()).build();
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
        applyCookies(client, cr);
        applyAuth(requestBuilder, chainedConfig);

        return execute(requestBuilder, chainedConfig);
    }

    @Override
    protected Object doHead(final ChainedHttpConfig chainedConfig) {
        final ChainedHttpConfig.ChainedRequest cr = chainedConfig.getChainedRequest();
        final Request.Builder requestBuilder = new Request.Builder().head().url(HttpUrl.get(cr.getUri().toURI()));

        applyHeaders(requestBuilder, cr);
        applyCookies(client, cr);
        applyAuth(requestBuilder, chainedConfig);

        return execute(requestBuilder, chainedConfig);
    }

    @Override
    protected Object doPost(final ChainedHttpConfig chainedConfig) {
        final ChainedHttpConfig.ChainedRequest cr = chainedConfig.getChainedRequest();
        final Request.Builder requestBuilder = new Request.Builder();

        requestBuilder.post(resolveRequestBody(chainedConfig, cr)).url(HttpUrl.get(cr.getUri().toURI()));

        applyHeaders(requestBuilder, cr);
        applyCookies(client, cr);
        applyAuth(requestBuilder, chainedConfig);

        return execute(requestBuilder, chainedConfig);
    }

    @Override
    protected Object doPut(final ChainedHttpConfig chainedConfig) {
        final ChainedHttpConfig.ChainedRequest cr = chainedConfig.getChainedRequest();
        final Request.Builder requestBuilder = new Request.Builder();

        requestBuilder.put(resolveRequestBody(chainedConfig, cr)).url(HttpUrl.get(cr.getUri().toURI()));

        applyHeaders(requestBuilder, cr);
        applyCookies(client, cr);
        applyAuth(requestBuilder, chainedConfig);

        return execute(requestBuilder, chainedConfig);
    }

    @Override
    protected Object doDelete(final ChainedHttpConfig chainedConfig) {
        final ChainedHttpConfig.ChainedRequest cr = chainedConfig.getChainedRequest();
        final Request.Builder requestBuilder = new Request.Builder().delete().url(HttpUrl.get(cr.getUri().toURI()));

        applyHeaders(requestBuilder, cr);
        applyCookies(client, cr);
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
            final OkHttpToServer toServer = new OkHttpToServer(chainedConfig.findContentType());
            chainedConfig.findEncoder().accept(chainedConfig, toServer);

            body = toServer;
        }
        return body;
    }

    private static void applyHeaders(final Request.Builder requestBuilder, final ChainedHttpConfig.ChainedRequest cr) {
        for (Map.Entry<String, String> entry : cr.actualHeaders(new LinkedHashMap<>()).entrySet()) {
            requestBuilder.addHeader(entry.getKey(), entry.getValue());
        }

        final String contentType = cr.actualContentType();
        if (contentType != null) {
            requestBuilder.addHeader("Content-Type", contentType);
        }
    }

    private static void applyCookies(final OkHttpClient client, final ChainedHttpConfig.ChainedRequest cr) {
        final URI uri = cr.getUri().toURI();
        final List<Cookie> okCookies = cr.actualCookies(new ArrayList<>()).stream().map(cookie -> new Cookie.Builder()
            .name(cookie.getName())
            .value(cookie.getValue())
            .domain(uri.getHost())
            .path(uri.getPath())
            .expiresAt(cookie.getExpires() != null ? cookie.getExpires().toInstant().toEpochMilli() : MAX_DATE)
            .build()).collect(toList());

        client.cookieJar().saveFromResponse(HttpUrl.get(uri), okCookies);
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

    private static class OkHttpFromServer implements FromServer {

        private final URI uri;
        private final Response response;

        private OkHttpFromServer(final URI uri, final Response response) {
            this.uri = uri;
            this.response = response;
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
            final Headers headers = response.headers();
            return headers.names().stream().map((Function<String, Header<?>>) name -> keyValue(name, headers.get(name))).collect(toList());
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

        private final String contentType;
        private InputStream inputStream;

        private OkHttpToServer(final String contentType) {
            this.contentType = contentType;
        }

        @Override
        public void toServer(final InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public MediaType contentType() {
            return MediaType.parse(contentType);
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

    /**
     * Implementation of the OkHttp `CookieJar` interface providing in-memory cookie persistence only. The library default has no cookies at all, so
     * this at least addresses the issue; however, there is an issue in the HttpBuilder NG project to address cookie support in general.
     */
    private static class NonPersistingCookieJar implements CookieJar {

        private final ConcurrentMap<HttpUrl, List<Cookie>> pantry = new ConcurrentHashMap<>();

        @Override
        public void saveFromResponse(final HttpUrl url, final List<Cookie> cookies) {
            pantry.put(url, cookies);
        }

        @Override
        public List<Cookie> loadForRequest(final HttpUrl url) {
            return pantry.getOrDefault(url, emptyList());
        }
    }
}

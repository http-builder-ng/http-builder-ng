/**
 * Copyright (C) 2017 David Clark
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
import org.codehaus.groovy.runtime.MethodClosure;

import java.lang.reflect.UndeclaredThrowableException;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;

import static java.util.Collections.singletonMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.emptyMap;

/**
 * This class is the main entry point into the "HttpBuilder-NG" API. It provides access to the HTTP Client configuration and the HTTP verbs to be
 * executed.
 *
 * The `HttpBuilder` is configured using one of the three static `configure` methods:
 *
 * [source,groovy]
 * ----
 * HttpBuilder configure(Function<HttpObjectConfig, ? extends HttpBuilder> factory)
 *
 * HttpBuilder configure(@DelegatesTo(HttpObjectConfig) Closure closure)
 *
 * HttpBuilder configure(Function<HttpObjectConfig, ? extends HttpBuilder> factory, @DelegatesTo(HttpObjectConfig) Closure closure)
 * ----
 *
 * Where the `factory` parameter is a `Function` used to instantiate the underlying builder with the appropriate HTTP Client implementation. Two
 * implementations are provided by default, one based on the https://hc.apache.org/httpcomponents-client-ga/[Apache HTTPClient], and the other based
 * on the {@link java.net.HttpURLConnection} class. It is generally preferable to use the Apache implementation; however, the `HttpURLConnection` is
 * instantiated by default to minimize required external dependencies. Both are fully supported.
 *
 * The `closure` parameter is used to provide configuration for the client instance - the allowed configuration values are provided by the configuration
 * interfaces (delegated to by the closure): {@link HttpConfig} and {@link HttpObjectConfig}.
 *
 * Once you have the `HttpBuilder` configured, you can execute HTTP verb operations (e.g. GET, POST):
 *
 * [source,groovy]
 * ----
 * def http = HttpBuilder.configure {
 *     request.uri = 'http://localhost:10101/rest'
 * }
 *
 * def content = http.get {
 *     request.uri.path = '/list'
 * }
 *
 * def result = http.post {
 *     request.uri.path = '/save'
 *     request.body = infoRecord
 *     request.contentType = ContentTypes.JSON[0]
 * }
 * ----
 *
 * :linkattrs:
 *
 * See the HTTP verb method docs (below) or the https://http-builder-ng.github.io/http-builder-ng/guide/html5/[User Guide^] for more examples.
 */
public abstract class HttpBuilder implements Closeable {

    private static final Function<HttpObjectConfig, ? extends HttpBuilder> factory = JavaHttpBuilder::new;

    @SuppressWarnings("unused")
    static void noOp() {}

    static Closure NO_OP = new MethodClosure(HttpBuilder.class, "noOp");

    /**
     * Creates an `HttpBuilder` with the default configuration using the provided factory function ({@link JavaHttpBuilder} or
     * one of the other HTTP client implementation functions.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure({ c -> new ApacheHttpBuilder(c); } as Function)
     * ----
     *
     * When configuring the `HttpBuilder` with this method, the verb configurations are required to specify the `request.uri` property.
     *
     * @param factory the `HttpObjectConfig` factory function ({@link JavaHttpBuilder} or one of the other HTTP client implementation functions
     * @return the configured `HttpBuilder`
     */
    public static HttpBuilder configure(final Function<HttpObjectConfig, ? extends HttpBuilder> factory) {
        return configure(factory, NO_OP);
    }

    /**
     * Creates an `HttpBuilder` using the `JavaHttpBuilder` factory instance configured with the provided configuration closure.
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
        return configure(factory, closure);
    }

    /**
     * Creates an `HttpBuilder` configured with the provided configuration closure, using the `defaultFactory` as the client factory.
     *
     * The configuration closure delegates to the {@link HttpObjectConfig} interface, which is an extension of the {@link HttpConfig} interface -
     * configuration properties from either may be applied to the global client configuration here. See the documentation for those interfaces for
     * configuration property details.
     *
     * [source,groovy]
     * ----
     * def factory = { c -> new ApacheHttpBuilder(c); } as Function
     *
     * def http = HttpBuilder.configure(factory){
     *     request.uri = 'http://localhost:10101'
     * }
     * ----
     *
     * @param factory the {@link HttpObjectConfig} factory function ({@link JavaHttpBuilder} or {@link groovyx.net.http.ApacheHttpBuilder})
     * @param closure the configuration closure (delegated to {@link HttpObjectConfig})
     * @return the configured `HttpBuilder`
     */
    public static HttpBuilder configure(final Function<HttpObjectConfig, ? extends HttpBuilder> factory, @DelegatesTo(HttpObjectConfig.class) final Closure closure) {
        HttpObjectConfig impl = new HttpObjectConfigImpl();
        closure.setDelegate(impl);
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        closure.call();
        return factory.apply(impl);
    }

    /**
     * Creates an `HttpBuilder` using the `JavaHttpBuilder` factory instance configured with the provided configuration function.
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
        return configure(factory, configuration);
    }

    /**
     * Creates an `HttpBuilder` using the provided client factory function, configured with the provided configuration function.
     *
     * The configuration {@link Consumer} function accepts an instance of the {@link HttpObjectConfig} interface, which is an extension of the {@link HttpConfig}
     * interface - configuration properties from either may be applied to the global client configuration here. See the documentation for those interfaces for
     * configuration property details.
     *
     * This configuration method is generally meant for use with standard Java.
     *
     * [source,java]
     * ----
     * HttpBuilder.configure(JavaHttpBuilder::new, new Consumer<HttpObjectConfig>() {
     *     public void accept(HttpObjectConfig config) {
     *         config.getRequest().setUri("http://localhost:10101");
     *     }
     * });
     * ----
     *
     * Or, using lambda expressions:
     *
     * [source,java]
     * ----
     * HttpBuilder.configure(JavaHttpBuilder::new, config -> {
     *     config.getRequest().setUri("http://localhost:10101");
     * });
     * ----
     *
     * @param factory the {@link HttpObjectConfig} factory function ({@link JavaHttpBuilder} or {@link groovyx.net.http.ApacheHttpBuilder})
     * @param configuration the configuration function (accepting {@link HttpObjectConfig})
     * @return the configured `HttpBuilder`
     */
    public static HttpBuilder configure(final Function<HttpObjectConfig, ? extends HttpBuilder> factory, final Consumer<HttpObjectConfig> configuration) {
        HttpObjectConfig impl = new HttpObjectConfigImpl();
        configuration.accept(impl);
        return factory.apply(impl);
    }

    private final EnumMap<HttpVerb, BiFunction<ChainedHttpConfig, Function<ChainedHttpConfig, Object>, Object>> interceptors;
    private final CookieManager cookieManager;

    protected HttpBuilder(final HttpObjectConfig objectConfig) {
        this.interceptors = new EnumMap<>(objectConfig.getExecution().getInterceptors());
        final File folder = objectConfig.getClient().getCookieFolder();
        CookieStore cookieStore = (folder == null ?
                                   new NonBlockingCookieStore() :
                                   new FileBackedCookieStore(folder, objectConfig.getExecution().getExecutor()));
        this.cookieManager = new CookieManager(cookieStore, CookiePolicy.ACCEPT_ALL);
    }

    protected CookieManager getCookieManager() {
        return cookieManager;
    }

    protected Map<String,String> cookiesToAdd(final HttpObjectConfig.Client clientConfig, final ChainedHttpConfig.ChainedRequest cr) throws URISyntaxException {
        Map<String,String> tmp = new HashMap<>();

        try {
            final URI uri = cr.getUri().toURI();
            for(HttpCookie cookie : cr.actualCookies(new ArrayList<>())) {
                final String keyName = clientConfig.getCookieVersion() == 0 ? "Set-Cookie" : "Set-Cookie2";
                final Map<String,List<String>> toPut = singletonMap(keyName, singletonList(cookie.toString()));
                cookieManager.put(cr.getUri().forCookie(cookie), toPut);
            }

            Map<String, List<String>> found = cookieManager.get(uri, emptyMap());
            for(Map.Entry<String,List<String>> e : found.entrySet()) {
                if(e.getValue() != null && !e.getValue().isEmpty()) {
                    tmp.put(e.getKey(), String.join("; ", e.getValue()));
                }
            }
        }
        catch(IOException ioe) {
            throw new TransportingException(ioe);
        }

        return tmp;
    }

    public static List<HttpCookie> cookies(final List<FromServer.Header<?>> headers) {
        final List<HttpCookie> cookies = new ArrayList<>();
        for(FromServer.Header<?> header : headers) {
            if(header.getKey().equalsIgnoreCase("Set-Cookie") ||
               header.getKey().equalsIgnoreCase("Set-Cookie2")) {
                final List<?> found = (List<?>) header.getParsed();
                for(Object o : found) {
                    cookies.add((HttpCookie) o);
                }
            }
        }

        return Collections.unmodifiableList(cookies);

    }

    protected void addCookieStore(final URI uri, final List<FromServer.Header<?>> headers) {
        for(HttpCookie cookie : cookies(headers)) {
            cookieManager.getCookieStore().add(uri, cookie);
        }
    }

    /**
     * Executes a GET request on the configured URI. The `request.uri` property should be configured in the global client configuration in order to
     * have a target for the request.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101'
     * }
     * def result = http.get()
     * ----
     *
     * @return the resulting content
     */
    public Object get() {
        return get(NO_OP);
    }

    /**
     * Executes a GET request on the configured URI, with additional configuration provided by the configuration closure. The result will be cast to
     * the specified `type`.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101'
     * }
     * String result = http.get(String){
     *     request.uri.path = '/something'
     * }
     * ----
     *
     * The configuration `closure` allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param type the type of the response content
     * @param closure the additional configuration closure (delegated to {@link HttpConfig})
     * @return the resulting content cast to the specified type
     */
    public <T> T get(final Class<T> type, @DelegatesTo(HttpConfig.class) final Closure closure) {
        return type.cast(interceptors.get(HttpVerb.GET).apply(configureRequest(type, closure), this::doGet));
    }

    /**
     * Executes a GET request on the configured URI, with additional configuration provided by the configuration function. The result will be cast to
     * the specified `type`.
     *
     * This method is generally used for Java-specific configuration.
     *
     * [source,groovy]
     * ----
     * HttpBuilder http = HttpBuilder.configure(config -> {
     *     config.getRequest().setUri("http://localhost:10101");
     * });
     * String result = http.get(String.class, config -> {
     *     config.getRequest().getUri().setPath("/foo");
     * });
     * ----
     *
     * The `configuration` {@link Consumer} allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param type the type of the response content
     * @param configuration the additional configuration function (delegated to {@link HttpConfig})
     * @return the resulting content cast to the specified type
     */
    public <T> T get(final Class<T> type, final Consumer<HttpConfig> configuration) {
        return type.cast(interceptors.get(HttpVerb.GET).apply(configureRequest(type, configuration), this::doGet));
    }

    /**
     * Executes an asynchronous GET request on the configured URI (asynchronous alias to the `get()` method. The `request.uri` property should be
     * configured in the global client configuration in order to have a target for the request.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101'
     * }
     * CompletableFuture future = http.getAsync()
     * def result = future.get()
     * ----
     *
     * @return a {@link CompletableFuture} for retrieving the resulting content
     */
    public CompletableFuture<Object> getAsync() {
        return CompletableFuture.supplyAsync(() -> get(), getExecutor());
    }

    /**
     * Executes an asynchronous GET request on the configured URI (asynchronous alias to the `get(Closure)` method), with additional configuration
     * provided by the configuration closure.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101'
     * }
     * CompletableFuture future = http.getAsync(){
     *     request.uri.path = '/something'
     * }
     * def result = future.get()
     * ----
     *
     * The configuration `closure` allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param closure the additional configuration closure (delegated to {@link HttpConfig})
     * @return the resulting content
     */
    public CompletableFuture<Object> getAsync(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return CompletableFuture.supplyAsync(() -> get(closure), getExecutor());
    }

    /**
     * Executes an asynchronous GET request on the configured URI (asynchronous alias to `get(Consumer)`), with additional configuration provided by the
     * configuration function.
     *
     * This method is generally used for Java-specific configuration.
     *
     * [source,java]
     * ----
     * HttpBuilder http = HttpBuilder.configure(config -> {
     *     config.getRequest().setUri("http://localhost:10101");
     * });
     * CompletableFuture<Object> future = http.getAsync(config -> {
     *     config.getRequest().getUri().setPath("/foo");
     * });
     * Object result = future.get();
     * ----
     *
     * The `configuration` function allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param configuration the additional configuration closure (delegated to {@link HttpConfig})
     * @return the resulting content wrapped in a {@link CompletableFuture}
     */
    public CompletableFuture<Object> getAsync(final Consumer<HttpConfig> configuration) {
        return CompletableFuture.supplyAsync(() -> get(configuration), getExecutor());
    }

    /**
     * Executes asynchronous GET request on the configured URI (alias for the `get(Class, Closure)` method), with additional configuration provided by
     * the configuration closure. The result will be cast to the specified `type`.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101'
     * }
     * CompletableFuture future = http.getAsync(String){
     *     request.uri.path = '/something'
     * }
     * String result = future.get()
     * ----
     *
     * The configuration `closure` allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param type the type of the response content
     * @param closure the additional configuration closure (delegated to {@link HttpConfig})
     * @return the {@link CompletableFuture} for the resulting content cast to the specified type
     */
    public <T> CompletableFuture<T> getAsync(final Class<T> type, @DelegatesTo(HttpConfig.class) final Closure closure) {
        return CompletableFuture.supplyAsync(() -> get(type, closure), getExecutor());
    }

    /**
     * Executes asynchronous GET request on the configured URI (alias for the `get(Class, Consumer)` method), with additional configuration provided by
     * the configuration function. The result will be cast to the specified `type`.
     *
     * This method is generally meant for use with standard Java.
     *
     * [source,groovy]
     * ----
     * HttpBuilder http = HttpBuilder.configure(config -> {
     *     config.getRequest().setUri("http://localhost:10101");
     * });
     * String result = http.get(String.class, config -> {
     *     config.getRequest().getUri().setPath("/foo");
     * });
     * ----
     *
     * @param type the type of the response content
     * @param configuration the additional configuration function (delegated to {@link HttpConfig})
     * @return the {@link CompletableFuture} for the resulting content cast to the specified type
     */
    public <T> CompletableFuture<T> getAsync(final Class<T> type, final Consumer<HttpConfig> configuration) {
        return CompletableFuture.supplyAsync(() -> get(type, configuration), getExecutor());
    }

    /**
     * Executes a HEAD request on the configured URI. The `request.uri` property should be configured in the global client configuration in order to
     * have a target for the request.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101'
     * }
     * http.head()
     * ----
     *
     * @return the resulting content - which should always be `null` for this method
     */
    public Object head() {
        return head(NO_OP);
    }

    /**
     * Executes a HEAD request on the configured URI, with additional configuration provided by the configuration closure. The result will be cast to
     * the specified `type`. A response to a HEAD request contains no data; however, the `response.when()` methods may provide data based on response
     * headers, which will be cast to the specified type.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101/date'
     * }
     * Date result = http.head(Date){
     *      response.success { FromServer fromServer ->
     *          Date.parse('yyyy.MM.dd HH:mm', fromServer.headers.find { it.key == 'stamp' }.value)
     *      }
     * }
     * ----
     *
     * The configuration `closure` allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param type the type of the response content
     * @param closure the additional configuration closure (delegated to {@link HttpConfig})
     * @return the resulting content cast to the specified type
     */
    public <T> T head(final Class<T> type, @DelegatesTo(HttpConfig.class) final Closure closure) {
        return type.cast(interceptors.get(HttpVerb.HEAD).apply(configureRequest(type, closure), this::doHead));
    }

    /**
     * Executes a HEAD request on the configured URI, with additional configuration provided by the configuration function. The result will be cast to
     * the specified `type`.
     *
     * This method is generally used for Java-specific configuration.
     *
     * [source,groovy]
     * ----
     * HttpBuilder http = HttpBuilder.configure(config -> {
     *     config.getRequest().setUri("http://localhost:10101");
     * });
     * String result = http.head(String.class, config -> {
     *     config.getRequest().getUri().setPath("/foo");
     * });
     * ----
     *
     * The `configuration` {@link Consumer} allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param type the type of the response content
     * @param configuration the additional configuration function (delegated to {@link HttpConfig})
     * @return the resulting content cast to the specified type
     */
    public <T> T head(final Class<T> type, final Consumer<HttpConfig> configuration) {
        return type.cast(interceptors.get(HttpVerb.HEAD).apply(configureRequest(type, configuration), this::doHead));
    }

    /**
     * Executes an asynchronous HEAD request on the configured URI (asynchronous alias to the `head()` method. The `request.uri` property should be
     * configured in the global client configuration in order to have a target for the request.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101'
     * }
     * http.headAsync()
     * ----
     *
     * There is no response content, unless provided by `request.when()` method closure.
     *
     * @return a {@link CompletableFuture} for retrieving the resulting content
     */
    public CompletableFuture<Object> headAsync() {
        return CompletableFuture.supplyAsync(() -> head(), getExecutor());
    }

    /**
     * Executes an asynchronous HEAD request on the configured URI (asynchronous alias to the `head(Closure)` method), with additional configuration
     * provided by the configuration closure.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101'
     * }
     * http.headAsync(){
     *     request.uri.path = '/something'
     * }
     * ----
     *
     * The configuration `closure` allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * The response will not contain content unless the `response.when()` method closure provides it based on the response headers.
     *
     * @param closure the additional configuration closure (delegated to {@link HttpConfig})
     * @return the resulting content
     */
    public CompletableFuture<Object> headAsync(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return CompletableFuture.supplyAsync(() -> head(closure), getExecutor());
    }

    /**
     * Executes an asynchronous HEAD request on the configured URI (asynchronous alias to `head(Consumer)`), with additional configuration provided by the
     * configuration function.
     *
     * This method is generally used for Java-specific configuration.
     *
     * [source,java]
     * ----
     * HttpBuilder http = HttpBuilder.configure(config -> {
     *     config.getRequest().setUri("http://localhost:10101");
     * });
     * CompletableFuture<Object> future = http.headAsync(config -> {
     *     config.getRequest().getUri().setPath("/foo");
     * });
     * Object result = future.get();
     * ----
     *
     * The `configuration` function allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param configuration the additional configuration closure (delegated to {@link HttpConfig})
     * @return the resulting content wrapped in a {@link CompletableFuture}
     */
    public CompletableFuture<Object> headAsync(final Consumer<HttpConfig> configuration) {
        return CompletableFuture.supplyAsync(() -> head(configuration), getExecutor());
    }

    /**
     * Executes an asynchronous HEAD request on the configured URI (asynchronous alias to the `head(Class,Closure)` method), with additional
     * configuration provided by the configuration closure. The result will be cast to the specified `type`. A response to a HEAD request contains no
     * data; however, the `response.when()` methods may provide data based on response headers, which will be cast to the specified type.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101/date'
     * }
     * CompletableFuture future = http.headAsync(Date){
     *      response.success { FromServer fromServer ->
     *          Date.parse('yyyy.MM.dd HH:mm', fromServer.headers.find { it.key == 'stamp' }.value)
     *      }
     * }
     * Date result = future.get()
     * ----
     *
     * The configuration `closure` allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param type the type of the response content
     * @param closure the additional configuration closure (delegated to {@link HttpConfig})
     * @return a {@link CompletableFuture} which may be used to access the resulting content (if present)
     */
    public <T> CompletableFuture<T> headAsync(final Class<T> type, @DelegatesTo(HttpConfig.class) final Closure closure) {
        return CompletableFuture.supplyAsync(() -> head(type, closure), getExecutor());
    }

    /**
     * Executes an asynchronous HEAD request on the configured URI (asynchronous alias to `head(Class,Consumer)`), with additional configuration
     * provided by the configuration function. The result will be cast to the specified `type`.
     *
     * This method is generally used for Java-specific configuration.
     *
     * [source,groovy]
     * ----
     * HttpBuilder http = HttpBuilder.configure(config -> {
     *     config.getRequest().setUri("http://localhost:10101");
     * });
     * String result = http.headAsync(String.class, config -> {
     *     config.getRequest().getUri().setPath("/foo");
     * });
     * ----
     *
     * The `configuration` {@link Consumer} allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param type the type of the response content
     * @param configuration the additional configuration function (delegated to {@link HttpConfig})
     * @return the resulting content cast to the specified type wrapped in a {@link CompletableFuture}
     */
    public <T> CompletableFuture<T> headAsync(final Class<T> type, final Consumer<HttpConfig> configuration) {
        return CompletableFuture.supplyAsync(() -> head(type, configuration), getExecutor());
    }

    /**
     * Executes a POST request on the configured URI. The `request.uri` property should be configured in the global client configuration in order to
     * have a target for the request.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101'
     * }
     * def result = http.post()
     * ----
     *
     * @return the resulting content
     */
    public Object post() {
        return post(NO_OP);
    }

    /**
     * Executes an POST request on the configured URI, with additional configuration provided by the configuration closure. The result will be cast
     * to the specified `type`.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101'
     * }
     * Date date = http.post(Date){
     *     request.uri.path = '/date'
     *     request.body = '{ "timezone": "America/Chicago" }'
     *     request.contentType = 'application/json'
     *     response.parser('text/date') { ChainedHttpConfig config, FromServer fromServer ->
     *         Date.parse('yyyy.MM.dd HH:mm', fromServer.inputStream.text)
     *     }
     * }
     * ----
     *
     * The configuration `closure` allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param closure the additional configuration closure (delegated to {@link HttpConfig})
     * @return the result of the request cast to the specified type
     */
    public <T> T post(final Class<T> type, @DelegatesTo(HttpConfig.class) final Closure closure) {
        return type.cast(interceptors.get(HttpVerb.POST).apply(configureRequest(type, closure), this::doPost));
    }

    /**
     * Executes a POST request on the configured URI, with additional configuration provided by the configuration function. The result will be cast to
     * the specified `type`.
     *
     * This method is generally used for Java-specific configuration.
     *
     * [source,groovy]
     * ----
     * HttpBuilder http = HttpBuilder.configure(config -> {
     *     config.getRequest().setUri("http://localhost:10101");
     * });
     * String result = http.post(String.class, config -> {
     *     config.getRequest().getUri().setPath("/foo");
     * });
     * ----
     *
     * The `configuration` {@link Consumer} allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param type the type of the response content
     * @param configuration the additional configuration function (delegated to {@link HttpConfig})
     * @return the resulting content cast to the specified type
     */
    public <T> T post(final Class<T> type, final Consumer<HttpConfig> configuration) {
        return type.cast(interceptors.get(HttpVerb.POST).apply(configureRequest(type, configuration), this::doPost));
    }

    /**
     * Executes an asynchronous POST request on the configured URI (asynchronous alias to the `post()` method). The `request.uri` property should be
     * configured in the global client configuration in order to have a target for the request.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101'
     * }
     * CompletableFuture future = http.postAsync()
     * def result = future.get()
     * ----
     *
     * @return the {@link CompletableFuture} containing the future access to the response
     */
    public CompletableFuture<Object> postAsync() {
        return CompletableFuture.supplyAsync(() -> post(NO_OP), getExecutor());
    }

    /**
     * Executes an asynchronous POST request on the configured URI (an asynchronous alias to the `post(Closure)` method), with additional configuration
     * provided by the configuration closure.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101'
     * }
     * def result = http.postAsync(){
     *     request.uri.path = '/something'
     *     request.body = 'My content'
     *     request.contentType = 'text/plain'
     * }
     * ----
     *
     * The configuration `closure` allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param closure the additional configuration closure (delegated to {@link HttpConfig})
     * @return the {@link CompletableFuture} containing the future result data
     */
    public CompletableFuture<Object> postAsync(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return CompletableFuture.supplyAsync(() -> post(closure), getExecutor());
    }

    /**
     * Executes an asynchronous POST request on the configured URI (asynchronous alias to `post(Consumer)`), with additional configuration provided by the
     * configuration function.
     *
     * This method is generally used for Java-specific configuration.
     *
     * [source,java]
     * ----
     * HttpBuilder http = HttpBuilder.configure(config -> {
     *     config.getRequest().setUri("http://localhost:10101");
     * });
     * CompletableFuture<Object> future = http.postAsync(config -> {
     *     config.getRequest().getUri().setPath("/foo");
     * });
     * Object result = future.get();
     * ----
     *
     * The `configuration` function allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param configuration the additional configuration closure (delegated to {@link HttpConfig})
     * @return the resulting content wrapped in a {@link CompletableFuture}
     */
    public CompletableFuture<Object> postAsync(final Consumer<HttpConfig> configuration) {
        return CompletableFuture.supplyAsync(() -> post(configuration), getExecutor());
    }

    /**
     * Executes an asynchronous POST request on the configured URI (asynchronous alias to the `post(Class,Closure)` method), with additional
     * configuration provided by the configuration closure. The result will be cast to the specified `type`.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101'
     * }
     * CompletedFuture<Date> future = http.postAsync(Date){
     *     request.uri.path = '/date'
     *     request.body = '{ "timezone": "America/Chicago" }'
     *     request.contentType = 'application/json'
     *     response.parser('text/date') { ChainedHttpConfig config, FromServer fromServer ->
     *         Date.parse('yyyy.MM.dd HH:mm', fromServer.inputStream.text)
     *     }
     * }
     * Date date = future.get()
     * ----
     *
     * The configuration `closure` allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param closure the additional configuration closure (delegated to {@link HttpConfig})
     * @return the {@link CompletableFuture} containing the result of the request
     */
    public <T> CompletableFuture<T> postAsync(final Class<T> type, @DelegatesTo(HttpConfig.class) final Closure closure) {
        return CompletableFuture.supplyAsync(() -> post(type, closure), getExecutor());
    }

    /**
     * Executes an asynchronous POST request on the configured URI (asynchronous alias to `put(Class,Consumer)`), with additional configuration provided
     * by the configuration function. The result will be cast to the specified `type`.
     *
     * This method is generally used for Java-specific configuration.
     *
     * [source,groovy]
     * ----
     * HttpBuilder http = HttpBuilder.configure(config -> {
     *     config.getRequest().setUri("http://localhost:10101");
     * });
     * CompletableFuture<String> future = http.postAsync(String.class, config -> {
     *     config.getRequest().getUri().setPath("/foo");
     * });
     * String result = future.get();
     * ----
     *
     * The `configuration` {@link Consumer} allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param type the type of the response content
     * @param configuration the additional configuration function (delegated to {@link HttpConfig})
     * @return the resulting content cast to the specified type, wrapped in a {@link CompletableFuture}
     */
    public <T> CompletableFuture<T> postAsync(final Class<T> type, final Consumer<HttpConfig> configuration) {
        return CompletableFuture.supplyAsync(() -> post(type, configuration), getExecutor());
    }

    /**
     * Executes a PUT request on the configured URI. The `request.uri` property should be configured in the global client configuration in order to
     * have a target for the request.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101'
     * }
     * def result = http.put()
     * ----
     *
     * @return the resulting content
     */
    public Object put() {
        return put(NO_OP);
    }

    /**
     * Executes an PUT request on the configured URI, with additional configuration provided by the configuration closure. The result will be cast
     * to the specified `type`.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101'
     * }
     * Date date = http.put(Date){
     *     request.uri.path = '/date'
     *     request.body = '{ "timezone": "America/Chicago" }'
     *     request.contentType = 'application/json'
     *     response.parser('text/date') { ChainedHttpConfig config, FromServer fromServer ->
     *         Date.parse('yyyy.MM.dd HH:mm', fromServer.inputStream.text)
     *     }
     * }
     * ----
     *
     * The configuration `closure` allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param closure the additional configuration closure (delegated to {@link HttpConfig})
     * @return the result of the request cast to the specified type
     */
    public <T> T put(final Class<T> type, @DelegatesTo(HttpConfig.class) final Closure closure) {
        return type.cast(interceptors.get(HttpVerb.PUT).apply(configureRequest(type, closure), this::doPut));
    }

    /**
     * Executes a PUT request on the configured URI, with additional configuration provided by the configuration function. The result will be cast to
     * the specified `type`.
     *
     * This method is generally used for Java-specific configuration.
     *
     * [source,groovy]
     * ----
     * HttpBuilder http = HttpBuilder.configure(config -> {
     *     config.getRequest().setUri("http://localhost:10101");
     * });
     * String result = http.get(String.class, config -> {
     *     config.getRequest().getUri().setPath("/foo");
     * });
     * ----
     *
     * The `configuration` {@link Consumer} allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param type the type of the response content
     * @param configuration the additional configuration function (delegated to {@link HttpConfig})
     * @return the resulting content cast to the specified type
     */
    public <T> T put(final Class<T> type, final Consumer<HttpConfig> configuration) {
        return type.cast(interceptors.get(HttpVerb.PUT).apply(configureRequest(type, configuration), this::doPut));
    }

    /**
     * Executes an asynchronous PUT request on the configured URI (asynchronous alias to the `put()` method). The `request.uri` property should be
     * configured in the global client configuration in order to have a target for the request.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101'
     * }
     * CompletableFuture future = http.putAsync()
     * def result = future.get()
     * ----
     *
     * @return the {@link CompletableFuture} containing the future access to the response
     */
    public CompletableFuture<Object> putAsync() {
        return CompletableFuture.supplyAsync(() -> put(NO_OP), getExecutor());
    }

    /**
     * Executes an asynchronous PUT request on the configured URI (an asynchronous alias to the `put(Closure)` method), with additional configuration
     * provided by the configuration closure.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101'
     * }
     * def result = http.putAsync(){
     *     request.uri.path = '/something'
     *     request.body = 'My content'
     *     request.contentType = 'text/plain'
     * }
     * ----
     *
     * The configuration `closure` allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param closure the additional configuration closure (delegated to {@link HttpConfig})
     * @return the {@link CompletableFuture} containing the future result data
     */
    public CompletableFuture<Object> putAsync(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return CompletableFuture.supplyAsync(() -> put(closure), getExecutor());
    }

    /**
     * Executes an asynchronous PUT request on the configured URI (asynchronous alias to `put(Consumer)`), with additional configuration provided by the
     * configuration function.
     *
     * This method is generally used for Java-specific configuration.
     *
     * [source,java]
     * ----
     * HttpBuilder http = HttpBuilder.configure(config -> {
     *     config.getRequest().setUri("http://localhost:10101");
     * });
     * CompletableFuture<Object> future = http.putAsync(config -> {
     *     config.getRequest().getUri().setPath("/foo");
     * });
     * Object result = future.get();
     * ----
     *
     * The `configuration` function allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param configuration the additional configuration closure (delegated to {@link HttpConfig})
     * @return the resulting content wrapped in a {@link CompletableFuture}
     */
    public CompletableFuture<Object> putAsync(final Consumer<HttpConfig> configuration) {
        return CompletableFuture.supplyAsync(() -> put(configuration), getExecutor());
    }

    /**
     * Executes an asynchronous PUT request on the configured URI (asynchronous alias to the `put(Class,Closure)` method), with additional
     * configuration provided by the configuration closure. The result will be cast to the specified `type`.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101'
     * }
     * CompletedFuture<Date> future = http.putAsync(Date){
     *     request.uri.path = '/date'
     *     request.body = '{ "timezone": "America/Chicago" }'
     *     request.contentType = 'application/json'
     *     response.parser('text/date') { ChainedHttpConfig config, FromServer fromServer ->
     *         Date.parse('yyyy.MM.dd HH:mm', fromServer.inputStream.text)
     *     }
     * }
     * Date date = future.get()
     * ----
     *
     * The configuration `closure` allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param closure the additional configuration closure (delegated to {@link HttpConfig})
     * @return the {@link CompletableFuture} containing the result of the request
     */
    public <T> CompletableFuture<T> putAsync(final Class<T> type, @DelegatesTo(HttpConfig.class) final Closure closure) {
        return CompletableFuture.supplyAsync(() -> put(type, closure), getExecutor());
    }

    /**
     * Executes an asynchronous PUT request on the configured URI (asynchronous alias to `put(Class,Consumer)`), with additional configuration provided
     * by the configuration function. The result will be cast to the specified `type`.
     *
     * This method is generally used for Java-specific configuration.
     *
     * [source,groovy]
     * ----
     * HttpBuilder http = HttpBuilder.configure(config -> {
     *     config.getRequest().setUri("http://localhost:10101");
     * });
     * CompletableFuture<String> future = http.putAsync(String.class, config -> {
     *     config.getRequest().getUri().setPath("/foo");
     * });
     * String result = future.get();
     * ----
     *
     * The `configuration` {@link Consumer} allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param type the type of the response content
     * @param configuration the additional configuration function (delegated to {@link HttpConfig})
     * @return the resulting content cast to the specified type, wrapped in a {@link CompletableFuture}
     */
    public <T> CompletableFuture<T> putAsync(final Class<T> type, final Consumer<HttpConfig> configuration) {
        return CompletableFuture.supplyAsync(() -> put(type, configuration), getExecutor());
    }

    /**
     * Executes a DELETE request on the configured URI. The `request.uri` property should be configured in the global client configuration in order to
     * have a target for the request.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101'
     * }
     * def result = http.delete()
     * ----
     *
     * @return the resulting content
     */
    public Object delete() {
        return delete(NO_OP);
    }

    /**
     * Executes an DELETE request on the configured URI, with additional configuration provided by the configuration closure. The result will be cast
     * to the specified `type`.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101'
     * }
     * Date date = http.delete(Date){
     *     request.uri.path = '/date'
     *     response.parser('text/date') { ChainedHttpConfig config, FromServer fromServer ->
     *         Date.parse('yyyy.MM.dd HH:mm', fromServer.inputStream.text)
     *     }
     * }
     * ----
     *
     * The configuration `closure` allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param closure the additional configuration closure (delegated to {@link HttpConfig})
     * @return the result of the request cast to the specified type
     */
    public <T> T delete(final Class<T> type, @DelegatesTo(HttpConfig.class) final Closure closure) {
        return type.cast(interceptors.get(HttpVerb.DELETE).apply(configureRequest(type, closure), this::doDelete));
    }

    /**
     * Executes a DELETE request on the configured URI, with additional configuration provided by the configuration function. The result will be cast to
     * the specified `type`.
     *
     * This method is generally used for Java-specific configuration.
     *
     * [source,groovy]
     * ----
     * HttpBuilder http = HttpBuilder.configure(config -> {
     *     config.getRequest().setUri("http://localhost:10101");
     * });
     * String result = http.delete(String.class, config -> {
     *     config.getRequest().getUri().setPath("/foo");
     * });
     * ----
     *
     * The `configuration` {@link Consumer} allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param type the type of the response content
     * @param configuration the additional configuration function (delegated to {@link HttpConfig})
     * @return the resulting content cast to the specified type
     */
    public <T> T delete(final Class<T> type, final Consumer<HttpConfig> configuration) {
        return type.cast(interceptors.get(HttpVerb.DELETE).apply(configureRequest(type, configuration), this::doDelete));
    }

    /**
     * Executes an asynchronous DELETE request on the configured URI (asynchronous alias to the `delete()` method). The `request.uri` property should be
     * configured in the global client configuration in order to have a target for the request.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101'
     * }
     * CompletableFuture future = http.deleteAsync()
     * def result = future.get()
     * ----
     *
     * @return the {@link CompletableFuture} containing the future access to the response
     */
    public CompletableFuture<Object> deleteAsync() {
        return CompletableFuture.supplyAsync(() -> delete(NO_OP), getExecutor());
    }

    /**
     * Executes an asynchronous DELETE request on the configured URI (an asynchronous alias to the `delete(Closure)` method), with additional configuration
     * provided by the configuration closure.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101'
     * }
     * def result = http.deleteAsync(){
     *     request.uri.path = '/something'
     * }
     * ----
     *
     * The configuration `closure` allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param closure the additional configuration closure (delegated to {@link HttpConfig})
     * @return the {@link CompletableFuture} containing the future result data
     */
    public CompletableFuture<Object> deleteAsync(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return CompletableFuture.supplyAsync(() -> delete(closure), getExecutor());
    }

    /**
     * Executes an asynchronous DELETE request on the configured URI (asynchronous alias to the `delete(Consumer)` method), with additional
     * configuration provided by the configuration function.
     *
     * This method is generally used for Java-specific configuration.
     *
     * [source,java]
     * ----
     * HttpBuilder http = HttpBuilder.configure(config -> {
     *     config.getRequest().setUri("http://localhost:10101");
     * });
     * CompletableFuture<Object> future = http.deleteAsync(config -> {
     *     config.getRequest().getUri().setPath("/foo");
     * });
     * Object result = future.get();
     * ----
     *
     * The configuration `closure` allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param configuration the additional configuration function (delegated to {@link HttpConfig})
     * @return the {@link CompletableFuture} containing the result of the request
     */
    public CompletableFuture<Object> deleteAsync(final Consumer<HttpConfig> configuration) {
        return CompletableFuture.supplyAsync(() -> delete(configuration), getExecutor());
    }

    /**
     * Executes an asynchronous DELETE request on the configured URI (asynchronous alias to the `delete(Class,Closure)` method), with additional
     * configuration provided by the configuration closure. The result will be cast to the specified `type`.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101'
     * }
     * CompletedFuture<Date> future = http.deleteAsync(Date){
     *     request.uri.path = '/date'
     *     response.parser('text/date') { ChainedHttpConfig config, FromServer fromServer ->
     *         Date.parse('yyyy.MM.dd HH:mm', fromServer.inputStream.text)
     *     }
     * }
     * Date date = future.get()
     * ----
     *
     * The configuration `closure` allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param type the type of the resulting object
     * @param closure the additional configuration closure (delegated to {@link HttpConfig})
     * @return the {@link CompletableFuture} containing the result of the request
     */
    public <T> CompletableFuture<T> deleteAsync(final Class<T> type, @DelegatesTo(HttpConfig.class) final Closure closure) {
        return CompletableFuture.supplyAsync(() -> delete(type, closure), getExecutor());
    }

    /**
     * Executes an asynchronous DELETE request on the configured URI (asynchronous alias to the `delete(Class,Consumer)` method), with additional
     * configuration provided by the configuration function. The result will be cast to the specified `type`.
     *
     * This method is generally used for Java-specific configuration.
     *
     * [source,java]
     * ----
     * HttpBuilder http = HttpBuilder.configure(config -> {
     *     config.getRequest().setUri("http://localhost:10101");
     * });
     * CompletableFuture<String> future = http.deleteAsync(String.class, config -> {
     *     config.getRequest().getUri().setPath("/foo");
     * });
     * String result = future.get();
     * ----
     *
     * The configuration `closure` allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param type the type of the resulting object
     * @param configuration the additional configuration function (delegated to {@link HttpConfig})
     * @return the {@link CompletableFuture} containing the result of the request
     */
    public <T> CompletableFuture<T> deleteAsync(final Class<T> type, final Consumer<HttpConfig> configuration) {
        return CompletableFuture.supplyAsync(() -> delete(type, configuration), getExecutor());
    }

    /**
     * Executes a GET request on the configured URI, with additional configuration provided by the configuration closure.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101'
     * }
     * def result = http.get(){
     *     request.uri.path = '/something'
     * }
     * ----
     *
     * The configuration `closure` allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param closure the additional configuration closure (delegated to {@link HttpConfig})
     * @return the resulting content
     */
    public Object get(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return get(Object.class, closure);
    }

    /**
     * Executes a GET request on the configured URI, with additional configuration provided by the configuration function.
     *
     * This method is generally used for Java-specific configuration.
     *
     * [source,java]
     * ----
     * HttpBuilder http = HttpBuilder.configure(config -> {
     *     config.getRequest().setUri("http://localhost:10101");
     * });
     * http.delete(config -> {
     *     config.getRequest().getUri().setPath("/foo");
     * });
     * ----
     *
     * The `configuration` function allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param configuration the additional configuration closure (delegated to {@link HttpConfig})
     * @return the resulting content
     */
    public Object get(final Consumer<HttpConfig> configuration) {
        return get(Object.class, configuration);
    }

    /**
     * Executes a HEAD request on the configured URI, with additional configuration provided by the configuration closure.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101'
     * }
     * http.head(){
     *     request.uri.path = '/something'
     * }
     * ----
     *
     * The configuration `closure` allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param closure the additional configuration closure (delegated to {@link HttpConfig})
     * @return the resulting content - which will be `null` unless provided by a `request.when()` closure
     */
    public Object head(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return head(Object.class, closure);
    }

    /**
     * Executes a HEAD request on the configured URI, with additional configuration provided by the configuration function.
     *
     * This method is generally used for Java-specific configuration.
     *
     * [source,java]
     * ----
     * HttpBuilder http = HttpBuilder.configure(config -> {
     *     config.getRequest().setUri("http://localhost:10101");
     * });
     * http.delete(config -> {
     *     config.getRequest().getUri().setPath("/foo");
     * });
     * ----
     *
     * The `configuration` function allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param configuration the additional configuration closure (delegated to {@link HttpConfig})
     * @return the resulting content
     */
    public Object head(final Consumer<HttpConfig> configuration) {
        return head(Object.class, configuration);
    }

    /**
     * Executes a POST request on the configured URI, with additional configuration provided by the configuration closure.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101'
     * }
     * def result = http.post(){
     *     request.uri.path = '/something'
     *     request.body = 'My content'
     *     request.contentType = 'text/plain'
     * }
     * ----
     *
     * The configuration `closure` allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param closure the additional configuration closure (delegated to {@link HttpConfig})
     * @return the resulting content
     */
    public Object post(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return post(Object.class, closure);
    }

    /**
     * Executes a POST request on the configured URI, with additional configuration provided by the configuration function.
     *
     * This method is generally used for Java-specific configuration.
     *
     * [source,java]
     * ----
     * HttpBuilder http = HttpBuilder.configure(config -> {
     *     config.getRequest().setUri("http://localhost:10101");
     * });
     * http.post(config -> {
     *     config.getRequest().getUri().setPath("/foo");
     * });
     * ----
     *
     * The `configuration` function allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param configuration the additional configuration closure (delegated to {@link HttpConfig})
     * @return the resulting content
     */
    public Object post(final Consumer<HttpConfig> configuration) {
        return post(Object.class, configuration);
    }

    /**
     * Executes a PUT request on the configured URI, with additional configuration provided by the configuration closure.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101'
     * }
     * def result = http.put(){
     *     request.uri.path = '/something'
     *     request.body = 'My content'
     *     request.contentType = 'text/plain'
     * }
     * ----
     *
     * The configuration `closure` allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param closure the additional configuration closure (delegated to {@link HttpConfig})
     * @return the resulting content
     */
    public Object put(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return put(Object.class, closure);
    }

    /**
     * Executes a PUT request on the configured URI, with additional configuration provided by the configuration function.
     *
     * This method is generally used for Java-specific configuration.
     *
     * [source,java]
     * ----
     * HttpBuilder http = HttpBuilder.configure(config -> {
     *     config.getRequest().setUri("http://localhost:10101");
     * });
     * http.put(config -> {
     *     config.getRequest().getUri().setPath("/foo");
     * });
     * ----
     *
     * The `configuration` function allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param configuration the additional configuration closure (delegated to {@link HttpConfig})
     * @return the resulting content
     */
    public Object put(final Consumer<HttpConfig> configuration) {
        return put(Object.class, configuration);
    }

    /**
     * Executes a DELETE request on the configured URI, with additional configuration provided by the configuration closure.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101'
     * }
     * def result = http.delete(){
     *     request.uri.path = '/something'
     * }
     * ----
     *
     * The configuration `closure` allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param closure the additional configuration closure (delegated to {@link HttpConfig})
     * @return the resulting content
     */
    public Object delete(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return delete(Object.class, closure);
    }

    /**
     * Executes a DELETE request on the configured URI, with additional configuration provided by the configuration function.
     *
     * This method is generally used for Java-specific configuration.
     *
     * [source,java]
     * ----
     * HttpBuilder http = HttpBuilder.configure(config -> {
     *     config.getRequest().setUri("http://localhost:10101");
     * });
     * http.delete(config -> {
     *     config.getRequest().getUri().setPath("/foo");
     * });
     * ----
     *
     * The `configuration` function allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param configuration the additional configuration closure (delegated to {@link HttpConfig})
     * @return the resulting content
     */
    public Object delete(final Consumer<HttpConfig> configuration) {
        return delete(Object.class, configuration);
    }

    /**
     * Executes a PATCH request on the configured URI. The `request.uri` property should be configured in the global client configuration in order to
     * have a target for the request.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101'
     * }
     * def result = http.patch()
     * ----
     *
     * @return the resulting content
     */
    public Object patch() {
        return patch(NO_OP);
    }

    /**
     * Executes a PATCH request on the configured URI, with additional configuration provided by the configuration closure.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101'
     * }
     * def result = http.patch(){
     *     request.uri.path = '/something'
     * }
     * ----
     *
     * The configuration `closure` allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param closure the additional configuration closure (delegated to {@link HttpConfig})
     * @return the resulting content
     */
    public Object patch(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return patch(Object.class, closure);
    }

    /**
     * Executes a PATCH request on the configured URI, with additional configuration provided by the configuration function.
     *
     * This method is generally used for Java-specific configuration.
     *
     * [source,java]
     * ----
     * HttpBuilder http = HttpBuilder.configure(config -> {
     *     config.getRequest().setUri("http://localhost:10101");
     * });
     * http.patch(config -> {
     *     config.getRequest().getUri().setPath("/foo");
     * });
     * ----
     *
     * The `configuration` function allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param configuration the additional configuration closure (delegated to {@link HttpConfig})
     * @return the resulting content
     */
    public Object patch(final Consumer<HttpConfig> configuration) {
        return patch(Object.class, configuration);
    }


    /**
     * Executes a PATCH request on the configured URI, with additional configuration provided by the configuration closure. The result will be cast to
     * the specified `type`.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101'
     * }
     * String result = http.patch(String){
     *     request.uri.path = '/something'
     * }
     * ----
     *
     * The configuration `closure` allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param type the type of the response content
     * @param closure the additional configuration closure (delegated to {@link HttpConfig})
     * @return the resulting content cast to the specified type
     */
    public <T> T patch(final Class<T> type, @DelegatesTo(HttpConfig.class) final Closure closure) {
        return type.cast(interceptors.get(HttpVerb.PATCH).apply(configureRequest(type, closure), this::doPatch));
    }

    /**
     * Executes a PATCH request on the configured URI, with additional configuration provided by the configuration function. The result will be cast to
     * the specified `type`.
     *
     * This method is generally used for Java-specific configuration.
     *
     * [source,groovy]
     * ----
     * HttpBuilder http = HttpBuilder.configure(config -> {
     *     config.getRequest().setUri("http://localhost:10101");
     * });
     * String result = http.patch(String.class, config -> {
     *     config.getRequest().getUri().setPath("/foo");
     * });
     * ----
     *
     * The `configuration` {@link Consumer} allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param type the type of the response content
     * @param configuration the additional configuration function (delegated to {@link HttpConfig})
     * @return the resulting content cast to the specified type
     */
    public <T> T patch(final Class<T> type, final Consumer<HttpConfig> configuration) {
        return type.cast(interceptors.get(HttpVerb.PATCH).apply(configureRequest(type, configuration), this::doPatch));
    }

    /**
     * Executes an asynchronous PATCH request on the configured URI (asynchronous alias to the `patch()` method. The `request.uri` property should be
     * configured in the global client configuration in order to have a target for the request.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101'
     * }
     * CompletableFuture future = http.patchAsync()
     * def result = future.get()
     * ----
     *
     * @return a {@link CompletableFuture} for retrieving the resulting content
     */
    public CompletableFuture<Object> patchAsync() {
        return CompletableFuture.supplyAsync(() -> patch(), getExecutor());
    }

    /**
     * Executes an asynchronous GET request on the configured URI (asynchronous alias to the `get(Closure)` method), with additional configuration
     * provided by the configuration closure.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101'
     * }
     * CompletableFuture future = http.getAsync(){
     *     request.uri.path = '/something'
     * }
     * def result = future.get()
     * ----
     *
     * The configuration `closure` allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param closure the additional configuration closure (delegated to {@link HttpConfig})
     * @return the resulting content
     */
    public CompletableFuture<Object> patchAsync(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return CompletableFuture.supplyAsync(() -> patch(closure), getExecutor());
    }

    /**
     * Executes an asynchronous PATCH request on the configured URI (asynchronous alias to `patch(Consumer)`), with additional configuration provided by the
     * configuration function.
     *
     * This method is generally used for Java-specific configuration.
     *
     * [source,java]
     * ----
     * HttpBuilder http = HttpBuilder.configure(config -> {
     *     config.getRequest().setUri("http://localhost:10101");
     * });
     * CompletableFuture<Object> future = http.patchAsync(config -> {
     *     config.getRequest().getUri().setPath("/foo");
     * });
     * Object result = future.get();
     * ----
     *
     * The `configuration` function allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param configuration the additional configuration closure (delegated to {@link HttpConfig})
     * @return the resulting content wrapped in a {@link CompletableFuture}
     */
    public CompletableFuture<Object> patchAsync(final Consumer<HttpConfig> configuration) {
        return CompletableFuture.supplyAsync(() -> patch(configuration), getExecutor());
    }

    /**
     * Executes asynchronous GET request on the configured URI (alias for the `patch(Class, Closure)` method), with additional configuration provided by
     * the configuration closure. The result will be cast to the specified `type`.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101'
     * }
     * CompletableFuture future = http.patchAsync(String){
     *     request.uri.path = '/something'
     * }
     * String result = future.get()
     * ----
     *
     * The configuration `closure` allows additional configuration for this request based on the {@link HttpConfig} interface.
     *
     * @param type the type of the response content
     * @param closure the additional configuration closure (delegated to {@link HttpConfig})
     * @return the {@link CompletableFuture} for the resulting content cast to the specified type
     */
    public <T> CompletableFuture<T> patchAsync(final Class<T> type, @DelegatesTo(HttpConfig.class) final Closure closure) {
        return CompletableFuture.supplyAsync(() -> patch(type, closure), getExecutor());
    }

    /**
     * Executes asynchronous PATCH request on the configured URI (alias for the `patch(Class, Consumer)` method), with additional configuration provided by
     * the configuration function. The result will be cast to the specified `type`.
     *
     * This method is generally meant for use with standard Java.
     *
     * [source,groovy]
     * ----
     * HttpBuilder http = HttpBuilder.configure(config -> {
     *     config.getRequest().setUri("http://localhost:10101");
     * });
     * String result = http.patch(String.class, config -> {
     *     config.getRequest().getUri().setPath("/foo");
     * });
     * ----
     *
     * @param type the type of the response content
     * @param configuration the additional configuration function (delegated to {@link HttpConfig})
     * @return the {@link CompletableFuture} for the resulting content cast to the specified type
     */
    public <T> CompletableFuture<T> patchAsync(final Class<T> type, final Consumer<HttpConfig> configuration) {
        return CompletableFuture.supplyAsync(() -> patch(type, configuration), getExecutor());
    }

    protected abstract Object doGet(final ChainedHttpConfig config);

    protected abstract Object doHead(final ChainedHttpConfig config);

    protected abstract Object doPost(final ChainedHttpConfig config);

    protected abstract Object doPut(final ChainedHttpConfig config);

    protected abstract Object doDelete(final ChainedHttpConfig config);

    // ksuderman
    protected abstract Object doPatch(final  ChainedHttpConfig config);

    protected abstract ChainedHttpConfig getObjectConfig();

    public abstract Executor getExecutor();

    private ChainedHttpConfig configureRequest(final Class<?> type, final Closure closure) {
        final HttpConfigs.BasicHttpConfig myConfig = HttpConfigs.requestLevel(getObjectConfig());
        closure.setDelegate(myConfig);
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        closure.call();
        myConfig.getChainedResponse().setType(type);
        return myConfig;
    }

    private ChainedHttpConfig configureRequest(final Class<?> type, final Consumer<HttpConfig> configuration) {
        final HttpConfigs.BasicHttpConfig myConfig = HttpConfigs.requestLevel(getObjectConfig());
        configuration.accept(myConfig);
        myConfig.getChainedResponse().setType(type);
        return myConfig;
    }

    public static Throwable findCause(final Exception e) {
        if(e instanceof TransportingException) {
            return e.getCause();
        }
        else if(e instanceof UndeclaredThrowableException) {
            final UndeclaredThrowableException ute = (UndeclaredThrowableException) e;
            if(ute.getCause() != null) {
                return ute.getCause();
            }
            else {
                return e;
            }
        }
        else {
            return e;
        }
    }
    
    protected Object handleException(final ChainedHttpConfig.ChainedResponse cr, final Exception e) {
        return cr.actualException().apply(findCause(e));
    }

    protected static class ResponseHandlerFunction implements BiFunction<ChainedHttpConfig, FromServer, Object> {

        static final ResponseHandlerFunction HANDLER_FUNCTION = new ResponseHandlerFunction();

        @Override
        public Object apply(ChainedHttpConfig requestConfig, FromServer fromServer) {
            try {
                final BiFunction<ChainedHttpConfig, FromServer, Object> parser = requestConfig.findParser(fromServer.getContentType());
                final BiFunction<FromServer, Object, ?> action = requestConfig.getChainedResponse().actualAction(fromServer.getStatusCode());

                return action.apply(fromServer, fromServer.getHasBody() ? parser.apply(requestConfig, fromServer) : null);

            } finally {
                fromServer.finish();
            }
        }
    }
}

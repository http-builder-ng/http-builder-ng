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
import org.codehaus.groovy.runtime.MethodClosure;

import java.io.Closeable;
import java.util.EnumMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * This class is the main entry point into the "Http Builder NG" API. It provides access to the HTTP Client configuration and the HTTP verbs to be
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
 * See the HTTP verb method docs (below) or the https://dwclark.github.io/http-builder-ng/guide/html5/[User Guide^] for more examples.
 */
public abstract class HttpBuilder implements Closeable {

    private static volatile Function<HttpObjectConfig, ? extends HttpBuilder> factory = JavaHttpBuilder::new;

    /**
     * Used to retrieve the default client factory.
     *
     * @return the default client factory function
     */
    public static Function<HttpObjectConfig, ? extends HttpBuilder> getDefaultFactory() {
        return factory;
    }

    /**
     * Used to specify the default client factory.
     *
     * @param val the default client factory as a ({@link Function}
     */
    public static void setDefaultFactory(final Function<HttpObjectConfig, ? extends HttpBuilder> val) {
        factory = val;
    }

    static void noOp() { }
    static Closure NO_OP = new MethodClosure(HttpBuilder.class, "noOp");

    /**
     * Creates an `HttpBuilder` with the default configuration using the provided factory function ({@link JavaHttpBuilder} or
     * {@link groovyx.net.http.optional.ApacheHttpBuilder}).
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure({ c -> new ApacheHttpBuilder(c); } as Function)
     * ----
     *
     * When configuring the `HttpBuilder` with this method, the verb configurations are required to specify the `request.uri` property.
     *
     * @param factory the `HttpObjectConfig` factory function ({@link JavaHttpBuilder} or {@link groovyx.net.http.optional.ApacheHttpBuilder})
     * @return the configured `HttpBuilder`
     */
    public static HttpBuilder configure(final Function<HttpObjectConfig, ? extends HttpBuilder> factory) {
        return configure(factory, NO_OP);
    }

    /**
     * Creates an `HttpBuilder` using the `defaultFactory` instance configured with the provided configuration closure.
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
     * @param factory the {@link HttpObjectConfig} factory function ({@link JavaHttpBuilder} or {@link groovyx.net.http.optional.ApacheHttpBuilder})
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
     * Creates an `HttpBuilder` using the `defaultFactory` instance configured with the provided configuration closure.
     *
     * The configuration {@link Consumer} function accepts an instance of the {@link HttpObjectConfig} interface, which is an extension of the {@link HttpConfig}
     * interface - configuration properties from either may be applied to the global client configuration here. See the documentation for those interfaces for
     * configuration property details.
     *
     * This configuration method is generally meant for use with standard Java.
     *
     * [source,java]
     * ----
     * FIXME: document
     * ----
     *
     * @param configuration the configuration function (accepting {@link HttpObjectConfig})
     * @return the configured `HttpBuilder`
     */
    public static HttpBuilder configure(final Consumer<HttpObjectConfig> configuration) {
        return configure(factory, configuration);
    }

    /**
     * FIXME: document
     *
     * @param factory
     * @param configuration
     * @return
     */
    public static HttpBuilder configure(final Function<HttpObjectConfig, ? extends HttpBuilder> factory, final Consumer<HttpObjectConfig> configuration) {
        HttpObjectConfig impl = new HttpObjectConfigImpl();
        configuration.accept(impl);
        return factory.apply(impl);
    }

    private final EnumMap<HttpVerb, BiFunction<ChainedHttpConfig, Function<ChainedHttpConfig, Object>, Object>> interceptors;

    protected HttpBuilder(final HttpObjectConfig objectConfig) {
        this.interceptors = new EnumMap<>(objectConfig.getExecution().getInterceptors());
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
        return type.cast(get(closure));
    }

    /**
     * FIXME: document
     *
     * @param type
     * @param configuration
     * @param <T>
     * @return
     */
    public <T> T get(final Class<T> type, final Consumer<HttpConfig> configuration){
        return type.cast(get(configuration));
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
     * FIXME: document
     *
     * @param configuration
     * @return
     */
    public CompletableFuture<Object> getAsync(final Consumer<HttpConfig> configuration){
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
     * FIXME: document
     *
     * @param type
     * @param configuration
     * @param <T>
     * @return
     */
    public <T> CompletableFuture<T> getAsync(final Class<T> type, final Consumer<HttpConfig> configuration){
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
        return type.cast(head(closure));
    }

    /**
     * FIXME: document
     *
     * @param type
     * @param configuration
     * @param <T>
     * @return
     */
    public <T> T head(final Class<T> type, final Consumer<HttpConfig> configuration){
        return type.cast(head(configuration));
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
     * FIXME: document
     *
     * @param configuration
     * @return
     */
    public CompletableFuture<Object> headAsync(final Consumer<HttpConfig> configuration){
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
     * FIXME: document
     *
     * @param type
     * @param configuration
     * @param <T>
     * @return
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
        return type.cast(post(closure));
    }

    /**
     * FIXME: document
     *
     * @param type
     * @param configuration
     * @param <T>
     * @return
     */
    public <T> T post(final Class<T> type, final Consumer<HttpConfig> configuration) {
        return type.cast(post(configuration));
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
     * FIXME: document
     *
     * @param configuration
     * @return
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
     * FIXME: document
     *
     * @param type
     * @param configuration
     * @param <T>
     * @return
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
        return type.cast(put(closure));
    }

    /**
     * FIXME: document
     * @param type
     * @param closure
     * @param <T>
     * @return
     */
    public <T> T put(final Class<T> type, final Consumer<HttpConfig> configuration) {
        return type.cast(put(configuration));
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
     * FIXME: document
     * @param closure
     * @return
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
     * FIXME: document
     * @param type
     * @param closure
     * @param <T>
     * @return
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
        return type.cast(delete(closure));
    }

    /**
     * FIXME: document
     * @param type
     * @param closure
     * @param <T>
     * @return
     */
    public <T> T delete(final Class<T> type, final Consumer<HttpConfig> configuration) {
        return type.cast(delete(configuration));
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
     * FIXME: document
     * @param closure
     * @return
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
     * @param closure the additional configuration closure (delegated to {@link HttpConfig})
     * @return the {@link CompletableFuture} containing the result of the request
     */
    public <T> CompletableFuture<T> deleteAsync(final Class<T> type, @DelegatesTo(HttpConfig.class) final Closure closure) {
        return CompletableFuture.supplyAsync(() -> delete(type, closure), getExecutor());
    }

    /**
     * FIXME: document
     * @param type
     * @param closure
     * @param <T>
     * @return
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
        return interceptors.get(HttpVerb.GET).apply(configureRequest(closure), this::doGet);
    }

    /**
     * FIXME: document
     *
     * @param configuration
     * @return
     */
    public Object get(final Consumer<HttpConfig> configuration){
        return interceptors.get(HttpVerb.GET).apply(configureRequest(configuration), this::doGet);
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
        return interceptors.get(HttpVerb.HEAD).apply(configureRequest(closure), this::doHead);
    }

    /**
     * FIXME: document
     *
     * @param configuration
     * @return
     */
    public Object head(final Consumer<HttpConfig> configuration){
        return interceptors.get(HttpVerb.HEAD).apply(configureRequest(configuration), this::doHead);
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
        return interceptors.get(HttpVerb.POST).apply(configureRequest(closure), this::doPost);
    }

    /**
     * FIXME: document
     * @param configuration
     * @return
     */
    public Object post(final Consumer<HttpConfig> configuration) {
        return interceptors.get(HttpVerb.POST).apply(configureRequest(configuration), this::doPost);
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
        return interceptors.get(HttpVerb.PUT).apply(configureRequest(closure), this::doPut);
    }

    /**
     * FIXME: document
     * @param closure
     * @return
     */
    public Object put(final Consumer<HttpConfig> configuration) {
        return interceptors.get(HttpVerb.PUT).apply(configureRequest(configuration), this::doPut);
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
        return interceptors.get(HttpVerb.DELETE).apply(configureRequest(closure), this::doDelete);
    }

    /**
     * FIMXE: document
     * @param configuration
     * @return
     */
    public Object delete(final Consumer<HttpConfig> configuration) {
        return interceptors.get(HttpVerb.DELETE).apply(configureRequest(configuration), this::doDelete);
    }

    protected abstract Object doGet(final ChainedHttpConfig config);

    protected abstract Object doHead(final ChainedHttpConfig config);

    protected abstract Object doPost(final ChainedHttpConfig config);

    protected abstract Object doPut(final ChainedHttpConfig config);

    protected abstract Object doDelete(final ChainedHttpConfig config);

    protected abstract ChainedHttpConfig getObjectConfig();

    public abstract Executor getExecutor();

    private ChainedHttpConfig configureRequest(final Closure closure) {
        final ChainedHttpConfig myConfig = HttpConfigs.requestLevel(getObjectConfig());
        closure.setDelegate(myConfig);
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        closure.call();
        return myConfig;
    }

    private ChainedHttpConfig configureRequest(final Consumer<HttpConfig> configuration) {
        final ChainedHttpConfig myConfig = HttpConfigs.requestLevel(getObjectConfig());
        configuration.accept(myConfig);
        return myConfig;
    }
}

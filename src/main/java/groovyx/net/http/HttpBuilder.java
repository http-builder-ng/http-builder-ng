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
    
    private ChainedHttpConfig configureRequest(final Closure closure) {
        final ChainedHttpConfig myConfig = HttpConfigs.requestLevel(getObjectConfig());
        closure.setDelegate(myConfig);
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        closure.call();
        return myConfig;
    }

    private final EnumMap<HttpVerb,BiFunction<ChainedHttpConfig,Function<ChainedHttpConfig,Object>, Object>> interceptors;
    
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
     * Executes an HEAD request on the configured URI.
     *
     * @return the resulting content
     */
    public Object head() {
        return head(NO_OP);
    }

    /**
     * Executes an HEAD request on the configured URI, with additional configuration provided by the configuration closure.
     *
     * @param type the type of the resulting response content
     * @param closure the additional configuration closure (delegated to `HttpConfig`)
     * @return the resulting content
     */
    public <T> T head(final Class<T> type, @DelegatesTo(HttpConfig.class) final Closure closure) {
        return type.cast(head(closure));
    }

    /**
     * Executes an asynchronous HEAD request on the configured URI.
     *
     * @return a CompletableFuture for retrieving the resulting content
     */
    public CompletableFuture<Object> headAsync() {
        return CompletableFuture.supplyAsync(() -> head(), getExecutor());
    }

    /**
     * Executes an asynchronous HEAD request on the configured URI, with additional configuration provided by the configuration closure.
     *
     * @param closure the additional configuration closure (delegated to `HttpConfig`)
     * @return a CompletableFuture for retrieving the resulting content
     */
    public CompletableFuture<Object> headAsync(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return CompletableFuture.supplyAsync(() -> head(closure), getExecutor());
    }

    /**
     * Executes an asynchronous HEAD request on the configured URI, with additional configuration provided by the configuration closure.
     *
     * @param type the type of the resulting response content
     * @param closure the additional configuration closure (delegated to `HttpConfig`)
     * @return a CompletableFuture for retrieving the resulting content
     */
    public <T> CompletableFuture<T> headAsync(final Class<T> type, @DelegatesTo(HttpConfig.class) final Closure closure) {
        return CompletableFuture.supplyAsync(() -> head(type, closure), getExecutor());
    }

    /**
     * Executes a POST request on the configured URI.
     *
     * @return the resulting content
     */
    public Object post() {
        return post(NO_OP);
    }

    /**
     * Executes a POST request on the configured URI, with additional configuration provided by the configuration closure.
     *
     * @param type the type of the resulting response content
     * @param closure the additional configuration closure (delegated to `HttpConfig`)
     * @return the resulting content
     */
    public <T> T post(final Class<T> type, @DelegatesTo(HttpConfig.class) final Closure closure) {
        return type.cast(post(closure));
    }

    /**
     * Executes an asynchronous POST request on the configured URI.
     *
     * @return the resulting content
     */
    public CompletableFuture<Object> postAsync() {
        return CompletableFuture.supplyAsync(() -> post(NO_OP), getExecutor());
    }

    /**
     * Executes an asynchronous POST request on the configured URI, with additional configuration provided by the configuration closure.
     *
     * @param closure the additional configuration closure (delegated to `HttpConfig`)
     * @return the resulting content
     */
    public Object postAsync(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return CompletableFuture.supplyAsync(() -> post(closure), getExecutor());
    }

    /**
     * Executes an asynchronous POST request on the configured URI, with additional configuration provided by the configuration closure.
     *
     * @param type the type of the resulting response content
     * @param closure the additional configuration closure (delegated to `HttpConfig`)
     * @return a CompletableFuture for retrieving the resulting content
     */
    public <T> CompletableFuture<T> postAsync(final Class<T> type, @DelegatesTo(HttpConfig.class) final Closure closure) {
        return CompletableFuture.supplyAsync(() -> post(type, closure), getExecutor());
    }

    /**
     * Executes a PUT request on the configured URI.
     *
     * @return the resulting content
     */
    public Object put() {
        return put(NO_OP);
    }

    /**
     * Executes a PUT request on the configured URI, with additional configuration provided by the configuration closure.
     *
     * @param type the type of the resulting response content
     * @param closure the additional configuration closure (delegated to `HttpConfig`)
     * @return the resulting content
     */
    public <T> T put(final Class<T> type, @DelegatesTo(HttpConfig.class) final Closure closure) {
        return type.cast(put(closure));
    }

    /**
     * Executes an asynchronous PUT request on the configured URI.
     *
     * @return a CompletableFuture for retrieving the resulting content
     */
    public CompletableFuture<Object> putAsync() {
        return CompletableFuture.supplyAsync(() -> put(NO_OP), getExecutor());
    }

    /**
     * Executes an asynchronous PUT request on the configured URI, with additional configuration provided by the configuration closure.
     *
     * @param closure the additional configuration closure (delegated to `HttpConfig`)
     * @return the resulting content
     */
    public Object putAsync(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return CompletableFuture.supplyAsync(() -> put(closure), getExecutor());
    }

    /**
     * Executes an asynchronous PUT request on the configured URI, with additional configuration provided by the configuration closure.
     *
     * @param type the type of the resulting response content
     * @param closure the additional configuration closure (delegated to `HttpConfig`)
     * @return a CompletableFuture for retrieving the resulting content
     */
    public <T> CompletableFuture<T> putAsync(final Class<T> type, @DelegatesTo(HttpConfig.class) final Closure closure) {
        return CompletableFuture.supplyAsync(() -> put(type, closure), getExecutor());
    }

    /**
     * Executes a DELETE request on the configured URI.
     *
     * @return the resulting content
     */
    public Object delete() {
        return delete(NO_OP);
    }

    /**
     * Executes a DELETE request on the configured URI, with additional configuration provided by the configuration closure.
     *
     * @param type the type of the resulting response content
     * @param closure the additional configuration closure (delegated to `HttpConfig`)
     * @return the resulting content
     */
    public <T> T delete(final Class<T> type, @DelegatesTo(HttpConfig.class) final Closure closure) {
        return type.cast(delete(closure));
    }

    /**
     * Executes an asynchronous DELETE request on the configured URI.
     *
     * @return a CompletableFuture for retrieving the resulting content
     */
    public CompletableFuture<Object> deleteAsync() {
        return CompletableFuture.supplyAsync(() -> delete(NO_OP), getExecutor());
    }

    /**
     * Executes an asynchronous DELETE request on the configured URI, with additional configuration provided by the configuration closure.
     *
     * @param closure the additional configuration closure (delegated to `HttpConfig`)
     * @return the resulting content
     */
    public Object deleteAsync(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return CompletableFuture.supplyAsync(() -> delete(closure), getExecutor());
    }

    /**
     * Executes an asynchronous DELETE request on the configured URI, with additional configuration provided by the configuration closure.
     *
     * @param type the type of the resulting response content
     * @param closure the additional configuration closure (delegated to `HttpConfig`)
     * @return a CompletableFuture for retrieving the resulting content
     */
    public <T> CompletableFuture<T> deleteAsync(final Class<T> type, @DelegatesTo(HttpConfig.class) final Closure closure) {
        return CompletableFuture.supplyAsync(() -> delete(type, closure), getExecutor());
    }

    /**
     * Executes a GET request on the configured URI, with additional configuration provided by the configuration closure.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *     request.uri = 'http://localhost:10101'
     * }
     * def result = http.getAsync(){
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
     * Executes an HEAD request on the configured URI, with additional configuration provided by the configuration closure.
     *
     * @param closure the additional configuration closure (delegated to `HttpConfig`)
     * @return the resulting content
     */
    public Object head(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return interceptors.get(HttpVerb.HEAD).apply(configureRequest(closure), this::doHead);
    }

    /**
     * Executes a POST request on the configured URI, with additional configuration provided by the configuration closure.
     *
     * @param closure the additional configuration closure (delegated to `HttpConfig`)
     * @return the resulting content
     */
    public Object post(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return interceptors.get(HttpVerb.POST).apply(configureRequest(closure), this::doPost);
    }

    /**
     * Executes a PUT request on the configured URI, with additional configuration provided by the configuration closure.
     *
     * @param closure the additional configuration closure (delegated to `HttpConfig`)
     * @return the resulting content
     */
    public Object put(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return interceptors.get(HttpVerb.PUT).apply(configureRequest(closure), this::doPut);
    }

    /**
     * Executes a DELETE request on the configured URI, with additional configuration provided by the configuration closure.
     *
     * @param closure the additional configuration closure (delegated to `HttpConfig`)
     * @return the resulting content
     */
    public Object delete(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return interceptors.get(HttpVerb.DELETE).apply(configureRequest(closure), this::doDelete);
    }

    protected abstract Object doGet(final ChainedHttpConfig config);
    protected abstract Object doHead(final ChainedHttpConfig config);
    protected abstract Object doPost(final ChainedHttpConfig config);
    protected abstract Object doPut(final ChainedHttpConfig config);
    protected abstract Object doDelete(final ChainedHttpConfig config);
    protected abstract ChainedHttpConfig getObjectConfig();
    
    public abstract Executor getExecutor();    
}

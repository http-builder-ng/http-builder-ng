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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.codehaus.groovy.runtime.MethodClosure;
import java.io.Closeable;
import java.util.function.Function;
import java.util.function.BiFunction;
import java.util.EnumMap;

/**
 * This class is the main entry point into the "Http Builder NG" API. It provides access to the HTTP Client configuration and the HTTP verbs to be
 * executed.
 */
public abstract class HttpBuilder implements Closeable {

    private static volatile Function<HttpObjectConfig, ? extends HttpBuilder> factory = JavaHttpBuilder::new;

    public static Function<HttpObjectConfig, ? extends HttpBuilder> getDefaultFactory() {
        return factory;
    }

    public static void setDefaultFactory(final Function<HttpObjectConfig, ? extends HttpBuilder> val) {
        factory = val;
    }

    static void noOp() { }
    static Closure NO_OP = new MethodClosure(HttpBuilder.class, "noOp");

    /**
     * Creates an <code>HttpBuilder</code> with the default configuration using the provided factory function (<code>JavaHttpBuilder</code> or
     * <code>ApacheHttpBuilder</code>).
     *
     * @param factory the <code>HttpObjectConfig</code> factory function (<code>JavaHttpBuilder</code> or <code>ApacheHttpBuilder</code>)
     * @return the configured <code>HttpBuilder</code>
     */
    public static HttpBuilder configure(final Function<HttpObjectConfig, ? extends HttpBuilder> factory) {
        return configure(factory, NO_OP);
    }

    /**
     * Creates an <code>HttpBuilder</code> configured by the provided configuration closure. The <code>JavaHttpBuilder</code> factory will be used
     * for the underlying configuration.
     *
     * @param closure the configuration closure (delegated to <code>HttpObjectConfig</code>)
     * @return the configured <code>HttpBuilder</code>
     */
    public static HttpBuilder configure(@DelegatesTo(HttpObjectConfig.class) final Closure closure) {
        return configure(factory, closure);
    }

    /**
     * Creates an <code>HttpBuilder</code> configured by the provided configuration closure.
     *
     * @param factory the <code>HttpObjectConfig</code> factory function (<code>JavaHttpBuilder</code> or <code>ApacheHttpBuilder</code>)
     * @param closure the configuration closure (delegated to <code>HttpObjectConfig</code>)
     * @return the configured <code>HttpBuilder</code>
     */
    public static HttpBuilder configure(final Function<HttpObjectConfig, ? extends HttpBuilder> factory,
                                        @DelegatesTo(HttpObjectConfig.class) final Closure closure) {
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
     * Executes a GET request on the configured URI.
     *
     * @return the resulting content
     */
    public Object get() {
        return get(NO_OP);
    }

    /**
     * Executes a GET request on the configured URI, with additional configuration provided by the configuration closure.
     *
     * @param type the type of the response content
     * @param closure the additional configuration closure (delegated to <code>HttpConfig</code>)
     * @return the resulting content
     */
    public <T> T get(final Class<T> type, @DelegatesTo(HttpConfig.class) final Closure closure) {
        return type.cast(get(closure));
    }

    /**
     * Executes an asynchronous GET request on the configured URI.
     *
     * @return a CompletableFuture for retrieving the resulting content
     */
    public CompletableFuture<Object> getAsync() {
        return CompletableFuture.supplyAsync(() -> get(), getExecutor());
    }

    /**
     * Executes an asynchronous GET request on the configured URI, with additional configuration provided by the configuration closure.
     *
     * @param closure the additional configuration closure (delegated to <code>HttpConfig</code>)
     * @return a CompletableFuture for retrieving the resulting content
     */
    public CompletableFuture<Object> getAsync(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return CompletableFuture.supplyAsync(() -> get(closure), getExecutor());
    }

    /**
     * Executes an asynchronous GET request on the configured URI, with additional configuration provided by the configuration closure.
     *
     * @param type the type of the response content
     * @param closure the additional configuration closure (delegated to <code>HttpConfig</code>)
     * @return a CompletableFuture for retrieving the resulting content
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
     * @param closure the additional configuration closure (delegated to <code>HttpConfig</code>)
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
     * @param closure the additional configuration closure (delegated to <code>HttpConfig</code>)
     * @return a CompletableFuture for retrieving the resulting content
     */
    public CompletableFuture<Object> headAsync(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return CompletableFuture.supplyAsync(() -> head(closure), getExecutor());
    }

    /**
     * Executes an asynchronous HEAD request on the configured URI, with additional configuration provided by the configuration closure.
     *
     * @param type the type of the resulting response content
     * @param closure the additional configuration closure (delegated to <code>HttpConfig</code>)
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
     * @param closure the additional configuration closure (delegated to <code>HttpConfig</code>)
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
     * @param closure the additional configuration closure (delegated to <code>HttpConfig</code>)
     * @return the resulting content
     */
    public Object postAsync(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return CompletableFuture.supplyAsync(() -> post(closure), getExecutor());
    }

    /**
     * Executes an asynchronous POST request on the configured URI, with additional configuration provided by the configuration closure.
     *
     * @param type the type of the resulting response content
     * @param closure the additional configuration closure (delegated to <code>HttpConfig</code>)
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
     * @param closure the additional configuration closure (delegated to <code>HttpConfig</code>)
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
     * @param closure the additional configuration closure (delegated to <code>HttpConfig</code>)
     * @return the resulting content
     */
    public Object putAsync(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return CompletableFuture.supplyAsync(() -> put(closure), getExecutor());
    }

    /**
     * Executes an asynchronous PUT request on the configured URI, with additional configuration provided by the configuration closure.
     *
     * @param type the type of the resulting response content
     * @param closure the additional configuration closure (delegated to <code>HttpConfig</code>)
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
     * @param closure the additional configuration closure (delegated to <code>HttpConfig</code>)
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
     * @param closure the additional configuration closure (delegated to <code>HttpConfig</code>)
     * @return the resulting content
     */
    public Object deleteAsync(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return CompletableFuture.supplyAsync(() -> delete(closure), getExecutor());
    }

    /**
     * Executes an asynchronous DELETE request on the configured URI, with additional configuration provided by the configuration closure.
     *
     * @param type the type of the resulting response content
     * @param closure the additional configuration closure (delegated to <code>HttpConfig</code>)
     * @return a CompletableFuture for retrieving the resulting content
     */
    public <T> CompletableFuture<T> deleteAsync(final Class<T> type, @DelegatesTo(HttpConfig.class) final Closure closure) {
        return CompletableFuture.supplyAsync(() -> delete(type, closure), getExecutor());
    }

    /**
     * Executes a GET request on the configured URI, with additional configuration provided by the configuration closure.
     *
     * @param closure the additional configuration closure (delegated to <code>HttpConfig</code>)
     * @return the resulting content
     */
    public Object get(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return interceptors.get(HttpVerb.GET).apply(configureRequest(closure), this::doGet);
    }

    /**
     * Executes an HEAD request on the configured URI, with additional configuration provided by the configuration closure.
     *
     * @param closure the additional configuration closure (delegated to <code>HttpConfig</code>)
     * @return the resulting content
     */
    public Object head(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return interceptors.get(HttpVerb.HEAD).apply(configureRequest(closure), this::doHead);
    }

    /**
     * Executes a POST request on the configured URI, with additional configuration provided by the configuration closure.
     *
     * @param closure the additional configuration closure (delegated to <code>HttpConfig</code>)
     * @return the resulting content
     */
    public Object post(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return interceptors.get(HttpVerb.POST).apply(configureRequest(closure), this::doPost);
    }

    /**
     * Executes a PUT request on the configured URI, with additional configuration provided by the configuration closure.
     *
     * @param closure the additional configuration closure (delegated to <code>HttpConfig</code>)
     * @return the resulting content
     */
    public Object put(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return interceptors.get(HttpVerb.PUT).apply(configureRequest(closure), this::doPut);
    }

    /**
     * Executes a DELETE request on the configured URI, with additional configuration provided by the configuration closure.
     *
     * @param closure the additional configuration closure (delegated to <code>HttpConfig</code>)
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

/**
 * Copyright (C) 2017 HttpBuilder-NG Project
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

import java.net.Proxy;
import java.net.UnknownHostException;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.util.EnumMap;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Extension of the {@link HttpConfig} interface, which provides additional client-level configuration options. These options should be configured in
 * the {@link HttpBuilder} `configure()` closure, rather than in the verb configuration closure.
 *
 * [source,groovy]
 * ----
 * def http = HttpBuilder.configure {
 *      execution.maxThreads = 2
 *      execution.executor = Executors.newFixedThreadPool(2)
 * }
 * ----
 */
public interface HttpObjectConfig extends HttpConfig {

    /**
     * The `Execution` configuration interface provides a means of configuring the execution-specific properties of the underlying HTTP client.
     */
    interface Execution {
        /**
         * Specifies the maximum number of connection threads to be used by the client.
         *
         * @param val the max thread count
         */
        void setMaxThreads(int val);

        /**
         * Retrieves the configured max number of connection threads used by the client.
         *
         * @return the max thread count
         */
        int getMaxThreads();

        /**
         * Specifies the executor to be used.
         *
         * @param val the executor to be used
         */
        void setExecutor(Executor val);

        /**
         * Retrieves the configured executor.
         *
         * @return the executor
         */
        Executor getExecutor();

        /**
         * Specifies the {@link SSLContext} to be used by the configured client.
         *
         * @param val the {@link SSLContext}
         */
        void setSslContext(SSLContext val);

        /**
         * Specifies the {@link HostnameVerifier} to be used by the configured client (related to SSL).
         *
         * @param verifier the hostname verifier
         */
        void setHostnameVerifier(HostnameVerifier verifier);

        /**
         * Used to retrieve the {@link HostnameVerifier} configured for the client.
         *
         * @return the configured {@link HostnameVerifier}
         */
        HostnameVerifier getHostnameVerifier();

        /**
         * Retrieves the {@link SSLContext} configured for the underlying HTTP client.
         *
         * @return the configured {@link SSLContext}
         */
        SSLContext getSslContext();

        /**
         * Configures an interceptor (similar to an Http Servlet filter) which allows operations to be performed before and after a request, even alteration
         * of the data.
         *
         * [source,groovy]
         * ----
         * long elapsed = configure {
         *      request.uri = 'http://localhost:10101/foo'
         *      execution.interceptor(GET) { ChainedHttpConfig cfg, Function<ChainedHttpConfig, Object> fx ->
         *          long started = System.currentTimeMillis()
         *          fx.apply(cfg)
         *          System.currentTimeMillis() - started
         *      }
         * }.get(Long, NO_OP)
         * ----
         *
         * The example above would return the elapsed time for the request as the result of the request.
         *
         * @param verb the HTTP verb to intercept
         * @param func the interceptor function
         */
        void interceptor(HttpVerb verb, BiFunction<ChainedHttpConfig,Function<ChainedHttpConfig,Object>, Object> func);

        /**
         * Configures an interceptor (similar to an Http Servlet filter) which allows operations to be performed before and after a request, even alteration
         * of the data.
         *
         * @param verbs the HTTP verbs to intercept
         * @param func the interceptor function
         */
        void interceptor(HttpVerb[] verbs, BiFunction<ChainedHttpConfig,Function<ChainedHttpConfig,Object>, Object> func);

        /**
         * Used to retrieve the interceptors configured.
         *
         * @return all interceptors
         */
        EnumMap<HttpVerb,BiFunction<ChainedHttpConfig,Function<ChainedHttpConfig,Object>, Object>> getInterceptors();

        /**
         * Configures proxy information for all requests using this client
         *
         * @param proxy the Proxy object used for configuring proxy connections
         * @param secure some clients use this parameter to configure application level ssl proxying. If the client
         * can determine the underlying protocol without issues this parameter will be ignored.
         */
        void proxy(Proxy proxy, boolean secure);

        /**
         * Configures proxy information for all requests using this client
         *
         * @param host the host of the proxy. This can be either an ip address or a domain name.
         * @param port the port of the proxy.
         * @param type the type of the proxy.
         * @param secure some clients use this parameter to configure application level ssl proxying. If the client
         * can determine the underlying protocol without issues this parameter will be ignored.
         */
        void proxy(String host, int port, Proxy.Type type, boolean secure) throws UnknownHostException;

        ProxyInfo getProxyInfo();
    }

    /**
     * The `Client` configuration interface allows configuration of client-centric properties. Currently, the only supported property is `cookieVersion`
     * which is the supported HTTP Cookie version used by the underlying clients. The {@link HttpBuilder} implementations will support Cookies at
     * version `0` by default, which is what the Java Servlet API accepts by default. This can be modified, but care must be taken to ensure that your
     * server supports and accepts the configured Cookie version.
     *
     * [source,groovy]
     * ----
     * def http = HttpBuilder.configure {
     *      cookieVersion = 1
     * }
     * ----
     */
    interface Client {

        /**
         * Used to specify the supported Cookie version. If not specified, a default of `0` is used to conform with the default used in the
         * Java Servlet Cookie API. Be aware that if you change the version here, you may need to modify the version expected by your server.
         *
         * @param version the Cookie version to be used.
         */
        void setCookieVersion(int version);

        /**
         * Retrieves the supported Cookie version. A version of `0` will be returned, if not explicitly overridden.
         *
         * @return the Cookie version supported
         */
        int getCookieVersion();

        /**
         * Specifies the location for storing cookies that will persist after your application terminates. If no folder is
         * specified an in memory cookie store and no cookies will be persisted after your application terminates. If cookies
         * are found here then the cookies will be loaded prior to sending any requests to remote servers.
         *
         * @param folder the folder used to store the cookies.
         */
        void setCookieFolder(File folder);

        /**
         * Retrieves the location for storing persistent cookies
         *
         * @return the folder containing persistent cookies, null if using an in memory store
         */
        File getCookieFolder();

        /**
         * Used to enable or disable cookies.
         *
         * @param val true if cookies are enabled, false if not enabled.
         */
        void setCookiesEnabled(boolean val);

        /**
         * Retrieves whether cookies are enabled or disabled
         *
         * @return true if cookies are enabled, false if not
         */
        boolean getCookiesEnabled();

        /**
         * A `Consumer<Object>` may be provided, which will have the internal client implementation reference passed into it to allow further
         * client configuration beyond what it supported directly by HttpBuilder-NG. The `Object` passed in will be an instance of the internal client
         * builder type, not necessarily the client itself.
         *
         * This configuration method should _only_ be used when the desire configuration is _not_ available directly through the `HttpBuilder`
         * configuration interfaces. Configuring in this manner may override helpful configuration already applied by the library.
         *
         * Note that a Groovy closure may be used to replace the `Consumer` with no modification of functionality.
         *
         * This operation is optional. If a client-implementation does not support it, an {@link UnsupportedOperationException} will be thrown.
         */
        void clientCustomizer(Consumer<Object> customizer);

        /**
         * Used to retrieve the configured client implementation customizer `Consumer`, if there is one.
         *
         * @return the configured client customizer
         */
        Consumer<Object> getClientCustomizer();
    }

    ChainedHttpConfig getChainedConfig();

    /**
     * Retrieves the execution configuration interface implementation.
     *
     * @return the `Execution` configuration instance
     */
    Execution getExecution();

    /**
     * Retrieves the client configuration interface implementation.
     *
     * @return the `Client` configuration instance
     */
    Client getClient();
}


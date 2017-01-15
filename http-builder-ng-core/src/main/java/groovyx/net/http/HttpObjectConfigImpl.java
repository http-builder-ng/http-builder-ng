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

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.util.EnumMap;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Function;

import static groovyx.net.http.HttpConfigs.basic;
import static groovyx.net.http.HttpConfigs.root;
import static groovyx.net.http.util.SslUtils.ANY_HOSTNAME;
import static groovyx.net.http.util.SslUtils.acceptingSslContext;
import static java.lang.System.getProperty;
import static org.apache.commons.lang3.BooleanUtils.toBoolean;

public class HttpObjectConfigImpl implements HttpObjectConfig {

    private final ChainedHttpConfig config = basic(root());

    public ChainedHttpConfig getChainedConfig() {
        return config;
    }

    final Exec exec = new Exec();
    final ClientConfig clientConfig = new ClientConfig();

    public static Object nullInterceptor(final ChainedHttpConfig config, final Function<ChainedHttpConfig, Object> func) {
        return func.apply(config);
    }

    private static class Exec implements Execution {
        private int maxThreads = 1;
        private Executor executor = SingleThreaded.instance;
        private SSLContext sslContext;
        private HostnameVerifier hostnameVerifier;
        private final EnumMap<HttpVerb, BiFunction<ChainedHttpConfig, Function<ChainedHttpConfig, Object>, Object>> interceptors;

        public Exec() {
            interceptors = new EnumMap<>(HttpVerb.class);
            for (HttpVerb verb : HttpVerb.values()) {
                interceptors.put(verb, HttpObjectConfigImpl::nullInterceptor);
            }

            if (toBoolean(getProperty("groovyx.net.http.ignore-ssl-issues"))) {
                setSslContext(acceptingSslContext());
                setHostnameVerifier(ANY_HOSTNAME);
            }
        }

        public void setMaxThreads(final int val) {
            if (val < 1) {
                throw new IllegalArgumentException("Max Threads cannot be less than 1");
            }

            this.maxThreads = val;
        }

        public int getMaxThreads() {
            return maxThreads;
        }

        public void setExecutor(final Executor val) {
            if (val == null) {
                throw new NullPointerException();
            }

            this.executor = val;
        }

        public Executor getExecutor() {
            return executor;
        }

        public void setSslContext(final SSLContext val) {
            this.sslContext = val;
        }

        public SSLContext getSslContext() {
            return sslContext;
        }

        @Override
        public void setHostnameVerifier(HostnameVerifier verifier) {
            this.hostnameVerifier = verifier;
        }

        @Override
        public HostnameVerifier getHostnameVerifier() {
            return hostnameVerifier;
        }

        public void interceptor(final HttpVerb verb, final BiFunction<ChainedHttpConfig, Function<ChainedHttpConfig, Object>, Object> func) {
            if (func == null) {
                throw new NullPointerException("func cannot be null");
            }

            interceptors.put(verb, func);
        }

        public void interceptor(final HttpVerb[] verbs, final BiFunction<ChainedHttpConfig, Function<ChainedHttpConfig, Object>, Object> func) {
            for (HttpVerb verb : verbs) {
                interceptors.put(verb, func);
            }
        }

        public EnumMap<HttpVerb, BiFunction<ChainedHttpConfig, Function<ChainedHttpConfig, Object>, Object>> getInterceptors() {
            return interceptors;
        }
    }

    private static class ClientConfig implements Client {

        private int cookieVersion = 0;
        private File cookieFolder;

        @Override
        public void setCookieVersion(int version) {
            this.cookieVersion = version;
        }

        @Override
        public int getCookieVersion() {
            return cookieVersion;
        }

        @Override
        public File getCookieFolder() {
            return cookieFolder;
        }

        @Override
        public void setCookieFolder(final File val) {
            this.cookieFolder = val;
        }
    }

    private static class SingleThreaded implements Executor {
        public void execute(Runnable r) {
            r.run();
        }

        public static final SingleThreaded instance = new SingleThreaded();
    }

    public Request getRequest() {
        return config.getRequest();
    }

    public Response getResponse() {
        return config.getResponse();
    }

    public HttpConfig getParent() {
        return config.getParent();
    }

    public Execution getExecution() {
        return exec;
    }

    @Override
    public Client getClient() {
        return clientConfig;
    }

    public void context(final String contentType, final Object id, final Object obj) {
        config.context(contentType, id, obj);
    }
}

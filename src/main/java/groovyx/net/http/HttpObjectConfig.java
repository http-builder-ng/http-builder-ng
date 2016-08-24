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

import java.util.concurrent.Executor;
import javax.net.ssl.SSLContext;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.EnumMap;

/**
 * Extension of the {@link HttpConfig} interface, which provides additional client-level configuration options.
 */
public interface HttpObjectConfig extends HttpConfig {

    /**
     * Defines execution configuration for the underlying HTTP client.
     */
    interface Execution {
        // TODO: is maxThreads used only by Apache client?
        void setMaxThreads(int val);
        int getMaxThreads();

        // TODO: what is relationship/difference between maxThreads and executor?
        void setExecutor(Executor val);
        Executor getExecutor();

        /**
         * FIXME: document
         */
        void setSslContext(SSLContext val);
        SSLContext getSslContext();

        /**
         * FIXME: document
         */
        void interceptor(HttpVerb verb, BiFunction<ChainedHttpConfig,Function<ChainedHttpConfig,Object>, Object> func);
        void interceptor(HttpVerb[] verbs, BiFunction<ChainedHttpConfig,Function<ChainedHttpConfig,Object>, Object> func);
        EnumMap<HttpVerb,BiFunction<ChainedHttpConfig,Function<ChainedHttpConfig,Object>, Object>> getInterceptors();
    }

    ChainedHttpConfig getChainedConfig();

    /**
     * Retrieves the execution configuration interface implementation.
     *
     * @return the Execution configuration instance
     */
    Execution getExecution();
}


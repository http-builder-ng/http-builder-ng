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

public interface HttpObjectConfig extends HttpConfig {

    interface Execution {
        void setMaxThreads(int val);
        int getMaxThreads();
        
        void setExecutor(Executor val);
        Executor getExecutor();
        
        void setSslContext(SSLContext val);
        SSLContext getSslContext();

        void interceptor(HttpVerb verb, BiFunction<ChainedHttpConfig,Function<ChainedHttpConfig,Object>, Object> func);
        void interceptor(HttpVerb[] verbs, BiFunction<ChainedHttpConfig,Function<ChainedHttpConfig,Object>, Object> func);
        EnumMap<HttpVerb,BiFunction<ChainedHttpConfig,Function<ChainedHttpConfig,Object>, Object>> getInterceptors();
    }

    ChainedHttpConfig getChainedConfig();
    Execution getExecution();
}


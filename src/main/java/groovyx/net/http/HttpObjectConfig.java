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


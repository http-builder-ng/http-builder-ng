package groovyx.net.http;

import groovyx.net.http.optional.ApacheHttpBuilder;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLContext;
import static groovyx.net.http.HttpConfigs.*;
import java.util.function.Function;
import java.util.function.BiFunction;
import java.util.EnumMap;

public class HttpObjectConfigImpl implements HttpObjectConfig {
  
    private final ChainedHttpConfig config = basic(root());

    public ChainedHttpConfig getChainedConfig() {
        return config;
    }
    
    final Exec exec = new Exec();

    public static Object nullInterceptor(final ChainedHttpConfig config, final Function<ChainedHttpConfig,Object> func) {
        return func.apply(config);
    }

    private static class Exec implements Execution {
        private int maxThreads = 1;
        private Executor executor = SingleThreaded.instance;
        private SSLContext sslContext;
        private final EnumMap<HttpVerb,BiFunction<ChainedHttpConfig,Function<ChainedHttpConfig,Object>, Object>> interceptors;

        public Exec() {
            interceptors = new EnumMap<>(HttpVerb.class);
            for(HttpVerb verb : HttpVerb.values()) {
                interceptors.put(verb, HttpObjectConfigImpl::nullInterceptor);
            }
        }
        
        public void setMaxThreads(final int val) {
            if(val < 1) {
                throw new IllegalArgumentException("Max Threads cannot be less than 1");
            }
            
            this.maxThreads = val;
        }

        public int getMaxThreads() {
            return maxThreads;
        }

        public void setExecutor(final Executor val) {
            if(val == null) {
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

        public void interceptor(final HttpVerb verb, final BiFunction<ChainedHttpConfig,Function<ChainedHttpConfig,Object>, Object> func) {
            if(func == null) {
                throw new NullPointerException("func cannot be null");
            }
            
            interceptors.put(verb, func);
        }

        public void interceptor(final HttpVerb[] verbs, final BiFunction<ChainedHttpConfig,Function<ChainedHttpConfig,Object>, Object> func) {
            for(HttpVerb verb : verbs) {
                interceptors.put(verb, func);
            }
        }

        public EnumMap<HttpVerb,BiFunction<ChainedHttpConfig,Function<ChainedHttpConfig,Object>, Object>> getInterceptors() {
            return interceptors;
        }
    }

    private static class SingleThreaded implements Executor {
        public void execute(Runnable r) {
            r.run();
        }

        public static final SingleThreaded instance = new SingleThreaded();
    }

    public HttpBuilder apacheHttpBuilder() {
        return new ApacheHttpBuilder(this);
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
}

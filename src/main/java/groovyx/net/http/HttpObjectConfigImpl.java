package groovyx.net.http;

import groovyx.net.http.libspecific.ApacheHttpBuilder;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLContext;
import static groovyx.net.http.HttpConfigs.*;

public class HttpObjectConfigImpl implements HttpObjectConfig {

    private final ChainedHttpConfig config = basic(root());

    public ChainedHttpConfig getChainedConfig() {
        return config;
    }
    
    final Exec exec = new Exec();

    private static class Exec implements Execution {
        private int maxThreads = 1;
        private Executor executor = SingleThreaded.instance;
        private SSLContext sslContext;

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
    }

    private static class SingleThreaded implements Executor {
        public void execute(Runnable r) {
            r.run();
        }

        public static final SingleThreaded instance = new SingleThreaded();
    }

    public HttpBuilder build(final ClientType type) {
        switch(type) {
        case APACHE_HTTP_CLIENT: return apacheHttpBuilder();
        default: throw new IllegalArgumentException();
        }
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

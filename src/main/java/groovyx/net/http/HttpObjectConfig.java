package groovyx.net.http;

import java.util.concurrent.Executor;
import javax.net.ssl.SSLContext;

public interface HttpObjectConfig extends HttpConfig {

    public interface Execution {
        void setMaxThreads(int val);
        int getMaxThreads();
        
        void setExecutor(Executor val);
        Executor getExecutor();
        
        void setSslContext(SSLContext val);
        SSLContext getSslContext();
    }

    ChainedHttpConfig getChainedConfig();
    Execution getExecution();
}


package groovyx.net.http;

import java.util.function.Supplier;
import java.util.List;
import java.util.function.Function;
import java.util.function.BiConsumer;
import java.util.Collections;

public class Safe {

    
    public static void register(final HttpConfig config,
                                final Supplier<Boolean> shouldRegister,
                                final List<String> contentTypes,
                                final BiConsumer<ChainedHttpConfig.ChainedRequest,ToServer> encoder,
                                final Function<FromServer,Object> parser) {
        
        if(shouldRegister.get()) {
            config.getRequest().encoder(contentTypes, encoder);
            config.getResponse().parser(contentTypes, parser);
        }
    }
    
    public static void register(final HttpConfig config,
                                final Supplier<Boolean> shouldRegister,
                                final String contentType,
                                final BiConsumer<ChainedHttpConfig.ChainedRequest,ToServer> encoder,
                                final Function<FromServer,Object> parser) {
        register(config, shouldRegister, Collections.singletonList(contentType), encoder, parser);
    }

    public static Supplier<Boolean> ifClassIsLoaded(final String className) {
        return () -> {
            try {
                Class.forName(className);
                return true;
            }
            catch(ClassNotFoundException cnfe) {
                return false;
            }
        };
    }
}

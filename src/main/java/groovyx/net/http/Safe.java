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
                                final Supplier<BiConsumer<ChainedHttpConfig.ChainedRequest,ToServer>> encoderSupplier,
                                final Supplier<Function<FromServer,Object>> parserSupplier) {
        
        if(shouldRegister.get()) {
            config.getRequest().encoder(contentTypes, encoderSupplier.get());
            config.getResponse().parser(contentTypes, parserSupplier.get());
        }
    }
    
    public static void register(final HttpConfig config,
                                final Supplier<Boolean> shouldRegister,
                                final String contentType,
                                final Supplier<BiConsumer<ChainedHttpConfig.ChainedRequest,ToServer>> encoderSupplier,
                                final Supplier<Function<FromServer,Object>> parserSupplier) {
        register(config, shouldRegister, Collections.singletonList(contentType), encoderSupplier, parserSupplier);
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

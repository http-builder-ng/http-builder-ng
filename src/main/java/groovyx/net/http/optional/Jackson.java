package groovyx.net.http.optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.BiFunction;
import java.util.function.BiConsumer;
import groovyx.net.http.*;
import java.io.StringWriter;
import java.io.IOException;
import static groovyx.net.http.NativeHandlers.Encoders.handleRawUpload;

public class Jackson {

    public static final String OBJECT_MAPPER_ID = "0w4XJJnlTNK8dvISuCDTlsusPQE=";
    public static final String RESPONSE_TYPE = "GWW35uTkrHwonPt5odeUqBdR3EU=";

    public static Object parse(final ChainedHttpConfig config, final FromServer fromServer) {
        try {
            final ObjectMapper mapper = (ObjectMapper) config.actualContext(fromServer.getContentType(), OBJECT_MAPPER_ID);
            final Class type = (Class) config.actualContext(fromServer.getContentType(), RESPONSE_TYPE);
            return mapper.readValue(fromServer.getReader(), type);
        }
        catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void encode(final ChainedHttpConfig config, final ToServer ts) {
        try {
            if(handleRawUpload(config, ts)) {
                return;
            }

            final ChainedHttpConfig.ChainedRequest request = config.getChainedRequest();
            final ObjectMapper mapper = (ObjectMapper) config.actualContext(request.actualContentType(), OBJECT_MAPPER_ID);
            final Class type = (Class) config.actualContext(request.actualContentType(), RESPONSE_TYPE);
            final StringWriter writer = new StringWriter();
            mapper.writeValue(writer, request.actualBody());
            ts.toServer(new CharSequenceInputStream(writer.toString(), request.actualCharset()));
        }
        catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void mapper(final HttpConfig config, final ObjectMapper mapper) {
        config.context(ContentTypes.JSON, OBJECT_MAPPER_ID, mapper);
    }

    public static void use(final HttpConfig config) {
        config.getRequest().encoder(ContentTypes.JSON, Jackson::encode);
        config.getResponse().parser(ContentTypes.JSON, Jackson::parse);
    }
    
    public static void toType(final HttpConfig config, final Class type) {
        use(config);
        config.context(ContentTypes.JSON, RESPONSE_TYPE, type);
    }
}

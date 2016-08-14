package groovyx.net.http.optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Function;
import java.util.function.BiConsumer;
import groovyx.net.http.*;
import java.io.StringWriter;
import java.io.IOException;

public class Jackson {

    public static <T> Function<FromServer,Object> parse(final ObjectMapper mapper, final Class<T> type) {
        return (fromServer) -> {
            try {
                return mapper.readValue(fromServer.getReader(), type);
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static BiConsumer<ChainedHttpConfig.ChainedRequest,ToServer> encode(final ObjectMapper mapper) {
        return (request, ts) -> {
            try {
                final StringWriter writer = new StringWriter();
                mapper.writeValue(writer, request.actualBody());
                ts.toServer(new CharSequenceInputStream(writer.toString(), request.actualCharset()));
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
        };
    }
}

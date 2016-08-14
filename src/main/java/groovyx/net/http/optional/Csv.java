package groovyx.net.http.optional;

import com.opencsv.*;
import groovyx.net.http.*;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import static groovyx.net.http.NativeHandlers.Encoders.*;

public class Csv {

    public static Function<FromServer,Object> parse(final Function<Reader,CSVReader> csvReaderMaker) {
        return (fromServer) -> {
            try {
                return csvReaderMaker.apply(fromServer.getReader()).readAll();
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static Function<FromServer,Object> parse() {
        return parse((r) -> new CSVReader(r));
    }

    public static Function<FromServer,Object> parse(final char separator) {
        return parse((r) -> new CSVReader(r, separator));
    }

    public static Function<FromServer,Object> parse(final char separator, final char quoteChar) {
        return parse((r) -> new CSVReader(r, separator, quoteChar));
    }

    public static BiConsumer<ChainedHttpConfig.ChainedRequest,ToServer> encode(final Function<Writer,CSVWriter> csvWriterMaker) {
        return (request, ts) -> {
            final Object body = checkNull(request.actualBody());
            checkTypes(body, new Class[] { List.class });
            final List<?> list = (List<?>) body;
            final StringWriter writer = new StringWriter();
            final CSVWriter csvWriter = csvWriterMaker.apply(writer);

            for(Object o : list) {
                final String[] line = (String[]) o;
                csvWriter.writeNext((String[]) o);
            }
            
            stringToStream(writer.toString(), request.actualCharset());
        };
    }

    public static BiConsumer<ChainedHttpConfig.ChainedRequest,ToServer> encode() {
        return encode((w) -> new CSVWriter(w));
    }

    public static BiConsumer<ChainedHttpConfig.ChainedRequest,ToServer> encode(final char separator) {
        return encode((w) -> new CSVWriter(w, separator));
    }

    public static BiConsumer<ChainedHttpConfig.ChainedRequest,ToServer> encode(final char separator, final char quoteChar) {
        return encode((w) -> new CSVWriter(w, separator, quoteChar));
    }
}

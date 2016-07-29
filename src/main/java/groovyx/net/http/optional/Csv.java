package groovyx.net.http.optional;

import groovyx.net.http.*;
import static groovyx.net.http.NativeHandlers.Encoders.*;
import com.opencsv.*;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.io.StringWriter;
import java.util.List;
import java.util.function.Function;
import java.util.function.BiConsumer;

public class Csv {

    public static Function<FromServer,List<String[]>> parse(final Function<Reader,CSVReader> csvReaderMaker) {
        return (fromServer) -> {
            try {
                final Reader reader = new InputStreamReader(fromServer.getInputStream(), fromServer.getCharset());
                final CSVReader csvReader = csvReaderMaker.apply(reader);
                return csvReader.readAll();
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static Function<FromServer,List<String[]>> parse() {
        return parse((r) -> new CSVReader(r));
    }

    public static Function<FromServer,List<String[]>> parse(final char separator) {
        return parse((r) -> new CSVReader(r, separator));
    }

    public static Function<FromServer,List<String[]>> parse(final char separator, final char quoteChar) {
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

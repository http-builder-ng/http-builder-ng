/**
 * Copyright (C) 2017 David Clark
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package groovyx.net.http.optional;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import groovyx.net.http.ChainedHttpConfig;
import groovyx.net.http.FromServer;
import groovyx.net.http.HttpConfig;
import groovyx.net.http.ToServer;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static groovyx.net.http.NativeHandlers.Encoders.*;

public class Csv {

    public static final Supplier<BiConsumer<ChainedHttpConfig,ToServer>> encoderSupplier = () -> Csv::encode;
    public static final Supplier<BiFunction<ChainedHttpConfig,FromServer,Object>> parserSupplier = () -> Csv::parse;
    
    public static class Context {

        public static final String ID = "3DOJ0FPjyD4GwLmpMjrCYnNJK60=";
        public static final Context DEFAULT_CSV = new Context(',');
        public static final Context DEFAULT_TSV = new Context('\t');
        
        private final Character separator;
        private final Character quoteChar;

        public Context(final Character separator) {
            this(separator, null);
        }
        
        public Context(final Character separator, final Character quoteChar) {
            this.separator = separator;
            this.quoteChar = quoteChar;
        }

        public char getSeparator() {
            return separator;
        }

        public boolean hasQuoteChar() {
            return quoteChar != null;
        }
        
        public char getQuoteChar() {
            return quoteChar;
        }

        private CSVReader makeReader(final Reader reader) {
            if(hasQuoteChar()) {
                return new CSVReader(reader, separator, quoteChar);
            }
            else {
                return new CSVReader(reader, separator);
            }
        }

        private CSVWriter makeWriter(final Writer writer) {
            if(hasQuoteChar()) {
                return new CSVWriter(writer, separator, quoteChar);
            }
            else {
                return new CSVWriter(writer, separator);
            }
        }
    }

    public static Object parse(final ChainedHttpConfig config, final FromServer fromServer) {
        try {
            final Csv.Context ctx = (Csv.Context) config.actualContext(fromServer.getContentType(), Csv.Context.ID);
            return ctx.makeReader(fromServer.getReader()).readAll();
        }
        catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void encode(final ChainedHttpConfig config, final ToServer ts) {
        if(handleRawUpload(config, ts)) {
            return;
        }

        final ChainedHttpConfig.ChainedRequest request = config.getChainedRequest();
        final Csv.Context ctx = (Csv.Context) config.actualContext(request.actualContentType(), Csv.Context.ID);
        final Object body = checkNull(request.actualBody());
        checkTypes(body, new Class[] { Iterable.class });
        final StringWriter writer = new StringWriter();
        final CSVWriter csvWriter = ctx.makeWriter(new StringWriter());

        Iterable<?> iterable = (Iterable<?>) body;
        for(Object o : iterable) {
            csvWriter.writeNext((String[]) o);
        }
        
        ts.toServer(stringToStream(writer.toString(), request.actualCharset()));
    }

    public static void toCsv(final HttpConfig delegate, final String contentType,
                             final Character separator, final Character quote) {
        delegate.context(contentType, Context.ID, new Context(separator, quote));
        delegate.getRequest().encoder(contentType, Csv::encode);
        delegate.getResponse().parser(contentType, Csv::parse);
    }
    
    public static void toCsv(final HttpConfig delegate, final char separator) {
        toCsv(delegate, "text/csv", separator, null);
    }

    public static void toCsv(final HttpConfig delegate, final char separator, final char quote) {
        toCsv(delegate, "text/csv", separator, quote);
    }

    public static void toTsv(final HttpConfig delegate, final char quote) {
        toCsv(delegate, "text/tab-separated-values", '\t', quote);
    }
}

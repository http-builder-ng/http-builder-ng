package groovyx.net.http;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public abstract class Header implements Map.Entry<String,String> {

    final String key;
    final String value;
    private Object parsed;
    
    protected static String key(final String raw) {
        return raw.substring(0, raw.indexOf(':')).trim();
    }

    protected static String cleanQuotes(final String str) {
        return str.startsWith("\"") ? str.substring(1, str.length() - 1) : str;
    }
    
    protected static String value(final String raw) {
        return cleanQuotes(raw.substring(raw.indexOf(':') + 1).trim());
    }
    
    public Header(final String key, final String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public String setValue(final String val) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(final Object o) {
        if(!(o instanceof Header)) {
            return false;
        }

        Header other = (Header) o;
        return (Objects.equals(getKey(), other.getKey()) &&
                Objects.equals(getValue(), other.getValue()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKey(), getValue());
    }

    public Object getParsed() {
        if(parsed == null) {
            this.parsed = parse();
        }

        return parsed;
    }

    protected abstract Object parse();

    public static Header header(final String raw) {
        return header(key(raw), value(raw));
    }
    
    public static Header header(String key, String value) {
        final BiFunction<String,String,? extends Header> func = constructors.get(key);
        return func == null ? new ValueOnly(key, value) : func.apply(key, value);
    }

    public static class ValueOnly extends Header {
        public ValueOnly(final String key, final String value) {
            super(key, value);
        }

        public String parse() {
            return getValue();
        }
    }

    public static class CombinedMap extends Header {
        public CombinedMap(final String key, final String value) {
            super(key, value);
        }

        public Map<String,String> parse() {
            Map<String,String> ret = new LinkedHashMap<>();
            final String[] ary = getValue().split(";");
            ret.put(key, cleanQuotes(ary[0].trim()));
            final String[] secondary = ary[1].split("=");
            ret.put(secondary[0].trim(), cleanQuotes(secondary[1].trim()));
            return unmodifiableMap(ret);
        }
    }

    public static class CsvList extends Header {
        public CsvList(final String key, final String value) {
            super(key, value);
        }
        
        public List<String> parse() {
            return unmodifiableList(stream(getValue().split(",")).map(String::trim).collect(toList()));
        }
    }

    public static class HttpDate extends Header {
        public HttpDate(final String key, final String value) {
            super(key, value);
        }

        private boolean isSimpleNumber() {
            for(int i = 0; i < getValue().length(); ++i) {
                if(!Character.isDigit(getValue().charAt(i))) {
                    return false;
                }
            }

            return true;
        }

        public ZonedDateTime parse() {
            if(isSimpleNumber()) {
                return ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(Long.parseLong(getValue()));
            }
            else {
                return ZonedDateTime.parse(getValue(), RFC_1123_DATE_TIME);
            }
        }
    }

    public static class MapPairs extends Header {
        public MapPairs(final String key, final String value) {
            super(key, value);
        }

        public Map<String,String> parse() {
            return stream(getValue().split(";"))
                .map(String::trim)
                .map((str) -> str.split("="))
                .collect(toMap((ary) -> ary[0].trim(),
                               (ary) -> {
                                   if(ary.length == 1) {
                                       return null;
                                   }
                                   else {
                                       return cleanQuotes(ary[1].trim());
                                   }
                               }));
        }
    }

    public static class SingleLong extends Header {
        public SingleLong(final String key, final String value) {
            super(key, value);
        }

        public Long parse() {
            return Long.valueOf(getValue());
        }
    }

    private static final Map<String,BiFunction<String,String,? extends Header>> constructors;
    
    static {
        final Map<String,BiFunction<String,String,? extends Header>> tmp = new LinkedHashMap<>();
        tmp.put("Access-Control-Allow-Origin", ValueOnly::new);
        tmp.put("Accept-Patch", CombinedMap::new);
        tmp.put("Accept-Ranges", ValueOnly::new);
        tmp.put("Age", SingleLong::new);
        tmp.put("Allow", CsvList::new);
        tmp.put("Alt-Svc", MapPairs::new);
        tmp.put("Cache-Control", MapPairs::new);
        tmp.put("Connection", ValueOnly::new);
        tmp.put("Content-Disposition", CombinedMap::new);
        tmp.put("Content-Encoding", ValueOnly::new);
        tmp.put("Content-Language", ValueOnly::new);
        tmp.put("Content-Length", SingleLong::new);
        tmp.put("Content-Location", ValueOnly::new);
        tmp.put("Content-MD5", ValueOnly::new);
        tmp.put("Content-Range", ValueOnly::new);
        tmp.put("Content-Type", CombinedMap::new);
        tmp.put("Date", HttpDate::new);
        tmp.put("ETag", ValueOnly::new);
        tmp.put("Expires", HttpDate::new);
        tmp.put("Last-Modified", HttpDate::new);
        tmp.put("Link", CombinedMap::new);
        tmp.put("Location", ValueOnly::new);
        tmp.put("P3P", MapPairs::new);
        tmp.put("Pragma", ValueOnly::new);
        tmp.put("Proxy-Authenticate", ValueOnly::new);
        tmp.put("Public-Key-Pins", MapPairs::new);
        tmp.put("Refresh", CombinedMap::new);
        tmp.put("Retry-After", HttpDate::new);
        tmp.put("Server", ValueOnly::new);
        tmp.put("Set-Cookie", MapPairs::new);
        tmp.put("Status", ValueOnly::new);
        tmp.put("Strict-Transport-Security", MapPairs::new);
        tmp.put("Trailer", ValueOnly::new);
        tmp.put("Transfer-Encoding", ValueOnly::new);
        tmp.put("TSV", ValueOnly::new);
        tmp.put("Upgrade", CsvList::new);
        tmp.put("Vary", ValueOnly::new);
        tmp.put("Via", CsvList::new);
        tmp.put("Warning", ValueOnly::new);
        tmp.put("WWW-Authenticate", ValueOnly::new);
        tmp.put("X-Frame-Options", ValueOnly::new);
        constructors = unmodifiableMap(tmp);
    }
}

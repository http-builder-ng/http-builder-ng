package groovyx.net.http;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

public interface FromServer {

    public static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    static class Header {
        private final String key;
        private final String value;
        private volatile Map<String,List<String>> keysValues;
        
        public static Header full(final String full) {
            final int pos = full.indexOf(':');
            return new Header(full.substring(0, pos).trim(), cleanValue(full.substring(pos + 1).trim()));
        }

        public static Header keyValue(final String key, final String value) {
            return new Header(key, value);
        }

        public static Header find(final List<Header> headers, final String key) {
            return headers.stream().filter((h) -> h.getKey().equalsIgnoreCase(key)).findFirst().orElse(null);
        }
        
        private Header(final String key, final String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        public boolean isMultiValued() {
            return value.indexOf(';') != -1;
        }

        public static String cleanValue(final String v) {
            if(v.startsWith("\"")) {
                return v.substring(1, v.length() - 1);
            }
            else {
                return v;
            }
        }

        public static void putValue(final Map<String,List<String>> map, final String v) {
            final String[] sub = v.split("=");
            final String subKey = sub[0].trim();
            final String subValue = cleanValue(sub[1].trim());
            if(map.containsKey(subKey)) {
                final List<String> list = new ArrayList(map.get(subKey));
                list.add(subValue);
                map.put(subKey, list);
            }
            else {
                map.put(subKey, Collections.singletonList(subValue));
            }
        }
        
        public Map<String,List<String>> getKeysValues() {
            if(keysValues != null) {
                return keysValues;
            }
            
            if(isMultiValued()) {
                final Map<String,List<String>> tmp = new LinkedHashMap<>();
                final String[] ary = value.split(";");

                if(ary[0].indexOf('=') == -1) {
                    tmp.put(key, Collections.singletonList(ary[0].trim()));
                }
                else {
                    tmp.put(key, Collections.singletonList(""));
                    putValue(tmp, ary[0].replace(key + ":", "").trim());
                }
                
                for(int i = 1; i < ary.length; ++i) {
                    putValue(tmp, ary[i].trim());
                }

                keysValues = Collections.unmodifiableMap(tmp);
            }
            else {
                keysValues = Collections.singletonMap(key, Collections.singletonList(value));
            }

            return keysValues;
        }
    }

    default String getContentType() {
        final Header header = Header.find(getHeaders(), "Content-Type");
        if(header == null) {
            return DEFAULT_CONTENT_TYPE;
        }
        else {
            return header.getKeysValues().get(header.getKey()).get(0);
        }
    }

    default Charset getCharset() {
        final Header header = Header.find(getHeaders(), "Content-Type");
        if(header == null || !header.isMultiValued() ||
           !header.getKeysValues().containsKey("charset") ||
           header.getKeysValues().get("charset").size() == 0) {
            return StandardCharsets.UTF_8;
        }
        else {
            return Charset.forName(header.getKeysValues().get("charset").get(0));
        }
    }

    InputStream getInputStream();
    int getStatusCode();
    String getMessage();
    List<Header> getHeaders();
    boolean getHasBody();
    void finish();
}

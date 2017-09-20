/**
 * Copyright (C) 2017 HttpBuilder-NG Project
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
package groovyx.net.http;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpCookie;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiFunction;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * Adapter interface used to provide a bridge for response data between the {@link HttpBuilder} API and the underlying client implementation.
 */
public interface FromServer {

    public static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    /**
     * Defines the interface to the HTTP headers contained in the response. (see also
     * https://en.wikipedia.org/wiki/List_of_HTTP_header_fields[List of HTTP Header Fields])
     */
    public static abstract class Header<T> implements Map.Entry<String, String> {

        final String key;
        final String value;
        private T parsed;

        protected static String key(final String raw) {
            return raw.substring(0, raw.indexOf(':')).trim();
        }

        protected static String cleanQuotes(final String str) {
            return str.startsWith("\"") ? str.substring(1, str.length() - 1) : str;
        }

        protected static String value(final String raw) {
            return cleanQuotes(raw.substring(raw.indexOf(':') + 1).trim());
        }

        protected Header(final String key, final String value) {
            this.key = key;
            this.value = value;
        }

        /**
         * Retrieves the header `key`.
         *
         * @return the header key
         */
        public String getKey() {
            return key;
        }

        /**
         * Retrieves the header `value`.
         *
         * @return the header value
         */
        public String getValue() {
            return value;
        }

        /**
         * Unsupported, headers are read-only.
         *
         * @throws UnsupportedOperationException always
         */
        public String setValue(final String val) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof Header)) {
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

        @Override
        public String toString() {
            return key + ": " + value;
        }

        /**
         * Retrieves the parsed representation of the 'value`. The type of
         * the returned `Object` depends on the header and will be given
         * by the `getParsedType()` property. 
         *
         * @return the parsed header value
         */
        public T getParsed() {
            if (parsed == null) {
                this.parsed = parse();
            }

            return parsed;
        }

        /**
         * Retrieves the type of the parsed representation of the 'value`.
         *
         * @return the parsed header value type
         */
        public abstract Class<?> getParsedType();

        /**
         * Performs the parse of the `value`
         *
         * @return the parsed header value
         */
        protected abstract T parse();

        /**
         * Creates a `Header` from a full header string. The string format is colon-delimited as `KEY:VALUE`.
         *
         * [source,groovy]
         * ----
         * Header header = Header.full('Content-Type:text/plain')
         * assert header.key == 'Content-Type'
         * assert header.value == 'text/plain'
         * ----
         *
         * @param raw the full header string
         * @return the `Header` representing the given header string
         */
        public static Header<?> full(final String raw) {
            return keyValue(key(raw), value(raw));
        }

        /**
         * Creates a `Header` from a given `key` and `value`.
         *
         * @param key the header key
         * @param value the header value
         * @return the populated `Header`
         */
        public static Header<?> keyValue(String key, String value) {
            final BiFunction<String, String, ? extends Header> func = constructors.get(key);
            return func == null ? new ValueOnly(key, value) : func.apply(key, value);
        }

        /**
         * Used to find a specific `Header` by key from a {@link Collection} of `Header`s.
         *
         * @param headers the {@link Collection} of `Header`s to be searched
         * @param key the key of the desired `Header`
         * @return the `Header` with the matching key (or `null`)
         */
        public static Header<?> find(final Collection<Header<?>> headers, final String key) {
            return headers.stream().filter((h) -> h.getKey().equalsIgnoreCase(key)).findFirst().orElse(null);
        }

        /**
         * Type representing headers that are simple key/values, with no parseable structure in the value. For example: `Accept-Ranges: bytes`.
         */
        public static class ValueOnly extends Header<String> {
            public ValueOnly(final String key, final String value) {
                super(key, value);
            }

            public String parse() {
                return getValue();
            }

            /**
             * Always returns {@link String}
             *
             * @return the parsed header type
             */
            public Class<?> getParsedType() {
                return String.class;
            }
        }

        /**
         * Type representing headers that have values which are parseable as key/value pairs,
         * provided the header hey is included in the key/value map.
         * For example: `Content-Type: text/html; charset=utf-8`
         */
        public static class CombinedMap extends Header<Map<String, String>> {
            public CombinedMap(final String key, final String value) {
                super(key, value);
            }

            public Map<String, String> parse() {
                Map<String, String> ret = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                final String[] ary = getValue().split(";");
                ret.put(key, cleanQuotes(ary[0].trim()));
                if (ary.length > 1) {
                    final String[] secondary = ary[1].split("=");
                    ret.put(secondary[0].trim(), cleanQuotes(secondary[1].trim()));
                }

                return unmodifiableMap(ret);
            }

            /**
             * Always returns {@link List}
             *
             * @return the parsed header type
             */
            public Class<?> getParsedType() {
                return Map.class;
            }
        }

        /**
         * Type representing headers that have values which are comma separated lists.
         * For example: `Allow: GET, HEAD`
         */
        public static class CsvList extends Header<List<String>> {
            public CsvList(final String key, final String value) {
                super(key, value);
            }

            public List<String> parse() {
                return unmodifiableList(stream(getValue().split(",")).map(String::trim).collect(toList()));
            }

            public Class<?> getParsedType() {
                return List.class;
            }
        }

        /**
         * Type representing headers that have values which are zoned date time values.
         * Values representing seconds from now are also converted to zoned date time values
         * with UTC/GMT zone offsets.
         *
         * * Example 1: `Retry-After: Fri, 07 Nov 2014 23:59:59 GMT`
         * * Example 2: `Retry-After: 120`
         */
        public static class HttpDate extends Header<ZonedDateTime> {
            public HttpDate(final String key, final String value) {
                super(key, value);
            }

            private boolean isSimpleNumber() {
                for (int i = 0; i < getValue().length(); ++i) {
                    if (!Character.isDigit(getValue().charAt(i))) {
                        return false;
                    }
                }

                return true;
            }

            /**
             * Always returns {@link ZonedDateTime}
             *
             * @return the parsed header type
             */
            public ZonedDateTime parse() {
                if (isSimpleNumber()) {
                    return ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(Long.parseLong(getValue()));
                } else {
                    return parse(RFC_1123_DATE_TIME);
                }
            }

            /**
             * Retrieves the {@link ZonedDateTime} value of the header using the provided {@link DateTimeFormatter}.
             *
             * @param formatter the formatter to be used
             * @return
             */
            public ZonedDateTime parse(final DateTimeFormatter formatter) {
                return ZonedDateTime.parse(getValue(), formatter);
            }

            public Class<?> getParsedType() {
                return ZonedDateTime.class;
            }
        }

        /**
         * Type representing headers that have values which are parseable as key/value pairs.
         * For example: `Alt-Svc: h2="http2.example.com:443"; ma=7200`
         */
        public static class MapPairs extends Header<Map<String, String>> {
            public MapPairs(final String key, final String value) {
                super(key, value);
            }

            public Map<String, String> parse() {
                return stream(getValue().split(";"))
                    .map(String::trim)
                    .map((str) -> str.split("="))
                    .collect(toMap((ary) -> ary[0].trim(),
                        (ary) -> {
                            if (ary.length == 1) {
                                return ary[0];
                            } else {
                                return cleanQuotes(ary[1].trim());
                            }
                        },
                        (oldVal, newVal) -> newVal,
                        () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER)));
            }

            /**
             * Always returns {@link Map}
             *
             * @return the parsed header type
             */
            public Class<?> getParsedType() {
                return Map.class;
            }
        }

        /**
         * Type representing headers that have values which are parseable as longs.
         * For example: `Content-Length: 348`
         */
        public static class SingleLong extends Header<Long> {
            public SingleLong(final String key, final String value) {
                super(key, value);
            }

            public Long parse() {
                return Long.valueOf(getValue());
            }

            /**
             * Always returns {@link Long}
             *
             * @return the parsed header type
             */
            public Class<?> getParsedType() {
                return Long.class;
            }
        }

        public static class HttpCookies extends Header<List<HttpCookie>> {
            public HttpCookies(final String key, final String value) {
                super(key, value);
            }

            public List<HttpCookie> parse() {
                return HttpCookie.parse(key + ": " + value);
            }

            public Class<?> getParsedType() {
                return List.class;
            }
        }

        private static final Map<String, BiFunction<String, String, ? extends Header>> constructors;

        static {
            final Map<String, BiFunction<String, String, ? extends Header>> tmp = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
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
            tmp.put("Set-Cookie", HttpCookies::new);
            tmp.put("Set-Cookie2", HttpCookies::new);
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

    /**
     * Retrieves the value of the "Content-Type" header from the response.
     *
     * @return the value of the "Content-Type" response header
     */
    default String getContentType() {
        final Header.CombinedMap header = (Header.CombinedMap) Header.find(getHeaders(), "Content-Type");
        if (header == null) {
            return DEFAULT_CONTENT_TYPE;
        } else {
            return header.getParsed().get("Content-Type");
        }
    }

    /**
     * Retrieves the value of the charset from the "Content-Type" response header.
     *
     * @return the value of the charset from the "Content-Type" response header
     */
    default Charset getCharset() {
        final Header.CombinedMap header = (Header.CombinedMap) Header.find(getHeaders(), "Content-Type");
        if (header == null) {
            return StandardCharsets.UTF_8;
        }

        if (header.getParsed().containsKey("charset")) {
            Charset.forName(header.getParsed().get("charset"));
        }

        return StandardCharsets.UTF_8;
    }

    default List<HttpCookie> getCookies() {
        return HttpBuilder.cookies(getHeaders());
    }

    /**
     * Retrieves the {@link InputStream} containing the response content (may have already been processed).
     *
     * @return the response content
     */
    InputStream getInputStream();

    /**
     * Retrieves the response status code (https://en.wikipedia.org/wiki/List_of_HTTP_status_codes[List of HTTP status code]).
     *
     * @return the response status code
     */
    int getStatusCode();

    /**
     * Retrieves the response status message.
     *
     * @return the response status message (or null)
     */
    String getMessage();

    /**
     * Retrieves a {@link List} of the response headers as ({@link Header} objects).
     *
     * @return a {@link List} of response headers
     */
    List<Header<?>> getHeaders();

    /**
     * Determines whether or not there is body content in the response.
     *
     * @return true if there is body content in the response
     */
    boolean getHasBody();

    /**
     * Retrieves the {@link URI} of the original request.
     *
     * @return the {@link URI} of the original request
     */
    URI getUri();

    /**
     * Performs any client-specific response finishing operations.
     */
    void finish();

    /**
     * Retrieves a {@link Reader} for the response body content (if there is any). The content may have already been processed.
     *
     * @return a {@link Reader} for the response body content (may be empty)
     */
    default Reader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }
}

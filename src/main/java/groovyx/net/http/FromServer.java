/**
 * Copyright (C) 2016 David Clark
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
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Adapter interface used to provide a bridge for response data between the {@link HttpBuilder} API and the underlying client implementation.
 */
public interface FromServer {

    public static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    /**
     * Defines the interface to the HTTP headers contained in the response. (see also
     * https://en.wikipedia.org/wiki/List_of_HTTP_header_fields[List of HTTP Header Fields])
     */
    static class Header {

        private final String key;
        private final String value;
        private volatile Map<String,List<String>> keysValues;

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
         * @param full the full header string
         * @return the `Header` representing the given header string
         */
        public static Header full(final String full) {
            final int pos = full.indexOf(':');
            return new Header(full.substring(0, pos).trim(), cleanValue(full.substring(pos + 1).trim()));
        }

        /**
         * Creates a `Header` from a given `key` and `value`.
         *
         * @param key the header key
         * @param value the header value
         * @return the populated `Header`
         */
        public static Header keyValue(final String key, final String value) {
            return new Header(key, value);
        }

        /**
         * Used to find a specific `Header` by key from a {@link List} of `Header`s.
         *
         * @param headers the {@link List} of `Header`s to be searched
         * @param key the key of the desired `Header`
         * @return the `Header` with the matching key (or `null`)
         */
        public static Header find(final List<Header> headers, final String key) {
            return headers.stream().filter((h) -> h.getKey().equalsIgnoreCase(key)).findFirst().orElse(null);
        }
        
        private Header(final String key, final String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            return key + ": " + value;
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
         * Used to determine whether or not a `Header` is multi-valued.
         *
         * FIXME: more details about what it means to be multi-valued (what string looks like)
         *
         * @return true if the header is multi-valued
         */
        public boolean isMultiValued() {
            return value.indexOf(';') != -1;
        }

        /**
         * FIXME: document
         */
        public static String cleanValue(final String v) {
            return v.startsWith("\"") ? v.substring(1, v.length() - 1) : v;
        }

        /**
         * FIXME: document
         */
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

        /**
         * FIXME: document - see `FromServerSpec:'Header.keysValues'` for questions about this method. Not sure it works.
         */
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

    /**
     * Retrieves the value of the "Content-Type" header from the response.
     *
     * @return the value of the "Content-Type" response header
     */
    default String getContentType() {
        final Header header = Header.find(getHeaders(), "Content-Type");
        return header == null ? DEFAULT_CONTENT_TYPE : header.getKeysValues().get(header.getKey()).get(0);
    }

    /**
     * Retrieves the value of the charset from the "Content-Type" response header.
     *
     * @return the value of the charset from the "Content-Type" response header
     */
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

    /**
     * Retrieves the {@link InputStream} containing the response content.
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
    List<Header> getHeaders();

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
     * FIXME: document
     */
    void finish();

    /**
     * Retrieves a {@link Reader} for the response body content (if there is any).
     *
     * @return a {@link Reader} for the response body content (may be empty)
     */
    default Reader getReader() {
        // TODO: verify what this does when there is no body content
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }
}

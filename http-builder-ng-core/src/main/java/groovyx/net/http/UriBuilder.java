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

import java.io.IOException;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static groovyx.net.http.Traverser.notValue;
import static groovyx.net.http.Traverser.traverse;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;

/**
 * Provides a simple means of creating a request URI and optionally overriding its parts.
 *
 * [source,groovy]
 * ----
 * def uri = UriBuilder.basic(UriBuilder.root())
 *      .setFull('http://localhost:10101')
 *      .setPath('/foo')
 *      .toURI()
 * ----
 *
 * Generally, this class is not instantiated directly, but created by the {@link HttpConfig} instance and modified.
 */
public abstract class UriBuilder {

    public static final int DEFAULT_PORT = -1;

    /**
     * Sets the scheme part of the URI.
     *
     * @param val the value to use as the scheme part of the URI
     * @return a reference to this builder
     */
    public abstract UriBuilder setScheme(String val);

    /**
     * Retrieves the scheme part of the URI.
     *
     * @return the URI scheme
     */
    public abstract String getScheme();

    /**
     * Sets the port part of the URI.
     *
     * @param val the value to use as the port part of the URI
     * @return a reference to this builder
     */
    public abstract UriBuilder setPort(int val);

    /**
     * Retrieves the port part of the URI.
     *
     * @return the port part of the URI
     */
    public abstract int getPort();

    /**
     * Sets the host part of the URI.
     *
     * @param val the value to use as the host part of the URI
     * @return a reference to this builder
     */
    public abstract UriBuilder setHost(String val);

    /**
     * Retrieves the host part of the URI.
     *
     * @return the host part of the URI
     */
    public abstract String getHost();

    /**
     * Retrieves the path part of the URI.
     *
     * @return the path part of the URI
     */
    public abstract String getPath();

    /**
     * Sets the query string part of the `URI` from the provided map. The query string key-value pairs will be generated from the key-value pairs
     * of the map and are NOT URL-encoded. Nested maps or other data structures are not supported.
     *
     * @param val the map of query string parameters
     * @return a reference to the builder
     */
    public abstract UriBuilder setQuery(Map<String, ?> val);

    /**
     * Retrieves the `Map` of query string parameters for the `URI`.
     *
     * @return the `Map` of query string parameters for the `URI`.
     */
    public abstract Map<String, ?> getQuery();

    /**
     * Sets the fragment part of the `URI`.
     *
     * @param val the fragment part of the `URI`
     * @return a reference to the builder
     */
    public abstract UriBuilder setFragment(String val);

    /**
     * Retrieves the fragment part of the `URI`.
     *
     * @return the fragment part of the `URI`
     */
    public abstract String getFragment();

    /**
     * Sets the user info part of the `URI`.
     *
     * @param val the user info part of the `URI`
     * @return a reference to the builder
     */
    public abstract UriBuilder setUserInfo(String val);

    /**
     * Retrieves the user info part of the `URI`.
     *
     * @return the user info part of the `URI`
     */
    public abstract String getUserInfo();

    public abstract UriBuilder getParent();

    /**
     * Sets the path part of the URI.
     *
     * @param str the path part of the URI
     * @return a reference to the builder
     */
    public UriBuilder setPath(final String str) {
        return setPath(str == null ? "" : str);
    }

    public URI forCookie(final HttpCookie cookie) throws URISyntaxException {
        final String scheme = traverse(this, UriBuilder::getParent, UriBuilder::getScheme, Traverser::notNull);
        final String userInfo = null;
        final String host = traverse(this, UriBuilder::getParent, UriBuilder::getHost, Traverser::notNull);
        final Integer port = traverse(this, UriBuilder::getParent, UriBuilder::getPort, notValue(DEFAULT_PORT));
        final String path = cookie.getPath();
        final String query = null;
        final String fragment = null;

        final String uri = getURIAsString(
                scheme,
                userInfo,
                host,
                port,
                path,
                query,
                fragment
        );

        return new URI(uri);
//        return new URI(scheme, userInfo, host, (port == null ? -1 : port), path, query, fragment);
    }

    /**
     * Converts the parts of the `UriBuilder` to the `URI` object instance.
     *
     * @return the generated `URI` representing the parts contained in the builder
     */
    public URI toURI() throws URISyntaxException {
        final String scheme = traverse(this, UriBuilder::getParent, UriBuilder::getScheme, Traverser::notNull);
        final String userInfo = traverse(this, UriBuilder::getParent, UriBuilder::getUserInfo, Traverser::notNull);
        final String host = traverse(this, UriBuilder::getParent, UriBuilder::getHost, Traverser::notNull);
        final Integer port = traverse(this, UriBuilder::getParent, UriBuilder::getPort, notValue(DEFAULT_PORT));
        final String path = traverse(this, UriBuilder::getParent, UriBuilder::getPath, Traverser::notNull);
        final String query = populateQueryString(traverse(this, UriBuilder::getParent, UriBuilder::getQuery, Traverser::nonEmptyMap));
        final String fragment = traverse(this, UriBuilder::getParent, UriBuilder::getFragment, Traverser::notNull);

        // <scheme>://[<userInfo@]<host>:<port><path>?<query>#<fragment>
        final String uri = getURIAsString(
                scheme,
                userInfo,
                host,
                port,
                path,
                query,
                fragment
        );

        return new URI(uri);
//        return new URI(scheme, userInfo, host, (port == null ? -1 : port), ((path == null) ? null : path.toString()), query, fragment);
    }

    /**
     * Given the individual `URI` elements, construct a literal string representation of the `URI` that can be used to
     * call {@link java.net.URI#URI(String)}.
     *
     * @param scheme
     * @param userInfo
     * @param host
     * @param port
     * @param path
     * @param query
     * @param fragment
     * @return
     */
    private String getURIAsString(String scheme, String userInfo, String host, Integer port, String path, String query, String fragment ) {
        if (scheme == null) {
            scheme = "";
        } else if (!scheme.endsWith("://")) {
            scheme += "://";
        }

        String portStr = port == null ? "" : port.toString();

        if (userInfo == null) {
            userInfo = "";
        } else if (!userInfo.endsWith("@")) {
            userInfo += "@";
        }

        if (host == null) {
            host = "";
        }

        if (!portStr.isEmpty()) {
            portStr = ":" + portStr;
        }

        if (path == null) {
            path = "";
        } else if (!path.startsWith("/") && !path.isEmpty()) {
            path = "/" + path;
        }

        if (query == null) {
            query = "";
        } else if ( !query.startsWith("?")) {
            query = "?" + query;
        }

        if (fragment == null) {
            fragment = "";
        } else if ( !fragment.startsWith("#")) {
            fragment = "#" + fragment;
        }

        String uri = String.format("%s%s%s%s%s%s%s",
                scheme,
                userInfo,
                host,
                portStr,
                path,
                query,
                fragment
        );

        return uri;
    }

    private static final Object[] EMPTY = new Object[0];

    private static String populateQueryString(final Map<String, ?> queryMap) {
        if (queryMap == null || queryMap.isEmpty()) {
            return null;

        } else {
            final List<String> nvps = new LinkedList<>();

            queryMap.entrySet().forEach((Consumer<Map.Entry<String, ?>>) entry -> {
                final Collection<?> values = entry.getValue() instanceof Collection ? (Collection<?>) entry.getValue() : singletonList(entry.getValue().toString());
                values.forEach(value -> {
                    nvps.add(entry.getKey() + "=" + value);
                });
            });

            return nvps.stream().collect(Collectors.joining("&"));
        }
    }

    protected final void populateFrom(final URI uri) {
        try {
            setScheme(uri.getScheme());
            setPort(uri.getPort());
            setHost(uri.getHost());

            final String path = uri.getRawPath();
            if (path != null) {
                setPath(path);
            }

            final String rawQuery = uri.getRawQuery();
            if (rawQuery != null) {
                setQuery(Form.decode(new StringBuilder(rawQuery), UTF_8));
            }

            setFragment(uri.getRawFragment());
            setUserInfo(uri.getRawUserInfo());
        }
        catch (IOException e) {
            //this seems o.k. to just convert to a runtime exception,
            //we started with a valid URI, so this should never happen.
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the full URI (all parts) as a String.
     *
     * @param str the full URI to be used by the `UriBuilder`
     * @return a reference to the builder
     * @throws IllegalArgumentException if there is a problem with the URI syntax
     */
    public final UriBuilder setFull(final String str) {
        try {
            return setFull(new URI(str));
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex.getMessage());
        }
    }

    /**
     * Sets the full URI (all parts) as a URI object.
     *
     * @param uri the full URI to be used by the `UriBuilder`
     * @return a reference to the builder
     */
    public final UriBuilder setFull(final URI uri) {
        populateFrom(uri);
        return this;
    }

    /**
     * Creates a basic `UriBuilder` from the provided parent builder. An empty `UriBuilder` may be created using the `root()` method as the `parent` value,
     * otherwise a new `UriBuilder` may be created from an existing builder:
     *
     * [source,groovy]
     * ----
     * def parent = UriBuilder.basic(UriBuilder.root()).setFull('http://localhost:10101/foo')
     * def child = UriBuilder.basic(parent)
     * child.setPath('/bar').toURI() == new URI('http://localhost:10101/bar')
     * ----
     *
     * The `UriBuilder` implementation generated with this method is _not_ thread-safe.
     *
     * @param parent the `UriBuilder` parent
     * @return the created `UriBuilder`
     */
    public static UriBuilder basic(final UriBuilder parent) {
        return new Basic(parent);
    }

    /**
     * Creates a thread-safe `UriBuilder` from the provided parent builder. An empty `UriBuilder` may be created using the `root()` method as the
     * `parent` value, otherwise a new `UriBuilder` may be created from an existing builder:
     *
     * [source,groovy]
     * ----
     * def parent = UriBuilder.threadSafe(UriBuilder.root()).setFull('http://localhost:10101/foo')
     * def child = UriBuilder.threadSafe(parent)
     * child.setPath('/bar').toURI() == new URI('http://localhost:10101/bar')
     * ----
     *
     * The `UriBuilder` implementation generated with this method is thread-safe.
     *
     * @param parent the `UriBuilder` parent
     * @return the created `UriBuilder`
     */
    public static UriBuilder threadSafe(final UriBuilder parent) {
        return new ThreadSafe(parent);
    }

    public static UriBuilder root() {
        return new ThreadSafe(null);
    }

    private static final class Basic extends UriBuilder {
        private String scheme;

        public UriBuilder setScheme(String val) {
            scheme = val;
            return this;
        }

        public String getScheme() {
            return scheme;
        }

        private int port = DEFAULT_PORT;

        public UriBuilder setPort(int val) {
            port = val;
            return this;
        }

        public int getPort() {
            return port;
        }

        private String host;

        public UriBuilder setHost(String val) {
            host = val;
            return this;
        }

        public String getHost() {
            return host;
        }

        private String path;

        public UriBuilder setPath(String val) {
            path = val;
            return this;
        }

        public String getPath() {
            return path;
        }

        private Map<String, Object> query = new LinkedHashMap<>(1);

        public UriBuilder setQuery(final Map<String, ?> val) {
            if(val != null) {
                query.putAll(val);
            }
            return this;
        }

        public Map<String, Object> getQuery() {
            return query;
        }

        private String fragment;

        public UriBuilder setFragment(String val) {
            fragment = val;
            return this;
        }

        public String getFragment() {
            return fragment;
        }

        private String userInfo;

        public UriBuilder setUserInfo(String val) {
            userInfo = val;
            return this;
        }

        public String getUserInfo() {
            return userInfo;
        }

        private final UriBuilder parent;

        public UriBuilder getParent() {
            return parent;
        }

        public Basic(final UriBuilder parent) {
            this.parent = parent;
        }
    }

    private static final class ThreadSafe extends UriBuilder {
        private volatile String scheme;

        public UriBuilder setScheme(String val) {
            scheme = val;
            return this;
        }

        public String getScheme() {
            return scheme;
        }

        private volatile int port = DEFAULT_PORT;

        public UriBuilder setPort(int val) {
            port = val;
            return this;
        }

        public int getPort() {
            return port;
        }

        private volatile String host;

        public UriBuilder setHost(String val) {
            host = val;
            return this;
        }

        public String getHost() {
            return host;
        }

        private volatile String path;

        public UriBuilder setPath(String val) {
            path = val;
            return this;
        }

        public String getPath() {
            return path;
        }

        private Map<String, Object> query = new ConcurrentHashMap<>();

        public UriBuilder setQuery(Map<String, ?> val) {
            query.putAll(val);
            return this;
        }

        public Map<String, ?> getQuery() {
            return query;
        }

        private volatile String fragment;

        public UriBuilder setFragment(String val) {
            fragment = val;
            return this;
        }

        public String getFragment() {
            return fragment;
        }

        private volatile String userInfo;

        public UriBuilder setUserInfo(String val) {
            userInfo = val;
            return this;
        }

        public String getUserInfo() {
            return userInfo;
        }

        private final UriBuilder parent;

        public UriBuilder getParent() {
            return parent;
        }

        public ThreadSafe(final UriBuilder parent) {
            this.parent = parent;
        }
    }
}

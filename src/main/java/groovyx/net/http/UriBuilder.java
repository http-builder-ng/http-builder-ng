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

import groovy.lang.GString;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.charset.StandardCharsets;
import static groovyx.net.http.Traverser.*;
import org.codehaus.groovy.runtime.GStringImpl;
import java.io.IOException;

public abstract class UriBuilder {

    public static final int DEFAULT_PORT = -1;

    public abstract UriBuilder setScheme(String val);
    public abstract String getScheme();
    public abstract UriBuilder setPort(int val);
    public abstract int getPort();
    public abstract UriBuilder setHost(String val);
    public abstract String getHost();
    public abstract UriBuilder setPath(GString val);
    public abstract GString getPath();
    public abstract UriBuilder setQuery(Map<String,?> val);
    public abstract Map<String,?> getQuery();
    public abstract UriBuilder setFragment(String val);
    public abstract String getFragment();
    public abstract UriBuilder setUserInfo(String val);
    public abstract String getUserInfo();
    public abstract UriBuilder getParent();

    public UriBuilder setPath(final String str) {
        return setPath(new GStringImpl(EMPTY, new String[] { str }));
    }
    
    public URI toURI() {
        try {
            final String scheme = traverse(this, (u) -> u.getParent(), (u) -> u.getScheme(), Traverser::notNull);
            final Integer port = traverse(this, (u) -> u.getParent(), (u) -> u.getPort(), notValue(DEFAULT_PORT));
            final String host = traverse(this, (u) -> u.getParent(), (u) -> u.getHost(), Traverser::notNull);
            final GString path = traverse(this, (u) -> u.getParent(), (u) -> u.getPath(), Traverser::notNull);
            final Map<String,?> queryMap = traverse(this, (u) -> u.getParent(), (u) -> u.getQuery(), Traverser::notNull);
            final String query = (queryMap == null || queryMap.isEmpty()) ? null : Form.encode(queryMap, StandardCharsets.UTF_8);
            final String fragment = traverse(this, (u) -> u.getParent(), (u) -> u.getFragment(), Traverser::notNull);
            final String userInfo = traverse(this, (u) -> u.getParent(), (u) -> u.getUserInfo(), Traverser::notNull);
            return new URI(scheme, userInfo, host, (port == null ? -1 : port), ((path == null) ? null : path.toString()), query, fragment);
        }
        catch(URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static final Object[] EMPTY = new Object[0];
    
    protected final void populateFrom(final URI uri) {
        try {
            setScheme(uri.getScheme());
            setPort(uri.getPort());
            setHost(uri.getHost());
            
            final String path = uri.getPath();
            if(path != null) {
                setPath(new GStringImpl(EMPTY, new String[] { path }));
            }
            
            final String rawQuery = uri.getQuery();
            if(rawQuery != null) {
                setQuery(Form.decode(new StringBuilder(rawQuery), StandardCharsets.UTF_8));
            }
            
            setFragment(uri.getFragment());
            setUserInfo(uri.getUserInfo());
        }
        catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    public final UriBuilder setFull(final String str) throws URISyntaxException {
        return setFull(new URI(str));
    }

    public final UriBuilder setFull(final URI uri) {
        populateFrom(uri);
        return this;
    }

    public static UriBuilder basic(final UriBuilder parent) {
        return new Basic(parent);
    }

    public static UriBuilder threadSafe(final UriBuilder parent) {
        return new ThreadSafe(parent);
    }

    public static UriBuilder root() {
        return new ThreadSafe(null);
    }

    private static final class Basic extends UriBuilder {
        private String scheme;
        public UriBuilder setScheme(String val) { scheme = val; return this; }
        public String getScheme() { return scheme; }

        private int port = DEFAULT_PORT;
        public UriBuilder setPort(int val) { port = val; return this; }
        public int getPort() { return port; }

        private String host;
        public UriBuilder setHost(String val) { host = val; return this; }
        public String getHost() { return host; }

        private GString path;
        public UriBuilder setPath(GString val) { path = val; return this; }
        public GString getPath() { return path; }
        
        private Map<String,Object> query = new LinkedHashMap<>(1);
        public UriBuilder setQuery(Map<String,?> val) { query.putAll(val);; return this; }
        public Map<String,Object> getQuery() { return query; }

        private String fragment;
        public UriBuilder setFragment(String val) { fragment = val; return this; }
        public String getFragment() { return fragment; }

        private String userInfo;
        public UriBuilder setUserInfo(String val) { userInfo = val; return this; }
        public String getUserInfo() { return userInfo; }

        private final UriBuilder parent;
        public UriBuilder getParent() { return parent; }

        public Basic(final UriBuilder parent) {
            this.parent = parent;
        }
    }

    private static final class ThreadSafe extends UriBuilder {
        private volatile String scheme;
        public UriBuilder setScheme(String val) { scheme = val; return this; }
        public String getScheme() { return scheme; }

        private volatile int port = DEFAULT_PORT;
        public UriBuilder setPort(int val) { port = val; return this; }
        public int getPort() { return port; }

        private volatile String host;
        public UriBuilder setHost(String val) { host = val; return this; }
        public String getHost() { return host; }

        private volatile GString path;
        public UriBuilder setPath(GString val) { path = val; return this; }
        public GString getPath() { return path; }

        private Map<String,Object> query = new ConcurrentHashMap();
        public UriBuilder setQuery(Map<String,?> val) { query.putAll(val); return this; }
        public Map<String,?> getQuery() { return query; }

        private volatile String fragment;
        public UriBuilder setFragment(String val) { fragment = val; return this; }
        public String getFragment() { return fragment; }

        private volatile String userInfo;
        public UriBuilder setUserInfo(String val) { userInfo = val; return this; }
        public String getUserInfo() { return userInfo; }

        private final UriBuilder parent;
        public UriBuilder getParent() { return parent; }

        public ThreadSafe(final UriBuilder parent) {
            this.parent = parent;
        }
    }
}

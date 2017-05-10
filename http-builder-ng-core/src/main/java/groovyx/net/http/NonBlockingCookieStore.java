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

import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

class NonBlockingCookieStore implements CookieStore {

    //public cookie store api
    public void add(final URI uri, final HttpCookie cookie) {
        if(cookie.getMaxAge() == 0) {
            return;
        }

        if(cookie.getDomain() != null) {
            add(new DomainKey(cookie), cookie);
        }
        
        if(uri != null) {
            add(new UriKey(uri, cookie), cookie);
        }
    }

    public List<HttpCookie> get(final URI uri) {
        List<HttpCookie> ret = (all.entrySet()
                                .stream()
                                .filter(entry -> entryValid(entry) && matches(entry, uri))
                                .map(Map.Entry::getValue)
                                .distinct()
                                .collect(Collectors.toList()));
        return ret;
    }

    public List<HttpCookie> getCookies() {
        return (all.entrySet()
                .stream()
                .filter(this::entryValid)
                .map(Map.Entry::getValue)
                .collect(Collectors.toList()));
    }

    public List<URI> getURIs() {
        return (all.entrySet()
                .stream()
                .filter(entry -> entry.getKey() instanceof UriKey)
                .filter(this::entryValid)
                .map(entry -> ((UriKey) entry.getKey()).getURI())
                .distinct()
                .collect(Collectors.toList()));
    }

    public boolean remove(final URI uri, final HttpCookie cookie) {
        boolean domainRemoved = false;
        boolean uriRemoved = false;
        
        if(cookie.getDomain() != null) {
            domainRemoved = remove(new DomainKey(cookie));
        }

        if(uri != null) {
            uriRemoved = remove(new UriKey(uri, cookie));
        }

        return domainRemoved || uriRemoved;
    }

    public boolean removeAll() {
        int initialSize = all.size();
        all.clear();
        return initialSize > 0;
    }

    protected abstract static class Key {
        final String name;
        final Instant createdAt;

        public Key(final String name) {
            this.name = name;
            this.createdAt = Instant.now();
        }

        abstract public String getKeyType();

        public static boolean specified(final String val) {
            return (val != null && !"".equals(val.trim()));
        }
        
        static Key make(final URI uri, final HttpCookie cookie) {
            if(!specified(cookie.getDomain())) {
                return new UriKey(uri, cookie);
            }
            else {
                return new DomainKey(cookie);
            }
        }

        public static String forStorage(final String str) {
            return str == null ? str : str.toLowerCase();
        }
    }

    protected static class UriKey extends Key {
        public static final String TYPE = "uri";
        
        final String host;

        public UriKey(final URI uri, final HttpCookie cookie) {
            super(cookie.getName());
            this.host = forStorage(uri.getHost());
        }

        public static boolean uriKey(final String type) {
            return TYPE.equals(type);
        }

        public String getKeyType() {
            return TYPE;
        }

        public URI getURI() {
            try {
                return new URI("http", host, null, null);
            }
            catch(URISyntaxException e) {
                //it's safe to ignore this, host already came from a valid
                //uri, so constructing a new one from the host is always valid
                return null;
            }
        }

        @Override
        public int hashCode() {
            return 37 * name.hashCode() + host.hashCode();
        }

        @Override
        public boolean equals(final Object o) {
            if(!(o instanceof UriKey)) {
                return false;
            }

            final UriKey rhs = (UriKey) o;
            return host.equals(rhs.host) && name.equals(rhs.name);
        }

        @Override
        public String toString() {
            return String.format("UriKey(name: %s, host: %s)", name, host);
        }
    }

    protected static class DomainKey extends Key {
        public static final String TYPE = "domain";
        
        final String domain;
        final String path;
                
        public DomainKey(final HttpCookie cookie) {
            super(cookie.getName());
            this.domain = cookie.getDomain();
            this.path = cookie.getPath();
        }

        public static boolean domainKey(final String type) {
            return TYPE.equals(type);
        }
        
        public String getKeyType() {
            return TYPE;
        }

        private boolean pathEquals(final DomainKey rhs) {
            if(path == null && rhs.path == null) {
                return true;
            }
            else if(path == null && rhs.path != null) {
                return false;
            }
            else if(path != null && rhs.path == null) {
                return false;
            }
            else {
                return path.equalsIgnoreCase(rhs.path);
            }
        }
        
        @Override
        public boolean equals(final Object o) {
            if(!(o instanceof DomainKey)) {
                return false;
            }
            
            final DomainKey rhs = (DomainKey) o;
            return (name.equalsIgnoreCase(rhs.name) &&
                    domain.equalsIgnoreCase(rhs.domain) &&
                    pathEquals(rhs));
        }

        @Override
        public int hashCode() {
            return 37 * (37 * name.hashCode() + domain.hashCode()) + (path == null ? 0 : path.hashCode());
        }

        @Override
        public String toString() {
            return String.format("DomainKey(name: %s, domain: %s, path: %s", name, domain, path);
        }
    }

    protected ConcurrentMap<Key,HttpCookie> all = new ConcurrentHashMap<>(100, 0.75f, 2);

    private static URI makeURI(final String domain) {
        try {
            return new URI("http", domain, null, null, null);
        }
        catch(URISyntaxException ex) {
            return null;
        }
    }

    public boolean entryValid(final Map.Entry<Key,HttpCookie> entry) {
        if(entry.getValue().hasExpired()) {
            remove(entry.getKey());
            return false;
        }
        else {
            return true;
        }
    }

    //shamelessly copied from jdk8 source code for InMemoryCookieStore
    private boolean netscapeDomainMatches(final String domain, final String host) {
        if (domain == null || host == null) {
            return false;
        }

        // if there's no embedded dot in domain and domain is not .local
        boolean isLocalDomain = ".local".equalsIgnoreCase(domain);
        int embeddedDotInDomain = domain.indexOf('.');
        if (embeddedDotInDomain == 0) {
            embeddedDotInDomain = domain.indexOf('.', 1);
        }
        if (!isLocalDomain && (embeddedDotInDomain == -1 || embeddedDotInDomain == domain.length() - 1)) {
            return false;
        }

        // if the host name contains no dot and the domain name is .local
        int firstDotInHost = host.indexOf('.');
        if (firstDotInHost == -1 && isLocalDomain) {
            return true;
        }

        int domainLength = domain.length();
        int lengthDiff = host.length() - domainLength;
        if (lengthDiff == 0) {
            // if the host name and the domain name are just string-compare euqal
            return host.equalsIgnoreCase(domain);
        }
        else if (lengthDiff > 0) {
            // need to check H & D component
            String H = host.substring(0, lengthDiff);
            String D = host.substring(lengthDiff);

            return (D.equalsIgnoreCase(domain));
        }
        else if (lengthDiff == -1) {
            // if domain is actually .host
            return (domain.charAt(0) == '.' &&
                    host.equalsIgnoreCase(domain.substring(1)));
        }

        return false;
    }

    private boolean matches(final Map.Entry<Key,HttpCookie> entry, final URI uri) {
        final HttpCookie cookie = entry.getValue();
        final boolean secureLink = "https".equalsIgnoreCase(uri.getScheme());
        if(!secureLink && cookie.getSecure()) {
            return false;
        }
        
        final String host = uri.getHost();
        if(entry.getKey() instanceof UriKey) {
            return ((UriKey) entry.getKey()).host.equalsIgnoreCase(host);
        }
        else {
            final String domain = cookie.getDomain();
            if(cookie.getVersion() == 0) {
                return netscapeDomainMatches(domain, host);
            }
            else {
                return HttpCookie.domainMatches(domain, host);
            }
        }
    }

    protected void add(final Key key, final HttpCookie cookie) {
        all.put(key, cookie);
    }

    protected boolean remove(final Key key) {
        return all.remove(key) != null;
    }
}

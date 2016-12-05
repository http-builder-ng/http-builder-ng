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
        add(new Key(uri, cookie), cookie);
    }

    public List<HttpCookie> get(final URI uri) {
        return (all.entrySet()
                .stream()
                .filter(entry -> entryValid(entry) && matches(entry, uri))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList()));
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
                .filter(this::entryValid)
                .map(entry -> entry.getKey().domain)
                .distinct()
                .map(NonBlockingCookieStore::makeURI)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
    }

    public boolean remove(final URI uri, final HttpCookie cookie) {
        return remove(new Key(uri, cookie));
    }

    public boolean removeAll() {
        int initialSize = all.size();
        all.clear();
        return initialSize > 0;
    }

    protected static class Key {
        final String name;
        final String domain;
        final String path;
        final int hash;
        final Instant createdAt;

        private static String forStorage(final String str) {
            return str == null ? str : str.toLowerCase();
        }
        
        public Key(final URI uri, final HttpCookie cookie) {
            this.name = forStorage(cookie.getName());
            if(uri != null && (cookie.getDomain() == null || cookie.getDomain().equals(""))) {
                this.domain = forStorage(uri.getHost());
                this.path = null;
            }
            else {
                this.domain = forStorage(cookie.getDomain());
                this.path = forStorage(cookie.getPath());
            }
            
            this.createdAt = Instant.now();
            this.hash = Objects.hash(name, domain, path);
        }

        public Key(final String name, final String domain, final String path, Instant createdAt) {
            this.name = name;
            this.domain = domain;
            this.path = path;
            this.createdAt = createdAt;
            this.hash = Objects.hash(name, domain, path);
        }
        
        @Override
        public boolean equals(final Object o) {
            if(!(o instanceof Key)) {
                return false;
            }

            final Key rhs = (Key) o;
            return (name.equals(rhs.name) &&
                    domain.equals(rhs.domain) &&
                    Objects.equals(path, rhs.path));
        }

        @Override
        public int hashCode() {
            return hash;
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
        final String domain = cookie.getDomain() == null ? entry.getKey().domain : cookie.getDomain();
        if(cookie.getVersion() == 0) {
            return netscapeDomainMatches(domain, host);
        }
        else {
            return HttpCookie.domainMatches(domain, host);
        }
    }

    protected void add(final Key key, final HttpCookie cookie) {
        all.put(key, cookie);
    }

    protected boolean remove(final Key key) {
        return all.remove(key) != null;
    }
}

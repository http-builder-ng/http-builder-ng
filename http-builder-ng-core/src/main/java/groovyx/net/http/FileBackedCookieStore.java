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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

class FileBackedCookieStore extends NonBlockingCookieStore {

    private static final ConcurrentMap<File,File> inUse = new ConcurrentHashMap<>(5, 0.75f, 1);
    
    private static final int NUM_LOCKS = 16;
    private static final String SUFFIX = ".properties";
    
    private final File directory;
    private final Object[] locks;
    private final Executor executor;
    private final Consumer<Throwable> onException;
    
    private volatile boolean live = true;

    public FileBackedCookieStore(final File directory, final Executor executor, final Consumer<Throwable> onException) {
        this.onException = onException;
        ensureUniqueControl(directory);
        this.directory = directory;
        this.locks = new Object[NUM_LOCKS];
        for(int i = 0; i < NUM_LOCKS; ++i) {
            locks[i] = new Object();
        }

        this.executor = executor;
        readAll();
    }
    
    public FileBackedCookieStore(final File directory, final Executor executor) {
        this(directory, executor, (t) -> {});
    }

    private static void ensureUniqueControl(final File directory) {
        if(null != inUse.putIfAbsent(directory, directory)) {
            throw new ConcurrentModificationException(directory + " is already being used by another " +
                                                      "cookie store in this process");
        }
    }
    
    private void withLock(final Key key, final Runnable runner) {
        Object lock = locks[Math.abs(key.hashCode() % NUM_LOCKS)];
        synchronized(lock) {
            runner.run();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void deleteFile(final Key key) {
        final File file = new File(directory, fileName(key));
        if(file.exists()) {
            file.delete();
        }
    }

    @Override
    public void add(final URI uri, final HttpCookie cookie) {
        assertLive();
        final Key key = Key.make(uri, cookie);
        add(key, cookie);
        if(cookie.getMaxAge() != -1L) {
            store(key, cookie);
        }
    }

    @Override
    public boolean remove(final URI uri, final HttpCookie cookie) {
        assertLive();
        return remove(Key.make(uri, cookie));
    }

    @Override
    public boolean removeAll() {
        assertLive();
        final boolean ret = all.size() > 0;
        for(Map.Entry<Key,HttpCookie> entry : all.entrySet()) {
            remove(entry.getKey());
        }

        return ret;
    }

    @Override
    public boolean remove(final Key key) {
        executor.execute(() -> withLock(key, () -> deleteFile(key)));
        return super.remove(key);
    }

    private static String clean(final String str) {
        if(str == null) {
            return "";
        }
        
        String ret = str;
        if(ret.indexOf('/') != -1) {
            ret = ret.replace('/', '_');
        }

        if(ret.indexOf('\\') != -1) {
            ret = ret.replace('\\', '_');
        }

        return ret;
    }
    
    private static String fileName(final Key key) {
        if(key instanceof UriKey) {
            final UriKey uriKey = (UriKey) key;
            return clean(uriKey.host) + clean(uriKey.name) + SUFFIX;
        }
        else {
            final DomainKey domainKey = (DomainKey) key;
            return clean(domainKey.domain) + clean(domainKey.path) + clean(domainKey.name) + SUFFIX;
        }
    }

    private void store(final Key key, final HttpCookie cookie) {
        final Runnable runner = () -> {
            File file = new File(directory, fileName(key));
            
            try(FileWriter fw = new FileWriter(file)) {
                toProperties(key, cookie).store(fw, "");
            }
            catch(IOException ioe) {
                onException.accept(ioe);
            } };
        
        executor.execute(() -> withLock(key, runner));
    }

    //since readAll happens in the constructor and there is a guarantee that
    //each cookie store controls its own directory, we do not need to synchronize
    private void readAll() {
        final List<CompletableFuture<Void>> futures = new ArrayList<>();
        for(File file : directory.listFiles()) {
            
            final Runnable loadFile = () -> {
                if(file.getName().endsWith(SUFFIX)) {
                    try(FileReader reader = new FileReader(file)) {
                        Properties props = new Properties();
                        props.load(reader);
                        Map.Entry<Key,HttpCookie> entry = fromProperties(props);
                        if(entry != null) {
                            add(entry.getKey(), entry.getValue());
                        }
                        else {
                            file.delete();
                        }
                    }
                    catch(IOException ioe) {
                        throw new RuntimeException(ioe);
                    }
                } };

            futures.add(CompletableFuture.runAsync(loadFile, executor));
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
        }
        catch(InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private void ifNotNull(final Properties props, final String key, final String value) {
        if(value != null) {
            props.setProperty(key, value);
        }
    }

    private Properties keyProperties(final Key key) {
        final Properties props = new Properties();
        props.setProperty("keyType", key.getKeyType());
        if(key instanceof UriKey) {
            props.setProperty("uri", String.format("http://%s", ((UriKey) key).getURI().toString()));
        }

        return props;
    }

    private Properties toProperties(final Key key, final HttpCookie cookie) {
        final Properties props = keyProperties(key);
        
        final Instant expires = key.createdAt.plusSeconds(cookie.getMaxAge());
        props.setProperty("expires", expires.toString());
        props.setProperty("name", cookie.getName());
        props.setProperty("value", cookie.getValue());
        if(cookie.getDomain() != null) {
            props.setProperty("domain", cookie.getDomain());
        }

        props.setProperty("discard", Boolean.toString(cookie.getDiscard()));
        props.setProperty("secure", Boolean.toString(cookie.getSecure()));
        props.setProperty("version", Integer.toString(cookie.getVersion()));
        props.setProperty("httpOnly", Boolean.toString(cookie.isHttpOnly()));

        ifNotNull(props, "comment", cookie.getComment());
        ifNotNull(props, "commentURL", cookie.getCommentURL());
        ifNotNull(props, "path", cookie.getPath());
        ifNotNull(props, "portlist", cookie.getPortlist());
        
        return props;
    }

    private Map.Entry<Key,HttpCookie> fromProperties(final Properties props, final HttpCookie cookie) {
        final String keyType = props.getProperty("keyType");
        if(UriKey.uriKey(keyType)) {
            try {
                return new AbstractMap.SimpleImmutableEntry<>(new UriKey(new URI(props.getProperty("uri")), cookie), cookie);
            }
            catch(URISyntaxException e) {
                //can ignore since the source should have come from a valid uri
                return null;
            }
        }
        else {
            return new AbstractMap.SimpleImmutableEntry<>(new DomainKey(cookie), cookie);
        }
    }

    private Map.Entry<Key,HttpCookie> fromProperties(final Properties props) {
        final Instant now = Instant.now();
        final Instant expires = Instant.parse(props.getProperty("expires"));
        if(now.isAfter(expires)) {
            return null;
        }

        final long maxAge = (expires.toEpochMilli() - now.toEpochMilli()) / 1_000L;
        final String name = props.getProperty("name");
        final String value = props.getProperty("value");
        
        final HttpCookie cookie = new HttpCookie(name, value);
        cookie.setDiscard(Boolean.valueOf(props.getProperty("discard")));
        cookie.setSecure(Boolean.valueOf(props.getProperty("secure")));
        cookie.setVersion(Integer.valueOf(props.getProperty("version")));
        cookie.setHttpOnly(Boolean.valueOf(props.getProperty("httpOnly")));

        final String domain = props.getProperty("domain", null);
        if(null != domain) cookie.setDomain(domain);
        
        final String comment = props.getProperty("comment", null);
        if(null != comment) cookie.setComment(comment);
        
        final String commentURL = props.getProperty("commentURL", null);
        if(null != commentURL) cookie.setCommentURL(commentURL);

        final String path = props.getProperty("path", null);
        if(null != path) cookie.setPath(path);

        final String portlist = props.getProperty("portlist", null);
        if(null != portlist) cookie.setPortlist(portlist);

        return fromProperties(props, cookie);
    }

    public void shutdown() {
        //not necessary to call, but can be useful
        live = false;
        inUse.remove(directory);
    }

    public void assertLive() {
        if(!live) {
            throw new IllegalStateException("You have already called shutdown on this object");
        }
    }
}

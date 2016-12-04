package groovyx.http.net;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpCookie;
import java.net.URI;
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

class FileBackedCookieStore extends NonBlockingCookieStore {

    private static final ConcurrentMap<File,File> inUse = new ConcurrentHashMap<>(5, 0.75f, 1);
    
    private static final int NUM_LOCKS = 16;
    private static final String SUFFIX = ".properties";
    
    private final File directory;
    private final Object[] locks;
    private final Executor executor;
    
    public FileBackedCookieStore(final File directory, final Executor executor) {
        ensureUniqueControl(directory);
        this.directory = directory;
        this.locks = new Object[NUM_LOCKS];
        for(int i = 0; i < NUM_LOCKS; ++i) {
            locks[i] = new Object();
        }

        this.executor = executor;
        readAll();
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

    private void deleteFile(final Key key) {
        final File file = new File(directory, fileName(key));
        if(file.exists()) {
            file.delete();
        }
    }

    @Override
    public void add(final URI uri, final HttpCookie cookie) {
        final Key key = new Key(uri, cookie);
        add(key, cookie);
        if(cookie.getMaxAge() != -1L) {
            store(key, cookie);
        }
    }

    @Override
    public boolean remove(final URI uri, final HttpCookie cookie) {
        final Key key = new Key(uri, cookie);
        executor.execute(() -> withLock(key, () -> deleteFile(key)));
        return remove(key);
    }

    @Override
    public boolean removeAll() {
        final boolean ret = all.size() > 0;
        for(Map.Entry<Key,HttpCookie> entry : all.entrySet()) {
            remove(entry.getKey());
            executor.execute(() -> withLock(entry.getKey(), () -> deleteFile(entry.getKey())));
        }

        return ret;
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
        return clean(key.domain) + clean(key.path) + clean(key.name) + SUFFIX;
    }

    private void store(final Key key, final HttpCookie cookie) {
        final Runnable runner = () -> {
            File file = new File(directory, fileName(key));
            
            try(FileWriter fw = new FileWriter(file)) {
                toProperties(key, cookie).store(fw, "");
            }
            catch(IOException ioe) {
                throw new RuntimeException(ioe);
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

        for(CompletableFuture<Void> future : futures) {
            try {
                future.get();
            }
            catch(InterruptedException | ExecutionException e) {
                //just swallow it, we can't do anything about it
                //since we aren't going to be able to load the cookie
            }
        }
    }

    private void ifNotNull(final Properties props, final String key, final String value) {
        if(value != null) {
            props.setProperty(key, value);
        }
    }

    private Properties toProperties(final Key key, final HttpCookie cookie) {
        final Properties props = new Properties();

        final Instant expires = key.createdAt.plusSeconds(cookie.getMaxAge());
        props.setProperty("expires", expires.toString());
        props.setProperty("name", cookie.getName());
        props.setProperty("value", cookie.getValue());
        props.setProperty("domain", cookie.getDomain() != null ? cookie.getDomain() : key.domain);

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

    private Map.Entry<Key,HttpCookie> fromProperties(final Properties props) {
        final Instant now = Instant.now();
        final Instant expires = Instant.parse(props.getProperty("expires"));
        if(now.isAfter(expires)) {
            return null;
        }

        final long maxAge = (expires.toEpochMilli() - now.toEpochMilli()) / 1_000L;
        final String name = props.getProperty("name");
        final String value = props.getProperty("value");
        final String domain = props.getProperty("domain");
        final HttpCookie cookie = new HttpCookie(name, value);
        cookie.setDiscard(Boolean.valueOf(props.getProperty("discard")));
        cookie.setSecure(Boolean.valueOf(props.getProperty("secure")));
        cookie.setVersion(Integer.valueOf(props.getProperty("version")));
        cookie.setHttpOnly(Boolean.valueOf(props.getProperty("httpOnly")));

        final String comment = props.getProperty("comment", null);
        if(null != comment) cookie.setComment(comment);
        
        final String commentURL = props.getProperty("commentURL", null);
        if(null != commentURL) cookie.setCommentURL(commentURL);

        final String path = props.getProperty("path", null);
        if(null != path) cookie.setPath(path);

        final String portlist = props.getProperty("portlist", null);
        if(null != portlist) cookie.setPortlist(portlist);

        return new AbstractMap.SimpleImmutableEntry<>(new Key(name, domain, path, now), cookie);
    }
}

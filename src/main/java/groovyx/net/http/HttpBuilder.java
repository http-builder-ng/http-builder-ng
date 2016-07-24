package groovyx.net.http;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.codehaus.groovy.runtime.MethodClosure;
import java.io.Closeable;

public interface HttpBuilder extends Closeable {

    public static class TypeHolder {
        protected static volatile ClientType clientType = ClientType.APACHE_HTTP_CLIENT;
    }

    public static ClientType getDefaultClientType() {
        return TypeHolder.clientType;
    }

    public static void setDefaultClientType(final ClientType val) {
        TypeHolder.clientType = val;
    }
    
    static void noOp() { }
    static Closure NO_OP = new MethodClosure(HttpBuilder.class, "noOp");

    public static HttpBuilder configure(final ClientType type) {
        return configure(type, NO_OP);
    }

    public static HttpBuilder configure(@DelegatesTo(HttpObjectConfig.class) final Closure closure) {
        return configure(getDefaultClientType(), closure);
    }
    
    public static HttpBuilder configure(final ClientType type, @DelegatesTo(HttpObjectConfig.class) final Closure closure) {
        HttpObjectConfigImpl impl = new HttpObjectConfigImpl();
        closure.setDelegate(impl);
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        closure.call();
        return impl.build(type);
    }

    default Object get() {
        return get(NO_OP);
    }
    
    default <T> T get(final Class<T> type, @DelegatesTo(HttpConfig.class) final Closure closure) {
        return type.cast(get(closure));
    }

    default CompletableFuture<Object> getAsync() {
        return CompletableFuture.supplyAsync(() -> get(), getExecutor());
    }
    
    default CompletableFuture<Object> getAsync(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return CompletableFuture.supplyAsync(() -> get(closure), getExecutor());
    }

    default <T> CompletableFuture<T> getAsync(final Class<T> type, @DelegatesTo(HttpConfig.class) final Closure closure) {
        return CompletableFuture.supplyAsync(() -> get(type, closure), getExecutor());
    }

    default Object head() {
        return head(NO_OP);
    }

    default <T> T head(final Class<T> type, @DelegatesTo(HttpConfig.class) final Closure closure) {
        return type.cast(head(closure));
    }

    default CompletableFuture<Object> headAsync() {
        return CompletableFuture.supplyAsync(() -> head(), getExecutor());
    }
    
    default CompletableFuture<Object> headAsync(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return CompletableFuture.supplyAsync(() -> head(closure), getExecutor());
    }

    default <T> CompletableFuture<T> headAsync(final Class<T> type, @DelegatesTo(HttpConfig.class) final Closure closure) {
        return CompletableFuture.supplyAsync(() -> head(type, closure), getExecutor());
    }
    
    default Object post() {
        return post(NO_OP);
    }

    default <T> T post(final Class<T> type, @DelegatesTo(HttpConfig.class) final Closure closure) {
        return type.cast(post(closure));
    }

    default CompletableFuture<Object> postAsync() {
        return CompletableFuture.supplyAsync(() -> post(NO_OP), getExecutor());
    }

    default Object postAsync(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return CompletableFuture.supplyAsync(() -> post(closure), getExecutor());
    }
    
    default <T> CompletableFuture<T> postAsync(final Class<T> type, @DelegatesTo(HttpConfig.class) final Closure closure) {
        return CompletableFuture.supplyAsync(() -> post(type, closure), getExecutor());
    }

    default Object put() {
        return put(NO_OP);
    }

    default <T> T put(final Class<T> type, @DelegatesTo(HttpConfig.class) final Closure closure) {
        return type.cast(put(closure));
    }

    default CompletableFuture<Object> putAsync() {
        return CompletableFuture.supplyAsync(() -> put(NO_OP), getExecutor());
    }

    default Object putAsync(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return CompletableFuture.supplyAsync(() -> put(closure), getExecutor());
    }
    
    default <T> CompletableFuture<T> putAsync(final Class<T> type, @DelegatesTo(HttpConfig.class) final Closure closure) {
        return CompletableFuture.supplyAsync(() -> put(type, closure), getExecutor());
    }

    //deletes
    default Object delete() {
        return delete(NO_OP);
    }

    default <T> T delete(final Class<T> type, @DelegatesTo(HttpConfig.class) final Closure closure) {
        return type.cast(delete(closure));
    }

    default CompletableFuture<Object> deleteAsync() {
        return CompletableFuture.supplyAsync(() -> delete(NO_OP), getExecutor());
    }

    default Object deleteAsync(@DelegatesTo(HttpConfig.class) final Closure closure) {
        return CompletableFuture.supplyAsync(() -> delete(closure), getExecutor());
    }
    
    default <T> CompletableFuture<T> deleteAsync(final Class<T> type, @DelegatesTo(HttpConfig.class) final Closure closure) {
        return CompletableFuture.supplyAsync(() -> delete(type, closure), getExecutor());
    }

    Object get(@DelegatesTo(HttpConfig.class) final Closure closure);
    Object head(@DelegatesTo(HttpConfig.class) final Closure closure);
    Object post(@DelegatesTo(HttpConfig.class) final Closure closure);
    Object put(@DelegatesTo(HttpConfig.class) final Closure closure);
    Object delete(@DelegatesTo(HttpConfig.class) final Closure closure);
    Executor getExecutor();
}

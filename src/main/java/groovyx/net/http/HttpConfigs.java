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

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.lang.GroovyShell;
import groovy.transform.TypeChecked;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import static groovyx.net.http.ChainedHttpConfig.*;
import static groovyx.net.http.Safe.*;

import groovyx.net.http.optional.*;

public class HttpConfigs {

    public static class BasicAuth implements Auth {
        private String user;
        private String password;
        private boolean preemptive;
        private AuthType authType;
        
        public void basic(final String user, final String password, final boolean preemptive) {
            this.user = user;
            this.password = password;
            this.preemptive = preemptive;
            this.authType = AuthType.BASIC;
        }

        public void digest(final String user, final String password, final boolean preemptive) {
            basic(user, password, preemptive);
            this.authType = AuthType.DIGEST;
        }

        public String getUser() {
            return user;
        }

        public String getPassword() {
            return password;
        }

        public AuthType getAuthType() {
            return authType;
        }
    }

    public static class ThreadSafeAuth implements Auth {
        volatile String user;
        volatile String password;
        volatile boolean preemptive;
        volatile AuthType authType;
        
        public ThreadSafeAuth() { }

        public ThreadSafeAuth(final BasicAuth toCopy) {
            this.user = toCopy.user;
            this.password = toCopy.password;
            this.preemptive = toCopy.preemptive;
            this.authType = toCopy.authType;
        }
        
        public void basic(final String user, final String password, final boolean preemptive) {
            this.user = user;
            this.password = password;
            this.preemptive = preemptive;
            this.authType = AuthType.BASIC;
        }

        public void digest(final String user, final String password, final boolean preemptive) {
            basic(user, password, preemptive);
            this.authType = AuthType.DIGEST;
        }

        public String getUser() {
            return user;
        }

        public String getPassword() {
            return password;
        }

        public AuthType getAuthType() {
            return authType;
        }
    }

    public static abstract class BaseRequest implements ChainedRequest {

        final ChainedRequest parent;

        public BaseRequest(final ChainedRequest parent) {
            this.parent = parent;
        }

        public ChainedRequest getParent() {
            return parent;
        }

        public void setCharset(final String val) {
            setCharset(Charset.forName(val));
        }
        
        public void setUri(final String val) throws URISyntaxException {
            getUri().setFull(val);
        }

        public void setUri(final URI val) {
            getUri().setFull(val);
        }

        public void setUri(final URL val) throws URISyntaxException {
            getUri().setFull(val.toURI());
        }

        public BiConsumer<ChainedHttpConfig,ToServer> encoder(final String contentType) {
            final BiConsumer<ChainedHttpConfig,ToServer> enc =  getEncoderMap().get(contentType);
            return enc != null ? enc : null;
        }
        
        public void encoder(final String contentType, final BiConsumer<ChainedHttpConfig,ToServer> val) {
            getEncoderMap().put(contentType, val);
        }
        
        public void encoder(final Iterable<String> contentTypes, final BiConsumer<ChainedHttpConfig,ToServer> val) {
            for(String contentType : contentTypes) {
                encoder(contentType, val);
            }
        }

        public void setAccept(final String[] values) {
            getHeaders().put("Accept", String.join(";", values));
        }

        public void setAccept(final Iterable<String> values) {
            getHeaders().put("Accept", String.join(";", values));
        }

        public void setHeaders(final Map<String,String> toAdd) {
            final Map<String,String> h = getHeaders();
            for(Map.Entry<String,String> entry : toAdd.entrySet()) {
                h.put(entry.getKey(), entry.getValue());
            }
        }

        public void cookie(final String name, final String value, final Date date) {
            getCookies().add(new Cookie(name, value, date));
        }

        public void cookie(final String name, final String value, final LocalDateTime dateTime){
            Date date = new Date(dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            getCookies().add(new Cookie(name, value, date));
        }
    }

    public static class BasicRequest extends BaseRequest {
        private String contentType;
        private Charset charset;
        private UriBuilder uriBuilder;
        private final Map<String,String> headers = new LinkedHashMap<>();
        private Object body;
        private final Map<String,BiConsumer<ChainedHttpConfig,ToServer>> encoderMap = new LinkedHashMap<>();
        private BasicAuth auth = new BasicAuth();
        private List<Cookie> cookies = new ArrayList(1);
        
        protected BasicRequest(ChainedRequest parent) {
            super(parent);
            this.uriBuilder = (parent == null) ? UriBuilder.basic(null) : UriBuilder.basic(parent.getUri());
        }

        public Map<String,BiConsumer<ChainedHttpConfig,ToServer>> getEncoderMap() {
            return encoderMap;
        }

        public List<Cookie> getCookies() {
            return cookies;
        }
        
        public String getContentType() {
            return contentType;
        }
        
        public void setContentType(final String val) {
            this.contentType = val;
        }

        public void setCharset(final Charset val) {
            this.charset = val;
        }

        public Charset getCharset() {
            return charset;
        }
        
        public UriBuilder getUri() {
            return uriBuilder;
        }
        
        public Map<String,String> getHeaders() {
            return headers;
        }
        
        public Object getBody() {
            return body;
        }
        
        public void setBody(Object val) {
            this.body = val;
        }

        public BasicAuth getAuth() {
            return auth;
        }
    }
    
    public static class ThreadSafeRequest extends BaseRequest {
        
        private volatile String contentType;
        private volatile Charset charset;
        private volatile UriBuilder uriBuilder;
        private final ConcurrentMap<String,String> headers = new ConcurrentHashMap<>();
        private volatile Object body;
        private final ConcurrentMap<String,BiConsumer<ChainedHttpConfig,ToServer>> encoderMap = new ConcurrentHashMap<>();
        private final ThreadSafeAuth auth;
        private final List<Cookie> cookies = new CopyOnWriteArrayList();

        public ThreadSafeRequest(final ChainedRequest parent) {
            super(parent);
            this.auth = new ThreadSafeAuth();
            this.uriBuilder = (parent == null) ? UriBuilder.threadSafe(null) : UriBuilder.threadSafe(parent.getUri());
        }

        public List<Cookie> getCookies() {
            return cookies;
        }
        
        public Map<String,BiConsumer<ChainedHttpConfig,ToServer>> getEncoderMap() {
            return encoderMap;
        }

        public String getContentType() {
            return contentType;
        }
        
        public void setContentType(final String val) {
            this.contentType = val;
        }

        public Charset getCharset() {
            return charset;
        }
        
        public void setCharset(final Charset val) {
            this.charset = val;
        }
        
        public UriBuilder getUri() {
            return uriBuilder;
        }
        
        public Map<String,String> getHeaders() {
            return headers;
        }
        
        public Object getBody() {
            return body;
        }
        
        public void setBody(Object val) {
            this.body = val;
        }

        public ThreadSafeAuth getAuth() {
            return auth;
        }
    }

    public static abstract class BaseResponse implements ChainedResponse {

        abstract protected Map<Integer,Closure<?>> getByCode();
        abstract protected Closure<?> getSuccess();
        abstract protected Closure<?> getFailure();
        abstract protected Map<String,BiFunction<ChainedHttpConfig,FromServer,Object>> getParserMap();
        
        private final ChainedResponse parent;

        public ChainedResponse getParent() {
            return parent;
        }

        protected BaseResponse(final ChainedResponse parent) {
            this.parent = parent;
        }
        
        public void when(String code, Closure<?> closure) {
            when(Integer.valueOf(code), closure);
        }
        
        public void when(Integer code, Closure<?> closure) {
            getByCode().put(code, closure);
        }
        
        public void when(final HttpConfig.Status status, Closure<?> closure) {
            if(status == HttpConfig.Status.SUCCESS) {
                success(closure);
            }
            else {
                failure(closure);
            }
        }

        public Closure<?> when(final Integer code) {
            if(getByCode().containsKey(code)) {
                return getByCode().get(code);
            }
            
            if(code < 400 && getSuccess() != null) {
                return getSuccess();
            }
            
            if(code >= 400 && getFailure() != null) {
                return getFailure();
            }

            return null;
        }
        
        public BiFunction<ChainedHttpConfig,FromServer,Object> parser(final String contentType) {
            final BiFunction<ChainedHttpConfig,FromServer,Object> p = getParserMap().get(contentType);
            return p != null ? p : null;
        }
        
        public void parser(final String contentType, BiFunction<ChainedHttpConfig,FromServer,Object> val) {
            getParserMap().put(contentType, val);
        }
        
        public void parser(final Iterable<String> contentTypes, BiFunction<ChainedHttpConfig,FromServer,Object> val) {
            for(String contentType : contentTypes) {
                parser(contentType, val);
            }
        }
    }

    public static class BasicResponse extends BaseResponse {
        private final Map<Integer,Closure<?>> byCode = new LinkedHashMap<>();
        private Closure<?> successHandler;
        private Closure<?> failureHandler;
        private final Map<String,BiFunction<ChainedHttpConfig,FromServer,Object>> parserMap = new LinkedHashMap<>();

        protected BasicResponse(final ChainedResponse parent) {
            super(parent);
        }
        
        public Map<String,BiFunction<ChainedHttpConfig,FromServer,Object>> getParserMap() {
            return parserMap;
        }
        
        protected Map<Integer,Closure<?>> getByCode() {
            return byCode;
        }

        protected Closure<?> getSuccess() {
            return successHandler;
        }
        
        protected Closure<?> getFailure() {
            return failureHandler;
        }

        public void success(final Closure<?> val) {
            successHandler = val;
        }

        public void failure(final Closure<?> val) {
            failureHandler = val;
        }
    }

    public static class ThreadSafeResponse extends BaseResponse {
        private final ConcurrentMap<String,BiFunction<ChainedHttpConfig,FromServer,Object>> parserMap = new ConcurrentHashMap<>();
        private final ConcurrentMap<Integer,Closure<?>> byCode = new ConcurrentHashMap<>();
        private volatile Closure<?> successHandler;
        private volatile Closure<?> failureHandler;

        public ThreadSafeResponse(final ChainedResponse parent) {
            super(parent);
        }

        protected Map<String,BiFunction<ChainedHttpConfig,FromServer,Object>> getParserMap() {
            return parserMap;
        }
        
        protected Map<Integer,Closure<?>> getByCode() {
            return byCode;
        }

        protected Closure<?> getSuccess() {
            return successHandler;
        }
        
        protected Closure<?> getFailure() {
            return failureHandler;
        }

        public void success(final Closure<?> val) {
            successHandler = val;
        }

        public void failure(final Closure<?> val) {
            failureHandler = val;
        }
    }

    public abstract static class BaseHttpConfig implements ChainedHttpConfig {

        private final ChainedHttpConfig parent;

        public BaseHttpConfig(ChainedHttpConfig parent) {
            this.parent = parent;
        }

        public ChainedHttpConfig getParent() {
            return parent;
        }
        
        public ChainedHttpConfig configure(final String scriptClassPath) {
            final CompilerConfiguration compilerConfig = new CompilerConfiguration();
            final ImportCustomizer icustom = new ImportCustomizer();
            icustom.addImports("groovyx.net.http.NativeHandlers");
            icustom.addStaticStars("groovyx.net.http.ContentTypes");
            final Map<String,String> map = Collections.singletonMap("extensions", TYPE_CHECKING_SCRIPT);
            final ASTTransformationCustomizer ast = new ASTTransformationCustomizer(map, TypeChecked.class);
            compilerConfig.addCompilationCustomizers(icustom, ast);
            final GroovyShell shell = new GroovyShell(getClass().getClassLoader(), compilerConfig);
            shell.setVariable("request", getRequest());
            shell.setVariable("response", getResponse());
            
            try(final InputStream is = getClass().getClassLoader().getResourceAsStream(scriptClassPath)) {
                final InputStreamReader reader = new InputStreamReader(is);
                shell.evaluate(reader);
                return this;
            }
            catch(IOException ioe) {
                throw new RuntimeException();
            }
        }

        public ChainedHttpConfig config(@DelegatesTo(HttpConfig.class) final Closure closure) {
            closure.setDelegate(this);
            closure.setResolveStrategy(Closure.DELEGATE_FIRST);
            closure.call();
            return this;
        }

        public void context(final String contentType, final Object id, final Object obj) {
            getContextMap().put(new AbstractMap.SimpleImmutableEntry<>(contentType, id), obj);
        }
    }
    
    public static class ThreadSafeHttpConfig extends BaseHttpConfig {
        private final ThreadSafeRequest request;
        private final ThreadSafeResponse response;
        private final ConcurrentMap<Map.Entry<String,Object>,Object> contextMap = new ConcurrentHashMap<>();
        
        public ThreadSafeHttpConfig(final ChainedHttpConfig parent) {
            super(parent);
            if(parent == null) {
                this.request = new ThreadSafeRequest(null);
                this.response = new ThreadSafeResponse(null);
            }
            else {
                this.request = new ThreadSafeRequest(parent.getChainedRequest());
                this.response = new ThreadSafeResponse(parent.getChainedResponse());
            }
        }

        public Request getRequest() {
            return request;
        }

        public Response getResponse() {
            return response;
        }

        public ChainedRequest getChainedRequest() {
            return request;
        }

        public ChainedResponse getChainedResponse() {
            return response;
        }

        public Map<Map.Entry<String,Object>,Object> getContextMap() {
            return contextMap;
        }
    }

    public static class BasicHttpConfig extends BaseHttpConfig {
        private final BasicRequest request;
        private final BasicResponse response;
        private final Map<Map.Entry<String,Object>,Object> contextMap = new LinkedHashMap<>(1);
        
        public BasicHttpConfig(final ChainedHttpConfig parent) {
            super(parent);
            if(parent == null) {
                this.request = new BasicRequest(null);
                this.response = new BasicResponse(null);
            }
            else {
                this.request = new BasicRequest(parent.getChainedRequest());
                this.response = new BasicResponse(parent.getChainedResponse());
            }
        }

        public Request getRequest() {
            return request;
        }

        public Response getResponse() {
            return response;
        }

        public ChainedRequest getChainedRequest() {
            return request;
        }

        public ChainedResponse getChainedResponse() {
            return response;
        }

        public Map<Map.Entry<String,Object>,Object> getContextMap() {
            return contextMap;
        }
    }

    private static final String CONFIG = "59f7b2e5d5a78b25c6b21eb3b6b4f9ff77d11671.groovy";
    private static final ThreadSafeHttpConfig root;

    static {
        root = (ThreadSafeHttpConfig) new ThreadSafeHttpConfig(null).configure(CONFIG);
        
        register(root, ifClassIsLoaded("org.cyberneko.html.parsers.SAXParser"),
                 "text/html", () -> NativeHandlers.Encoders::xml, Html.neckoParserSupplier);
        
        register(root, ifClassIsLoaded("org.jsoup.Jsoup"),
                 "text/html", Html.jsoupEncoderSupplier, Html.jsoupParserSupplier);
        
        if(register(root, ifClassIsLoaded("com.opencsv.CSVReader"),
                    "text/csv", Csv.encoderSupplier, Csv.parserSupplier)) {
            root.context("text/csv", Csv.Context.ID, Csv.Context.DEFAULT_CSV);
        }
        
        if(register(root, ifClassIsLoaded("com.opencsv.CSVReader"),
                    "text/tab-separated-values", Csv.encoderSupplier, Csv.parserSupplier)) {
            root.context("text/tab-separated-values", Csv.Context.ID, Csv.Context.DEFAULT_TSV);
        }
    }

    public static ChainedHttpConfig root() {
        return root;
    }

    public static ChainedHttpConfig threadSafe(final ChainedHttpConfig parent) {
        return new ThreadSafeHttpConfig(parent);
    }

    public static ChainedHttpConfig classLevel(final boolean threadSafe) {
        return threadSafe ? threadSafe(root) : basic(root);
    }

    public static ChainedHttpConfig basic(final ChainedHttpConfig parent) {
        return new BasicHttpConfig(parent);
    }

    public static ChainedHttpConfig requestLevel(final ChainedHttpConfig parent) {
        return new BasicHttpConfig(parent);
    }

    private static final String TYPE_CHECKING_SCRIPT = "typecheckhttpconfig.groovy";
}

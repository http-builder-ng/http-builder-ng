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

import groovyx.net.http.optional.Csv;
import groovyx.net.http.optional.Html;

import java.io.IOException;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import static groovyx.net.http.ChainedHttpConfig.Auth;
import static groovyx.net.http.ChainedHttpConfig.AuthType;
import static groovyx.net.http.ChainedHttpConfig.ChainedRequest;
import static groovyx.net.http.ChainedHttpConfig.ChainedResponse;
import static groovyx.net.http.ContentTypes.BINARY;
import static groovyx.net.http.ContentTypes.JSON;
import static groovyx.net.http.ContentTypes.TEXT;
import static groovyx.net.http.ContentTypes.URLENC;
import static groovyx.net.http.ContentTypes.XML;
import static groovyx.net.http.Safe.ifClassIsLoaded;
import static groovyx.net.http.Safe.register;

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
        HttpVerb verb;

        public BaseRequest(final ChainedRequest parent) {
            this.parent = parent;
        }

        public ChainedRequest getParent() {
            return parent;
        }

        public void setCharset(final String val) {
            setCharset(Charset.forName(val));
        }

        public void setUri(final String val) {
            getUri().setFull(val);
        }

        public void setRaw(final String val){
            UriBuilder uriBuilder = getUri();
            uriBuilder.setUseRawValues(true);
            uriBuilder.setFull(val);
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

        public void setHeaders(final Map<String,CharSequence> toAdd) {
            final Map<String,CharSequence> h = getHeaders();
            if(toAdd != null){
                for(final Map.Entry<String,CharSequence> entry : toAdd.entrySet()) {
                    h.put(entry.getKey(), entry.getValue());
                }
            }
        }

        public void cookie(final String name, final String value, final Instant instant) {
            final HttpCookie cookie = new HttpCookie(name, value);
            cookie.setPath("/");
            final Instant now = Instant.now();
            if(instant != null && now.isBefore(instant)) {
                cookie.setMaxAge(instant.getEpochSecond() - now.getEpochSecond());
            }

            getCookies().add(cookie);
        }

        public void cookie(final String name, final String value, final Date date) {
            cookie(name, value, date == null ? (Instant) null : date.toInstant());
        }

        public void cookie(final String name, final String value, final LocalDateTime dateTime){
            cookie(name, value, dateTime == null ? (Instant) null : dateTime.atZone(ZoneId.systemDefault()).toInstant());
        }

        @Override
        public HttpVerb getVerb() {
            return verb;
        }

        @Override
        public void setVerb(final HttpVerb verb) {
            this.verb = verb;
        }
    }

    public static class BasicRequest extends BaseRequest {
        private String contentType;
        private Charset charset;
        private UriBuilder uriBuilder;
        private final Map<String, CharSequence> headers = new LinkedHashMap<>();
        private Object body;
        private final Map<String,BiConsumer<ChainedHttpConfig,ToServer>> encoderMap = new LinkedHashMap<>();
        private BasicAuth auth = new BasicAuth();
        private List<HttpCookie> cookies = new ArrayList<>(1);

        protected BasicRequest(ChainedRequest parent) {
            super(parent);
            this.uriBuilder = (parent == null) ? UriBuilder.basic(null) : UriBuilder.basic(parent.getUri());
        }

        public Map<String,BiConsumer<ChainedHttpConfig,ToServer>> getEncoderMap() {
            return encoderMap;
        }

        public List<HttpCookie> getCookies() {
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

        public Map<String,CharSequence> getHeaders() {
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
        private final ConcurrentMap<String,CharSequence> headers = new ConcurrentHashMap<>();
        private volatile Object body;
        private final ConcurrentMap<String,BiConsumer<ChainedHttpConfig,ToServer>> encoderMap = new ConcurrentHashMap<>();
        private final ThreadSafeAuth auth;
        private final List<HttpCookie> cookies = new CopyOnWriteArrayList<>();

        public ThreadSafeRequest(final ChainedRequest parent) {
            super(parent);
            this.auth = new ThreadSafeAuth();
            this.uriBuilder = (parent == null) ? UriBuilder.threadSafe(null) : UriBuilder.threadSafe(parent.getUri());
        }

        public List<HttpCookie> getCookies() {
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

        public Map<String,CharSequence> getHeaders() {
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

        abstract protected Map<Integer,BiFunction<FromServer, Object, ?>> getByCode();
        abstract protected BiFunction<FromServer, Object, ?> getSuccess();
        abstract protected BiFunction<FromServer, Object, ?> getFailure();
        abstract protected Map<String,BiFunction<ChainedHttpConfig,FromServer,Object>> getParserMap();

        private final ChainedResponse parent;

        public ChainedResponse getParent() {
            return parent;
        }

        protected BaseResponse(final ChainedResponse parent) {
            this.parent = parent;
        }

        public void when(String code, BiFunction<FromServer, Object, ?> closure) {
            when(Integer.valueOf(code), closure);
        }

        public void when(Integer code, BiFunction<FromServer, Object, ?> closure) {
            getByCode().put(code, closure);
        }

        public void when(final HttpConfig.Status status, final BiFunction<FromServer, Object, ?> closure) {
            if(status == HttpConfig.Status.SUCCESS) {
                success(closure);
            } else {
                failure(closure);
            }
        }

        public BiFunction<FromServer, Object, ?> when(final Integer code) {
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

        public abstract void setType(Class<?> type);
    }

    public static class BasicResponse extends BaseResponse {

        private final Map<Integer,BiFunction<FromServer, Object, ?>> byCode = new LinkedHashMap<>();
        private BiFunction<FromServer, Object, ?> successHandler;
        private BiFunction<FromServer, Object, ?> failureHandler;
        private Function<Throwable,?> exceptionHandler;
        private final Map<String,BiFunction<ChainedHttpConfig,FromServer,Object>> parserMap = new LinkedHashMap<>();
        private Class<?> type = Object.class;

        protected BasicResponse(final ChainedResponse parent) {
            super(parent);
        }

        public Map<String,BiFunction<ChainedHttpConfig,FromServer,Object>> getParserMap() {
            return parserMap;
        }

        protected Map<Integer,BiFunction<FromServer, Object, ?>> getByCode() {
            return byCode;
        }

        protected BiFunction<FromServer, Object, ?> getSuccess() {
            return successHandler;
        }

        protected BiFunction<FromServer, Object, ?> getFailure() {
            return failureHandler;
        }

        public Function<Throwable,?> getException() {
            return exceptionHandler;
        }

        public void success(final BiFunction<FromServer, Object, ?> val) {
            successHandler = val;
        }

        public void failure(final BiFunction<FromServer, Object, ?> val) {
            failureHandler = val;
        }

        public void exception(final Function<Throwable,?> val) {
            exceptionHandler = val;
        }

        public Class<?> getType() {
            return type;
        }

        public void setType(Class<?> val) {
            type = val;
        }
    }

    public static class ThreadSafeResponse extends BaseResponse {

        private final ConcurrentMap<String,BiFunction<ChainedHttpConfig,FromServer,Object>> parserMap = new ConcurrentHashMap<>();
        private final ConcurrentMap<Integer,BiFunction<FromServer, Object, ?>> byCode = new ConcurrentHashMap<>();
        private volatile BiFunction<FromServer, Object, ?> successHandler;
        private volatile BiFunction<FromServer, Object, ?> failureHandler;
        private volatile Function<Throwable,?> exceptionHandler;
        private volatile Class<?> type = Object.class;

        public ThreadSafeResponse(final ChainedResponse parent) {
            super(parent);
        }

        protected Map<String,BiFunction<ChainedHttpConfig,FromServer,Object>> getParserMap() {
            return parserMap;
        }

        protected Map<Integer,BiFunction<FromServer, Object, ?>> getByCode() {
            return byCode;
        }

        protected BiFunction<FromServer, Object, ?> getSuccess() {
            return successHandler;
        }

        protected BiFunction<FromServer, Object, ?> getFailure() {
            return failureHandler;
        }

        public Function<Throwable,?> getException() {
            return exceptionHandler;
        }

        public void success(final BiFunction<FromServer, Object, ?> val) {
            successHandler = val;
        }

        public void failure(final BiFunction<FromServer, Object, ?> val) {
            failureHandler = val;
        }

        public void exception(final Function<Throwable,?> val) {
            this.exceptionHandler = val;
        }

        public Class<?> getType() {
            return type;
        }

        public void setType(final Class<?> val) {
            type = val;
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

        public ChainedHttpConfig configure() {
            getRequest().setCharset(StandardCharsets.UTF_8);
            getRequest().encoder(BINARY, NativeHandlers.Encoders::binary);
            getRequest().encoder(TEXT, (f,s) -> {
                    try {
                        NativeHandlers.Encoders.text(f, s);
                    }
                    catch(IOException e) {
                        throw new TransportingException(e);
                    } });

            getRequest().encoder(URLENC, NativeHandlers.Encoders::form);
            getRequest().encoder(XML, NativeHandlers.Encoders::xml);
            getRequest().encoder(JSON, NativeHandlers.Encoders::json);

            getResponse().success(NativeHandlers::success);
            getResponse().failure(NativeHandlers::failure);
            getResponse().exception(NativeHandlers::exception);

            getResponse().parser(BINARY, NativeHandlers.Parsers::streamToBytes);
            getResponse().parser(TEXT, NativeHandlers.Parsers::textToString);
            getResponse().parser(URLENC, NativeHandlers.Parsers::form);
            getResponse().parser(XML, NativeHandlers.Parsers::xml);
            getResponse().parser(JSON, NativeHandlers.Parsers::json);

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

        public BasicRequest getRequest() {
            return request;
        }

        public BasicResponse getResponse() {
            return response;
        }

        public BasicRequest getChainedRequest() {
            return request;
        }

        public BasicResponse getChainedResponse() {
            return response;
        }

        public Map<Map.Entry<String,Object>,Object> getContextMap() {
            return contextMap;
        }
    }

    private static final ThreadSafeHttpConfig root;

    static {
        root = (ThreadSafeHttpConfig) new ThreadSafeHttpConfig(null).configure();

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

    public static BasicHttpConfig requestLevel(final ChainedHttpConfig parent) {
        return new BasicHttpConfig(parent);
    }
}

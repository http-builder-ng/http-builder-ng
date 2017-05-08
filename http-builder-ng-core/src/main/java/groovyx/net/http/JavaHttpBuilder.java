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

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import static groovyx.net.http.HttpBuilder.ResponseHandlerFunction.HANDLER_FUNCTION;

/**
 * `HttpBuilder` implementation based on the {@link HttpURLConnection} class.
 *
 * Generally, this class should not be used directly, the preferred method of instantiation is via the
 * `groovyx.net.http.HttpBuilder.configure(java.util.function.Function)` or
 * `groovyx.net.http.HttpBuilder.configure(java.util.function.Function, groovy.lang.Closure)` methods.
 */
public class JavaHttpBuilder extends HttpBuilder {

    protected class Action {

        private final HttpURLConnection connection;
        private final ChainedHttpConfig requestConfig;
        private final URI theUri;
        boolean failed = false;
        
        public Action(final ChainedHttpConfig requestConfig, final String verb) throws IOException, URISyntaxException {
            this.requestConfig = requestConfig;
            final ChainedHttpConfig.ChainedRequest cr = requestConfig.getChainedRequest();
            this.theUri = cr.getUri().toURI();
            this.connection = (HttpURLConnection) theUri.toURL().openConnection();
            this.connection.setRequestMethod(verb);
            
            if (cr.actualBody() != null) {
                this.connection.setDoOutput(true);
            }
        }
        
        private void addHeaders() throws URISyntaxException {
            final ChainedHttpConfig.ChainedRequest cr = requestConfig.getChainedRequest();
            for (Map.Entry<String, String> entry : cr.actualHeaders(new LinkedHashMap<>()).entrySet()) {
                connection.addRequestProperty(entry.getKey(), entry.getValue());
            }

            final String contentType = cr.actualContentType();
            if (contentType != null) {
                connection.addRequestProperty("Content-Type", contentType);
            }

            connection.addRequestProperty("Accept-Encoding", "gzip, deflate");
            for (Map.Entry<String, String> e : cookiesToAdd(clientConfig, cr).entrySet()) {
                connection.addRequestProperty(e.getKey(), e.getValue());
            }
        }

        private PasswordAuthentication getAuthInfo() {
            final HttpConfig.Auth auth = requestConfig.getChainedRequest().actualAuth();
            if (auth == null) {
                return null;
            }

            if (auth.getAuthType() == HttpConfig.AuthType.BASIC || auth.getAuthType() == HttpConfig.AuthType.DIGEST) {
                return new PasswordAuthentication(auth.getUser(), auth.getPassword().toCharArray());
            } else {
                throw new UnsupportedOperationException("HttpURLConnection does not support " + auth.getAuthType() + " authentication");
            }
        }

        private Object handleFromServer() throws IOException {
            return HANDLER_FUNCTION.apply(requestConfig, new JavaFromServer(theUri));
        }

        public Object execute() throws Exception {
            return ThreadLocalAuth.with(getAuthInfo(), () -> {
                    if (sslContext != null && connection instanceof HttpsURLConnection) {
                        HttpsURLConnection https = (HttpsURLConnection) connection;
                        
                        if (hostnameVerifier != null) {
                            https.setHostnameVerifier(hostnameVerifier);
                        }

                        https.setSSLSocketFactory(sslContext.getSocketFactory());
                    }

                    final ChainedHttpConfig.ChainedRequest cr = requestConfig.getChainedRequest();

                    JavaToServer j2s = null;
                    if (cr.actualBody() != null) {
                        j2s = new JavaToServer();
                        requestConfig.findEncoder().accept(requestConfig, j2s);
                    }

                    addHeaders();

                    connection.connect();

                    if (j2s != null) {
                        j2s.transfer();
                    }

                    return handleFromServer();
                });
        }

        protected class JavaToServer implements ToServer {

            private InputStream inputStream;

            public void toServer(final InputStream inputStream) {
                this.inputStream = inputStream;
            }

            void transfer() throws IOException {
                NativeHandlers.Parsers.transfer(inputStream, connection.getOutputStream(), true);
            }
        }

        protected class JavaFromServer implements FromServer {

            private final InputStream is;
            private final List<Header<?>> headers;
            private final URI uri;
            private final int statusCode;
            private final String message;

            public JavaFromServer(final URI originalUri) throws IOException {
                this.uri = originalUri;
                headers = populateHeaders();
                addCookieStore(uri, headers);
                statusCode = connection.getResponseCode();
                message = connection.getResponseMessage();
                BufferedInputStream bis = buffered(correctInputStream());
                is = (bis == null) ? null : handleEncoding(bis);
            }

            private BufferedInputStream buffered(final InputStream is) throws IOException {
                if(is == null) {
                    return null;
                }
                
                final BufferedInputStream bis = new BufferedInputStream(is);
                bis.mark(0);
                if(bis.read() == -1) {
                    return null;
                }
                else {
                    bis.reset();
                    return bis;
                }
            }

            private InputStream correctInputStream() throws IOException {
                if(getStatusCode() < 400) {
                    return connection.getInputStream();
                }
                else {
                    return connection.getErrorStream();
                }
            }

            private InputStream handleEncoding(final InputStream is) throws IOException {
                Header<?> encodingHeader = Header.find(headers, "Content-Encoding");
                if (encodingHeader != null) {
                    if (encodingHeader.getValue().equals("gzip")) {
                        return new GZIPInputStream(is);
                    } else if (encodingHeader.getValue().equals("deflate")) {
                        return new InflaterInputStream(is);
                    }
                }

                return is;
            }

            private String clean(final String str) {
                if (str == null) {
                    return null;
                }

                final String tmp = str.trim();
                return "".equals(tmp) ? null : tmp;
            }

            private List<Header<?>> populateHeaders() {
                final List<Header<?>> ret = new ArrayList<>();
                for (int i = 0; i < Integer.MAX_VALUE; ++i) {
                    final String key = clean(connection.getHeaderFieldKey(i));
                    final String value = clean(connection.getHeaderField(i));
                    if (key == null && value == null) {
                        break;
                    }

                    if (key != null && value != null) {
                        ret.add(Header.keyValue(key.trim(), value.trim()));
                    }
                }

                return Collections.unmodifiableList(ret);
            }

            public InputStream getInputStream() {
                return is;
            }

            public final int getStatusCode() {
                return statusCode;
            }

            public String getMessage() {
                return message;
            }
            
            public List<Header<?>> getHeaders() {
                return headers;
            }

            public boolean getHasBody() {
                return is != null;
            }

            public URI getUri() {
                return uri;
            }

            public void finish() {
                //do nothing, should auto cleanup
            }
        }
    }

    protected static class ThreadLocalAuth extends Authenticator {
        private static final ThreadLocal<PasswordAuthentication> tlAuth = new ThreadLocal<PasswordAuthentication>();

        public PasswordAuthentication getPasswordAuthentication() {
            return tlAuth.get();
        }

        public static final <V> V with(final PasswordAuthentication pa, final Callable<V> callable) throws Exception {
            tlAuth.set(pa);
            try {
                return callable.call();
            } finally {
                tlAuth.set(null);
            }
        }
    }

    static {
        Authenticator.setDefault(new ThreadLocalAuth());
    }

    final private ChainedHttpConfig config;
    final private Executor executor;
    final private SSLContext sslContext;
    final private HostnameVerifier hostnameVerifier;
    final private HttpObjectConfig.Client clientConfig;

    public JavaHttpBuilder(final HttpObjectConfig config) {
        super(config);
        this.config = new HttpConfigs.ThreadSafeHttpConfig(config.getChainedConfig());
        this.executor = config.getExecution().getExecutor();
        this.clientConfig = config.getClient();
        this.hostnameVerifier = config.getExecution().getHostnameVerifier();
        this.sslContext = config.getExecution().getSslContext();
    }

    protected ChainedHttpConfig getObjectConfig() {
        return config;
    }
    
    private Object createAndExecute(final ChainedHttpConfig config, final String verb) {
        try {
            Action action = new Action(config, verb);
            return action.execute();
        }
        catch(Exception e) {
            return handleException(config.getChainedResponse(), e);
        }
    }

    protected Object doGet(final ChainedHttpConfig requestConfig) {
        return createAndExecute(requestConfig, "GET");
    }

    protected Object doHead(final ChainedHttpConfig requestConfig) {
        return createAndExecute(requestConfig, "HEAD");
    }

    protected Object doPost(final ChainedHttpConfig requestConfig) {
        return createAndExecute(requestConfig, "POST");
    }

    protected Object doPut(final ChainedHttpConfig requestConfig) {
        return createAndExecute(requestConfig, "PUT");
    }

    protected Object doDelete(final ChainedHttpConfig requestConfig) {
        return createAndExecute(requestConfig, "DELETE");
    }

    protected Object doPatch(final ChainedHttpConfig requestConfig) {
        // TODO Fail fast here?
        // We know that HttpURLConnection.setRequestMethod will throw an exception
        // for the PATCH method.
        // A hack-around is to set header "X-HTTP-Method-Override=PATCH" and
        // send a POST message instead.  It looks like this would be done around
        // line 58 above.
        return createAndExecute(requestConfig, "PATCH");
    }

    public Executor getExecutor() {
        return executor;
    }

    public void close() {
        //do nothing, not needed
    }
}

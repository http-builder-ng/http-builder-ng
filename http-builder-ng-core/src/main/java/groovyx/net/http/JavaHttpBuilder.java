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

import groovyx.net.http.util.IoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
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

    private static final Logger log = LoggerFactory.getLogger(JavaHttpBuilder.class);
    private static final Logger contentLog = LoggerFactory.getLogger("groovy.net.http.JavaHttpBuilder.content");
    private static final Logger headerLog = LoggerFactory.getLogger("groovy.net.http.JavaHttpBuilder.headers");

    protected class Action {

        private final HttpURLConnection connection;
        private final ChainedHttpConfig requestConfig;
        private final URI theUri;

        private boolean isProxied() {
            return proxyInfo != null && proxyInfo.getProxy().type() != Proxy.Type.DIRECT;
        }

        public Action(final Consumer<Object> clientCustomizer, final ChainedHttpConfig requestConfig, final String verb) throws IOException, URISyntaxException {
            this.requestConfig = requestConfig;

            final ChainedHttpConfig.ChainedRequest cr = requestConfig.getChainedRequest();
            theUri = cr.getUri().toURI();

            final URL url = theUri.toURL();
            connection = (HttpURLConnection) (isProxied() ? url.openConnection(proxyInfo.getProxy()) : url.openConnection());
            connection.setRequestMethod(verb);

            if (cr.actualBody() != null) {
                connection.setDoOutput(true);
            }

            if (clientCustomizer != null) {
                clientCustomizer.accept(connection);
            }
        }

        private void addHeaders() throws URISyntaxException {
            final ChainedHttpConfig.ChainedRequest cr = requestConfig.getChainedRequest();
            for (Map.Entry<String, CharSequence> entry : cr.actualHeaders(new LinkedHashMap<>()).entrySet()) {
                connection.addRequestProperty(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : null);
            }

            final String contentType = cr.actualContentType();
            if (contentType != null) {
                final Charset charset = cr.actualCharset();
                if (charset != null) {
                    connection.addRequestProperty("Content-Type", contentType + "; charset=" + charset.toString().toLowerCase());
                } else {
                    connection.addRequestProperty("Content-Type", contentType);
                }
            }

            connection.addRequestProperty("Accept-Encoding", "gzip, deflate");

            for (Map.Entry<String, String> e : cookiesToAdd(clientConfig, cr).entrySet()) {
                connection.addRequestProperty(e.getKey(), e.getValue());
            }

            if (headerLog.isDebugEnabled()) {
                connection.getRequestProperties().forEach((name, values) -> headerLog.debug("Request-Header: {} -> {}", name, values));
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

                if (log.isDebugEnabled()) {
                    log.debug("Request-URI({}): {}", connection.getRequestMethod(), theUri);
                }

                addHeaders();

                connection.connect();

                if (j2s != null) {
                    if (contentLog.isDebugEnabled()) {
                        contentLog.debug("Request-Body({}): {}", requestConfig.getChainedRequest().actualContentType(), j2s.content());
                    }

                    j2s.transfer();

                }

                final JavaFromServer fromServer = new JavaFromServer(theUri);

                if (contentLog.isDebugEnabled()) {
                    contentLog.debug("Response-Body: {}", fromServer.content());
                }

                if (headerLog.isDebugEnabled()) {
                    fromServer.getHeaders().forEach(header -> headerLog.debug("Response-Header: {} -> {}", header.getKey(), header.getValue()));
                }

                return HANDLER_FUNCTION.apply(requestConfig, fromServer);
            });
        }

        protected class JavaToServer implements ToServer {

            private BufferedInputStream inputStream;

            public void toServer(final InputStream inputStream) {
                this.inputStream = inputStream instanceof BufferedInputStream ? (BufferedInputStream) inputStream : new BufferedInputStream(inputStream);
            }

            void transfer() throws IOException {
                IoUtils.transfer(inputStream, connection.getOutputStream(), true);
            }

            public String content() {
                try {
                    return IoUtils.copyAsString(inputStream);
                } catch (IOException ioe) {
                    if (log.isWarnEnabled()) {
                        log.warn("Unable to render request stream due to error (may not affect actual content)", ioe);
                    }
                } catch (IllegalStateException ise) {
                    if (log.isErrorEnabled()) {
                        log.error("Unable to reset request stream - actual content may be corrupted (consider disabling content logging)", ise);
                    }
                }
                return "<no-information>";
            }
        }

        protected class JavaFromServer implements FromServer {

            private final BufferedInputStream is;
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

            String content() {
                try {
                    return IoUtils.copyAsString(is);
                } catch (IOException ioe) {
                    log.warn("Unable to render response stream due to error (may not affect actual content)", ioe);
                } catch (IllegalStateException ise) {
                    log.error("Unable to reset response stream - actual content may be corrupted (consider disabling content logging)", ise);
                }
                return "<no-information>";
            }

            private BufferedInputStream buffered(final InputStream is) throws IOException {
                if (is == null) {
                    return null;
                }

                final BufferedInputStream bis = new BufferedInputStream(is);
                bis.mark(0);
                if (bis.read() == -1) {
                    return null;
                } else {
                    bis.reset();
                    return bis;
                }
            }

            private InputStream correctInputStream() throws IOException {
                if (getStatusCode() < 400) {
                    return connection.getInputStream();
                } else {
                    return connection.getErrorStream();
                }
            }

            private BufferedInputStream handleEncoding(final BufferedInputStream is) throws IOException {
                Header<?> encodingHeader = Header.find(headers, "Content-Encoding");
                if (encodingHeader != null) {
                    if (encodingHeader.getValue().equals("gzip")) {
                        return new BufferedInputStream(new GZIPInputStream(is));
                    } else if (encodingHeader.getValue().equals("deflate")) {
                        return new BufferedInputStream(new InflaterInputStream(is));
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

    private final ChainedHttpConfig config;
    private final Executor executor;
    private final SSLContext sslContext;
    private final ProxyInfo proxyInfo;
    private final HostnameVerifier hostnameVerifier;
    private final HttpObjectConfig.Client clientConfig;

    protected JavaHttpBuilder(final HttpObjectConfig config) {
        super(config);
        this.config = config.getChainedConfig();
        this.executor = config.getExecution().getExecutor();
        this.clientConfig = config.getClient();
        this.hostnameVerifier = config.getExecution().getHostnameVerifier();
        this.sslContext = config.getExecution().getSslContext();
        this.proxyInfo = config.getExecution().getProxyInfo();
    }

    /**
     * The core Java client implementation does not support direct client access. This method will throw an {@link UnsupportedOperationException}.
     */
    @Override
    public Object getClientImplementation() {
        throw new UnsupportedOperationException("The core Java implementation does not support direct client access.");
    }

    protected ChainedHttpConfig getObjectConfig() {
        return config;
    }

    private Object createAndExecute(final ChainedHttpConfig config, final String verb) {
        try {
            Action action = new Action(clientConfig.getClientCustomizer(), config, verb);
            return action.execute();
        } catch (Exception e) {
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
        // The Java HttpURLConnection class only allows standard HTTP/1.1 verbs and will
        // throw a ProtocolException if the user tries to specified PATCH as the HTTP method.
        // See https://docs.oracle.com/javase/8/docs/api/java/net/HttpURLConnection.html#setRequestMethod-java.lang.String-
        throw new UnsupportedOperationException("java.net.HttpURLConnection does not support the PATCH method. Use the Apache or OkHttp providers instead.");
    }

    @Override
    protected Object doOptions(final ChainedHttpConfig config) {
        return createAndExecute(config, "OPTIONS");
    }

    @Override
    protected Object doTrace(final ChainedHttpConfig config) {
        return createAndExecute(config, "TRACE");
    }

    public Executor getExecutor() {
        return executor;
    }

    public void close() {
        //do nothing, not needed
    }
}

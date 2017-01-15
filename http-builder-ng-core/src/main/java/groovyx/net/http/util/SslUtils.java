package groovyx.net.http.util;

import groovyx.net.http.HttpObjectConfig;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/**
 * SSL helper utilities.
 */
public class SslUtils {

    /**
     * A `HostnameVerifier` that accepts any host name.
     */
    @SuppressWarnings("WeakerAccess")
    public static HostnameVerifier ANY_HOSTNAME = (s, sslSession) -> true;

    // trust manager that trusts everything
    private static final TrustManager[] TRUST_MANAGERS = {new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
        }

        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }

        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }
    }};

    /**
     * Configuration helper used to ignore any SSL certificate-related issues by configuring an `SSLContext` that allows everything.
     *
     * [source,groovy]
     * ----
     * def http = JavaHttpBuilder.configure {
     *     ignoreSslIssues(execution)
     * }
     * ----
     *
     * This will inject the correct configuration to set the `sslContext` and `hostnameVerifier` - these configuration properties should not
     * be directly specified when this method is applied.
     *
     * @param execution the {@link HttpObjectConfig.Execution} instance
     */
    public static void ignoreSslIssues(final HttpObjectConfig.Execution execution){
        execution.setSslContext(sslContext());
        execution.setHostnameVerifier(ANY_HOSTNAME);
    }

    private static SSLContext sslContext() {
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, TRUST_MANAGERS, new SecureRandom());
            return sslContext;

        } catch (NoSuchAlgorithmException | KeyManagementException ex) {
            throw new RuntimeException("Unable to create issue-ignoring SSLContext: " + ex.getMessage(), ex);
        }
    }
}

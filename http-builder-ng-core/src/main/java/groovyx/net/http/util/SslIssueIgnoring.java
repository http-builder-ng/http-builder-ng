package groovyx.net.http.util;

import groovyx.net.http.HttpObjectConfig;

import javax.net.ssl.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/**
 * FIXME: document
 */
public class SslIssueIgnoring {

    // trust manager that trusts everthging
    public static final TrustManager[] TRUST_MANAGERS = {new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
        }

        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }

        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }
    }};

    public static HostnameVerifier ANY_HOSTNAME = (s, sslSession) -> true;

    public static SSLContext sslContext() {
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, TRUST_MANAGERS, new SecureRandom());
            return sslContext;

        } catch (NoSuchAlgorithmException | KeyManagementException ex) {
            // FIXME: log this
            return null;
        }
    }

    public static void ignoreSslIssues(final HttpObjectConfig.Execution execution){
        execution.setSslContext(sslContext());
        execution.setHostnameVerifier(ANY_HOSTNAME);
    }
}

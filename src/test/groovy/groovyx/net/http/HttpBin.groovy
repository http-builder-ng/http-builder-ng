package groovyx.net.http

import groovy.transform.InheritConstructors
import groovy.transform.TypeChecked

/**
 * Used as the argument of a Spock @Requires annotation to denote that the annotated test(s) require the presence of the
 * httpbin.org web site for testing. If this site is not available, the annotated test(s) will be skipped.
 */
@InheritConstructors @TypeChecked
class HttpBin extends Closure<Boolean> {

    private static final URL HTTPBIN = 'http://httpbin.org/'.toURL()

    Boolean doCall() {
        HttpURLConnection conn = HTTPBIN.openConnection() as HttpURLConnection
        conn.setRequestMethod('HEAD')
        conn.setUseCaches(true)

        boolean status = conn.responseCode < 400
        conn.disconnect()

        status
    }
}
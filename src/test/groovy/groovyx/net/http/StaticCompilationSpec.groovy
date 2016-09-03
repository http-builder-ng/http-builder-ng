package groovyx.net.http;

import spock.lang.*;
import groovy.transform.CompileStatic;

class StaticCompilationSpec extends Specification {

    @CompileStatic
    def 'typed handlers should compile static'() {
        setup:
        def httpBin = HttpBuilder.configure {
            request.uri = 'http://httpbin.org/';
        }

        def resp = httpBin.get {
            response.success { FromServer fs, Object o ->
                return o.toString();
            }
        }

        String strResp = httpBin.get(String) {
            response.success { FromServer fs, Object o ->
                return o.toString();
            }
        }
    }
}

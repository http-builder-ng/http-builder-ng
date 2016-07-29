package groovyx.net.http;

import groovy.transform.TypeChecked;
import spock.lang.*
import java.util.function.Function;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import groovy.json.JsonSlurper;
import java.util.concurrent.Executors;
import java.util.function.Function;
import groovyx.net.http.libspecific.JavaHttpBuilder;
import static groovyx.net.http.NativeHandlers.*;

class JavaHttpBuilderTest extends Specification {

    static final Function javaBuilder = { c -> new JavaHttpBuilder(c); } as Function;

    def httpBin, google, pool;

    def setup() {
        def max = 2;
        def pool = Executors.newFixedThreadPool(max);
        
        httpBin = HttpBuilder.configure(javaBuilder) {
            request.uri = 'http://httpbin.org/';
            execution.maxThreads = max
            execution.executor = pool;
        }

        google = HttpBuilder.configure(javaBuilder) {
            request.uri = 'http://www.google.com';
            execution.maxThreads = max
            execution.executor = pool;
        };
    }

    //@Ignore
    def "Basic GET"() {
        expect:
        google.get {
            response.parser "text/html", Parsers.&textToString
        }.with {
            indexOf('</html>') != -1;
        }
    }

    //@Ignore
    def "GET with Parameters"() {
        expect:
        google.get(String) {
            response.parser "text/html", Parsers.&textToString
            request.uri.query = [ q: 'Big Bird' ];
        }.with {
            contains('Big Bird');
        }
    }

    //@Ignore
    def "Basic POST Form"() {
        setup:
        def toSend = [ foo: 'my foo', bar: 'my bar' ];

        expect:
        httpBin.post {
            request.uri.path = '/post'
            request.body = toSend;
            request.contentType = 'application/x-www-form-urlencoded';
        }.with {
            form == toSend;
        }
    }

    //@Ignore
    def "No Op POST Form"() {
        setup:
        def toSend = [ foo: 'my foo', bar: 'my bar' ];
        
        def http = HttpBuilder.configure(javaBuilder) {
            request.uri = 'http://httpbin.org/post'
            request.body = toSend;
            request.contentType = 'application/x-www-form-urlencoded';
        }
        
        expect:
        http.post().form == toSend;
    }

    //@Ignore
    def "POST Json With Parameters"() {
        setup:
        def toSend = [ lastName: 'Count', firstName: 'The', address: [ street: '123 Sesame Street' ] ];
        def accept = [ 'application/json','application/javascript','text/javascript' ];

        expect:
        httpBin.post {
            request.uri.path = '/post'
            request.uri.query = [ one: '1', two: '2' ];
            request.accept = accept;
            request.body = toSend;
            request.contentType = 'application/json';
        }.with {
            (it instanceof Map &&
             headers.Accept.split(';') as List<String> == accept && 
             new JsonSlurper().parseText(data) == toSend);
        }
    }

    //@Ignore
    def "Test POST Random Headers"() {
        setup:
        final myHeaders = [ One: '1', Two: '2', Buckle: 'my shoe' ].asImmutable();

        expect:
        httpBin.post {
            request.uri.path = '/post'
            request.contentType = 'application/json';
            request.headers = myHeaders;
        }.with {
            myHeaders.every { key, value -> headers[key] == value; };
        }
    }

    //@Ignore
    def "Test Head"() {
        setup:
        def result = google.head {
            response.success { resp, o ->
                assert(o == null);
                assert(resp.headers.size() > 0);
                return "Success"
            }
        }
        
        expect:
        result == "Success";
    }

    //@Ignore
    def "Test Multi-Threaded Head"() {
        expect:
        (0..<2).collect {
            google.headAsync();
        }.every {
            future -> future.get() == null;
        }
    }

    //@Ignore
    def "PUT Json With Parameters"() {
        setup:
        def toSend = [ lastName: 'Count', firstName: 'The', address: [ street: '123 Sesame Street' ] ];

        expect:
        httpBin.put {
            request.uri.path = '/put';
            request.uri.query = [ one: '1', two: '2' ];
            request.accept = ContentTypes.JSON;
            request.body = toSend;
            request.contentType = 'application/json';
        }.with {
            (it instanceof Map &&
             headers.Accept.split(';') as List<String> == ContentTypes.JSON &&
             new JsonSlurper().parseText(data) == toSend);
        }
    }

    //@Ignore
    def "Gzip and Deflate"() {
        expect:
        httpBin.get { request.uri.path = '/gzip'; }.gzipped;
        httpBin.get { request.uri.path = '/deflate'; }.deflated;
    }

    //@Ignore
    def "Basic Auth"() {
        expect:
        httpBin.get {
            request.uri.path = '/basic-auth/barney/rubble'
            request.auth.basic 'barney', 'rubble'
        }.with {
            authenticated && user == 'barney';
        }
    }

    //possibly this will work: https://gist.github.com/slightfoot/5624590
    //Even easier, Java auth: https://docs.oracle.com/javase/8/docs/technotes/guides/net/http-auth.html
    def "Digest Auth"() {
        expect:
        "Ignored" == httpBin.get {
            request.uri.path = '/digest-auth/auth/david/clark'
            request.auth.digest 'david', 'clark'
            response.failure { r -> "Ignored" } 
        };

        httpBin.get {
            request.uri.path = '/digest-auth/auth/david/clark'
            request.auth.digest 'david', 'clark'
        }.with {
            authenticated && user == 'david';
        }
    }
    
    def "Test Set Cookies"() {
        expect:
        httpBin.get {
            request.uri.path = '/cookies';
            request.cookie 'foocookie', 'barcookie'
        }.with {
            cookies.foocookie == 'barcookie';
        }
        
        httpBin.get {
            request.uri.path = '/cookies';
            request.cookie 'requestcookie', '12345'
        }.with {
            cookies.foocookie == 'barcookie' && cookies.requestcookie == '12345';
        }
    }

    def "Test Delete"() {
        setup:
        def myArgs = [ one: 'i', two: 'ii' ]

        expect:
        httpBin.delete {
            request.uri.path = '/delete'
            request.uri.query = myArgs
        }.with {
            args == myArgs;
        }
    }

    def "Test Custom Parser"() {
        setup:
        def newParser = { fromServer -> Parsers.textToString(fromServer).readLines(); };
        expect:
        httpBin.get {
            request.uri.path = '/stream/25'
            response.parser "application/json", newParser
        }.with {
            size() == 25;
        }
    }

    def "Basic Https"() {
        expect:
        HttpBuilder.configure(javaBuilder) {
            request.uri = 'https://www.google.com'
            response.parser "text/html", Parsers.&textToString
        }.get().with {
            indexOf('</html>') != -1;
        }
    }
}

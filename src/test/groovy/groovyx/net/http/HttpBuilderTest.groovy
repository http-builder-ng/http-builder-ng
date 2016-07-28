package groovyx.net.http;

import groovy.transform.TypeChecked;
import spock.lang.*
import java.util.function.Function;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import groovy.json.JsonSlurper;
import java.util.concurrent.Executors;
import java.util.function.Function;
import groovyx.net.http.libspecific.ApacheHttpBuilder;
import static groovyx.net.http.NativeHandlers.*;

class HttpBuilderTest extends Specification {

    static final Function apacheBuilder = { c -> new ApacheHttpBuilder(c); } as Function;

    def httpBin, google, pool;

    def setup() {
        def max = 2;
        def pool = Executors.newFixedThreadPool(max);
        
        httpBin = HttpBuilder.configure(apacheBuilder) {
            request.uri = 'http://httpbin.org/';
            execution.maxThreads = max
            execution.executor = pool;
        }

        google = HttpBuilder.configure(apacheBuilder) {
            request.uri = 'http://www.google.com';
            execution.maxThreads = max
            execution.executor = pool;
        };
    }
    
    def "Basic GET"() {
        expect:
        google.get {
            response.parser "text/html", Parsers.&textToString
        }.with {
            indexOf('</html>') != -1;
        }
    }
    
    def "GET with Parameters"() {
        expect:
        google.get(String) {
            response.parser "text/html", Parsers.&textToString
            request.uri.query = [ q: 'Big Bird' ];
        }.with {
            contains('Big Bird');
        }
    }

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

    def "No Op POST Form"() {
        setup:
        def toSend = [ foo: 'my foo', bar: 'my bar' ];

        def http = HttpBuilder.configure(apacheBuilder) {
            request.uri = 'http://httpbin.org/post'
            request.body = toSend;
            request.contentType = 'application/x-www-form-urlencoded';
        }
        
        expect:
        http.post().form == toSend;
    }

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

    def "Test Head"() {
        expect:
        !google.head();
    }

    def "Test Multi-Threaded Head"() {
        expect:
        (0..<2).collect {
            google.headAsync();
        }.every {
            future -> future.get() == null;
        }
    }

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

    def "Gzip and Deflate"() {
        expect:
        httpBin.get { request.uri.path = '/gzip'; }.gzipped;
        httpBin.get { request.uri.path = '/deflate'; }.deflated;
    }

    def "Basic Auth"() {
        expect:
        httpBin.get {
            request.uri.path = '/basic-auth/barney/rubble'
            request.auth.basic 'barney', 'rubble'
        }.with {
            authenticated && user == 'barney';
        }
    }

    def "Digest Auth"() {
        //NOTE, httpbin.org oddly requires cookies to be set during digest authentication,
        //which of course httpclient won't do. If you let the first request fail, then the cookie will
        //be set, which means the next request will have the cookie and will allow auth to succeed.
        expect:
        "Ignored" == httpBin.get {
            request.uri.path = '/digest-auth/auth/david/clark'
            request.auth.digest 'david', 'clark'
            response.failure = { r -> "Ignored" } 
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
            request.uri.path = '/cookies'
            request.cookie 'foocookie', 'barcookie'
        }.with {
            cookies.foocookie == 'barcookie';
        }

        httpBin.get {
            request.uri.path = '/cookies'
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
}

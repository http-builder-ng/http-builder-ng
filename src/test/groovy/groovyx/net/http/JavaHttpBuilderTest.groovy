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
    
    def "Basic GET"() {
        setup:
        def result = google.get {
            response.parser "text/html", NativeHandlers.Parsers.&textToString
        };

        println(result);

        expect:
        result instanceof String;
        result.indexOf('</html>') != -1;
    }

    // def "GET with Parameters"() {
    //     setup:
    //     String result = google.get(String) {
    //         response.parser "text/html", NativeHandlers.Parsers.&textToString
    //         request.uri.query = [ q: 'Big Bird' ];
    //     }

    //     expect:
    //     result.contains('Big Bird');
    // }

    // def "Basic POST Form"() {
    //     setup:
    //     def toSend = [ foo: 'my foo', bar: 'my bar' ];
    //     def result = httpBin.post {
    //         request.uri.path = '/post'
    //         request.body = toSend;
    //         request.contentType = 'application/x-www-form-urlencoded';
    //     }

    //     expect:
    //     result;
    //     result.form == toSend;
    // }

    // def "No Op POST Form"() {
    //     setup:
    //     def toSend = [ foo: 'my foo', bar: 'my bar' ];
    //     def http = HttpBuilder.configure(apacheBuilder) {
    //         request.uri = 'http://httpbin.org/post'
    //         request.body = toSend;
    //         request.contentType = 'application/x-www-form-urlencoded';
    //     }
        
    //     def result = http.post();

    //     expect:
    //     result;
    //     result.form == toSend;
    // }

    // def "POST Json With Parameters"() {
    //     setup:
    //     def toSend = [ lastName: 'Count', firstName: 'The', address: [ street: '123 Sesame Street' ] ];
    //     def accept = [ 'application/json','application/javascript','text/javascript' ];
        
    //     def result = httpBin.post {
    //         request.uri.path = '/post'
    //         request.uri.query = [ one: '1', two: '2' ];
    //         request.accept = accept;
    //         request.body = toSend;
    //         request.contentType = 'application/json';
    //     }

    //     expect:
    //     result instanceof Map;
    //     result.headers.Accept.split(';') as List<String> == accept;
    //     new JsonSlurper().parseText(result.data) == toSend;
    // }

    // def "Test POST Random Headers"() {
    //     setup:
    //     final headers = [ One: '1', Two: '2', Buckle: 'my shoe' ].asImmutable();
    //     def results = httpBin.post {
    //         request.uri.path = '/post'
    //         request.contentType = 'application/json';
    //         request.headers = headers;
    //     }

    //     expect:
    //     headers.every { key, value -> results.headers[key] == value; };
    // }

    // def "Test Head"() {
    //     setup:
    //     def result = google.head();

    //     expect:
    //     !result
    // }

    // def "Test Multi-Threaded Head"() {
    //     setup:
    //     def futures = (0..<2).collect { google.headAsync(); }

    //     expect:
    //     futures.size() == 2;
    //     futures.every { future -> future.get() == null; };
    // }

    // def "PUT Json With Parameters"() {
    //     setup:
    //     def toSend = [ lastName: 'Count', firstName: 'The', address: [ street: '123 Sesame Street' ] ];

    //     def result = httpBin.put {
    //         request.uri.path = '/put';
    //         request.uri.query = [ one: '1', two: '2' ];
    //         request.accept = ContentTypes.JSON;
    //         request.body = toSend;
    //         request.contentType = 'application/json';
    //     }

    //     expect:
    //     result instanceof Map;
    //     result.headers.Accept.split(';') as List<String> == ContentTypes.JSON;
    //     new JsonSlurper().parseText(result.data) == toSend;
    // }

    // def "Gzip and Deflate"() {
    //     expect:
    //     httpBin.get { request.uri.path = '/gzip'; }.gzipped;
    //     httpBin.get { request.uri.path = '/deflate'; }.deflated;
    // }

    // def "Basic Auth"() {
    //     expect:
    //     httpBin.get {
    //         request.uri.path = '/basic-auth/barney/rubble'
    //         request.auth.basic 'barney', 'rubble'
    //     }.with {
    //         authenticated && user == 'barney';
    //     }
    // }

    // def "Digest Auth"() {
    //     //NOTE, httpbin.org oddly requires cookies to be set during digest authentication,
    //     //which of course httpclient won't do. If you let the first request fail, then the cookie will
    //     //be set, which means the next request will have the cookie and will allow auth to succeed.
    //     expect:
    //     "Ignored" == httpBin.get {
    //         request.uri.path = '/digest-auth/auth/david/clark'
    //         request.auth.digest 'david', 'clark'
    //         response.failure = { r -> "Ignored" } 
    //     };

    //     httpBin.get {
    //         request.uri.path = '/digest-auth/auth/david/clark'
    //         request.auth.digest 'david', 'clark'
    //     }.with {
    //         authenticated && user == 'david';
    //     }
    // }

    // //TODO: Continue simplification
    // def "Test Set Cookies"() {
    //     when:
    //     def http = HttpBuilder.configure(apacheBuilder) {
    //         request.uri = 'http://httpbin.org';
    //         request.cookie 'foocookie', 'barcookie'
    //     }

    //     def data = http.get {
    //         request.uri.path = '/cookies'
    //     }

    //     then:
    //     data.cookies.foocookie == 'barcookie';

    //     when:
    //     data = http.get {
    //         request.uri.path = '/cookies'
    //         request.cookie 'requestcookie', '12345'
    //     }

    //     then:
    //     data.cookies.foocookie == 'barcookie';
    //     data.cookies.requestcookie == '12345';
    // }

    // def "Test Delete"() {
    //     setup:
    //     def args = [ one: 'i', two: 'ii' ]
    //     def http = HttpBuilder.configure(apacheBuilder) {
    //         request.uri = 'http://httpbin.org'
    //         execution.maxThreads = 5
    //     }

    //     when:
    //     def data = http.delete {
    //         request.uri.path = '/delete'
    //         request.uri.query = args
    //     }

    //     then:
    //     data.args == args;
    // }

    // def "Test Custom Parser"() {
    //     setup:
    //     def http = HttpBuilder.configure(apacheBuilder) {
    //         request.uri = 'http://httpbin.org/'
    //     }

    //     when:
    //     def lines = http.get {
    //         request.uri.path = '/stream/25'
    //         response.parser "application/json", { fromServer ->
    //             NativeHandlers.Parsers.textToString(fromServer).readLines();
    //         }
    //     }

    //     then:
    //     lines.size() == 25
    // }
}

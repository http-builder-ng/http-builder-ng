/*
 * Copyright (C) 2016 David Clark
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
package groovyx.net.http

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonSlurper
import groovyx.net.http.optional.ApacheHttpBuilder
import groovyx.net.http.optional.Jackson
import spock.lang.Requires
import spock.lang.Specification

import java.util.concurrent.Executors
import java.util.function.Function

import static groovyx.net.http.NativeHandlers.Parsers
import static groovyx.net.http.optional.Csv.toCsv
import static groovyx.net.http.optional.Download.toTempFile

@Requires(HttpBin)
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
            request.uri.query = [q: 'Big Bird'];
        }.with {
            contains('Big Bird');
        }
    }

    def "Basic POST Form"() {
        setup:
        def toSend = [foo: 'my foo', bar: 'my bar'];

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
        def toSend = [foo: 'my foo', bar: 'my bar'];

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
        def toSend = [lastName: 'Count', firstName: 'The', address: [street: '123 Sesame Street']];
        def accept = ['application/json', 'application/javascript', 'text/javascript'];

        expect:
        httpBin.post {
            request.uri.path = '/post'
            request.uri.query = [one: '1', two: '2'];
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
        final myHeaders = [One: '1', Two: '2', Buckle: 'my shoe'].asImmutable();

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
        setup:
        def result = google.head {
            response.success { resp, o ->
                assert (o == null);
                assert (resp.headers.size() > 0);
            }
        }

        expect:
        !result
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
        def toSend = [lastName: 'Count', firstName: 'The', address: [street: '123 Sesame Street']];

        expect:
        httpBin.put {
            request.uri.path = '/put';
            request.uri.query = [one: '1', two: '2'];
            request.accept = ContentTypes.JSON;
            request.body = toSend;
            request.contentType = 'application/json';
        }.with {
            (it instanceof Map &&
                (headers.Accept.split(';') as List<String>).containsAll(ContentTypes.JSON as List<String>) &&
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
        def myArgs = [one: 'i', two: 'ii']

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
        def newParser = { config, fromServer -> Parsers.textToString(config, fromServer).readLines(); };
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
        HttpBuilder.configure(apacheBuilder) {
            request.uri = 'https://www.google.com'
            response.parser "text/html", Parsers.&textToString
        }.get().with {
            indexOf('</html>') != -1;
        }
    }

    def "Download"() {
        setup:
        def file = google.get { toTempFile(delegate); };

        expect:
        file.length() > 0;

        cleanup:
        file.delete();
    }

    def "Interceptors"() {
        setup:
        def obj = HttpBuilder.configure(apacheBuilder) {
            execution.interceptor(HttpVerb.values()) { config, func ->
                def orig = func.apply(config);
                return [orig: orig, msg: "I intercepted"]
            }

            request.uri = 'http://www.google.com'
        }

        def output = obj.get();

        expect:
        output instanceof Map
        output.orig
        output.msg == "I intercepted";
    }

    def "Optional HTML"() {
        setup:
        def obj = google.get();

        expect:
        obj;
        obj instanceof org.jsoup.nodes.Document;
    }

    def "Optional POST Json With Parameters With Jackson"() {
        setup:
        def objectMapper = new ObjectMapper();
        def toSend = [lastName: 'Count', firstName: 'The', address: [street: '123 Sesame Street']];
        def accept = ['application/json', 'application/javascript', 'text/javascript'];

        expect:
        httpBin.post {
            request.uri.path = '/post'
            request.uri.query = [one: '1', two: '2'];
            request.accept = accept;
            request.body = toSend;
            request.contentType = 'application/json';
            Jackson.mapper(delegate, objectMapper);
            Jackson.toType(delegate, Map);
        }.with {
            (it instanceof Map &&
                headers.Accept.split(';') as List<String> == accept &&
                new JsonSlurper().parseText(data) == toSend);
        }
    }

    def "Robots.txt as CSV"() {
        setup:
        List<String[]> result = httpBin.get {
            request.uri.path = '/robots.txt'
            toCsv(delegate, 'text/plain', ':' as Character, null)
        }

        expect:
        result.size() == 2
        result[0][0] == 'User-agent'
        result[0][1].trim() == '*'
        result[1][0] == 'Disallow'
        result[1][1].trim() == '/deny'
    }

    def 'Failure Handler Without Success Handler'() {
        setup:
        def statusCode = 0;

        def response = google.get {
            response.parser "text/html", Parsers.&textToString
            response.failure { FromServer fs ->
                statusCode = fs.statusCode;
            }
        }

        expect:
        statusCode == 0;
    }
}

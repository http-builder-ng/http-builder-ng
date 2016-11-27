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
package groovyx.net.http.tk

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonSlurper
import groovyx.net.http.ContentTypes
import groovyx.net.http.FromServer
import groovyx.net.http.HttpBuilder
import groovyx.net.http.HttpVerb
import groovyx.net.http.NativeHandlers
import groovyx.net.http.optional.Jackson
import org.jsoup.nodes.Document

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import static groovyx.net.http.optional.Csv.toCsv
import static groovyx.net.http.optional.Download.toTempFile

/**
 * Test kit used for testing different HttpBuilder implementations.
 */
abstract class HttpBuilderTestKit extends TestKit {

    protected static final int MAX_THREADS = 2

    protected HttpBuilder httpBin, google
    protected ExecutorService pool = Executors.newFixedThreadPool(MAX_THREADS)

    def init() {
        httpBin = HttpBuilder.configure(clientFactory) {
            request.uri = 'http://httpbin.org/'
            execution.maxThreads = MAX_THREADS
            execution.executor = pool
        }

        google = HttpBuilder.configure(clientFactory) {
            request.uri = 'http://www.google.com'
            execution.maxThreads = MAX_THREADS
            execution.executor = pool
        }
    }

    def "Basic GET"() {
        expect:
        google.get {
            response.parser "text/html", NativeHandlers.Parsers.&textToString
        }.with {
            indexOf('</html>') != -1
        }
    }

    def "GET with Parameters"() {
        expect:
        google.get(String) {
            response.parser "text/html", NativeHandlers.Parsers.&textToString
            request.uri.query = [q: 'Big Bird']
        }.with {
            contains('Big Bird')
        }
    }

    def "Basic POST Form"() {
        setup:
        def toSend = [foo: 'my foo', bar: 'my bar']

        expect:
        httpBin.post {
            request.uri.path = '/post'
            request.body = toSend
            request.contentType = 'application/x-www-form-urlencoded'
        }.with {
            form == toSend
        }
    }

    def "No Op POST Form"() {
        setup:
        def toSend = [foo: 'my foo', bar: 'my bar']

        def http = HttpBuilder.configure(clientFactory) {
            request.uri = 'http://httpbin.org/post'
            request.body = toSend
            request.contentType = 'application/x-www-form-urlencoded'
        }

        expect:
        http.post().form == toSend
    }

    def "POST Json With Parameters"() {
        setup:
        def toSend = [lastName: 'Count', firstName: 'The', address: [street: '123 Sesame Street']]
        def accept = ['application/json', 'application/javascript', 'text/javascript']

        expect:
        httpBin.post {
            request.uri.path = '/post'
            request.uri.query = [one: '1', two: '2']
            request.accept = accept
            request.body = toSend
            request.contentType = 'application/json'
        }.with {
            (it instanceof Map &&
                headers.Accept.split(';') as List<String> == accept &&
                new JsonSlurper().parseText(data) == toSend)
        }
    }

    def "Test POST Random Headers"() {
        setup:
        final myHeaders = [One: '1', Two: '2', Buckle: 'my shoe'].asImmutable()

        expect:
        httpBin.post {
            request.uri.path = '/post'
            request.contentType = 'application/json'
            request.headers = myHeaders
        }.with {
            myHeaders.every { key, value -> headers[key] == value }
        }
    }

    def "Test Head"() {
        setup:
        def result = google.head {
            response.success { resp, o ->
                assert (o == null)
                assert (resp.headers.size() > 0)
                return "Success"
            }
        }

        expect:
        result == "Success"
    }

    def "Test Multi-Threaded Head"() {
        expect:
        (0..<2).collect {
            google.headAsync()
        }.every {
            future -> future.get() == null
        }
    }

    def "PUT Json With Parameters"() {
        setup:
        def toSend = [lastName: 'Count', firstName: 'The', address: [street: '123 Sesame Street']]

        expect:
        httpBin.put {
            request.uri.path = '/put'
            request.uri.query = [one: '1', two: '2']
            request.accept = ContentTypes.JSON
            request.body = toSend
            request.contentType = 'application/json'
        }.with {
            (it instanceof Map &&
                headers.Accept.split(';') as List<String> == (ContentTypes.JSON as List<String>) &&
                new JsonSlurper().parseText(data) == toSend)
        }
    }

    def "Gzip and Deflate"() {
        expect:
        httpBin.get { request.uri.path = '/gzip' }.gzipped
        httpBin.get { request.uri.path = '/deflate' }.deflated
    }

    def "Basic Auth"() {
        expect:
        httpBin.get {
            request.uri.path = '/basic-auth/barney/rubble'
            request.auth.basic 'barney', 'rubble'
        }.with {
            authenticated && user == 'barney'
        }
    }

    def "Digest Auth"() {
        //Setting fake: fake_value is necessary to get httpbin to work correctly during authentication
        expect:
        httpBin.get {
            request.uri.path = '/digest-auth/auth/david/clark'
            request.cookie('fake', 'fake_value')
            request.auth.digest 'david', 'clark'
        }.with {
            authenticated && user == 'david'
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
            args == myArgs
        }
    }

    def "Test Custom Parser"() {
        setup:
        def newParser = { config, fromServer -> NativeHandlers.Parsers.textToString(config, fromServer).readLines() }
        expect:
        httpBin.get {
            request.uri.path = '/stream/25'
            response.parser "application/json", newParser
        }.with {
            size() == 25
        }
    }

    def "Basic Https"() {
        expect:
        HttpBuilder.configure(clientFactory) {
            request.uri = 'https://www.google.com'
            response.parser "text/html", NativeHandlers.Parsers.&textToString
        }.get().with {
            indexOf('</html>') != -1
        }
    }

    def "Download"() {
        setup:
        def file = google.get { toTempFile(delegate) }

        expect:
        file.length() > 0

        cleanup:
        file.delete()
    }

    def "Interceptors"() {
        setup:
        def obj = HttpBuilder.configure(clientFactory) {
            execution.interceptor(HttpVerb.values()) { config, func ->
                def orig = func.apply(config)
                return [orig: orig, msg: "I intercepted"]
            }

            request.uri = 'http://www.google.com'
        }

        def output = obj.get()

        expect:
        output instanceof Map
        output.orig
        output.msg == "I intercepted"
    }

    def "Optional HTML"() {
        setup:
        def obj = google.get()

        expect:
        obj
        obj instanceof Document
    }

    def "Optional POST Json With Parameters With Jackson"() {
        setup:
        def objectMapper = new ObjectMapper()
        def toSend = [lastName: 'Count', firstName: 'The', address: [street: '123 Sesame Street']]
        def accept = ['application/json', 'application/javascript', 'text/javascript']

        expect:
        httpBin.post(Map) {
            request.uri.path = '/post'
            request.uri.query = [one: '1', two: '2']
            request.accept = accept
            request.body = toSend
            request.contentType = 'application/json'
            Jackson.mapper(delegate, objectMapper)
        }.with {
            (it instanceof Map &&
                headers.Accept.split(';') as List<String> == accept &&
                new JsonSlurper().parseText(data) == toSend)
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

    def "Failure Handler Without Success Handler"() {
        setup:
        def statusCode = 0

        def response = google.get {
            response.failure { FromServer fs ->
                statusCode = fs.statusCode
            }
        }

        expect:
        statusCode == 0
    }

    def cleanup() {
        pool.shutdownNow()
    }
}

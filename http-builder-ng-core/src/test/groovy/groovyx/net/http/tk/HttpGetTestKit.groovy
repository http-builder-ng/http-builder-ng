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

import groovyx.net.http.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import spock.lang.Issue
import spock.lang.Requires
import spock.lang.Unroll

import java.util.concurrent.Executors

/**
 * Test kit for testing the HTTP GET method with different clients.
 */
abstract class HttpGetTestKit extends TestKit {

    @Unroll def 'GET /status(#status): verify when handler'() {
        given:
        serverRule.dispatcher { RecordedRequest request ->
            if (request.method == 'GET') {
                if (request.path == '/status200') {
                    return new MockResponse().setResponseCode(200)
                } else if (request.path == '/status300') {
                    return new MockResponse().setResponseCode(300)
                } else if (request.path == '/status400') {
                    return new MockResponse().setResponseCode(400)
                } else if (request.path == '/status500') {
                    return new MockResponse().setResponseCode(500)
                }
            }
            return new MockResponse().setResponseCode(404)
        }

        CountedClosure counter = new CountedClosure()

        def config = {
            request.uri.path = "/status${status}"
            response.when status, counter.closure
        }

        when:
        httpBuilder(serverRule.serverUrl).get config

        then:
        counter.called
        counter.clear()

        when:
        httpBuilder(serverRule.serverUrl).getAsync(config).get()

        then:
        counter.called

        where:
        status << ['200', '300', '400', '500']
    }

    @Unroll def 'GET /status(#status): success/failure handler'() {
        given:
        serverRule.dispatcher { RecordedRequest request ->
            if (request.method == 'GET') {
                if (request.path == '/status200') {
                    return new MockResponse().setResponseCode(200)
                } else if (request.path == '/status300') {
                    return new MockResponse().setResponseCode(300)
                } else if (request.path == '/status400') {
                    return new MockResponse().setResponseCode(400)
                } else if (request.path == '/status500') {
                    return new MockResponse().setResponseCode(500)
                }
            }
            return new MockResponse().setResponseCode(404)
        }

        CountedClosure successCounter = new CountedClosure()
        CountedClosure failureCounter = new CountedClosure()

        def config = {
            request.uri.path = "/status${status}"
            response.success successCounter.closure
            response.failure failureCounter.closure
        }

        when:
        httpBuilder(serverRule.serverUrl).get config

        then:
        successCounter.called == success
        successCounter.clear()

        failureCounter.called == failure
        failureCounter.clear()

        when:
        httpBuilder(serverRule.serverUrl).getAsync(config).get()

        then:
        successCounter.called == success
        failureCounter.called == failure

        where:
        status | success | failure
        200    | true    | false
        300    | true    | false
        400    | false   | true
        500    | false   | true
    }

    @Unroll def 'GET /status(#status): with only failure handler'() {
        given:
        serverRule.dispatcher { RecordedRequest request ->
            if (request.method == 'GET') {
                if (request.path == '/status200') {
                    return new MockResponse().setResponseCode(200)
                } else if (request.path == '/status300') {
                    return new MockResponse().setResponseCode(300)
                } else if (request.path == '/status400') {
                    return new MockResponse().setResponseCode(400)
                } else if (request.path == '/status500') {
                    return new MockResponse().setResponseCode(500)
                }
            }
            return new MockResponse().setResponseCode(404)
        }

        CountedClosure failureCounter = new CountedClosure()

        def config = {
            request.uri.path = "/status${status}"
            response.failure failureCounter.closure
        }

        when:
        httpBuilder(serverRule.serverUrl).get config

        then:
        failureCounter.called == failure
        failureCounter.clear()

        when:
        httpBuilder(serverRule.serverUrl).getAsync(config).get()

        then:
        failureCounter.called == failure

        where:
        status | failure
        200    | false
        300    | false
        400    | true
        500    | true
    }

    def 'GET /: returns content'() {
        setup:
        serverRule.dispatcher('GET', '/', new MockResponse().setHeader('Content-Type', 'text/plain').setBody(htmlContent()))

        expect:
        httpBuilder(serverRule.serverUrl).get() == htmlContent()

        and:
        httpBuilder(serverRule.serverUrl).getAsync().get() == htmlContent()
    }

    def 'GET /foo: returns content'() {
        given:
        serverRule.dispatcher('GET', '/foo', new MockResponse().setHeader('Content-Type', 'text/plain').setBody(htmlContent()))

        def config = {
            request.uri.path = '/foo'
        }

        expect:
        httpBuilder(serverRule.serverUrl).get(config) == htmlContent()

        and:
        httpBuilder(serverRule.serverUrl).getAsync(config).get() == htmlContent()
    }

    def 'GET /xml: returns xml'() {
        given:
        serverRule.dispatcher('GET', '/xml', new MockResponse().setHeader('Content-Type', 'text/xml').setBody(xmlContent()))

        def config = {
            request.uri.path = '/xml'
            response.parser 'text/xml', NativeHandlers.Parsers.&xml
        }

        expect:
        httpBuilder(serverRule.serverUrl).get(config).child.text.text() == 'Nothing special'

        and:
        httpBuilder(serverRule.serverUrl).getAsync(config).get().child.text.text() == 'Nothing special'
    }

    def 'GET /json: returns json'() {
        given:
        serverRule.dispatcher('GET', '/json', new MockResponse().setHeader('Content-Type', 'text/json').setBody(jsonContent()))

        def config = {
            request.uri.path = '/json'
            response.parser 'text/json', NativeHandlers.Parsers.&json
        }

        when:
        def result = httpBuilder(serverRule.serverUrl).get(config)

        then:
        result.items[0].text == 'Nothing special'
        result.items[0].name == 'alpha'
        result.items[0].score == 123

        when:
        result = httpBuilder(serverRule.serverUrl).getAsync(config).get()

        then:
        result.items[0].text == 'Nothing special'
        result.items[0].name == 'alpha'
        result.items[0].score == 123
    }

    @Issue('https://github.com/http-builder-ng/http-builder-ng/issues/49')
    def 'GET /foo (cookie): returns content'() {
        given:
        serverRule.dispatcher { RecordedRequest request ->
            if (request.method == 'GET' && request.path == '/foo' && request.getHeader('Cookie').contains('biscuit=wafer')) {
                return new MockResponse().setHeader('Content-Type', 'text/plain').setBody(htmlContent())
            }
            return new MockResponse().setResponseCode(404)
        }

        def config = {
            request.uri.path = '/foo'
            request.cookie 'biscuit', 'wafer'
        }

        expect:
        httpBuilder(serverRule.serverUrl).get(config) == htmlContent()

        and:
        httpBuilder(serverRule.serverUrl).getAsync(config).get() == htmlContent()
    }

    @Issue('https://github.com/http-builder-ng/http-builder-ng/issues/49')
    def 'GET /foo (cookie2): returns content'() {
        given:
        serverRule.dispatcher { RecordedRequest request ->
            if (request.method == 'GET' && request.path == '/foo' && request.getHeader('Cookie').contains('coffee=black')) {
                return new MockResponse().setHeader('Content-Type', 'text/plain').setBody(htmlContent())
            }
            return new MockResponse().setResponseCode(404)
        }

        def config = {
            request.uri.path = '/foo'
            request.cookie 'coffee', 'black'
        }

        expect:
        httpBuilder(serverRule.serverUrl).get(config) == htmlContent()

        and:
        httpBuilder(serverRule.serverUrl).getAsync(config).get() == htmlContent()
    }

    def 'GET /foo?alpha=bravo: returns content'() {
        given:
        serverRule.dispatcher('GET', '/foo?alpha=bravo', new MockResponse().setHeader('Content-Type', 'text/plain').setBody(htmlContent()))

        def config = {
            request.uri.path = '/foo'
            request.uri.query = [alpha: 'bravo']
        }

        expect:
        httpBuilder(serverRule.serverUrl).get(config) == htmlContent()

        and:
        httpBuilder(serverRule.serverUrl).getAsync(config).get() == htmlContent()
    }

    def 'GET (BASIC) /basic: returns content'() {
        given:
        serverRule.dispatcher { RecordedRequest request ->
            if (request.method == 'GET') {
                String encodedCred = "Basic ${'admin:$3cr3t'.bytes.encodeBase64()}"

                if (request.path == '/basic' && !request.getHeader('Authorization')) {
                    return new MockResponse().setHeader('WWW-Authenticate', 'Basic realm="Test Realm"').setResponseCode(401)
                } else if (request.path == '/basic' && request.getHeader('Authorization') == encodedCred) {
                    return new MockResponse().setHeader('Authorization', encodedCred).setHeader('Content-Type', 'text/plain').setBody(htmlContent())
                }
            }
            return new MockResponse().setResponseCode(404)
        }

        def config = {
            request.uri.path = '/basic'
            request.auth.basic 'admin', '$3cr3t'
        }

        expect:
        httpBuilder(serverRule.serverUrl).get(config) == htmlContent()

        and:
        httpBuilder(serverRule.serverUrl).getAsync(config).get() == htmlContent()
    }

    def 'GET /date: returns content of specified type'() {
        given:
        serverRule.dispatcher('GET', '/date', new MockResponse().setHeader('Content-Type', 'text/date').setBody('2016.08.25 14:43'))

        def config = {
            request.uri.path = '/date'
            response.parser('text/date') { ChainedHttpConfig config, FromServer fromServer ->
                Date.parse('yyyy.MM.dd HH:mm', fromServer.inputStream.text)
            }
        }

        when:
        def result = httpBuilder(serverRule.serverUrl).get(Date, config)

        then:
        result instanceof Date
        result.format('MM/dd/yyyy HH:mm') == '08/25/2016 14:43'

        when:
        result = httpBuilder(serverRule.serverUrl).getAsync(Date, config).get()

        then:
        result instanceof Date
        result.format('MM/dd/yyyy HH:mm') == '08/25/2016 14:43'
    }

    @Requires(HttpBin) def 'GET (DIGEST) /digest-auth'() {
        given:
        def config = {
            request.uri = 'http://httpbin.org/'
            execution.maxThreads = 2
            execution.executor = Executors.newFixedThreadPool(2)
        }

        when:
        def httpClient = httpBuilder(config)
        def result = httpClient.get {
            request.uri.path = '/digest-auth/auth/david/clark'
            request.auth.digest 'david', 'clark'
            request.cookie('fake', 'fake_value')
        }

        then:
        result.authenticated
        result.user == 'david'

        when:
        httpClient = httpBuilder(config)
        result = httpClient.getAsync {
            request.uri.path = '/digest-auth/auth/david/clark'
            request.auth.digest 'david', 'clark'
            request.cookie('fake', 'fake_value')
        }.get()

        then:
        result.authenticated
        result.user == 'david'
    }
}

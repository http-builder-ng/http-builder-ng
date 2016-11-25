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

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Rule
import spock.lang.Issue
import spock.lang.Requires
import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.Executors

import static groovyx.net.http.HttpClientType.APACHE
import static groovyx.net.http.HttpClientType.JAVA

class HttpHeadSpec extends Specification {

    @Rule MockWebServerRule serverRule = new MockWebServerRule()

    private static final Map<String, String> HEADERS_A = [
        alpha: '100', sometime: '03/04/2015 15:45', Accept: 'text/plain', Connection: 'keep-alive'
    ].asImmutable()
    private static final Map<String, String> HEADERS_B = [bravo: '200', Accept: 'text/html', Connection: 'keep-alive'].asImmutable()
    private static final Map<String, String> HEADERS_C = [charlie: '200', Connection: 'keep-alive'].asImmutable()

    @Unroll def '[#client] HEAD /: returns no content'() {
        setup:
        serverRule.dispatcher('HEAD', '/', responseHeaders())

        expect:
        !HttpContent.httpBuilder(client, serverRule.serverPort).head()

        and:
        !HttpContent.httpBuilder(client, serverRule.serverPort).headAsync().get()

        where:
        client << [groovyx.net.http.HttpClientType.APACHE, groovyx.net.http.HttpClientType.JAVA]
    }

    @Unroll def '[#client] HEAD /foo: returns headers only'() {
        given:
        serverRule.dispatcher('HEAD', '/foo', responseHeaders())

        def capturedHeaders = [:]
        boolean hasBody = true

        def config = {
            request.uri.path = '/foo'
            response.success { resp ->
                hasBody = resp.hasBody
                resp.headers.each { FromServer.Header h ->
                    capturedHeaders[h.key] = h.value
                }
            }
        }

        when:
        HttpContent.httpBuilder(client, serverRule.serverPort).head(config)

        then:
        !hasBody
        applyDefaultHeaders(HEADERS_A) == capturedHeaders
        capturedHeaders.clear()

        when:
        HttpContent.httpBuilder(client, serverRule.serverPort).headAsync(config).get()

        then:
        !hasBody
        applyDefaultHeaders(HEADERS_A) == capturedHeaders

        where:
        client << [groovyx.net.http.HttpClientType.APACHE, groovyx.net.http.HttpClientType.JAVA]
    }

    @Unroll def '[#client] HEAD (BASIC) /basic: returns only headers'() {
        given:
        serverRule.dispatcher { RecordedRequest request ->
            if (request.method == 'HEAD') {
                String encodedCred = "Basic ${'admin:$3cr3t'.bytes.encodeBase64()}"

                if (request.path == '/basic' && !request.getHeader('Authorization')) {
                    return new MockResponse().setHeader('WWW-Authenticate', 'Basic realm="Test Realm"').setResponseCode(401)
                } else if (request.path == '/basic' && request.getHeader('Authorization') == encodedCred) {
                    return responseHeaders()
                }
            }
            return new MockResponse().setResponseCode(404)
        }

        def capturedHeaders = [:]
        boolean hasBody = true

        def config = {
            request.uri.path = '/basic'
            request.auth.basic 'admin', '$3cr3t'
            response.success { resp ->
                hasBody = resp.hasBody
                resp.headers.each { FromServer.Header h ->
                    capturedHeaders[h.key] = h.value
                }
            }
        }

        when:
        HttpContent.httpBuilder(client, serverRule.serverPort).head(config)

        then:
        !hasBody
        applyDefaultHeaders(HEADERS_A) == capturedHeaders
        capturedHeaders.clear()

        and:
        HttpContent.httpBuilder(client, serverRule.serverPort).headAsync(config).get()

        then:
        !hasBody
        applyDefaultHeaders(HEADERS_A) == capturedHeaders

        where:
        client << [groovyx.net.http.HttpClientType.APACHE, groovyx.net.http.HttpClientType.JAVA]
    }

    @Unroll @Requires(HttpBin) def '[#client] HEAD (DIGEST) /digest-auth'() {
        given:
        def config = {
            request.uri = 'http://httpbin.org/'
            execution.maxThreads = 2
            execution.executor = Executors.newFixedThreadPool(2)
        }

        when:
        def httpClient = HttpContent.httpBuilder(client, config)
        def authenticated = httpClient.head {
            request.uri.path = '/digest-auth/auth/david/clark'
            request.auth.digest 'david', 'clark'
            request.cookie('fake', 'fake_value')
            response.success { true }
        }

        then:
        authenticated

        when:
        httpClient = HttpContent.httpBuilder(client, config)
        authenticated = httpClient.headAsync {
            request.uri.path = '/digest-auth/auth/david/clark'
            request.auth.digest 'david', 'clark'
            request.cookie('fake', 'fake_value')
            response.success { true }
        }.get()

        then:
        authenticated

        where:
        client << [groovyx.net.http.HttpClientType.APACHE, groovyx.net.http.HttpClientType.JAVA]
    }

    @Issue('https://github.com/http-builder-ng/http-builder-ng/issues/49')
    @Unroll def '[#client] HEAD /foo (cookie): returns headers only'() {
        given:
        serverRule.dispatcher { RecordedRequest request ->
            if (request.method == 'HEAD' && request.path == '/foo' && request.getHeader('Cookie').contains('biscuit=wafer')) {
                return responseHeaders(new MockResponse(), HEADERS_B)
            }
            return new MockResponse().setResponseCode(404)
        }

        def capturedHeaders = [:]
        boolean hasBody = true

        def config = {
            request.uri.path = '/foo'
            request.cookie 'biscuit', 'wafer'
            response.success { resp ->
                hasBody = resp.hasBody
                resp.headers.each { FromServer.Header h ->
                    capturedHeaders[h.key] = h.value
                }
            }
        }

        when:
        HttpContent.httpBuilder(client, serverRule.serverPort).head(config)

        then:
        !hasBody
        applyDefaultHeaders(HEADERS_B) == capturedHeaders
        capturedHeaders.clear()

        when:
        HttpContent.httpBuilder(client, serverRule.serverPort).headAsync(config).get()

        then:
        !hasBody
        applyDefaultHeaders(HEADERS_B) == capturedHeaders

        where:
        client << [groovyx.net.http.HttpClientType.APACHE, groovyx.net.http.HttpClientType.JAVA]
    }

    @Unroll def '[#client] HEAD /foo?alpha=bravo: returns headers only'() {
        given:
        serverRule.dispatcher('HEAD', '/foo?alpha=bravo', responseHeaders(new MockResponse(), HEADERS_C))

        def capturedHeaders = [:]
        boolean hasBody = true

        def config = {
            request.uri.path = '/foo'
            request.uri.query = [alpha: 'bravo']
            response.success { resp ->
                hasBody = resp.hasBody
                resp.headers.each { FromServer.Header h ->
                    capturedHeaders[h.key] = h.value
                }
            }
        }

        when:
        HttpContent.httpBuilder(client, serverRule.serverPort).head(config)

        then:
        !hasBody
        applyDefaultHeaders(HEADERS_C) == capturedHeaders
        capturedHeaders.clear()

        when:
        HttpContent.httpBuilder(client, serverRule.serverPort).headAsync(config).get()

        then:
        !hasBody
        applyDefaultHeaders(HEADERS_C) == capturedHeaders

        where:
        client << [groovyx.net.http.HttpClientType.APACHE, groovyx.net.http.HttpClientType.JAVA]
    }

    @Unroll def '[#client] HEAD /status#status: verify when handler'() {
        given:
        serverRule.dispatcher { RecordedRequest request ->
            if (request.method == 'HEAD') {
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
        HttpContent.httpBuilder(client, serverRule.serverPort).head config

        then:
        counter.called
        counter.clear()

        when:
        HttpContent.httpBuilder(client, serverRule.serverPort).headAsync(config).get()

        then:
        counter.called

        where:
        client                                 | status
        groovyx.net.http.HttpClientType.APACHE | '200'
        groovyx.net.http.HttpClientType.APACHE | '300'
        groovyx.net.http.HttpClientType.APACHE | '400'
        groovyx.net.http.HttpClientType.APACHE | '500'
        groovyx.net.http.HttpClientType.JAVA   | '200'
        groovyx.net.http.HttpClientType.JAVA   | '300'
        groovyx.net.http.HttpClientType.JAVA   | '400'
        groovyx.net.http.HttpClientType.JAVA   | '500'
    }

    @Unroll def '[#client] HEAD /status#status: verify success/failure handler'() {
        given:
        serverRule.dispatcher { RecordedRequest request ->
            if (request.method == 'HEAD') {
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
        HttpContent.httpBuilder(client, serverRule.serverPort).head config

        then:
        successCounter.called == success
        successCounter.clear()

        failureCounter.called == failure
        failureCounter.clear()

        when:
        HttpContent.httpBuilder(client, serverRule.serverPort).headAsync(config).get()

        then:
        successCounter.called == success
        failureCounter.called == failure

        where:
        client                                 | status | success | failure
        groovyx.net.http.HttpClientType.APACHE | 200    | true    | false
        groovyx.net.http.HttpClientType.APACHE | 300    | true    | false
        groovyx.net.http.HttpClientType.APACHE | 400    | false   | true
        groovyx.net.http.HttpClientType.APACHE | 500    | false   | true
        groovyx.net.http.HttpClientType.JAVA   | 200    | true    | false
        groovyx.net.http.HttpClientType.JAVA   | 300    | true    | false
        groovyx.net.http.HttpClientType.JAVA   | 400    | false   | true
        groovyx.net.http.HttpClientType.JAVA   | 500    | false   | true
    }

    @Unroll def '[#client] HEAD /date: returns content of specified type'() {
        given:
        serverRule.dispatcher('HEAD', '/date', responseHeaders(new MockResponse(), [stamp: '2016.08.25 14:43']))

        def config = {
            request.uri.path = '/date'
            response.success { FromServer fromServer ->
                Date.parse('yyyy.MM.dd HH:mm', fromServer.headers.find { it.key == 'stamp' }.value)
            }
        }

        when:
        def result = HttpContent.httpBuilder(client, serverRule.serverPort).head(Date, config)

        then:
        result instanceof Date
        result.format('MM/dd/yyyy HH:mm') == '08/25/2016 14:43'

        when:
        result = HttpContent.httpBuilder(client, serverRule.serverPort).headAsync(Date, config).get()

        then:
        result instanceof Date
        result.format('MM/dd/yyyy HH:mm') == '08/25/2016 14:43'

        where:
        client << [groovyx.net.http.HttpClientType.APACHE, groovyx.net.http.HttpClientType.JAVA]
    }

    private static Map<String, String> applyDefaultHeaders(final Map<String, String> headers) {
        headers + [
            'Content-Length': '0',
            'Connection'    : 'keep-alive'
        ]
    }

    private static MockResponse responseHeaders(final MockResponse response = new MockResponse(), Map<String, String> headers = HEADERS_A) {
        headers.each { k, v ->
            response.setHeader(k, v)
        }
        response
    }
}

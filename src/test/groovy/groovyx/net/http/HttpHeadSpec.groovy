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

import org.junit.Rule
import org.mockserver.client.server.MockServerClient
import org.mockserver.junit.MockServerRule
import org.mockserver.model.HttpResponse
import org.mockserver.model.NottableString
import spock.lang.Requires
import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.Executors

import static groovyx.net.http.HttpClientType.APACHE
import static groovyx.net.http.HttpClientType.JAVA
import static groovyx.net.http.MockServerHelper.head
import static groovyx.net.http.MockServerHelper.httpBuilder
import static org.mockserver.model.HttpRequest.request
import static org.mockserver.model.HttpResponse.response

class HttpHeadSpec extends Specification {

    /*
    Object head()
    T head(final Class<T> type, @DelegatesTo(HttpConfig.class) final Closure closure)
    CompletableFuture<Object> headAsync()
    CompletableFuture<Object> headAsync(@DelegatesTo(HttpConfig.class)
    CompletableFuture<T> headAsync(final Class<T> type, @DelegatesTo(HttpConfig.class)
    Object head(@DelegatesTo(HttpConfig.class)
     */

    @Rule public MockServerRule serverRule = new MockServerRule(this)

    private static final Map<String, String> HEADERS_A = [alpha: '100', sometime: '03/04/2015 15:45', Accept: 'text/plain'].asImmutable()
    private static final Map<String, String> HEADERS_B = [bravo: '200', Accept: 'text/html'].asImmutable()
    private static final Map<String, String> HEADERS_C = [charlie: '200'].asImmutable()

    private MockServerClient server

    def setup() {
        server.when(head('/')).respond(responseHeaders())
        server.when(head('/foo').withQueryStringParameter('alpha', 'bravo')).respond(responseHeaders(HEADERS_C))
        server.when(head('/foo').withCookie('biscuit', 'wafer')).respond(responseHeaders(HEADERS_B))
        server.when(head('/foo')).respond(responseHeaders())

        server.when(head('/date')).respond(responseHeaders(stamp: '2016.08.25 14:43'))

        // Status handlers

        (2..5).each { s ->
            server.when(head("/status${s}00")).respond(response().withStatusCode(s * 100))
        }

        // BASIC auth

        String encodedCred = "Basic ${'admin:$3cr3t'.bytes.encodeBase64()}"
        server.when(head('/basic').withHeader(NottableString.not('Authorization'), NottableString.not(encodedCred)))
            .respond(responseHeaders('WWW-Authenticate': 'Basic realm="Test Realm"').withStatusCode(401))

        server.when(head('/basic').withHeader('Authorization', encodedCred)).respond(responseHeaders(HEADERS_A))
    }

    private static HttpResponse responseHeaders(final Map<String, Object> headers = HEADERS_A) {
        def resp = response()
        headers.each { k, v ->
            resp.withHeader(k as String, v as String)
        }
        resp
    }

    @Unroll def '[#client] HEAD /: returns no content'() {
        expect:
        !httpBuilder(client,serverRule.port).head()

        and:
        !httpBuilder(client,serverRule.port).headAsync().get()

        where:
        client << [APACHE, JAVA]
    }

    @Unroll def '[#client] HEAD /foo: returns headers only'() {
        given:
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
        httpBuilder(client,serverRule.port).head(config)

        then:
        !hasBody
        applyDefaultHeaders(HEADERS_A) == capturedHeaders
        capturedHeaders.clear()

        when:
        httpBuilder(client,serverRule.port).headAsync(config).get()

        then:
        !hasBody
        applyDefaultHeaders(HEADERS_A) == capturedHeaders

        where:
        client << [APACHE, JAVA]
    }

    @Unroll def '[#client] HEAD (BASIC) /basic: returns only headers'() {
        given:
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
        httpBuilder(client,serverRule.port).head(config)

        then:
        !hasBody
        applyDefaultHeaders(HEADERS_A) == capturedHeaders
        capturedHeaders.clear()

        and:
        httpBuilder(client,serverRule.port).headAsync(config).get()

        then:
        !hasBody
        applyDefaultHeaders(HEADERS_A) == capturedHeaders

        where:
        client << [APACHE, JAVA]
    }

    @Unroll @Requires(HttpBin) def '[#client] HEAD (DIGEST) /digest-auth'() {
        /* NOTE: httpbin.org oddly requires cookies to be set during digest authentication, which of course HttpClient won't do. If you let the first request fail,
                 then the cookie will be set, which means the next request will have the cookie and will allow auth to succeed.
         */
        given:
        def config = {
            request.uri = 'http://httpbin.org/'
            execution.maxThreads = 2
            execution.executor = Executors.newFixedThreadPool(2)
        }

        when:
        def httpClient = httpBuilder(client, config)
        def result = httpClient.head {
            request.uri.path = '/digest-auth/auth/david/clark'
            request.auth.digest 'david', 'clark'
            response.failure { r -> 'Ignored' }
        }

        then:
        result == 'Ignored'

        when:
        boolean authenticated = httpClient.head {
            request.uri = '/digest-auth/auth/david/clark'
            request.auth.digest 'david', 'clark'
            response.success { true }
        }

        then:
        authenticated

        when:
        httpClient = httpBuilder(client, config)
        result = httpClient.headAsync {
            request.uri.path = '/digest-auth/auth/david/clark'
            request.auth.digest 'david', 'clark'
            response.failure { r -> 'Ignored' }
        }.get()

        then:
        result == 'Ignored'

        when:
        authenticated = httpClient.headAsync() {
            request.uri = '/digest-auth/auth/david/clark'
            request.auth.digest 'david', 'clark'
            response.success { true }
        }.get()

        then:
        authenticated

        where:
        client << [APACHE, JAVA]
    }

    @Unroll def '[#client] HEAD /foo (cookie): returns headers only'() {
        given:
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
        httpBuilder(client,serverRule.port).head(config)

        then:
        !hasBody
        applyDefaultHeaders(HEADERS_B) == capturedHeaders
        capturedHeaders.clear()

        when:
        httpBuilder(client,serverRule.port).headAsync(config).get()

        then:
        !hasBody
        applyDefaultHeaders(HEADERS_B) == capturedHeaders

        where:
        client << [APACHE, JAVA]
    }

    @Unroll def '[#client] HEAD /foo?alpha=bravo: returns headers only'() {
        given:
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
        httpBuilder(client,serverRule.port).head(config)

        then:
        !hasBody
        applyDefaultHeaders(HEADERS_C) == capturedHeaders
        capturedHeaders.clear()

        when:
        httpBuilder(client,serverRule.port).headAsync(config).get()

        then:
        !hasBody
        applyDefaultHeaders(HEADERS_C) == capturedHeaders

        where:
        client << [APACHE, JAVA]
    }

    @Unroll def '[#client] HEAD /status#status: verify when handler'() {
        given:
        CountedClosure counter = new CountedClosure()

        def config = {
            request.uri.path = "/status${status}"
            response.when status, counter.closure
        }

        when:
        httpBuilder(client,serverRule.port).head config

        then:
        counter.called
        counter.clear()

        when:
        httpBuilder(client,serverRule.port).headAsync(config).get()

        then:
        counter.called

        where:
        client | status
        APACHE | '200'
        APACHE | '300'
        APACHE | '400'
        APACHE | '500'
        JAVA   | '200'
        JAVA   | '300'
        JAVA   | '400'
        JAVA   | '500'
    }

    @Unroll def '[#client] HEAD /status#status: verify success/failure handler'() {
        given:
        CountedClosure successCounter = new CountedClosure()
        CountedClosure failureCounter = new CountedClosure()

        def config = {
            request.uri.path = "/status${status}"
            response.success successCounter.closure
            response.failure failureCounter.closure
        }

        when:
        httpBuilder(client,serverRule.port).head config

        then:
        successCounter.called == success
        successCounter.clear()

        failureCounter.called == failure
        failureCounter.clear()

        when:
        httpBuilder(client,serverRule.port).headAsync(config).get()

        then:
        successCounter.called == success
        failureCounter.called == failure

        where:
        client | status | success | failure
        APACHE | 200    | true    | false
        APACHE | 300    | true    | false
        APACHE | 400    | false   | true
        APACHE | 500    | false   | true
        JAVA   | 200    | true    | false
        JAVA   | 300    | true    | false
        JAVA   | 400    | false   | true
        JAVA   | 500    | false   | true
    }

    @Unroll def '[#client] HEAD /date: returns content of specified type'() {
        given:
        def config = {
            request.uri.path = '/date'
            response.success { FromServer fromServer ->
                Date.parse('yyyy.MM.dd HH:mm', fromServer.headers.find { it.key == 'stamp' }.value)
            }
        }

        when:
        def result = httpBuilder(client,serverRule.port).head(Date, config)

        then:
        result instanceof Date
        result.format('MM/dd/yyyy HH:mm') == '08/25/2016 14:43'

        when:
        result = httpBuilder(client,serverRule.port).headAsync(Date, config).get()

        then:
        result instanceof Date
        result.format('MM/dd/yyyy HH:mm') == '08/25/2016 14:43'

        where:
        client << [APACHE, JAVA]
    }

    // TODO: maybe move this and the MockServerHelper to a Trait?
    private static Map<String, String> applyDefaultHeaders(final Map<String, String> headers) {
        headers + [
            'Content-Length': '0',
            'Connection'    : 'keep-alive'
        ]
    }
}

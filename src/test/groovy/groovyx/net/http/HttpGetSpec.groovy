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
import spock.lang.Ignore
import spock.lang.Requires
import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.Executors

import static HttpClientType.APACHE
import static HttpClientType.JAVA
import static groovyx.net.http.MockServerHelper.*

class HttpGetSpec extends Specification {

    private static final String HTML_CONTENT_B = htmlContent('Testing B')
    private static final String HTML_CONTENT_C = htmlContent('Testing C')

    @Rule MockWebServerRule serverRule = new MockWebServerRule()

    @Unroll def '[#client] GET /status(#status): verify when handler'() {
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
        httpBuilder(client).get config

        then:
        counter.called
        counter.clear()

        when:
        httpBuilder(client).getAsync(config).get()

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

    @Unroll def '[#label] GET /status(#status): success/failure handler'() {
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
        httpBuilder(label).get config

        then:
        successCounter.called == success
        successCounter.clear()

        failureCounter.called == failure
        failureCounter.clear()

        when:
        httpBuilder(label).getAsync(config).get()

        then:
        successCounter.called == success
        failureCounter.called == failure

        where:
        label  | status | success | failure
        APACHE | 200    | true    | false
        APACHE | 300    | true    | false
        APACHE | 400    | false   | true
        APACHE | 500    | false   | true
        JAVA   | 200    | true    | false
        JAVA   | 300    | true    | false
        JAVA   | 400    | false   | true
        JAVA   | 500    | false   | true
    }

    @Unroll def '[#label] GET /status(#status): with only failure handler'() {
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
        httpBuilder(label).get config

        then:
        failureCounter.called == failure
        failureCounter.clear()

        when:
        httpBuilder(label).getAsync(config).get()

        then:
        failureCounter.called == failure

        where:
        label  | status | failure
        APACHE | 200    | false
        APACHE | 300    | false
        APACHE | 400    | true
        APACHE | 500    | true
        JAVA   | 200    | false
        JAVA   | 300    | false
        JAVA   | 400    | true
        JAVA   | 500    | true
    }

    @Unroll def '[#label] GET /: returns content'() {
        setup:
        serverRule.dispatcher('GET', '/', new MockResponse().setHeader('Content-Type', 'text/plain').setBody(htmlContent()))

        expect:
        httpBuilder(label).get() == htmlContent()

        and:
        httpBuilder(label).getAsync().get() == htmlContent()

        where:
        label << [APACHE, JAVA]
    }

    @Unroll def '[#label] GET /foo: returns content'() {
        given:
        serverRule.dispatcher('GET', '/foo', new MockResponse().setHeader('Content-Type', 'text/plain').setBody(htmlContent()))

        def config = {
            request.uri.path = '/foo'
        }

        expect:
        httpBuilder(label).get(config) == htmlContent()

        and:
        httpBuilder(label).getAsync(config).get() == htmlContent()

        where:
        label << [APACHE, JAVA]
    }

    @Unroll def '[#label] GET /xml: returns xml'() {
        given:
        serverRule.dispatcher('GET', '/xml', new MockResponse().setHeader('Content-Type', 'text/xml').setBody(xmlContent()))

        def config = {
            request.uri.path = '/xml'
            response.parser 'text/xml', NativeHandlers.Parsers.&xml
        }

        expect:
        httpBuilder(label).get(config).child.text.text() == 'Nothing special'

        and:
        httpBuilder(label).getAsync(config).get().child.text.text() == 'Nothing special'

        where:
        label << [APACHE, JAVA]
    }

    @Unroll def '[#label] GET /json: returns json'() {
        given:
        serverRule.dispatcher('GET', '/json', new MockResponse().setHeader('Content-Type', 'text/json').setBody(jsonContent()))

        def config = {
            request.uri.path = '/json'
            response.parser 'text/json', NativeHandlers.Parsers.&json
        }

        when:
        def result = httpBuilder(label).get(config)

        then:
        result.items[0].text == 'Nothing special'
        result.items[0].name == 'alpha'
        result.items[0].score == 123

        when:
        result = httpBuilder(label).getAsync(config).get()

        then:
        result.items[0].text == 'Nothing special'
        result.items[0].name == 'alpha'
        result.items[0].score == 123

        where:
        label << [APACHE, JAVA]
    }

    @Ignore
    @Unroll def '[#label] GET /foo (cookie): returns content'() {
        given:
        serverRule.dispatcher { RecordedRequest request ->
            if (request.method == 'GET' && request.path == '/foo' && request.getHeader('Cookie') == 'biscuit=wafer') {
                return new MockResponse().setHeader('Content-Type', 'text/plain').setBody(HTML_CONTENT_C)
            }
            return new MockResponse().setResponseCode(404)
        }

        def config = {
            request.uri.path = '/foo'
            request.cookie 'biscuit', 'wafer'
        }

        expect:
        httpBuilder(label).get(config) == HTML_CONTENT_C

        and:
        httpBuilder(label).getAsync(config).get() == HTML_CONTENT_C

        where:
        label << [APACHE, JAVA]
    }

    @Unroll def '[#label] GET /foo?alpha=bravo: returns content'() {
        given:
        serverRule.dispatcher('GET', '/foo?alpha=bravo', new MockResponse().setHeader('Content-Type', 'text/plain').setBody(HTML_CONTENT_B))

        def config = {
            request.uri.path = '/foo'
            request.uri.query = [alpha: 'bravo']
        }

        expect:
        httpBuilder(label).get(config) == HTML_CONTENT_B

        and:
        httpBuilder(label).getAsync(config).get() == HTML_CONTENT_B

        where:
        label << [APACHE, JAVA]
    }

    @Unroll def '[#label] GET (BASIC) /basic: returns content'() {
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
        httpBuilder(label).get(config) == htmlContent()

        and:
        httpBuilder(label).getAsync(config).get() == htmlContent()

        where:
        label << [APACHE, JAVA]
    }

    @Unroll def '[#label] GET /date: returns content of specified type'() {
        given:
        serverRule.dispatcher('GET', '/date', new MockResponse().setHeader('Content-Type', 'text/date').setBody('2016.08.25 14:43'))

        def config = {
            request.uri.path = '/date'
            response.parser('text/date') { ChainedHttpConfig config, FromServer fromServer ->
                Date.parse('yyyy.MM.dd HH:mm', fromServer.inputStream.text)
            }
        }

        when:
        def result = httpBuilder(label).get(Date, config)

        then:
        result instanceof Date
        result.format('MM/dd/yyyy HH:mm') == '08/25/2016 14:43'

        when:
        result = httpBuilder(label).getAsync(Date, config).get()

        then:
        result instanceof Date
        result.format('MM/dd/yyyy HH:mm') == '08/25/2016 14:43'

        where:
        label << [APACHE, JAVA]
    }

    @Unroll @Requires(HttpBin) def '[#client] GET (DIGEST) /digest-auth'() {
        given:
        def config = {
            request.uri = 'http://httpbin.org/'
            execution.maxThreads = 2
            execution.executor = Executors.newFixedThreadPool(2)
        }

        when:
        def httpClient = httpBuilder(client, config);
        def result = httpClient.get {
            request.uri.path = '/digest-auth/auth/david/clark'
            request.auth.digest 'david', 'clark'
            request.cookie('fake', 'fake_value')
        }

        then:
        result.authenticated
        result.user == 'david'

        when:
        httpClient = httpBuilder(client, config)
        result = httpClient.getAsync {
            request.uri.path = '/digest-auth/auth/david/clark'
            request.auth.digest 'david', 'clark'
            request.cookie('fake', 'fake_value')
        }.get()

        then:
        result.authenticated
        result.user == 'david'

        where:
        client << [APACHE, JAVA]
    }

    private HttpBuilder httpBuilder(final HttpClientType clientType, Closure config = { request.uri = serverRule.serverUrl }) {
        MockServerHelper.httpBuilder(clientType, config)
    }
}

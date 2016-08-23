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

import groovyx.net.http.optional.ApacheHttpBuilder
import org.junit.Rule
import org.mockserver.client.server.MockServerClient
import org.mockserver.junit.MockServerRule
import org.mockserver.model.Header
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.model.NottableString
import spock.lang.Specification
import spock.lang.Unroll

import java.util.function.Function

import static HttpClientType.APACHE
import static HttpClientType.JAVA
import static org.mockserver.model.HttpRequest.request
import static org.mockserver.model.HttpResponse.response

class HttpGetSpec extends Specification {

    // FIXME: test digest support - probably just use the httpbin and make it conditional

    @Rule public MockServerRule serverRule = new MockServerRule(this)

    private static final String HTML_CONTENT_B = htmlContent('Testing B')
    private static final String HTML_CONTENT_C = htmlContent('Testing C')

    private MockServerClient server
    private HttpBuilder apacheHttp, javaHttp

    def setup() {
        apacheHttp = HttpBuilder.configure({ c -> new ApacheHttpBuilder(c); } as Function) {
            request.uri = "http://localhost:${serverRule.port}"
        }

        javaHttp = HttpBuilder.configure({ c -> new JavaHttpBuilder(c); } as Function) {
            request.uri = "http://localhost:${serverRule.port}"
        }

        server.when(get('/')).respond(responseContent(htmlContent()))

        server.when(get('/foo').withQueryStringParameter('alpha', 'bravo')).respond(responseContent(HTML_CONTENT_B))
        server.when(get('/foo').withCookie('biscuit', 'wafer')).respond(responseContent(HTML_CONTENT_C))
        server.when(get('/foo')).respond(responseContent(htmlContent()))

        // parsers

        server.when(get('/xml')).respond(responseContent(xmlContent(), 'text/xml'))
        server.when(get('/json')).respond(responseContent(jsonContent(), 'text/json'))

        server.when(get('/date')).respond(responseContent('2016.08.25 14:43', 'text/date'))

        // Status handlers

        (2..5).each { s ->
            server.when(get("/status${s}00")).respond(response().withStatusCode(s * 100))
        }

        // BASIC auth

        String encodedCred = "Basic ${'admin:$3cr3t'.bytes.encodeBase64()}"
        def authHeader = new Header('Authorization', encodedCred)

        server.when(get('/basic').withHeader(NottableString.not('Authorization'), NottableString.not(encodedCred)))
            .respond(response().withHeader('WWW-Authenticate', 'Basic realm="Test Realm"').withStatusCode(401))

        server.when(get('/basic').withHeader(authHeader)).respond(responseContent(htmlContent()))
    }

    private static HttpRequest get(final String path) {
        request().withMethod('GET').withPath(path)
    }

    private static HttpResponse responseContent(final String content, final String type = 'text/plain') {
        response().withBody(content).withStatusCode(200).withHeader('Content-Type', type)
    }

    @Unroll def '[#client] GET /status(#status): verify when handler'() {
        given:
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

    @Unroll def '[#label] GET /: returns content'() {
        expect:
        httpBuilder(label).get() == htmlContent()

        and:
        httpBuilder(label).getAsync().get() == htmlContent()

        where:
        label << [APACHE, JAVA]
    }

    @Unroll def '[#label] GET /foo: returns content'() {
        given:
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

    @Unroll def '[#label] GET /foo (cookie): returns content'() {
        given:
        def config = {
            request.uri.path = '/foo'
            request.cookie 'biscuit', 'wafer'
        }

        expect:
        httpBuilder(label).get(config) == HTML_CONTENT_C

        and:
        httpBuilder(label).getAsync(config).get() == HTML_CONTENT_C

        where:
        label << [APACHE] //, JAVA]  // FIXME: the JAVA fails - determine if impl wrong or server wrong
    }

    @Unroll def '[#label] GET /foo?alpha=bravo: returns content'() {
        given:
        def config = {
            request.uri.path = '/foo'
            request.uri.query = [alpha: 'bravo']
        }

        expect:
        httpBuilder(label).get(config) == HTML_CONTENT_B

        and:
        httpBuilder(label).getAsync(config).get() == HTML_CONTENT_B

        where:
        label << [APACHE] //, JAVA]  // FIXME: the JAVA fails - determine if impl wrong or server wrong
    }

    @Unroll def '[#label] GET (BASIC) /basic: returns content'() {
        given:
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
        result = httpBuilder(label).get(Date, config)

        then:
        result instanceof Date
        result.format('MM/dd/yyyy HH:mm') == '08/25/2016 14:43'

        where:
        label << [APACHE, JAVA]
    }

    private HttpBuilder httpBuilder(final HttpClientType factory) {
        factory == APACHE ? apacheHttp : javaHttp
    }

    private static String htmlContent(String text = 'Nothing special') {
        "<html><body><!-- a bunch of really interesting content that you would be sorry to miss -->$text</body></html>" as String
    }

    private static String xmlContent(String text = 'Nothing special') {
        "<?xml version=\"1.0\"?><root><child><elt name='foo' /><text>$text</text></child></root>" as String
    }

    private static String jsonContent(String text = 'Nothing special') {
        """
            {
                "items":[
                    {
                        "name":"alpha",
                        "score":123,
                        "text": "${text}"
                    }
                ]
            }
        """.stripIndent()
    }
}

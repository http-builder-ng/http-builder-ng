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

import com.stehno.ersatz.ErsatzServer
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import spock.lang.Ignore
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Unroll

import java.util.function.Function

import static com.stehno.ersatz.Verifiers.once

class JavaHttpGetSpec extends Specification {

    Function clientFactory
    Map<Object, Object> options = [:]

    private final ErsatzServer ersatzServer = new ErsatzServer()

    def cleanup() {
        ersatzServer.stop()
    }

    @Unroll def 'GET /status(#status): verify when handler'() {
        given:
        ersatzServer.expectations {
            [200, 300, 400, 500].each { code ->
                get("/status$code").verifier(once()).responds().code(code)
            }
        }

        CountedClosure counter = new CountedClosure()

        def config = {
            request.uri.path = "/status${status}"
            response.when status, counter.closure
        }

        ersatzServer.start()

        when:
        httpBuilder(ersatzServer.serverUrl).get config

        then:
        counter.called
        counter.clear()

        when:
        httpBuilder(ersatzServer.serverUrl).getAsync(config).get()

        then:
        counter.called

        where:
        status << ['200', '300', '400', '500']
    }

    @Unroll def 'GET /status(#status): success/failure handler'() {
        given:
        ersatzServer.expectations {
            [200, 300, 400, 500].each { code ->
                get("/status$code").verifier(once()).responds().code(code)
            }
        }

        CountedClosure successCounter = new CountedClosure()
        CountedClosure failureCounter = new CountedClosure()

        def config = {
            request.uri.path = "/status${status}"
            response.success successCounter.closure
            response.failure failureCounter.closure
        }

        ersatzServer.start()

        when:
        httpBuilder(ersatzServer.serverUrl).get config

        then:
        successCounter.called == success
        successCounter.clear()

        failureCounter.called == failure
        failureCounter.clear()

        when:
        httpBuilder(ersatzServer.serverUrl).getAsync(config).get()

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
        ersatzServer.expectations {
            [200, 300, 400, 500].each { code ->
                get("/status$code").verifier(once()).responds().code(code)
            }
        }

        CountedClosure failureCounter = new CountedClosure()

        def config = {
            request.uri.path = "/status${status}"
            response.failure failureCounter.closure
        }

        ersatzServer.start()

        when:
        httpBuilder(ersatzServer.serverUrl).get config

        then:
        failureCounter.called == failure
        failureCounter.clear()

        when:
        httpBuilder(ersatzServer.serverUrl).getAsync(config).get()

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
        ersatzServer.expectations {
            get('/').responds().contentType('text/plain').body(htmlContent())
        }.start()

        expect:
        httpBuilder(ersatzServer.serverUrl).get() == htmlContent()

        and:
        httpBuilder(ersatzServer.serverUrl).getAsync().get() == htmlContent()
    }

    def 'GET /foo: returns content'() {
        given:
        ersatzServer.expectations {
            get('/foo').responds().contentType('text/plain').body(htmlContent())
        }.start()

        def config = {
            request.uri.path = '/foo'
        }

        expect:
        httpBuilder(ersatzServer.serverUrl).get(config) == htmlContent()

        and:
        httpBuilder(ersatzServer.serverUrl).getAsync(config).get() == htmlContent()
    }

    def 'GET /xml: returns xml'() {
        given:
        ersatzServer.expectations {
            get('/xml').responds().contentType('text/xml').body(xmlContent())
        }.start()

        def config = {
            request.uri.path = '/xml'
            response.parser 'text/xml', NativeHandlers.Parsers.&xml
        }

        expect:
        httpBuilder(ersatzServer.serverUrl).get(config).child.text.text() == 'Nothing special'

        and:
        httpBuilder(ersatzServer.serverUrl).getAsync(config).get().child.text.text() == 'Nothing special'
    }

    def 'GET /json: returns json'() {
        given:
        ersatzServer.expectations {
            get('/json').responds().contentType('text/json').body(jsonContent())
        }.start()

        def config = {
            request.uri.path = '/json'
            response.parser 'text/json', NativeHandlers.Parsers.&json
        }

        when:
        def result = httpBuilder(ersatzServer.serverUrl).get(config)

        then:
        result.items[0].text == 'Nothing special'
        result.items[0].name == 'alpha'
        result.items[0].score == 123

        when:
        result = httpBuilder(ersatzServer.serverUrl).getAsync(config).get()

        then:
        result.items[0].text == 'Nothing special'
        result.items[0].name == 'alpha'
        result.items[0].score == 123
    }

    @Issue('https://github.com/http-builder-ng/http-builder-ng/issues/49')
    def 'GET /foo (cookie): returns content'() {
        given:
        ersatzServer.expectations {
            get('/foo').cookie('biscuit', 'wafer').responds().contentType('text/plain').body(htmlContent())
        }.start()

        def config = {
            request.uri.path = '/foo'
            request.cookie 'biscuit', 'wafer'
        }

        expect:
        httpBuilder(ersatzServer.serverUrl).get(config) == htmlContent()

        and:
        httpBuilder(ersatzServer.serverUrl).getAsync(config).get() == htmlContent()
    }

    @Issue('https://github.com/http-builder-ng/http-builder-ng/issues/49')
    def 'GET /foo (cookie2): returns content'() {
        given:
        ersatzServer.expectations {
            get('/foo').cookie('coffee', 'black').responds().contentType('text/plain').body(htmlContent())
        }.start()

        def config = {
            request.uri.path = '/foo'
            request.cookie 'coffee', 'black'
        }

        expect:
        httpBuilder(ersatzServer.serverUrl).get(config) == htmlContent()

        and:
        httpBuilder(ersatzServer.serverUrl).getAsync(config).get() == htmlContent()
    }

    def 'GET /foo?alpha=bravo: returns content'() {
        given:
        ersatzServer.expectations {
            get('/foo').query('alpha', 'bravo').responds().contentType('text/plain').body(htmlContent())
        }.start()

        def config = {
            request.uri.path = '/foo'
            request.uri.query = [alpha: 'bravo']
        }

        expect:
        httpBuilder(ersatzServer.serverUrl).get(config) == htmlContent()

        and:
        httpBuilder(ersatzServer.serverUrl).getAsync(config).get() == htmlContent()
    }

    @Ignore("Ignore this until I have the BASIC plugin stuff working")
    def 'GET (BASIC) /basic: returns content'() {
        given:
        String encodedCred = "Basic ${'admin:$3cr3t'.bytes.encodeBase64()}"

        ersatzServer.expectations {
            get('/basic'){
                condition { r-> !r.header('Authorization') }
                responder {
                    header 'WWW-Authenticate', 'Basic realm="Test Realm"'
                    code 401
                }
            }

            get('/basic'){
                header 'Authorization', encodedCred
                responder {
                    header 'Authorization', encodedCred
                    contentType 'text/plain'
                    body htmlContent()
                }
            }
        }.start()

        def config = {
            request.uri.path = '/basic'
            request.auth.basic 'admin', '$3cr3t'
        }

        expect:
        httpBuilder(ersatzServer.serverUrl).get(config) == htmlContent()

        and:
        httpBuilder(ersatzServer.serverUrl).getAsync(config).get() == htmlContent()
    }

    def 'GET /date: returns content of specified type'() {
        given:
        ersatzServer.expectations {
            get('/date').responds().contentType('text/date').body('2016.08.25 14:43')
        }.start()

        def config = {
            request.uri.path = '/date'
            response.parser('text/date') { ChainedHttpConfig config, FromServer fromServer ->
                Date.parse('yyyy.MM.dd HH:mm', fromServer.inputStream.text)
            }
        }

        when:
        def result = httpBuilder(ersatzServer.serverUrl).get(Date, config)

        then:
        result instanceof Date
        result.format('MM/dd/yyyy HH:mm') == '08/25/2016 14:43'

        when:
        result = httpBuilder(ersatzServer.serverUrl).getAsync(Date, config).get()

        then:
        result instanceof Date
        result.format('MM/dd/yyyy HH:mm') == '08/25/2016 14:43'
    }

    def setup() {
        clientFactory = { c -> new JavaHttpBuilder(c) } as Function
    }

    protected HttpBuilder httpBuilder(Closure config) {
        HttpBuilder.configure(clientFactory, config)
    }

    protected HttpBuilder httpBuilder(String uri) {
        httpBuilder {
            request.uri = uri
        }
    }

    protected HttpBuilder httpBuilder(int port) {
        httpBuilder {
            request.uri = "http://localhost:${port}"
        }
    }

    protected void option(final Object key, final boolean value) {
        options[key] = value
    }

    protected boolean enabled(final Object key) {
        options[key]
    }

    static String htmlContent(String text = 'Nothing special') {
        "<html><body><!-- a bunch of really interesting content that you would be sorry to miss -->$text</body></html>" as String
    }

    static String xmlContent(String text = 'Nothing special') {
        "<?xml version=\"1.0\"?><root><child><elt name='foo' /><text>$text</text></child></root>" as String
    }

    static String jsonContent(String text = 'Nothing special') {
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

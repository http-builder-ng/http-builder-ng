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

import com.stehno.ersatz.feat.BasicAuthFeature
import groovyx.net.http.ChainedHttpConfig
import groovyx.net.http.CountedClosure
import groovyx.net.http.FromServer
import groovyx.net.http.NativeHandlers
import spock.lang.Unroll

/**
 * Test kit for testing the HTTP GET method with different clients.
 */
abstract class HttpGetTestKit extends HttpMethodTestKit {

    @Unroll 'GET /status(#status): verify when handler'() {
        given:
        ersatzServer.expectations {
            [200, 300, 400, 500].each { code ->
                get("/status$code").called(1).responds().code(code)
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

    @Unroll 'GET /status(#status): success/failure handler'() {
        given:
        ersatzServer.expectations {
            [200, 300, 400, 500].each { code ->
                get("/status$code").called(1).responds().code(code)
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

    @Unroll 'GET /status(#status): with only failure handler'() {
        given:
        ersatzServer.expectations {
            [200, 300, 400, 500].each { code ->
                get("/status$code").called(1).responds().code(code)
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
            get('/').responds().contentType('text/plain').content(htmlContent())
        }.start()

        expect:
        httpBuilder(ersatzServer.serverUrl).get() == htmlContent()

        and:
        httpBuilder(ersatzServer.serverUrl).getAsync().get() == htmlContent()
    }

    def 'GET /foo: returns content'() {
        given:
        ersatzServer.expectations {
            get('/foo').responds().contentType('text/plain').content(htmlContent())
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
            get('/xml').responds().contentType('text/xml').content(xmlContent())
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
            get('/json').responds().contentType('text/json').content(jsonContent())
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

    def 'GET /foo (cookie): returns content'() {
        given:
        ersatzServer.expectations {
            get('/foo').cookie('biscuit', 'wafer').responds().contentType('text/plain').content(htmlContent())
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

    def 'GET /foo (cookie2): returns content'() {
        given:
        ersatzServer.expectations {
            get('/foo').cookie('coffee', 'black').responds().contentType('text/plain').content(htmlContent())
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
            get('/foo').query('alpha', 'bravo').responds().contentType('text/plain').content(htmlContent())
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

    def 'GET (BASIC) /basic: returns content'() {
        given:
        ersatzServer.addFeature new BasicAuthFeature()

        ersatzServer.expectations {
            get('/basic').responds().contentType('text/plain').content(htmlContent())
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
            get('/date').responds().contentType('text/date').content('2016.08.25 14:43')
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
}

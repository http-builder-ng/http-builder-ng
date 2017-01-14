/*
 * Copyright (C) 2017 David Clark
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
import groovyx.net.http.CountedClosure
import groovyx.net.http.FromServer
import spock.lang.Unroll

/**
 * Test kit for testing the HTTP HEAD method with different clients.
 */
abstract class HttpHeadTestKit extends HttpMethodTestKit {

    private static final Map<String, String> HEADERS_A = [
        alpha: '100', sometime: '03/04/2015 15:45', Accept: 'text/plain', Connection: 'keep-alive'
    ].asImmutable()
    private static final Map<String, String> HEADERS_B = [bravo: '200', Accept: 'text/html', Connection: 'keep-alive'].asImmutable()
    private static final Map<String, String> HEADERS_C = [charlie: '200', Connection: 'keep-alive'].asImmutable()

    def 'HEAD /: returns no content'() {
        setup:
        ersatzServer.expectations {
            head('/').responds().headers(HEADERS_A)
        }.start()

        expect:
        !httpBuilder(ersatzServer.port).head()

        and:
        !httpBuilder(ersatzServer.port).headAsync().get()

        and:
        ersatzServer.verify()
    }

    def 'HEAD /foo: returns headers only'() {
        given:
        ersatzServer.expectations {
            head('/foo').responds().headers(HEADERS_A)
        }.start()

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
        httpBuilder(ersatzServer.port).head(config)

        then:
        !hasBody
        assertHeaders HEADERS_A, capturedHeaders
        capturedHeaders.clear()

        when:
        httpBuilder(ersatzServer.port).headAsync(config).get()

        then:
        !hasBody
        assertHeaders HEADERS_A, capturedHeaders

        and:
        ersatzServer.verify()
    }

    def 'HEAD (BASIC) /basic: returns only headers'() {
        given:
        ersatzServer.feature new BasicAuthFeature()

        ersatzServer.expectations {
            head('/basic').responds().headers(HEADERS_A)
        }.start()

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
        httpBuilder(ersatzServer.port).head(config)

        then:
        !hasBody
        assertHeaders HEADERS_A, capturedHeaders
        capturedHeaders.clear()

        and:
        httpBuilder(ersatzServer.port).headAsync(config).get()

        then:
        !hasBody
        assertHeaders HEADERS_A, capturedHeaders
    }

    def 'HEAD /foo (cookie): returns headers only'() {
        given:
        ersatzServer.expectations {
            head('/foo').cookie('biscuit', 'wafer').responds().headers(HEADERS_B)
        }.start()

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
        httpBuilder(ersatzServer.port).head(config)

        then:
        !hasBody
        assertHeaders HEADERS_B, capturedHeaders
        capturedHeaders.clear()

        when:
        httpBuilder(ersatzServer.port).headAsync(config).get()

        then:
        !hasBody
        assertHeaders HEADERS_B, capturedHeaders

        and:
        ersatzServer.verify()
    }

    def 'HEAD /foo?alpha=bravo: returns headers only'() {
        given:
        ersatzServer.expectations {
            head('/foo').query('alpha', 'bravo').responds().headers(HEADERS_C)
        }.start()

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
        httpBuilder(ersatzServer.port).head(config)

        then:
        !hasBody
        assertHeaders HEADERS_C, capturedHeaders
        capturedHeaders.clear()

        when:
        httpBuilder(ersatzServer.port).headAsync(config).get()

        then:
        !hasBody
        assertHeaders HEADERS_C, capturedHeaders

        and:
        ersatzServer.verify()
    }

    @Unroll def 'HEAD /status#status: verify when handler'() {
        given:
        ersatzServer.expectations {
            [200, 300, 400, 500].each { s ->
                head("/status$s").responds().code(s)
            }
        }.start()

        CountedClosure counter = new CountedClosure()

        def config = {
            request.uri.path = "/status${status}"
            response.when status, counter.closure
        }

        when:
        httpBuilder(ersatzServer.port).head config

        then:
        counter.called
        counter.clear()

        when:
        httpBuilder(ersatzServer.port).headAsync(config).get()

        then:
        counter.called

        where:
        status << ['200', '300', '400', '500']
    }

    @Unroll def 'HEAD /status#status: verify success/failure handler'() {
        given:
        ersatzServer.expectations {
            [200, 300, 400, 500].each { s ->
                head("/status$s").responds().code(s)
            }
        }.start()

        CountedClosure successCounter = new CountedClosure()
        CountedClosure failureCounter = new CountedClosure()

        def config = {
            request.uri.path = "/status${status}"
            response.success successCounter.closure
            response.failure failureCounter.closure
        }

        when:
        httpBuilder(ersatzServer.port).head config

        then:
        successCounter.called == success
        successCounter.clear()

        failureCounter.called == failure
        failureCounter.clear()

        when:
        httpBuilder(ersatzServer.port).headAsync(config).get()

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

    def 'HEAD /date: returns content of specified type'() {
        given:
        ersatzServer.expectations {
            head('/date').responds().header('stamp', '2016.08.25 14:43')
        }.start()

        def config = {
            request.uri.path = '/date'
            response.success { FromServer fromServer ->
                Date.parse('yyyy.MM.dd HH:mm', fromServer.headers.find { it.key == 'stamp' }.value)
            }
        }

        when:
        def result = httpBuilder(ersatzServer.port).head(Date, config)

        then:
        result instanceof Date
        result.format('MM/dd/yyyy HH:mm') == '08/25/2016 14:43'

        when:
        result = httpBuilder(ersatzServer.port).headAsync(Date, config).get()

        then:
        result instanceof Date
        result.format('MM/dd/yyyy HH:mm') == '08/25/2016 14:43'
    }

    private static void assertHeaders(Map expected, Map captured) {
        expected.each { k, v ->
            assert captured[k] == v
        }
    }
}

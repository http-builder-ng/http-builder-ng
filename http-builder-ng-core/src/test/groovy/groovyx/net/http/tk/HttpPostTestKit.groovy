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

import groovyx.net.http.ChainedHttpConfig
import groovyx.net.http.FromServer
import groovyx.net.http.NativeHandlers
import groovyx.net.http.ToServer
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import spock.lang.Ignore
import spock.lang.Issue
import spock.lang.Unroll

import static groovyx.net.http.ContentTypes.JSON
import static groovyx.net.http.ContentTypes.TEXT

/**
 * Test kit for testing the HTTP POST method with different clients.
 */
abstract class HttpPostTestKit extends HttpMethodTestKit {

    private static final String DATE_STRING = '2016.08.25 14:43'
    private static final Date DATE_OBJECT = Date.parse('yyyy.MM.dd HH:mm', DATE_STRING)
    private static final String JSON_STRING = '{"name":"Bob","age":42}'
    private static final String BODY_STRING = 'This is CONTENT!!'
    private static final String HTML_CONTENT = htmlContent('Something Different')
    private static final String JSON_CONTENT = '{ "accepted":true, "id":100 }'

    def 'POST /: returns content'() {
        setup:
        serverRule.dispatcher('POST', '/', new MockResponse().setHeader('Content-Type', 'text/plain').setBody(htmlContent()))

        expect:
        httpBuilder(serverRule.serverPort).post() == htmlContent()

        and:
        httpBuilder(serverRule.serverPort).postAsync().get() == htmlContent()
    }

    @Unroll def 'POST /foo (#contentType): returns content'() {
        given:
        serverRule.dispatcher { RecordedRequest request ->
            if (request.method == 'POST' && request.path == '/foo') {
                String requestType = request.getHeader('Content-Type')
                if (requestType == TEXT[0]) {
                    return new MockResponse().setHeader('Content-Type', requestType).setBody(HTML_CONTENT)
                } else if (requestType == JSON[0]) {
                    return new MockResponse().setHeader('Content-Type', requestType).setBody(JSON_CONTENT)
                }
            }
            return new MockResponse().setResponseCode(404)
        }

        def config = {
            request.uri.path = '/foo'
            request.body = content
            request.contentType = contentType[0]
            response.parser contentType, parser
        }

        expect:
        httpBuilder(serverRule.serverPort).post(config) == result

        and:
        httpBuilder(serverRule.serverPort).postAsync(config).get() == result

        where:
        content     | contentType | parser                               || result
        BODY_STRING | TEXT        | NativeHandlers.Parsers.&textToString || HTML_CONTENT
        JSON_STRING | JSON        | NativeHandlers.Parsers.&json         || [accepted: true, id: 100]
    }

    @Unroll def 'POST /foo (#contentType): encodes and decodes properly and returns content'() {
        given:
        serverRule.dispatcher { RecordedRequest request ->
            if (request.method == 'POST' && request.path == '/foo' && request.getHeader('Content-Type') == JSON[0]) {
                return new MockResponse().setHeader('Content-Type', JSON[0]).setBody(JSON_CONTENT)
            }
            return new MockResponse().setResponseCode(404)
        }

        def config = {
            request.uri.path = '/foo'
            request.body = content
            request.contentType = contentType[0]
        }

        expect:
        httpBuilder(serverRule.serverPort).post(config) == result

        and:
        httpBuilder(serverRule.serverPort).postAsync(config).get() == result

        where:
        contentType | content                || result
        JSON        | [name: 'Bob', age: 42] || [accepted: true, id: 100]
        JSON        | { name 'Bob'; age 42 } || [accepted: true, id: 100]
    }

    @Issue('https://github.com/http-builder-ng/http-builder-ng/issues/49')
    def 'POST /foo (cookie): returns content'() {
        given:
        serverRule.dispatcher { RecordedRequest request ->
            if (request.method == 'POST' && request.path == '/foo' && request.getHeader('Content-Type') == TEXT[0] && request.getHeader('Cookie').contains('userid=spock')) {
                return new MockResponse().setHeader('Content-Type', TEXT[0]).setBody(htmlContent())
            }
            return new MockResponse().setResponseCode(404)
        }

        def config = {
            request.uri.path = '/foo'
            request.body = BODY_STRING
            request.contentType = TEXT[0]
            request.cookie 'userid', 'spock'
        }

        expect:
        httpBuilder(serverRule.serverPort).post(config) == htmlContent()

        and:
        httpBuilder(serverRule.serverPort).postAsync(config).get() == htmlContent()
    }

    def 'POST /foo (query string): returns content'() {
        given:
        serverRule.dispatcher { RecordedRequest request ->
            if (request.method == 'POST' && request.path == '/foo?action=login' && request.getHeader('Content-Type') == TEXT[0]) {
                return new MockResponse().setHeader('Content-Type', TEXT[0]).setBody(htmlContent())
            }
            return new MockResponse().setResponseCode(404)
        }

        def config = {
            request.uri.path = '/foo'
            request.uri.query = [action: 'login']
            request.body = BODY_STRING
            request.contentType = TEXT[0]
        }

        expect:
        httpBuilder(serverRule.serverPort).post(config) == htmlContent()

        and:
        httpBuilder(serverRule.serverPort).postAsync(config).get() == htmlContent()
    }

    def 'POST /date: returns content as Date'() {
        given:
        serverRule.dispatcher { RecordedRequest request ->
            if (request.method == 'POST' && request.path == '/date' && request.getHeader('Content-Type') == 'text/datetime' && request.getBody().toString() == '[text=DATE-TIME: 20160825-1443]') {
                return new MockResponse().setHeader('Content-Type', 'text/date').setBody(DATE_STRING)
            }
            return new MockResponse().setResponseCode(404)
        }

        def config = {
            request.uri.path = '/date'
            request.body = DATE_OBJECT
            request.contentType = 'text/datetime'
            request.encoder('text/datetime') { ChainedHttpConfig config, ToServer req ->
                req.toServer(new ByteArrayInputStream("DATE-TIME: ${config.request.body.format('yyyyMMdd-HHmm')}".bytes))
            }
            response.parser('text/date') { ChainedHttpConfig config, FromServer fromServer ->
                Date.parse('yyyy.MM.dd HH:mm', fromServer.inputStream.text)
            }
        }

        expect:
        httpBuilder(serverRule.serverPort).post(Date, config).format('yyyy.MM.dd HH:mm') == DATE_STRING

        and:
        httpBuilder(serverRule.serverPort).postAsync(Date, config).get().format('yyyy.MM.dd HH:mm') == DATE_STRING
    }

    @Ignore @Issue('https://github.com/http-builder-ng/http-builder-ng/issues/10')
    def '[#client] POST (BASIC) /basic: returns content'() {
        expect:
        httpBuilder(serverRule.serverPort).post({
            request.uri.path = '/basic'
            request.body = JSON_STRING
            request.contentType = JSON[0]
            request.auth.basic 'admin', '$3cr3t'
        }) == htmlContent()

        and:
        httpBuilder(serverRule.serverPort).postAsync({
            request.uri.path = '/basic'
            request.body = JSON_STRING
            request.contentType = JSON[0]
            request.auth.basic 'admin', '$3cr3t'
        }).get() == htmlContent()
    }

    // FIXME: is/should DIGEST be supported for POST request? - seems to not work at all in this impl
}

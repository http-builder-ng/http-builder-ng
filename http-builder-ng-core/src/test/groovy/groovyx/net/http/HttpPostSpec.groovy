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
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Unroll

import static groovyx.net.http.ContentTypes.JSON
import static groovyx.net.http.ContentTypes.TEXT
import static groovyx.net.http.HttpClientType.APACHE
import static groovyx.net.http.HttpClientType.JAVA
import static HttpContent.htmlContent
import static HttpContent.httpBuilder

class HttpPostSpec extends Specification {

    @Rule MockWebServerRule serverRule = new MockWebServerRule()

    private static final String DATE_STRING = '2016.08.25 14:43'
    private static final Date DATE_OBJECT = Date.parse('yyyy.MM.dd HH:mm', DATE_STRING)
    private static final String JSON_STRING = '{"name":"Bob","age":42}'
    private static final String BODY_STRING = 'This is CONTENT!!'
    private static final String HTML_CONTENT = htmlContent('Something Different')
    private static final String JSON_CONTENT = '{ "accepted":true, "id":100 }'

    @Unroll def '[#client] POST /: returns content'() {
        setup:
        serverRule.dispatcher('POST', '/', new MockResponse().setHeader('Content-Type', 'text/plain').setBody(htmlContent()))

        expect:
        httpBuilder(client, serverRule.serverPort).post() == htmlContent()

        and:
        httpBuilder(client, serverRule.serverPort).postAsync().get() == htmlContent()

        where:
        client << [APACHE, JAVA]
    }

    @Unroll def '[#client] POST /foo (#contentType): returns content'() {
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
        httpBuilder(client, serverRule.serverPort).post(config) == result

        and:
        httpBuilder(client, serverRule.serverPort).postAsync(config).get() == result

        where:
        client | content     | contentType | parser                               || result
        APACHE | BODY_STRING | TEXT        | NativeHandlers.Parsers.&textToString || HTML_CONTENT
        JAVA   | BODY_STRING | TEXT        | NativeHandlers.Parsers.&textToString || HTML_CONTENT

        APACHE | JSON_STRING | JSON        | NativeHandlers.Parsers.&json         || [accepted: true, id: 100]
        JAVA   | JSON_STRING | JSON        | NativeHandlers.Parsers.&json         || [accepted: true, id: 100]
    }

    @Unroll def '[#client] POST /foo (#contentType): encodes and decodes properly and returns content'() {
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
        httpBuilder(client, serverRule.serverPort).post(config) == result

        and:
        httpBuilder(client, serverRule.serverPort).postAsync(config).get() == result

        where:
        client | content                | contentType || result
        APACHE | [name: 'Bob', age: 42] | JSON        || [accepted: true, id: 100]
        JAVA   | [name: 'Bob', age: 42] | JSON        || [accepted: true, id: 100]

        APACHE | { name 'Bob'; age 42 } | JSON        || [accepted: true, id: 100]
        JAVA   | { name 'Bob'; age 42 } | JSON        || [accepted: true, id: 100]
    }

    @Issue('https://github.com/http-builder-ng/http-builder-ng/issues/49')
    @Unroll def '[#client] POST /foo (cookie): returns content'() {
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
        httpBuilder(client, serverRule.serverPort).post(config) == htmlContent()

        and:
        httpBuilder(client, serverRule.serverPort).postAsync(config).get() == htmlContent()

        where:
        client << [APACHE, JAVA]
    }

    @Unroll def '[#client] POST /foo (query string): returns content'() {
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
        httpBuilder(client, serverRule.serverPort).post(config) == htmlContent()

        and:
        httpBuilder(client, serverRule.serverPort).postAsync(config).get() == htmlContent()

        where:
        client << [APACHE, JAVA]
    }

    @Unroll def '[#client] POST /date: returns content as Date'() {
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
        httpBuilder(client, serverRule.serverPort).post(Date, config).format('yyyy.MM.dd HH:mm') == DATE_STRING

        and:
        httpBuilder(client, serverRule.serverPort).postAsync(Date, config).get().format('yyyy.MM.dd HH:mm') == DATE_STRING

        where:
        client << [APACHE, JAVA]
    }

    @Ignore @Issue('https://github.com/http-builder-ng/http-builder-ng/issues/10')
    @Unroll def '[#client] POST (BASIC) /basic: returns content'() {
        expect:
        httpBuilder(client, serverRule.serverPort).post({
            request.uri.path = '/basic'
            request.body = JSON_STRING
            request.contentType = JSON[0]
            request.auth.basic 'admin', '$3cr3t'
        }) == htmlContent()

        and:
        httpBuilder(client, serverRule.serverPort).postAsync({
            request.uri.path = '/basic'
            request.body = JSON_STRING
            request.contentType = JSON[0]
            request.auth.basic 'admin', '$3cr3t'
        }).get() == htmlContent()

        where:
        client << [APACHE, JAVA]
    }

    // FIXME: is/should DIGEST be supported for POST request? - seems to not work at all in this impl
}

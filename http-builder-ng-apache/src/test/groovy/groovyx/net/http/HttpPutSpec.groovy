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
import spock.lang.Specification
import spock.lang.Unroll

import static groovyx.net.http.ContentTypes.JSON
import static groovyx.net.http.ContentTypes.TEXT
import static groovyx.net.http.HttpClientType.APACHE
import static groovyx.net.http.HttpClientType.JAVA
import static HttpContent.htmlContent

class HttpPutSpec extends Specification {

    @Rule MockWebServerRule serverRule = new MockWebServerRule()

    private static final String DATE_STRING = '2016.08.25 14:43'
    private static final String BODY_STRING = 'Something Interesting'
    private static final String HTML_CONTENT = htmlContent('Something Cool')
    private static final String JSON_STRING = '{ "name":"Chuck", "age":56 }'
    private static final String JSON_CONTENT = '{ "accepted":false, "id":123 }'

    //        server.when(put('/date', BODY_STRING)).respond(responseContent(DATE_STRING, 'text/date'))
    //
    //        // BASIC auth
    //
    //        String encodedCred = "Basic ${'admin:$3cr3t'.bytes.encodeBase64()}"
    //        def authHeader = new Header('Authorization', encodedCred)
    //
    //        server.when(put('/basic', JSON_STRING, JSON[0]).withHeader(NottableString.not('Authorization'), NottableString.not(encodedCred)))
    //            .respond(response().withHeader('WWW-Authenticate', 'Basic realm="Test Realm"').withStatusCode(401))
    //
    //        server.when(put('/basic', JSON_STRING, JSON[0]).withHeader(authHeader)).respond(responseContent(htmlContent()))
    //    }

    @Unroll def '[#client] PUT /: returns content'() {
        setup:
        serverRule.dispatcher('PUT','/', new MockResponse().setHeader('Content-Type', 'text/plain').setBody(htmlContent()))

        expect:
        HttpContent.httpBuilder(client, serverRule.serverPort).put() == htmlContent()

        and:
        HttpContent.httpBuilder(client, serverRule.serverPort).putAsync().get() == htmlContent()

        where:
        client << [groovyx.net.http.HttpClientType.APACHE, groovyx.net.http.HttpClientType.JAVA]
    }

    @Unroll def '[#client] PUT /foo (#contentType): returns content'() {
        given:
        serverRule.dispatcher { RecordedRequest request ->
            if (request.method == 'PUT' && request.path == '/foo') {
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
        HttpContent.httpBuilder(client, serverRule.serverPort).put(config) == result

        and:
        HttpContent.httpBuilder(client, serverRule.serverPort).putAsync(config).get() == result

        where:
        client                                 | content     | contentType | parser                               || result
        groovyx.net.http.HttpClientType.APACHE | BODY_STRING | TEXT        | NativeHandlers.Parsers.&textToString || HTML_CONTENT
        groovyx.net.http.HttpClientType.JAVA   | BODY_STRING | TEXT        | NativeHandlers.Parsers.&textToString || HTML_CONTENT

        groovyx.net.http.HttpClientType.APACHE | JSON_STRING | JSON        | NativeHandlers.Parsers.&json         || [accepted: false, id: 123]
        groovyx.net.http.HttpClientType.JAVA   | JSON_STRING | JSON        | NativeHandlers.Parsers.&json         || [accepted: false, id: 123]
    }

    @Issue('https://github.com/http-builder-ng/http-builder-ng/issues/49')
    @Unroll def '[#client] PUT /foo (cookie): returns content'() {
        given:
        serverRule.dispatcher { RecordedRequest request ->
            if (request.method == 'PUT' && request.path == '/foo' && request.getHeader('Content-Type') == TEXT[0] && request.getHeader('Cookie').contains('userid=spock') ) {
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
        HttpContent.httpBuilder(client, serverRule.serverPort).put(config) == htmlContent()

        and:
        HttpContent.httpBuilder(client, serverRule.serverPort).putAsync(config).get() == htmlContent()

        where:
        client << [groovyx.net.http.HttpClientType.APACHE, groovyx.net.http.HttpClientType.JAVA]
    }

    @Unroll def '[#client] PUT /foo (query string): returns content'() {
        given:
        serverRule.dispatcher { RecordedRequest request ->
            if (request.method == 'PUT' && request.path == '/foo?action=login' && request.getHeader('Content-Type') == TEXT[0]) {
                return new MockResponse().setHeader('Content-Type', TEXT[0]).setBody(htmlContent('Authenticate'))
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
        HttpContent.httpBuilder(client, serverRule.serverPort).put(config) == htmlContent('Authenticate')

        and:
        HttpContent.httpBuilder(client, serverRule.serverPort).putAsync(config).get() == htmlContent('Authenticate')

        where:
        client << [groovyx.net.http.HttpClientType.APACHE, groovyx.net.http.HttpClientType.JAVA]
    }

    @Unroll def '[#client] PUT /date: returns content as Date'() {
        given:
        serverRule.dispatcher { RecordedRequest request ->
            if (request.method == 'PUT' && request.path == '/date' && request.getHeader('Content-Type') == 'text/plain' && request.getBody().toString() == '[text=Something Interesting]') {
                return new MockResponse().setHeader('Content-Type', 'text/date').setBody(DATE_STRING)
            }
            return new MockResponse().setResponseCode(404)
        }

        def config = {
            request.uri.path = '/date'
            request.body = BODY_STRING
            request.contentType = TEXT[0]
            response.parser('text/date') { ChainedHttpConfig config, FromServer fromServer ->
                Date.parse('yyyy.MM.dd HH:mm', fromServer.inputStream.text)
            }
        }

        expect:
        HttpContent.httpBuilder(client, serverRule.serverPort).put(Date, config).format('yyyy.MM.dd HH:mm') == DATE_STRING

        and:
        HttpContent.httpBuilder(client, serverRule.serverPort).putAsync(Date, config).get().format('yyyy.MM.dd HH:mm') == DATE_STRING

        where:
        client << [groovyx.net.http.HttpClientType.APACHE, groovyx.net.http.HttpClientType.JAVA]
    }

    // FIXME: need to test BASIC and DIGEST requests
}

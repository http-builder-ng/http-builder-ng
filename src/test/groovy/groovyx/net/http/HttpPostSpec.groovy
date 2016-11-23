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
import org.mockserver.model.Header
import org.mockserver.model.NottableString
import spock.lang.Ignore
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Unroll

import static groovyx.net.http.ContentTypes.JSON
import static groovyx.net.http.ContentTypes.TEXT
import static groovyx.net.http.HttpClientType.APACHE
import static groovyx.net.http.HttpClientType.JAVA
import static groovyx.net.http.MockServerHelper.htmlContent
import static groovyx.net.http.MockServerHelper.httpBuilder
import static groovyx.net.http.MockServerHelper.post
import static groovyx.net.http.MockServerHelper.responseContent
import static org.mockserver.model.HttpResponse.response

class HttpPostSpec extends Specification {

    @Rule public MockServerRule serverRule = new MockServerRule(this)

    private static final String DATE_STRING = '2016.08.25 14:43'
    private static final Date DATE_OBJECT = Date.parse('yyyy.MM.dd HH:mm', DATE_STRING)
    private static final String JSON_STRING = '{"name":"Bob","age":42}'
    private static final String BODY_STRING = 'This is CONTENT!!'
    private static final String HTML_CONTENT = htmlContent('Something Different')
    private static final String JSON_CONTENT = '{ "accepted":true, "id":100 }'

    private MockServerClient server

    def setup() {
        server.when(post('/')).respond(responseContent(htmlContent()))

        server.when(post('/foo', BODY_STRING).withQueryStringParameter('action', 'login')).respond(responseContent(htmlContent('Authenticate')))
        server.when(post('/foo', BODY_STRING).withCookie('userid', 'spock')).respond(responseContent(htmlContent()))
        server.when(post('/foo', BODY_STRING)).respond(responseContent(HTML_CONTENT))
        server.when(post('/foo', JSON_STRING, JSON[0])).respond(responseContent(JSON_CONTENT, JSON[0]))

        server.when(post('/date', 'DATE-TIME: 20160825-1443', 'text/datetime')).respond(responseContent(DATE_STRING, 'text/date'))

        // BASIC auth

        String encodedCred = "Basic ${'admin:$3cr3t'.bytes.encodeBase64()}"
        def authHeader = new Header('Authorization', encodedCred)

        server.when(post('/basic', JSON_STRING, JSON[0]).withHeader(NottableString.not('Authorization'), NottableString.not(encodedCred)))
            .respond(response().withHeader('WWW-Authenticate', 'Basic realm="Test Realm"').withStatusCode(401))

        server.when(post('/basic', JSON_STRING, JSON[0]).withHeader(authHeader)).respond(responseContent(htmlContent()))
    }

    @Unroll def '[#client] POST /: returns content'() {
        expect:
        httpBuilder(client, serverRule.port).post() == htmlContent()

        and:
        httpBuilder(client, serverRule.port).postAsync().get() == htmlContent()

        where:
        client << [APACHE, JAVA]
    }

    @Unroll def '[#client] POST /foo (#contentType): returns content'() {
        given:
        def config = {
            request.uri.path = '/foo'
            request.body = content
            request.contentType = contentType[0]
            response.parser contentType, parser
        }

        expect:
        httpBuilder(client, serverRule.port).post(config) == result

        and:
        httpBuilder(client, serverRule.port).postAsync(config).get() == result

        where:
        client | content     | contentType | parser                               || result
        APACHE | BODY_STRING | TEXT        | NativeHandlers.Parsers.&textToString || HTML_CONTENT
        JAVA   | BODY_STRING | TEXT        | NativeHandlers.Parsers.&textToString || HTML_CONTENT

        APACHE | JSON_STRING | JSON        | NativeHandlers.Parsers.&json         || [accepted: true, id: 100]
        JAVA   | JSON_STRING | JSON        | NativeHandlers.Parsers.&json         || [accepted: true, id: 100]
    }

    @Unroll def '[#client] POST /foo (#contentType): encodes and decodes properly and returns content'() {
        given:
        def config = {
            request.uri.path = '/foo'
            request.body = content
            request.contentType = contentType[0]
        }

        expect:
        httpBuilder(client, serverRule.port).post(config) == result

        and:
        httpBuilder(client, serverRule.port).postAsync(config).get() == result

        where:
        client | content                | contentType || result
        APACHE | [name: 'Bob', age: 42] | JSON        || [accepted: true, id: 100]
        JAVA   | [name: 'Bob', age: 42] | JSON        || [accepted: true, id: 100]

        APACHE | { name 'Bob'; age 42 } | JSON        || [accepted: true, id: 100]
        JAVA   | { name 'Bob'; age 42 } | JSON        || [accepted: true, id: 100]
    }

    @Unroll def '[#client] POST /foo (cookie): returns content'() {
        given:
        def config = {
            request.uri.path = '/foo'
            request.body = BODY_STRING
            request.contentType = TEXT[0]
            request.cookie 'userid', 'spock'
        }

        expect:
        httpBuilder(client, serverRule.port).post(config) == htmlContent()

        and:
        httpBuilder(client, serverRule.port).postAsync(config).get() == htmlContent()

        where:
        client << [APACHE, JAVA]
    }

    @Unroll def '[#client] POST /foo (query string): returns content'() {
        given:
        def config = {
            request.uri.path = '/foo'
            request.uri.query = [action: 'login']
            request.body = BODY_STRING
            request.contentType = TEXT[0]
        }

        expect:
        httpBuilder(client, serverRule.port).post(config) == htmlContent('Authenticate')

        and:
        httpBuilder(client, serverRule.port).postAsync(config).get() == htmlContent('Authenticate')

        where:
        client << [APACHE, JAVA]
    }

    @Unroll def '[#client] POST /date: returns content as Date'() {
        given:
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
        httpBuilder(client, serverRule.port).post(Date, config).format('yyyy.MM.dd HH:mm') == DATE_STRING

        and:
        httpBuilder(client, serverRule.port).postAsync(Date, config).get().format('yyyy.MM.dd HH:mm') == DATE_STRING

        where:
        client << [APACHE, JAVA]
    }

    @Ignore @Issue('https://github.com/http-builder-ng/http-builder-ng/issues/10')
    @Unroll def '[#client] POST (BASIC) /basic: returns content'() {
        expect:
        httpBuilder(client, serverRule.port).post({
            request.uri.path = '/basic'
            request.body = JSON_STRING
            request.contentType = JSON[0]
            request.auth.basic 'admin', '$3cr3t'
        }) == htmlContent()

        and:
        httpBuilder(client, serverRule.port).postAsync({
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

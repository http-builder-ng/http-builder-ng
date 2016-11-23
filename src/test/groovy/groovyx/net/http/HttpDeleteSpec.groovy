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

import static groovyx.net.http.ContentTypes.TEXT
import static groovyx.net.http.HttpClientType.APACHE
import static groovyx.net.http.HttpClientType.JAVA
import static groovyx.net.http.MockServerHelper.htmlContent
import static groovyx.net.http.MockServerHelper.httpBuilder

class HttpDeleteSpec extends Specification {

    @Rule MockWebServerRule serverRule = new MockWebServerRule()

    private static final String DATE_STRING = '2016.08.25 14:43'

    @Unroll def '[#client] DELETE /: returns content'() {
        setup:
        serverRule.dispatcher('DELETE', '/', responseContent())

        expect:
        httpBuilder(client, serverRule.serverPort).delete() == htmlContent()

        and:
        httpBuilder(client, serverRule.serverPort).deleteAsync().get() == htmlContent()

        where:
        client << [APACHE, JAVA]
    }

    @Unroll def '[#client] DELETE /foo: returns content'() {
        given:
        serverRule.dispatcher('DELETE', '/foo', responseContent())

        def config = {
            request.uri.path = '/foo'
        }

        expect:
        httpBuilder(client, serverRule.serverPort).delete(config) == htmlContent()

        and:
        httpBuilder(client, serverRule.serverPort).deleteAsync(config).get() == htmlContent()

        where:
        client << [APACHE, JAVA]
    }

    @Unroll def '[#client] DELETE /foo (cookie): returns content'() {
        given:
        serverRule.dispatcher { RecordedRequest request ->
            if (request.method == 'DELETE' && request.path == '/foo' && request.getHeader('Cookie') == 'userid=spock') {
                return responseContent()
            }
            return new MockResponse().setResponseCode(404)
        }

        def config = {
            request.uri.path = '/foo'
            request.cookie 'userid', 'spock'
        }

        expect:
        httpBuilder(client, serverRule.serverPort).delete(config) == htmlContent()

        and:
        httpBuilder(client, serverRule.serverPort).deleteAsync(config).get() == htmlContent()

        where:
        client << [APACHE, JAVA]
    }

    @Unroll def '[#client] DELETE /foo (query string): returns content'() {
        given:
        serverRule.dispatcher('DELETE', '/foo?action=login', responseContent())

        def config = {
            request.uri.path = '/foo'
            request.uri.query = [action: 'login']
            request.contentType = TEXT[0]
        }

        expect:
        httpBuilder(client, serverRule.serverPort).delete(config) == htmlContent()

        and:
        httpBuilder(client, serverRule.serverPort).deleteAsync(config).get() == htmlContent()

        where:
        client << [APACHE, JAVA]
    }

    @Unroll def '[#client] DELETE /date: returns content as Date'() {
        given:
        serverRule.dispatcher('DELETE', '/date', new MockResponse().setHeader('Content-Type', 'text/date').setBody(DATE_STRING))

        def config = {
            request.uri.path = '/date'
            response.parser('text/date') { ChainedHttpConfig config, FromServer fromServer ->
                Date.parse('yyyy.MM.dd HH:mm', fromServer.inputStream.text)
            }
        }

        expect:
        httpBuilder(client, serverRule.serverPort).delete(Date, config).format('yyyy.MM.dd HH:mm') == DATE_STRING

        and:
        httpBuilder(client, serverRule.serverPort).deleteAsync(Date, config).get().format('yyyy.MM.dd HH:mm') == DATE_STRING

        where:
        client << [APACHE, JAVA]
    }

    @Unroll def '[#client] DELETE (BASIC) /basic: returns only headers'() {
        given:
        serverRule.dispatcher { RecordedRequest request ->
            if (request.method == 'DELETE') {
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
        httpBuilder(client, serverRule.serverPort).delete(config) == htmlContent()

        and:
        httpBuilder(client, serverRule.serverPort).deleteAsync(config).get() == htmlContent()

        where:
        client << [APACHE, JAVA]
    }

    @Unroll @Requires(HttpBin)
    @Ignore('Results in Method Not Allowed exception - should Delete support DIGEST?')
    def '[#client] DELETE (DIGEST) /digest-auth'() {
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
        def result = httpClient.delete {
            request.uri.path = '/digest-auth/auth/david/clark'
            request.auth.digest 'david', 'clark'
            response.failure { r -> 'Ignored' }
        }

        then:
        result == 'Ignored'

        when:
        boolean authenticated = httpClient.delete {
            request.uri = '/digest-auth/auth/david/clark'
            request.auth.digest 'david', 'clark'
            response.success { true }
        }

        then:
        authenticated

        when:
        httpClient = httpBuilder(client, config)
        result = httpClient.deleteAsync {
            request.uri.path = '/digest-auth/auth/david/clark'
            request.auth.digest 'david', 'clark'
            response.failure { r -> 'Ignored' }
        }.get()

        then:
        result == 'Ignored'

        when:
        authenticated = httpClient.deleteAsync() {
            request.uri = '/digest-auth/auth/david/clark'
            request.auth.digest 'david', 'clark'
            response.success { true }
        }.get()

        then:
        authenticated

        where:
        client << [APACHE, JAVA]
    }

    private static MockResponse responseContent(final String body = htmlContent()) {
        new MockResponse().setHeader('Content-Type', 'text/plain').setBody(body)
    }
}

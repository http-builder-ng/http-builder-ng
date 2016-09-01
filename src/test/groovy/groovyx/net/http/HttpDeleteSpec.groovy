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
import spock.lang.Requires
import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.Executors

import static groovyx.net.http.ContentTypes.TEXT
import static groovyx.net.http.HttpClientType.APACHE
import static groovyx.net.http.HttpClientType.JAVA
import static groovyx.net.http.MockServerHelper.*
import static org.mockserver.model.HttpResponse.response

class HttpDeleteSpec extends Specification {

    @Rule public MockServerRule serverRule = new MockServerRule(this)

    private static final String DATE_STRING = '2016.08.25 14:43'
    private static final String BODY_STRING = 'Something Interesting'
    private static final String HTML_CONTENT = htmlContent('Something Cool')
    private static final String JSON_CONTENT = '{ "accepted":false, "id":123 }'

    private MockServerClient server

    def setup() {
        server.when(delete('/')).respond(responseContent(htmlContent()))

        server.when(delete('/foo').withQueryStringParameter('action', 'login')).respond(responseContent(htmlContent('Authenticate')))
        server.when(delete('/foo').withCookie('userid', 'spock')).respond(responseContent(htmlContent()))
        server.when(delete('/foo')).respond(responseContent(HTML_CONTENT))

        server.when(delete('/date')).respond(responseContent(DATE_STRING, 'text/date'))

        // BASIC auth

        String encodedCred = "Basic ${'admin:$3cr3t'.bytes.encodeBase64()}"
        def authHeader = new Header('Authorization', encodedCred)

        server.when(delete('/basic').withHeader(NottableString.not('Authorization'), NottableString.not(encodedCred)))
            .respond(response().withHeader('WWW-Authenticate', 'Basic realm="Test Realm"').withStatusCode(401))

        server.when(delete('/basic').withHeader(authHeader)).respond(responseContent(htmlContent()))

        server.dumpToLog()
    }

    @Unroll def '[#client] DELETE /: returns content'() {
        expect:
        httpBuilder(client, serverRule.port).delete() == htmlContent()

        and:
        httpBuilder(client, serverRule.port).deleteAsync().get() == htmlContent()

        where:
        client << [APACHE, JAVA]
    }

    @Unroll def '[#client] DELETE /foo: returns content'() {
        given:
        def config = {
            request.uri.path = '/foo'
        }

        expect:
        httpBuilder(client, serverRule.port).delete(config) == HTML_CONTENT

        and:
        httpBuilder(client, serverRule.port).deleteAsync(config).get() == HTML_CONTENT

        where:
        client << [APACHE, JAVA]
    }

    @Unroll def '[#client] DELETE /foo (cookie): returns content'() {
        given:
        def config = {
            request.uri.path = '/foo'
            request.cookie 'userid', 'spock'
        }

        expect:
        httpBuilder(client, serverRule.port).delete(config) == htmlContent()

        and:
        httpBuilder(client, serverRule.port).deleteAsync(config).get() == htmlContent()

        where:
        client << [APACHE, JAVA]
    }

    @Unroll def '[#client] DELETE /foo (query string): returns content'() {
        given:
        def config = {
            request.uri.path = '/foo'
            request.uri.query = [action: 'login']
            request.contentType = TEXT[0]
        }

        expect:
        httpBuilder(client, serverRule.port).delete(config) == htmlContent('Authenticate')

        and:
        httpBuilder(client, serverRule.port).deleteAsync(config).get() == htmlContent('Authenticate')

        where:
        client << [APACHE, JAVA]
    }

    @Unroll def '[#client] DELETE /date: returns content as Date'() {
        given:
        def config = {
            request.uri.path = '/date'
            response.parser('text/date') { ChainedHttpConfig config, FromServer fromServer ->
                Date.parse('yyyy.MM.dd HH:mm', fromServer.inputStream.text)
            }
        }

        expect:
        httpBuilder(client, serverRule.port).delete(Date, config).format('yyyy.MM.dd HH:mm') == DATE_STRING

        and:
        httpBuilder(client, serverRule.port).deleteAsync(Date, config).get().format('yyyy.MM.dd HH:mm') == DATE_STRING

        where:
        client << [APACHE, JAVA]
    }

    @Unroll def '[#client] DELETE (BASIC) /basic: returns only headers'() {
        given:
        def config = {
            request.uri.path = '/basic'
            request.auth.basic 'admin', '$3cr3t'
        }

        expect:
        httpBuilder(client, serverRule.port).delete(config) == htmlContent()

        and:
        httpBuilder(client, serverRule.port).deleteAsync(config).get() == htmlContent()

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
}

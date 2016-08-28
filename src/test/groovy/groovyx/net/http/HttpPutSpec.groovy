package groovyx.net.http

import org.junit.Rule
import org.mockserver.client.server.MockServerClient
import org.mockserver.junit.MockServerRule
import org.mockserver.model.Header
import org.mockserver.model.NottableString
import spock.lang.Specification
import spock.lang.Unroll

import static groovyx.net.http.ContentTypes.JSON
import static groovyx.net.http.ContentTypes.TEXT
import static groovyx.net.http.HttpClientType.APACHE
import static groovyx.net.http.HttpClientType.JAVA
import static groovyx.net.http.MockServerHelper.*
import static org.mockserver.model.HttpResponse.response

class HttpPutSpec extends Specification {

    @Rule public MockServerRule serverRule = new MockServerRule(this)

    private static final String DATE_STRING = '2016.08.25 14:43'
    private static final String BODY_STRING = 'Something Interesting'
    private static final String HTML_CONTENT = htmlContent('Something Cool')
    private static final String JSON_STRING = '{ "name":"Chuck", "age":56 }'
    private static final String JSON_CONTENT = '{ "accepted":false, "id":123 }'

    private MockServerClient server

    def setup() {
        server.when(put('/')).respond(responseContent(htmlContent()))

        server.when(put('/foo', BODY_STRING).withQueryStringParameter('action', 'login')).respond(responseContent(htmlContent('Authenticate')))
        server.when(put('/foo', BODY_STRING).withCookie('userid', 'spock')).respond(responseContent(htmlContent()))
        server.when(put('/foo', BODY_STRING)).respond(responseContent(HTML_CONTENT))
        server.when(put('/foo', JSON_STRING, JSON[0])).respond(responseContent(JSON_CONTENT, JSON[0]))

        server.when(put('/date', BODY_STRING)).respond(responseContent(DATE_STRING, 'text/date'))

        // BASIC auth

        String encodedCred = "Basic ${'admin:$3cr3t'.bytes.encodeBase64()}"
        def authHeader = new Header('Authorization', encodedCred)

        server.when(put('/basic', JSON_STRING, JSON[0]).withHeader(NottableString.not('Authorization'), NottableString.not(encodedCred)))
            .respond(response().withHeader('WWW-Authenticate', 'Basic realm="Test Realm"').withStatusCode(401))

        server.when(put('/basic', JSON_STRING, JSON[0]).withHeader(authHeader)).respond(responseContent(htmlContent()))
    }

    @Unroll def '[#client] PUT /: returns content'() {
        expect:
        httpBuilder(client, serverRule.port).put() == htmlContent()

        and:
        httpBuilder(client, serverRule.port).putAsync().get() == htmlContent()

        where:
        client << [APACHE, JAVA]
    }

    @Unroll def '[#client] PUT /foo (#contentType): returns content'() {
        given:
        def config = {
            request.uri.path = '/foo'
            request.body = content
            request.contentType = contentType[0]
            response.parser contentType, parser
        }

        expect:
        httpBuilder(client, serverRule.port).put(config) == result

        and:
        httpBuilder(client, serverRule.port).putAsync(config).get() == result

        where:
        client | content     | contentType | parser                               || result
        APACHE | BODY_STRING | TEXT        | NativeHandlers.Parsers.&textToString || HTML_CONTENT
        JAVA   | BODY_STRING | TEXT        | NativeHandlers.Parsers.&textToString || HTML_CONTENT

        APACHE | JSON_STRING | JSON        | NativeHandlers.Parsers.&json         || [accepted: false, id: 123]
        JAVA   | JSON_STRING | JSON        | NativeHandlers.Parsers.&json         || [accepted: false, id: 123]
    }

    @Unroll def '[#client] PUT /foo (cookie): returns content'() {
        given:
        def config = {
            request.uri.path = '/foo'
            request.body = BODY_STRING
            request.contentType = TEXT[0]
            request.cookie 'userid', 'spock'
        }

        expect:
        httpBuilder(client, serverRule.port).put(config) == htmlContent()

        and:
        httpBuilder(client, serverRule.port).putAsync(config).get() == htmlContent()

        where:
        client << [APACHE, JAVA]
    }

    @Unroll def '[#client] PUT /foo (query string): returns content'() {
        given:
        def config = {
            request.uri.path = '/foo'
            request.uri.query = [action: 'login']
            request.body = BODY_STRING
            request.contentType = TEXT[0]
        }

        expect:
        httpBuilder(client, serverRule.port).put(config) == htmlContent('Authenticate')

        and:
        httpBuilder(client, serverRule.port).putAsync(config).get() == htmlContent('Authenticate')

        where:
        client << [APACHE, JAVA]
    }

    @Unroll def '[#client] PUT /date: returns content as Date'() {
        given:
        def config = {
            request.uri.path = '/date'
            request.body = BODY_STRING
            request.contentType = TEXT[0]
            response.parser('text/date') { ChainedHttpConfig config, FromServer fromServer ->
                Date.parse('yyyy.MM.dd HH:mm', fromServer.inputStream.text)
            }
        }

        expect:
        httpBuilder(client, serverRule.port).put(Date, config).format('yyyy.MM.dd HH:mm') == DATE_STRING

        and:
        httpBuilder(client, serverRule.port).putAsync(Date, config).get().format('yyyy.MM.dd HH:mm') == DATE_STRING

        where:
        client << [APACHE, JAVA]
    }

    // FIXME: need to test BASIC and DIGEST requests
}

package groovyx.net.http

import groovyx.net.http.optional.ApacheHttpBuilder
import org.junit.Rule
import org.mockserver.client.server.MockServerClient
import org.mockserver.junit.MockServerRule
import org.mockserver.model.Header
import org.mockserver.model.NottableString
import spock.lang.Ignore
import spock.lang.Specification

import java.util.concurrent.CompletableFuture
import java.util.function.Function

import static org.mockserver.model.HttpRequest.request
import static org.mockserver.model.HttpResponse.response

class HttpGetSpec extends Specification {

    // FIXME: may need to use Apache HttpClient for this so that https works
    // FIXME: need the old ignoreSslIssues functionality back

    // TODO: test each for BASIC - these may require SSL in java version?
    // TODO: test each for DIGEST - these may require SSL?
    // TODO: test with both builder factories

    // TODO: should probably test all the build-in encoders/decoders with each verb

    @Rule public MockServerRule serverRule = new MockServerRule(this)

    private static final String HTML_CONTENT = '<html><body>Testing</body></html>'
    private static final Function apacheBuilder = { c -> new ApacheHttpBuilder(c); } as Function
    private MockServerClient server

    private HttpBuilder http

    def setup() {
        http = HttpBuilder.configure(apacheBuilder) {
            request.uri = "http://localhost:${serverRule.port}"
        }

        server.when(request().withMethod('GET').withPath('/')).respond(response().withBody(HTML_CONTENT))

        server.when(request().withMethod('GET').withPath('/foo')).respond(response().withBody(HTML_CONTENT))

        server.when(request().withMethod('GET').withPath('/date'))
            .respond(response().withBody('2016.08.25 14:43').withHeader('Content-Type', 'text/date'))

        // BASIC

        String encodedCred = "Basic ${'admin:$3cr3t'.bytes.encodeBase64()}"
        def authHeader = new Header('Authorization', encodedCred)

        server.when(request().withMethod('GET').withPath('/basic')
            .withHeader(NottableString.not('Authorization'), NottableString.not(encodedCred)))
            .respond(response().withHeader('WWW-Authenticate', 'Basic realm="Test Realm"').withStatusCode(401))

        server.when(request().withMethod('GET').withPath('/basic').withHeader(authHeader))
            .respond(response().withBody(HTML_CONTENT))

        // DIGEST

        // FIXME: this needs to be a proper digest request conversation
//        server.dumpToLog().when(request().withMethod('GET').withPath('/digest'))
//            .respond(response().withBody(HTML_CONTENT))
    }

    def 'GET /: returns content'() {
        when:
        def result = http.get()

        then:
        result == HTML_CONTENT
    }

    def 'GET (async) /: returns content'() {
        when:
        CompletableFuture futureResult = http.getAsync()

        then:
        futureResult.get() == HTML_CONTENT
    }

    def 'GET /foo: returns content'() {
        when:
        def result = http.get {
            request.uri.path = '/foo'
        }

        then:
        result == HTML_CONTENT
    }

    def 'GET (BASIC) /basic: returns content'() {
        when:
        def result = http.get {
            request.uri.path = '/basic'
            request.auth.basic 'admin', '$3cr3t'
        }

        then:
        result == HTML_CONTENT
    }

    @Ignore('Need to get the digest conversation configured')
    def 'GET (DIGEST) /basic: returns content'() {
        when:
        def result = http.get {
            request.uri.path = '/digest'
            request.auth.digest 'admin', '$3cr3t'
        }

        then:
        result == HTML_CONTENT
    }

    def 'GET (async) /foo: returns content'() {
        when:
        CompletableFuture futureResult = http.getAsync {
            request.uri.path = '/foo'
        }

        then:
        futureResult.get() == HTML_CONTENT
    }

    def 'GET (async BASIC) /basic: returns content'() {
        when:
        CompletableFuture futureResult = http.getAsync {
            request.uri.path = '/basic'
            request.auth.basic 'admin', '$3cr3t'
        }

        then:
        futureResult.get() == HTML_CONTENT
    }

    @Ignore('Implement once the digest conversation is configured')
    def 'GET (async DIGEST) /basic: returns content'(){

    }

    def 'GET /date: returns content of specified type'() {
        when:
        def result = http.get(Date) {
            request.uri.path = '/date'
            response.parser('text/date') { ChainedHttpConfig config, FromServer fromServer ->
                Date.parse('yyyy.MM.dd HH:mm', fromServer.inputStream.text)
            }
        }

        then:
        result instanceof Date
        result.format('MM/dd/yyyy HH:mm') == '08/25/2016 14:43'
    }

    def 'GET (async) /date: returns content of specified type'() {
        when:
        CompletableFuture futureResult = http.getAsync(Date) {
            request.uri.path = '/date'
            response.parser('text/date') { ChainedHttpConfig config, FromServer fromServer ->
                Date.parse('yyyy.MM.dd HH:mm', fromServer.inputStream.text)
            }
        }

        then:
        def result = futureResult.get()
        result instanceof Date
        result.format('MM/dd/yyyy HH:mm') == '08/25/2016 14:43'
    }
}

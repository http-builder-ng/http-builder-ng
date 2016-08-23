package groovyx.net.http

import groovyx.net.http.optional.ApacheHttpBuilder
import org.junit.Rule
import org.mockserver.client.server.MockServerClient
import org.mockserver.junit.MockServerRule
import org.mockserver.model.Header
import org.mockserver.model.NottableString
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.CompletableFuture
import java.util.function.Function

import static org.mockserver.model.HttpRequest.request
import static org.mockserver.model.HttpResponse.response

class HttpGetSpec extends Specification {

    // FIXME: may need to use Apache HttpClient for this so that https works
    // FIXME: need the old ignoreSslIssues functionality back

    // TODO: test with both builder factories

    // TODO: should probably test all the build-in encoders/decoders with each verb

    @Rule public MockServerRule serverRule = new MockServerRule(this)

    private static final String HTML_CONTENT_A = '<html><body>Testing A</body></html>'
    private static final String HTML_CONTENT_B = '<html><body>Testing B</body></html>'
    private static final String HTML_CONTENT_C = '<html><body>Testing C</body></html>'
    private static final Function apacheBuilder = { c -> new ApacheHttpBuilder(c); } as Function
    private MockServerClient server

    private HttpBuilder http

    def setup() {
        http = HttpBuilder.configure(apacheBuilder) {
            request.uri = "http://localhost:${serverRule.port}"
        }

        server.when(request().withMethod('GET').withPath('/')).respond(response().withBody(HTML_CONTENT_A))

        server.when(request().withMethod('GET').withPath('/foo').withQueryStringParameter('alpha', 'bravo')).respond(response().withBody(HTML_CONTENT_B))
        server.when(request().withMethod('GET').withPath('/foo').withCookie('biscuit', 'wafer')).respond(response().withBody(HTML_CONTENT_C))
        server.when(request().withMethod('GET').withPath('/foo')).respond(response().withBody(HTML_CONTENT_A))

        server.when(request().withMethod('GET').withPath('/date'))
            .respond(response().withBody('2016.08.25 14:43').withHeader('Content-Type', 'text/date'))

        // Status handlers

        (2..5).each { s ->
            server.when(request().withMethod('GET').withPath("/status${s}00")).respond(response().withStatusCode(s * 100))
        }

        // BASIC

        String encodedCred = "Basic ${'admin:$3cr3t'.bytes.encodeBase64()}"
        def authHeader = new Header('Authorization', encodedCred)

        server.when(request().withMethod('GET').withPath('/basic')
            .withHeader(NottableString.not('Authorization'), NottableString.not(encodedCred)))
            .respond(response().withHeader('WWW-Authenticate', 'Basic realm="Test Realm"').withStatusCode(401))

        server.when(request().withMethod('GET').withPath('/basic').withHeader(authHeader))
            .respond(response().withBody(HTML_CONTENT_A))

        // DIGEST

        // FIXME: this needs to be a proper digest request conversation
//        server.dumpToLog().when(request().withMethod('GET').withPath('/digest'))
//            .respond(response().withBody(HTML_CONTENT_A))
    }

    @Unroll def 'GET /status(#status): verify when(int) handler'() {
        when:
        boolean called = false

        http.get {
            request.uri.path = "/status${status}"
            response.when(status) {
                called = true
            }
        }

        then:
        called

        where:
        status << [200, 300, 400, 500]
    }

    @Unroll def 'GET (async) /status(#status): verify when(int) handler'() {
        when:
        boolean called = false

        http.getAsync {
            request.uri.path = "/status${status}"
            response.when(status) {
                called = true
            }
        }

        then:
        called

        where:
        status << [200, 300, 400, 500]
    }

    @Unroll def 'GET /status(#status): success/failure handler'() {
        when:
        boolean successCalled = false
        boolean failureCalled = false

        http.get {
            request.uri.path = "/status${status}"
            response.success {
                successCalled = true
            }
            response.failure {
                failureCalled = true
            }
        }

        then:
        successCalled == success
        failureCalled == failure

        where:
        status | success | failure
        200    | true    | false
        300    | true    | false
        400    | false   | true
        500    | false   | true
    }

    @Unroll def 'GET (async) /status(#status): success/failure handler'() {
        when:
        boolean successCalled = false
        boolean failureCalled = false

        http.getAsync {
            request.uri.path = "/status${status}"
            response.success {
                successCalled = true
            }
            response.failure {
                failureCalled = true
            }
        }

        then:
        successCalled == success
        failureCalled == failure

        where:
        status | success | failure
        200    | true    | false
        300    | true    | false
        400    | false   | true
        500    | false   | true
    }

    def 'GET /: returns content'() {
        when:
        def result = http.get()

        then:
        result == HTML_CONTENT_A
    }

    def 'GET (async) /: returns content'() {
        when:
        CompletableFuture futureResult = http.getAsync()

        then:
        futureResult.get() == HTML_CONTENT_A
    }

    def 'GET /foo: returns content'() {
        when:
        def result = http.get {
            request.uri.path = '/foo'
        }

        then:
        result == HTML_CONTENT_A
    }

    def 'GET /foo (cookie): retruns content'() {
        when:
        def result = http.get {
            request.uri.path = '/foo'
            request.cookie 'biscuit', 'wafer'
        }

        then:
        result == HTML_CONTENT_C
    }

    def 'GET (async) /foo (cookie): returns content'() {
        when:
        CompletableFuture futureResult = http.getAsync {
            request.uri.path = '/foo'
            request.cookie 'biscuit', 'wafer'
        }

        then:
        futureResult.get() == HTML_CONTENT_C
    }

    def 'GET /foo?alpha=bravo: returns content'() {
        when:
        def result = http.get {
            request.uri.path = '/foo'
            request.uri.query = [alpha: 'bravo']
        }

        then:
        result == HTML_CONTENT_B
    }

    def 'GET (async) /foo?alpha=bravo: returns content'() {
        when:
        CompletableFuture futureResult = http.getAsync {
            request.uri.path = '/foo'
            request.uri.query = [alpha: 'bravo']
        }

        then:
        futureResult.get() == HTML_CONTENT_B
    }

    def 'GET (BASIC) /basic: returns content'() {
        when:
        def result = http.get {
            request.uri.path = '/basic'
            request.auth.basic 'admin', '$3cr3t'
        }

        then:
        result == HTML_CONTENT_A
    }

    @Ignore('Need to get the digest conversation configured')
    def 'GET (DIGEST) /digest: returns content'() {
        when:
        def result = http.get {
            request.uri.path = '/digest'
            request.auth.digest 'admin', '$3cr3t'
        }

        then:
        result == HTML_CONTENT_A
    }

    def 'GET (async) /foo: returns content'() {
        when:
        CompletableFuture futureResult = http.getAsync {
            request.uri.path = '/foo'
        }

        then:
        futureResult.get() == HTML_CONTENT_A
    }

    def 'GET (async BASIC) /basic: returns content'() {
        when:
        CompletableFuture futureResult = http.getAsync {
            request.uri.path = '/basic'
            request.auth.basic 'admin', '$3cr3t'
        }

        then:
        futureResult.get() == HTML_CONTENT_A
    }

    @Ignore('Implement once the digest conversation is configured')
    def 'GET (async DIGEST) /digest: returns content'() {

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

package groovyx.net.http

import groovyx.net.http.optional.ApacheHttpBuilder
import org.junit.Rule
import org.mockserver.client.server.MockServerClient
import org.mockserver.junit.MockServerRule
import org.mockserver.model.Header
import org.mockserver.model.NottableString
import spock.lang.Specification
import spock.lang.Unroll

import java.util.function.Function

import static HttpClientType.APACHE
import static HttpClientType.JAVA
import static org.mockserver.model.HttpRequest.request
import static org.mockserver.model.HttpResponse.response

class HttpGetSpec extends Specification {

    // FIXME: test digest support
    // TODO: should probably test all the build-in encoders/decoders with each verb

    @Rule public MockServerRule serverRule = new MockServerRule(this)

    private static final String HTML_CONTENT_B = htmlContent('Testing B')
    private static final String HTML_CONTENT_C = htmlContent('Testing C')

    private MockServerClient server
    private HttpBuilder apacheHttp, javaHttp

    def setup() {
        apacheHttp = HttpBuilder.configure({ c -> new ApacheHttpBuilder(c); } as Function) {
            request.uri = "http://localhost:${serverRule.port}"
        }

        javaHttp = HttpBuilder.configure({ c -> new JavaHttpBuilder(c); } as Function) {
            request.uri = "http://localhost:${serverRule.port}"
        }

        server.when(request().withMethod('GET').withPath('/')).respond(response().withBody(htmlContent()))

        server.when(request().withMethod('GET').withPath('/foo').withQueryStringParameter('alpha', 'bravo')).respond(response().withBody(HTML_CONTENT_B).withStatusCode(200))
        server.when(request().withMethod('GET').withPath('/foo').withCookie('biscuit', 'wafer')).respond(response().withBody(HTML_CONTENT_C).withStatusCode(200))
        server.when(request().withMethod('GET').withPath('/foo')).respond(response().withBody(htmlContent()).withStatusCode(200))

        server.when(request().withMethod('GET').withPath('/date')).respond(response().withBody('2016.08.25 14:43').withHeader('Content-Type', 'text/date'))

        // Status handlers

        (2..5).each { s ->
            server.when(request().withMethod('GET').withPath("/status${s}00")).respond(response().withStatusCode(s * 100))
        }

        // BASIC

        String encodedCred = "Basic ${'admin:$3cr3t'.bytes.encodeBase64()}"
        def authHeader = new Header('Authorization', encodedCred)

        server.when(request().withMethod('GET').withPath('/basic').withHeader(NottableString.not('Authorization'), NottableString.not(encodedCred)))
            .respond(response().withHeader('WWW-Authenticate', 'Basic realm="Test Realm"').withStatusCode(401))

        server.when(request().withMethod('GET').withPath('/basic').withHeader(authHeader))
            .respond(response().withBody(htmlContent()))

        // DIGEST

        // FIXME: this needs to be a proper digest request conversation
//        server.dumpToLog().when(request().withMethod('GET').withPath('/digest'))
//            .respond(response().withBody(HTML_CONTENT_A))
    }

    @Unroll def '[#label] GET /status(#status): verify when(int) handler'() {
        given:
        CountedClosure counter = new CountedClosure()

        when:
        httpBuilder(label).get {
            request.uri.path = "/status${status}"
            response.when status, counter.closure
        }

        then:
        counter.called

        where:
        label  | status
        APACHE | 200
        APACHE | 300
        APACHE | 400
        APACHE | 500
        JAVA   | 200
        JAVA   | 300
        JAVA   | 400
        JAVA   | 500
    }

    @Unroll def '[#label] GET (async) /status(#status): verify when(int) handler'() {
        given:
        CountedClosure counter = new CountedClosure()

        when:
        httpBuilder(label).getAsync {
            request.uri.path = "/status${status}"
            response.when status, counter.closure
        }.get()

        then:
        counter.called

        where:
        label  | status
        APACHE | 200
        APACHE | 300
        APACHE | 400
        APACHE | 500
        JAVA   | 200
        JAVA   | 300
        JAVA   | 400
        JAVA   | 500
    }

    @Unroll def '[#label] GET /status(#status): success/failure handler'() {
        given:
        CountedClosure successCounter = new CountedClosure()
        CountedClosure failureCounter = new CountedClosure()

        when:
        httpBuilder(label).get {
            request.uri.path = "/status${status}"
            response.success successCounter.closure
            response.failure failureCounter.closure
        }

        then:
        successCounter.called == success
        failureCounter.called == failure

        where:
        label  | status | success | failure
        APACHE | 200    | true    | false
        APACHE | 300    | true    | false
        APACHE | 400    | false   | true
        APACHE | 500    | false   | true
        JAVA   | 200    | true    | false
        JAVA   | 300    | true    | false
        JAVA   | 400    | false   | true
        JAVA   | 500    | false   | true
    }

    @Unroll def '[#label] GET (async) /status(#status): success/failure handler'() {
        given:
        CountedClosure successCounter = new CountedClosure()
        CountedClosure failureCounter = new CountedClosure()

        when:
        httpBuilder(label).getAsync {
            request.uri.path = "/status${status}"
            response.success successCounter.closure
            response.failure failureCounter.closure
        }.get()

        then:
        successCounter.called == success
        failureCounter.called == failure

        where:
        label  | status | success | failure
        APACHE | 200    | true    | false
        APACHE | 300    | true    | false
        APACHE | 400    | false   | true
        APACHE | 500    | false   | true
        JAVA   | 200    | true    | false
        JAVA   | 300    | true    | false
        JAVA   | 400    | false   | true
        JAVA   | 500    | false   | true
    }

    @Unroll def '[#label] GET /: returns content'() {
        expect:
        httpBuilder(label).get() == htmlContent()

        where:
        label << [APACHE, JAVA]
    }

    @Unroll def '[#label] GET (async) /: returns content'() {
        expect:
        httpBuilder(label).getAsync().get() == htmlContent()

        where:
        label << [APACHE, JAVA]
    }

    @Unroll def '[#label] GET /foo: returns content'() {
        expect:
        httpBuilder(label).get {
            request.uri.path = '/foo'
        } == htmlContent()

        where:
        label << [APACHE, JAVA]
    }

    @Unroll def '[#label] GET (async) /foo: returns content'() {
        expect:
        httpBuilder(label).getAsync {
            request.uri.path = '/foo'
        }.get() == htmlContent()

        where:
        label << [APACHE, JAVA]
    }

    @Unroll def '[#label] GET /foo (cookie): returns content'() {
        expect:
        httpBuilder(label).get {
            request.uri.path = '/foo'
            request.cookie 'biscuit', 'wafer'
        } == HTML_CONTENT_C

        where:
        label << [APACHE] //, JAVA]  // FIXME: the JAVA fails - determine if impl wrong or server wrong
    }

    @Unroll def '[#label] GET (async) /foo (cookie): returns content'() {
        expect:
        httpBuilder(label).getAsync {
            request.uri.path = '/foo'
            request.cookie 'biscuit', 'wafer'
        }.get() == HTML_CONTENT_C

        where:
        label << [APACHE] //, JAVA]  // FIXME: the JAVA fails - determine if impl wrong or server wrong
    }

    @Unroll def '[#label] GET /foo?alpha=bravo: returns content'() {
        expect:
        httpBuilder(label).get {
            request.uri.path = '/foo'
            request.uri.query = [alpha: 'bravo']
        } == HTML_CONTENT_B

        where:
        label << [APACHE] //, JAVA]  // FIXME: the JAVA fails - determine if impl wrong or server wrong
    }

    @Unroll def '[#label] GET (async) /foo?alpha=bravo: returns content'() {
        expect:
        httpBuilder(label).getAsync() {
            request.uri.path = '/foo'
            request.uri.query = [alpha: 'bravo']
        }.get() == HTML_CONTENT_B

        where:
        label << [APACHE] //, JAVA]  // FIXME: the JAVA fails - determine if impl wrong or server wrong
    }

    @Unroll def '[#label] GET (BASIC) /basic: returns content'() {
        expect:
        httpBuilder(label).get {
            request.uri.path = '/basic'
            request.auth.basic 'admin', '$3cr3t'
        } == htmlContent()

        where:
        label << [APACHE, JAVA]
    }

    @Unroll def '[#label] GET (async BASIC) /basic: returns content'() {
        expect:
        httpBuilder(label).getAsync {
            request.uri.path = '/basic'
            request.auth.basic 'admin', '$3cr3t'
        }.get() == htmlContent()

        where:
        label << [APACHE, JAVA]
    }

    @Unroll def '[#label] GET /date: returns content of specified type'() {
        when:
        def result = httpBuilder(label).get(Date) {
            request.uri.path = '/date'
            response.parser('text/date') { ChainedHttpConfig config, FromServer fromServer ->
                Date.parse('yyyy.MM.dd HH:mm', fromServer.inputStream.text)
            }
        }

        then:
        result instanceof Date
        result.format('MM/dd/yyyy HH:mm') == '08/25/2016 14:43'

        where:
        label << [APACHE, JAVA]
    }

    @Unroll def '[#label] GET (async) /date: returns content of specified type'() {
        when:
        def result = httpBuilder(label).get(Date) {
            request.uri.path = '/date'
            response.parser('text/date') { ChainedHttpConfig config, FromServer fromServer ->
                Date.parse('yyyy.MM.dd HH:mm', fromServer.inputStream.text)
            }
        }

        then:
        result instanceof Date
        result.format('MM/dd/yyyy HH:mm') == '08/25/2016 14:43'

        where:
        label << [APACHE, JAVA]
    }

    private HttpBuilder httpBuilder(final HttpClientType factory) {
        factory == APACHE ? apacheHttp : javaHttp
    }

    private static String htmlContent(String text = 'Nothing special') {
        "<html><body><!-- a bunch of really interesting content that you would be sorry to miss -->$text</body></html>" as String
    }
}

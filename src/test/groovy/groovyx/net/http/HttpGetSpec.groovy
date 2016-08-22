package groovyx.net.http

import org.junit.Rule
import org.mockserver.client.server.MockServerClient
import org.mockserver.junit.MockServerRule
import spock.lang.Specification

import java.util.concurrent.CompletableFuture

import static org.mockserver.model.HttpRequest.request
import static org.mockserver.model.HttpResponse.response

class HttpGetSpec extends Specification {

    // FIXME: may need to use Apache HttpClient for this so that https works

    @Rule public MockServerRule serverRule = new MockServerRule(this)

    private static final String HTML_CONTENT = '<html><body>Testing</body></html>'
    private MockServerClient server

    private HttpBuilder http

    def setup() {
        http = HttpBuilder.configure {
            request.uri = "http://localhost:${serverRule.port}"
        }
    }

    /*
        http
        https

        basic
        digest
     */

    def 'GET /: returns content'() {
        setup:
        server.when(request().withMethod('GET').withPath('/'))
            .respond(response().withBody(HTML_CONTENT))

        when:
        def result = http.get()

        then:
        result == HTML_CONTENT
    }

    def 'GET (async) /: returns content'() {
        setup:
        server.when(request().withMethod('GET').withPath('/'))
            .respond(response().withBody(HTML_CONTENT))

        when:
        CompletableFuture futureResult = http.getAsync()

        then:
        futureResult.get() == HTML_CONTENT
    }

    def 'GET /foo: returns content'() {
        setup:
        server.when(request().withMethod('GET').withPath('/foo'))
            .respond(response().withBody(HTML_CONTENT))

        when:
        def result = http.get {
            request.uri.path = '/foo'
        }

        then:
        result == HTML_CONTENT
    }

    def 'GET (async) /foo: returns content'() {
        setup:
        server.when(request().withMethod('GET').withPath('/foo'))
            .respond(response().withBody(HTML_CONTENT))

        when:
        CompletableFuture futureResult = http.getAsync {
            request.uri.path = '/foo'
        }

        then:
        futureResult.get() == HTML_CONTENT
    }

    def 'GET /date: returns content of specified type'() {
        setup:
        server.when(request().withMethod('GET').withPath('/date'))
            .respond(response().withBody('2016.08.25 14:43').withHeader('Content-Type', 'text/date'))

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
        setup:
        server.when(request().withMethod('GET').withPath('/date'))
            .respond(response().withBody('2016.08.25 14:43').withHeader('Content-Type', 'text/date'))

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

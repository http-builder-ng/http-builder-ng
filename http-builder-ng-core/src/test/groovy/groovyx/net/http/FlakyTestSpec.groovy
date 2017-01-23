package groovyx.net.http

import com.stehno.ersatz.Decoders
import com.stehno.ersatz.ErsatzServer
import com.stehno.ersatz.RequestDecoders
import spock.lang.Specification

import static com.stehno.ersatz.ContentType.APPLICATION_JSON
import static com.stehno.ersatz.ContentType.APPLICATION_URLENCODED
import static com.stehno.ersatz.ContentType.TEXT_PLAIN
import static groovyx.net.http.ContentTypes.TEXT

/**
 * Created by cjstehno on 1/23/17.
 */
class FlakyTestSpec extends Specification {

    protected final ErsatzServer ersatzServer = new ErsatzServer({
        enableHttps()
    })

    protected final RequestDecoders commonDecoders = new RequestDecoders({
        register TEXT_PLAIN, Decoders.utf8String
        register APPLICATION_URLENCODED, Decoders.urlEncoded
        register APPLICATION_JSON, Decoders.parseJson
    })

    // FIXME: this seems to be able to make it fail every time - basically it will work N times and the send a request with no body
    // - its not just that the server is not getting the body, it's not in the client side request after the closure is processed
    def 'rooting out flaky test bug'() {
        given:
        ersatzServer.expectations {
            post('/foo').decoders(commonDecoders).body('This is CONTENT!!', TEXT[0]).query('action', 'login').responds().content(htmlContent(), TEXT[0])
        }.start()

        def results = []

        when:
        for (int i = 0; i < 1000; i++) {
            results << JavaHttpBuilder.configure {
                request.uri = "${ersatzServer.httpUrl}"
            }.post({
                request.uri.path = '/foo'
                request.uri.query = [action: 'login']
                request.body = 'This is CONTENT!!'
                request.contentType = TEXT[0]
            })
        }

        then:
        results.every {
            it == htmlContent()
        }
    }

    private static String htmlContent(String text = 'Nothing special') {
        "<html><body><!-- a bunch of really interesting content that you would be sorry to miss -->$text</body></html>" as String
    }
}

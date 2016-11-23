package groovyx.net.http

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.rules.ExternalResource

/**
 * JUnit Rule used to manage a mock web server for testing.
 */
class MockWebServerRule extends ExternalResource {

    private MockWebServer server

    @Override
    protected void before() throws Throwable {
        server = new MockWebServer()
        server.start()
    }

    void dispatcher(final Closure<MockResponse> closure) {
        server.dispatcher = closure as Dispatcher
    }

    void dispatcher(final String path, final MockResponse response) {
        dispatcher { RecordedRequest request ->
            if (request.path == path) {
                return response
            }
            return new MockResponse().setResponseCode(404)
        }
    }

    String getServerUrl() {
        "http://${server.hostName}:${server.port}"
    }

    @Override
    protected void after() {
        server.shutdown()
    }
}

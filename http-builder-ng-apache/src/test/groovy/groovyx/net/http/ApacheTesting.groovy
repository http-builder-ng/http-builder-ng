package groovyx.net.http

import java.util.function.Function

/**
 * Helpers for testing with the Apache client HttpBuilder.
 */
class ApacheTesting {

    static final Function clientFactory = { c -> new ApacheHttpBuilder(c); } as Function

    static HttpBuilder httpBuilder(Closure config) {
        HttpBuilder.configure(clientFactory, config)
    }

    static HttpBuilder httpBuilder(String uri) {
        httpBuilder {
            request.uri = uri
        }
    }

    static HttpBuilder httpBuilder(int port) {
        httpBuilder {
            request.uri = "http://localhost:${port}"
        }
    }
}

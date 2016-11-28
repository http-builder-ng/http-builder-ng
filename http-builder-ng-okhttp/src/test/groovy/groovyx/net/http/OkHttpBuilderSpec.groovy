package groovyx.net.http

import groovyx.net.http.tk.HttpBuilderTestKit

import java.util.function.Function

class OkHttpBuilderSpec extends HttpBuilderTestKit {

    def setup() {
        clientFactory = { c -> new OkHttpBuilder(c) } as Function

        option COMPRESSION_OPTION, false

        init()
    }
}

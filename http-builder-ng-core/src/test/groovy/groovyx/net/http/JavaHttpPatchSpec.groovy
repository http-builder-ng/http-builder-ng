/*
 * Copyright (C) 2017 HttpBuilder-NG Project
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

import groovyx.net.http.tk.HttpMethodTestKit
import spock.lang.Unroll

class JavaHttpPatchSpec extends HttpMethodTestKit {
    @Unroll "patch() throws UnsupportedOperationException"() {
        setup:
        ersatzServer.expectations {
            // The expected exception should be thrown before the server is called.
            patch('/').called(0)
        }
        def http = HttpBuilder.configure {
            request.uri = serverUri(proto)
        }

        when:
        http.patch()

        then:
        thrown(UnsupportedOperationException)
        ersatzServer.verify()

        where:
        proto << ['HTTP', 'HTTPS']
    }

    @Unroll "patchAsync() throws UnsupportedOperationException"() {
        setup:
        ersatzServer.expectations {
            // The expected exception should be thrown before the server is called.
            patch('/').called(0)
        }
        def http = HttpBuilder.configure {
            request.uri = serverUri(proto)
        }
        def thrownException = null

        when:
        http.patchAsync().exceptionally { thrownException = it.cause }

        then:
        thrownException != null
        thrownException instanceof UnsupportedOperationException
        ersatzServer.verify()

        where:
        proto << ['HTTP', 'HTTPS']
    }

    @Unroll "patch(Consumer<HttpConfig>) throws UnsupportedOperationException"() {
        setup:
        ersatzServer.expectations {
            // The expected exception should be thrown before the server is called.
            patch('/').called(0)
        }
        def http = HttpBuilder.configure { }
        def config = {
            request.uri = serverUri(proto)
        }

        when:
        http.patch(config)

        then:
        thrown(UnsupportedOperationException)
        ersatzServer.verify()

        where:
        proto << ['HTTP', 'HTTPS']
    }

    @Unroll "patchAsync(Consumer<HttpConfig>) throws UnsupportedOperationException"() {
        setup:
        ersatzServer.expectations {
            // The expected exception should be thrown before the server is called.
            patch('/').called(0)
        }
        def http = HttpBuilder.configure { }
        def config = {
            request.uri = serverUri(proto)
        }
        def thrownException = null

        when:
        http.patchAsync(config).exceptionally { e ->
            thrownException = e.cause
        }

        then:
        thrownException != null
        thrownException instanceof UnsupportedOperationException
        ersatzServer.verify()

        where:
        proto << ['HTTP', 'HTTPS']
    }
}

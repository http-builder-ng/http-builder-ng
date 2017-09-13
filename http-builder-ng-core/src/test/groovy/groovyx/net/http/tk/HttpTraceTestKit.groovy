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
package groovyx.net.http.tk

import groovyx.net.http.FromServer
import groovyx.net.http.HttpBuilder
import groovyx.net.http.HttpConfig
import spock.lang.Unroll

import java.util.function.Consumer

/**
 * Test kit used to test HTTP TRACE methods on different clients.
 */
abstract class HttpTraceTestKit extends HttpMethodTestKit {

    // NOTE: TRACE method does not support HTTPS

    def setup() {
        ersatzServer.start()
    }

    @Unroll 'trace()'() {
        setup:
        HttpBuilder http = setupHttpBuilder()

        expect:
        http.trace().startsWith('TRACE / HTTP/1.1')

        and:
        http.traceAsync().get().startsWith('TRACE / HTTP/1.1')
    }

    @Unroll 'trace(Closure)'() {
        setup:
        HttpBuilder http = setupHttpBuilder()

        def config = {
            request.uri.path = '/something'
        }

        expect:
        http.trace(config).startsWith('TRACE /something HTTP/1.1')

        and:
        http.traceAsync(config).get().startsWith('TRACE /something HTTP/1.1')
    }

    @Unroll 'trace(Class,Closure)'() {
        setup:
        HttpBuilder http = setupHttpBuilder()

        def config = {
            request.uri.path = '/something'
        }

        expect:
        http.trace(String, config).startsWith('TRACE /something HTTP/1.1')

        and:
        http.traceAsync(String, config).get().startsWith('TRACE /something HTTP/1.1')
    }

    @Unroll 'trace(Consumer)'() {
        setup:
        HttpBuilder http = setupHttpBuilder()

        Consumer<HttpConfig> config = new Consumer<HttpConfig>() {
            @Override void accept(HttpConfig httpConfig) {
                httpConfig.request.uri.path = '/something'
            }
        }

        expect:
        http.trace(config).startsWith('TRACE /something HTTP/1.1')

        and:
        http.traceAsync(config).get().startsWith('TRACE /something HTTP/1.1')
    }

    @Unroll 'trace(Class,Consumer)'() {
        setup:
        HttpBuilder http = setupHttpBuilder()

        Consumer<HttpConfig> config = new Consumer<HttpConfig>() {
            @Override void accept(HttpConfig httpConfig) {
                httpConfig.request.uri.path = '/something'
            }
        }

        expect:
        http.trace(String, config).startsWith('TRACE /something HTTP/1.1')

        and:
        http.traceAsync(String, config).get().startsWith('TRACE /something HTTP/1.1')
    }

    private HttpBuilder setupHttpBuilder() {
        httpBuilder {
            request.uri = ersatzServer.httpUrl
            response.success { FromServer fs, Object body ->
                new String(body as byte[]).trim()
            }
        }
    }
}
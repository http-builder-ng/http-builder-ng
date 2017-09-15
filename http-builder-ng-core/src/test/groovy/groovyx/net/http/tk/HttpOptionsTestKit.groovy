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

import static com.stehno.ersatz.HttpMethod.*
import static groovyx.net.http.util.SslUtils.ignoreSslIssues

/**
 * Test kit for testing the HTTP OPTIONS method with different clients.
 */
abstract class HttpOptionsTestKit extends HttpMethodTestKit {

    def setup() {
        ersatzServer.expectations {
            options('/foo').called(2).responds().code(200).allows(GET, HEAD, POST)
        }
    }

    @Unroll 'options(): #protocol'() {
        setup:
        HttpBuilder http = setupHttpBuilder("${serverUri(protocol)}/foo")

        expect:
        http.options()

        and:
        http.optionsAsync().get()

        and:
        ersatzServer.verify()

        where:
        protocol << ['HTTP', 'HTTPS']
    }

    @Unroll 'options(Closure): #protocol'() {
        setup:
        HttpBuilder http = setupHttpBuilder(serverUri(protocol))

        def config = {
            request.uri.path = '/foo'
        }

        expect:
        http.options(config)

        and:
        http.optionsAsync(config).get()

        and:
        ersatzServer.verify()

        where:
        protocol << ['HTTP', 'HTTPS']
    }

    @Unroll 'options(Consumer): #protocol'() {
        setup:
        HttpBuilder http = setupHttpBuilder(serverUri(protocol))

        Consumer<HttpConfig> config = new Consumer<HttpConfig>() {
            @Override void accept(HttpConfig httpConfig) {
                httpConfig.request.uri.path = '/foo'
            }
        }

        expect:
        http.options(config)

        and:
        http.optionsAsync(config).get()

        and:
        ersatzServer.verify()

        where:
        protocol << ['HTTP', 'HTTPS']
    }

    @Unroll 'options(Class,Closure): #protocol'() {
        setup:
        HttpBuilder http = setupHttpBuilder(serverUri(protocol))

        def config = {
            request.uri.path = '/foo'
        }

        expect:
        http.options(Boolean,config) instanceof Boolean

        and:
        http.optionsAsync(Boolean, config).get() instanceof Boolean

        and:
        ersatzServer.verify()

        where:
        protocol << ['HTTP', 'HTTPS']
    }

    @Unroll 'options(Class,Consumer): #protocol'() {
        setup:
        HttpBuilder http = setupHttpBuilder(serverUri(protocol))

        Consumer<HttpConfig> config = new Consumer<HttpConfig>() {
            @Override void accept(HttpConfig httpConfig) {
                httpConfig.request.uri.path = '/foo'
            }
        }

        expect:
        http.options(Boolean,config) instanceof Boolean

        and:
        http.optionsAsync(Boolean, config).get() instanceof Boolean

        and:
        ersatzServer.verify()

        where:
        protocol << ['HTTP', 'HTTPS']
    }

    private HttpBuilder setupHttpBuilder(final String uri) {
        httpBuilder {
            ignoreSslIssues execution
            request.uri = uri
            response.success { FromServer fs, Object body ->
                assert !body
                assert fs.headers.findAll { h -> h.key == 'Allow' && h.value in ['GET', 'POST', 'HEAD'] }.size() == 3
                Boolean.TRUE
            }
        }
    }
}

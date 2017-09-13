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

import com.stehno.ersatz.ErsatzServer
import spock.lang.AutoCleanup
import spock.lang.Specification

import static com.stehno.ersatz.ContentType.TEXT_PLAIN

class JavaHttpBuilderSpec extends Specification {

    @AutoCleanup('stop')
    private ErsatzServer ersatzServer = new ErsatzServer({
        expectations {
            get('/foo').responds().content('ok', TEXT_PLAIN)
            get('/bar').responds().content('ok2', TEXT_PLAIN)
        }
    })

    def 'client-specific configuration'() {
        setup:
        HttpBuilder http = JavaHttpBuilder.configure {
            request.uri = "${ersatzServer.httpUrl}/foo"
        }

        expect:
        http.get() == 'ok'

        and:
        http instanceof JavaHttpBuilder
    }

    def 'access to client implementation unsupported'() {
        setup:
        HttpBuilder http = JavaHttpBuilder.configure {
            request.uri = "${ersatzServer.httpUrl}/foo"
        }

        when:
        http.clientImplementation

        then:
        thrown(UnsupportedOperationException)
    }

    def 'client customization unsupported'() {
        when:
        HttpBuilder http = JavaHttpBuilder.configure {
            client.clientCustomizer { HttpURLConnection connection ->
                connection.connectTimeout = 8675309
            }
            request.uri = "${ersatzServer.httpUrl}/foo"
        }

        then: 'just ensure no failure'
        http
    }

    def 'copying the client'() {
        when:
        HttpBuilder http = JavaHttpBuilder.configure {
            request.uri = "${ersatzServer.httpUrl}/foo"
        }
        HttpBuilder http2 = http.copy {
            request.uri = "${ersatzServer.httpUrl}/bar"
        }

        then:
            http.get() == 'ok'
            http2.get() == 'ok2'
    }
}

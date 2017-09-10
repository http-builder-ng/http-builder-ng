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
import okhttp3.OkHttpClient
import spock.lang.AutoCleanup
import spock.lang.Specification

import static com.stehno.ersatz.ContentType.TEXT_PLAIN
import static java.util.concurrent.TimeUnit.MILLISECONDS
import static java.util.concurrent.TimeUnit.MINUTES

class OkHttpBuilderSpec extends Specification {

    @AutoCleanup('stop')
    private ErsatzServer ersatzServer = new ErsatzServer({
        expectations {
            get('/foo').responds().content('ok', TEXT_PLAIN)
        }
    })

    def 'client-specific configuration'() {
        setup:
        HttpBuilder http = OkHttpBuilder.configure {
            request.uri = "${ersatzServer.httpUrl}/foo"
        }

        expect:
        http.get() == 'ok'

        and:
        http instanceof OkHttpBuilder
    }

    def 'access to client implementation'() {
        setup:
        HttpBuilder http = OkHttpBuilder.configure {
            request.uri = "${ersatzServer.httpUrl}/foo"
        }

        expect:
        http.clientImplementation instanceof OkHttpClient
    }

    def 'client customization'() {
        setup:
        HttpBuilder http = OkHttpBuilder.configure {
            client.clientCustomizer { OkHttpClient.Builder builder ->
                builder.connectTimeout(5, MINUTES)
            }
            request.uri = "${ersatzServer.httpUrl}/foo"
        }

        when:
        OkHttpClient client = http.clientImplementation

        then:
        client.connectTimeoutMillis() == MILLISECONDS.convert(5, MINUTES)
    }
}

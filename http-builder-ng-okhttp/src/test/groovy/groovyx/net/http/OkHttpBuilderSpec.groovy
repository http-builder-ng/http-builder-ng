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
import groovy.transform.Canonical
import okhttp3.OkHttpClient
import spock.lang.AutoCleanup
import spock.lang.Specification

import static com.stehno.ersatz.ContentType.TEXT_PLAIN
import static groovyx.net.http.ContentTypes.JSON
import static groovyx.net.http.ContentTypes.JSON
import static groovyx.net.http.NativeHandlers.Parsers.json
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

    def 'FromServer hasBody should return false when there is no content'() {
        setup:
        ersatzServer.expectations {
            get('/foasdfasdfo').responds().code(200)
        }

        when:
        String result = OkHttpBuilder.configure {
            request.uri = ersatzServer.httpUrl
        }.get(String) {
            request.uri.path = '/foasdfasdfo'
            response.success { FromServer fs, Object body ->
                assert !fs.hasBody
                body
            }
        }

        then:
        !result
    }

    def 'FromServer hasBody should return true when there is content'() {
        setup:
        ersatzServer.expectations {
            get('/gooasdfasdf').responds().code(200).content('GOOD', TEXT_PLAIN)
        }

        when:
        String result = OkHttpBuilder.configure {
            request.uri = ersatzServer.httpUrl
        }.get(String) {
            request.uri.path = '/gooasdfasdf'
            response.success { FromServer fs, Object body ->
                assert fs.hasBody
                body
            }
        }

        then:
        result == 'GOOD'
    }

    def 'Sending/Receiving JSON Data (POST)'() {
        when:
        ItemScore itemScore = OkHttpBuilder.configure {
            request.uri = 'http://httpbin.org'
            request.contentType = JSON[0]
            response.parser(JSON[0]) { config, resp ->
                new ItemScore(json(config, resp).json)
            }
        }.post(ItemScore) {
            request.uri.path = '/post'
            request.body = new ItemScore('ASDFASEACV235', 90786)
        }

        then:
        "Your score for item (${itemScore.item}) was (${itemScore.score})." == "Your score for item (ASDFASEACV235) was (90786)."
    }

    @Canonical
    static class ItemScore {
        String item
        Long score
    }
}

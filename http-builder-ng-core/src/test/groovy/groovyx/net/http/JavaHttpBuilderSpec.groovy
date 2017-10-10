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
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.util.function.BiFunction
import java.util.function.Function

import static com.stehno.ersatz.ContentType.TEXT_PLAIN
import static groovyx.net.http.ContentTypes.JSON
import static groovyx.net.http.ContentTypes.JSON
import static groovyx.net.http.NativeHandlers.Parsers.json

class JavaHttpBuilderSpec extends Specification {

    @AutoCleanup('stop')
    private ErsatzServer ersatzServer = new ErsatzServer({
        expectations {
            get('/foo').responds().content('ok', TEXT_PLAIN)
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

    def 'FromServer hasBody should return false when there is no content'() {
        setup:
        ersatzServer.expectations {
            post('/foo').responds().code(200)
        }

        when:
        String result = JavaHttpBuilder.configure {
            request.uri = ersatzServer.httpUrl
        }.post(String) {
            request.uri.path = '/foo'
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
            post('/foo').responds().code(200).content('OK', TEXT_PLAIN)
        }

        Logger log = LoggerFactory.getLogger('TESTING')

        when:
        String result = JavaHttpBuilder.configure {
            request.uri = ersatzServer.httpUrl

            execution.interceptor(HttpVerb.POST){ ChainedHttpConfig config, fx ->
                log.info 'Configuration: {}->{}', config.chainedRequest.verb, config.chainedRequest.uri.toURI()
                fx.apply(config)
            }

        }.post(String) {
            request.uri.path = '/foo'
            response.success { FromServer fs, Object body ->
                assert fs.hasBody
                body
            }
        }

        then:
        result == 'OK'
    }

    def 'Sending/Receiving JSON Data (POST)'() {
        when:
        ItemScore itemScore = JavaHttpBuilder.configure {
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

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
import org.apache.http.client.HttpClient
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.HttpClientBuilder
import spock.lang.AutoCleanup
import spock.lang.Specification

import static com.stehno.ersatz.ContentType.TEXT_PLAIN
import static groovyx.net.http.ContentTypes.JSON
import static groovyx.net.http.NativeHandlers.Parsers.json

class ApacheHttpBuilderSpec extends Specification {

    @AutoCleanup('stop')
    private ErsatzServer ersatzServer = new ErsatzServer({
        expectations {
            get('/foo').responds().content('ok', TEXT_PLAIN)
        }
    })

    def 'client-specific configuration'() {
        setup:
        HttpBuilder http = ApacheHttpBuilder.configure {
            request.uri = "${ersatzServer.httpUrl}/foo"
        }

        expect:
        http.get() == 'ok'

        and:
        http instanceof ApacheHttpBuilder
    }

    def 'access to client implementation'() {
        setup:
        HttpBuilder http = ApacheHttpBuilder.configure {
            request.uri = "${ersatzServer.httpUrl}/foo"
        }

        expect:
        http.clientImplementation instanceof HttpClient
    }

    def 'client customization'() {
        setup:
        HttpBuilder http = ApacheHttpBuilder.configure {
            client.clientCustomizer { HttpClientBuilder builder ->
                RequestConfig.Builder requestBuilder = RequestConfig.custom()
                requestBuilder.connectTimeout = 1234567
                requestBuilder.connectionRequestTimeout = 98765

                builder.defaultRequestConfig = requestBuilder.build()
            }
            request.uri = "${ersatzServer.httpUrl}/foo"
        }

        when:
        HttpClient client = http.clientImplementation

        then:
        client.defaultConfig.connectTimeout == 1234567
        client.defaultConfig.connectionRequestTimeout == 98765
    }

    def 'FromServer hasBody should return false when there is no content'() {
        setup:
        ersatzServer.expectations {
            post('/foo').responds().code(200)
        }

        when:
        String result = ApacheHttpBuilder.configure {
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

        when:
        String result = ApacheHttpBuilder.configure {
            request.uri = ersatzServer.httpUrl
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
        ItemScore itemScore = ApacheHttpBuilder.configure {
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

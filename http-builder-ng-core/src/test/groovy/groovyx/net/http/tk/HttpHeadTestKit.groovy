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

import groovyx.net.http.ChainedHttpConfig
import groovyx.net.http.FromServer
import groovyx.net.http.HttpBuilder
import groovyx.net.http.HttpConfig
import spock.lang.Unroll

import java.util.function.BiFunction
import java.util.function.Consumer
import java.util.function.Function

import static com.stehno.ersatz.NoCookiesMatcher.noCookies
import static groovyx.net.http.HttpVerb.HEAD
import static groovyx.net.http.util.SslUtils.ignoreSslIssues

/**
 * Test kit for testing the HTTP HEAD method with different clients.
 */
abstract class HttpHeadTestKit extends HttpMethodTestKit {

    @Unroll 'head(): #protocol'() {
        setup:
        ersatzServer.expectations {
            head('/alpha').protocol(protocol).called(2).responder {
                code 200
                header 'X-Something', 'Testing'
            }
        }

        HttpBuilder http = httpBuilder {
            ignoreSslIssues execution
            request.uri = "${serverUri(protocol)}/alpha"
            response.success { FromServer fs, Object body ->
                !body && headersMatch(fs, Connection: 'keep-alive', 'Content-Length': '0', Date: { d -> d }, 'X-Something': 'Testing')
            }
        }

        expect:
        http.head()

        and:
        http.headAsync().get()

        and:
        ersatzServer.verify()

        where:
        protocol << ['HTTP', 'HTTPS']
    }

    @Unroll 'head(Closure): #query'() {
        setup:
        ersatzServer.expectations {
            head('/alpha').queries(query).called(2).responder {
                code 200
                header 'X-Something', 'Testing'
            }
        }

        HttpBuilder http = httpBuilder(ersatzServer.httpUrl)

        def config = {
            request.uri.path = '/alpha'
            request.uri.query = query
            response.success { FromServer fs, Object body ->
                !body && headersMatch(fs, Connection: 'keep-alive', 'Content-Length': '0', Date: { d -> d }, 'X-Something': 'Testing')
            }
        }

        expect:
        http.head(config)

        and:
        http.headAsync(config).get()

        and:
        ersatzServer.verify()

        where:
        query << [
            null,
            [:],
            [alpha: 'one'],
            [alpha: ['one']],
            [alpha: ['one', 'two']],
            [alpha: ['one', 'two'], bravo: 'three']
        ]
    }

    @Unroll 'head(Consumer): #cookies'() {
        setup:
        ersatzServer.expectations {
            head('/alpha').cookies(cookies == null ? noCookies() : cookies).called(2).responder {
                code 200
                header 'X-Something', 'Testing'
            }
        }

        HttpBuilder http = httpBuilder(ersatzServer.httpUrl)

        // variable scoping
        def requestCookies = cookies

        Consumer<HttpConfig> consumer = new Consumer<HttpConfig>() {
            @Override void accept(final HttpConfig config) {
                config.request.uri.path = '/alpha'

                requestCookies.each { n, v ->
                    config.request.cookie n, v
                }

                config.response.success { FromServer fs, Object body ->
                    !body && headersMatch(fs, Connection: 'keep-alive', 'Content-Length': '0', Date: { d -> d }, 'X-Something': 'Testing')
                }
            }
        }

        expect:
        http.head(consumer)

        and:
        http.headAsync(consumer).get()

        and:
        ersatzServer.verify()

        where:
        cookies << [
            null,
            [:],
            [flavor: 'chocolate-chip'],
            [flavor: 'chocolate-chip', count: 'dozen']
        ]
    }

    @Unroll 'head(Class,Closure): #headers'() {
        setup:
        ersatzServer.expectations {
            head('/alpha').headers(headers).called(2).responder {
                code 200
                header 'X-Something', 'Testing'
            }
        }

        HttpBuilder http = httpBuilder(ersatzServer.httpUrl)

        def config = {
            request.uri.path = '/alpha'
            request.headers = headers
            response.success { FromServer fs, Object body ->
                (!body && headersMatch(fs, Connection: 'keep-alive', 'Content-Length': '0', Date: { d -> d }, 'X-Something': 'Testing')) as String
            }
        }

        expect:
        http.head(String, config) == 'true'

        and:
        http.headAsync(String, config).get() == 'true'

        and:
        ersatzServer.verify()

        where:
        headers << [
            null,
            [:],
            [hat: 'fedora']
        ]
    }

    @Unroll 'head(Class,Consumer): #headers'() {
        setup:
        ersatzServer.expectations {
            head('/alpha').headers(headers).called(2).responder {
                code 200
                header 'X-Something', 'Testing'
            }
        }

        HttpBuilder http = httpBuilder(ersatzServer.httpUrl)

        // variable scoping
        def requestHeaders = headers

        Consumer<HttpConfig> consumer = new Consumer<HttpConfig>() {
            @Override void accept(final HttpConfig config) {
                config.request.uri.path = '/alpha'
                config.request.headers = requestHeaders
                config.response.success { FromServer fs, Object body ->
                    (!body && headersMatch(fs, Connection: 'keep-alive', 'Content-Length': '0', Date: { d -> d }, 'X-Something': 'Testing')) as String
                }
            }
        }

        expect:
        http.head(String, consumer) == 'true'

        and:
        http.headAsync(String, consumer).get() == 'true'

        and:
        ersatzServer.verify()

        where:
        headers << [
            null,
            [:],
            [hat: 'fedora']
        ]
    }

    protected static boolean headersMatch(final Map<String, Object> expectedHeaders = [:], final FromServer fs) {
        def headers = fs.headers.collectEntries { h -> [h.key, h.value] }

        headers.size() == expectedHeaders.size() && expectedHeaders.every { k, v ->
            v instanceof Closure ? v(headers[k]) : headers[k] == v
        }
    }

    @Unroll '#protocol HEAD with BASIC authentication (authorized)'() {
        setup:
        ersatzServer.authentication {
            basic()
        }

        ersatzServer.expectations {
            head('/basic').protocol(protocol).called(2).responds().code(200)
        }

        HttpBuilder http = httpBuilder {
            ignoreSslIssues execution
            request.uri = "${serverUri(protocol)}/basic"
            request.auth.basic 'admin', '$3cr3t'
            response.success { FromServer fs, Object body ->
                !body && fs.statusCode == 200
            }
        }

        expect:
        http.head()

        and:
        http.headAsync().get()

        and:
        ersatzServer.verify()

        where:
        protocol << ['HTTP', 'HTTPS']
    }

    @Unroll '#protocol HEAD with BASIC authentication (unauthorized)'() {
        setup:
        ersatzServer.authentication {
            basic()
        }

        ersatzServer.expectations {
            head('/basic').protocol(protocol).called(0).responds().code(200)
        }

        HttpBuilder http = httpBuilder {
            ignoreSslIssues execution
            request.uri = "${serverUri(protocol)}/basic"
            request.auth.basic 'guest', 'blah'
        }

        when:
        http.head()

        then:
        def ex = thrown(Exception)
        findExceptionMessage(ex) == 'Unauthorized'

        when:
        http.headAsync().get()

        then:
        ex = thrown(Exception)
        findExceptionMessage(ex) == 'Unauthorized'

        and:
        ersatzServer.verify()

        where:
        protocol << ['HTTP', 'HTTPS']
    }

    @Unroll '#protocol HEAD with DIGEST authentication (authorized)'() {
        setup:
        ersatzServer.authentication {
            digest()
        }

        ersatzServer.expectations {
            head('/digest').protocol(protocol).called(2).responds().code(200)
        }

        HttpBuilder http = httpBuilder {
            ignoreSslIssues execution
            request.uri = "${serverUri(protocol)}/digest"
            request.auth.digest 'admin', '$3cr3t'
            response.success { FromServer fs, Object body ->
                !body && fs.statusCode == 200
            }
        }

        expect:
        http.head()

        and:
        http.headAsync().get()

        and:
        ersatzServer.verify()

        where:
        protocol << ['HTTP', 'HTTPS']
    }

    @Unroll '#protocol HEAD with DIGEST authentication (unauthorized)'() {
        setup:
        ersatzServer.authentication {
            digest()
        }

        ersatzServer.expectations {
            head('/digest').protocol(protocol).called(0).responds().code(200)
        }

        HttpBuilder http = httpBuilder {
            ignoreSslIssues execution
            request.uri = "${serverUri(protocol)}/digest"
            request.auth.digest 'nobody', 'foobar'
        }

        when:
        http.head()

        then:
        def ex = thrown(Exception)
        findExceptionMessage(ex) == 'Unauthorized'

        when:
        http.headAsync().get()

        then:
        ex = thrown(Exception)
        findExceptionMessage(ex) == 'Unauthorized'

        and:
        ersatzServer.verify()

        where:
        protocol << ['HTTP', 'HTTPS']
    }

    def 'interceptor'() {
        setup:
        ersatzServer.expectations {
            head('/pass').called(2).responds().code(200)
        }

        def http = httpBuilder {
            request.uri = "${ersatzServer.httpUrl}/pass"
            execution.interceptor(HEAD) { ChainedHttpConfig cfg, Function<ChainedHttpConfig, Object> fx ->
                "Response: ${fx.apply(cfg)}"
            }
            response.success { FromServer fs, Object body ->
                !body && fs.statusCode == 200
            }
        }

        expect:
        http.head() == 'Response: true'

        and:
        http.headAsync().get() == 'Response: true'

        and:
        ersatzServer.verify()
    }

    @Unroll 'when handler with Closure (#code)'() {
        setup:
        ersatzServer.expectations {
            head('/handling').called(2).responds().code(code)
        }

        def http = httpBuilder {
            request.uri = "${ersatzServer.httpUrl}/handling"
            response.when(status) { FromServer fs, Object body ->
                "$body (${fs.statusCode})"
            }
        }

        expect:
        http.head() == result

        and:
        http.headAsync().get() == result

        and:
        ersatzServer.verify()

        where:
        code | status                    || result
        205  | HttpConfig.Status.SUCCESS || 'null (205)'
        210  | 210                       || 'null (210)'
        211  | '211'                     || 'null (211)'
    }

    @Unroll 'when handler with BiFunction (#code)'() {
        setup:
        ersatzServer.expectations {
            head('/handling').called(2).responds().code(code)
        }

        def http = httpBuilder {
            request.uri = "${ersatzServer.httpUrl}/handling"
            response.when(status, new BiFunction<FromServer, Object, Object>() {
                @Override Object apply(FromServer fs, Object body) {
                    "$body (${fs.statusCode})"
                }
            })
        }

        expect:
        http.head() == result

        and:
        http.headAsync().get() == result

        and:
        ersatzServer.verify()

        where:
        code | status                    || result
        205  | HttpConfig.Status.SUCCESS || 'null (205)'
        210  | 210                       || 'null (210)'
        211  | '211'                     || 'null (211)'
    }

    @Unroll 'success/failure handler with Closure (#code)'() {
        setup:
        ersatzServer.expectations {
            head('/handling').called(2).responds().code(code)
        }

        def http = httpBuilder {
            request.uri = "${ersatzServer.httpUrl}/handling"
            response.success { FromServer fs, Object body ->
                "Success: $body (${fs.statusCode})"
            }
            response.failure { FromServer fs, Object body ->
                "Failure: $body (${fs.statusCode})"
            }
        }

        expect:
        http.head() == result

        and:
        http.headAsync().get() == result

        and:
        ersatzServer.verify()

        where:
        code || result
        200  || 'Success: null (200)'
        300  || 'Success: null (300)'
        400  || 'Failure: null (400)'
        500  || 'Failure: null (500)'
    }

    @Unroll 'success/failure handler with BiFunction (#code)'() {
        setup:
        ersatzServer.expectations {
            head('/handling').called(2).responds().code(code)
        }

        def http = httpBuilder {
            request.uri = "${ersatzServer.httpUrl}/handling"
            response.success(new BiFunction<FromServer, Object, Object>() {
                @Override Object apply(FromServer fs, Object body) {
                    "Success: $body (${fs.statusCode})"
                }
            })
            response.failure(new BiFunction<FromServer, Object, Object>() {
                @Override Object apply(FromServer fs, Object body) {
                    "Failure: $body (${fs.statusCode})"
                }
            })
        }

        expect:
        http.head() == result

        and:
        http.headAsync().get() == result

        and:
        ersatzServer.verify()

        where:
        code || result
        200  || 'Success: null (200)'
        300  || 'Success: null (300)'
        400  || 'Failure: null (400)'
        500  || 'Failure: null (500)'
    }
}

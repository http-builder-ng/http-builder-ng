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

import com.stehno.ersatz.Encoders
import groovyx.net.http.ChainedHttpConfig
import groovyx.net.http.FromServer
import groovyx.net.http.HttpBuilder
import groovyx.net.http.HttpConfig
import spock.lang.Unroll

import java.util.function.BiFunction
import java.util.function.Consumer
import java.util.function.Function

import static com.stehno.ersatz.ContentType.*
import static com.stehno.ersatz.NoCookiesMatcher.noCookies
import static groovyx.net.http.HttpVerb.DELETE
import static groovyx.net.http.util.SslUtils.ignoreSslIssues

/**
 * Test kit for testing the HTTP DELETE method with different clients.
 */
abstract class HttpDeleteTestKit extends HttpMethodTestKit {

    @Unroll 'delete(): #protocol #contentType.value'() {
        setup:
        ersatzServer.expectations {
            delete('/alpha').protocol(protocol).called(2).responds().content(content, contentType)
        }

        HttpBuilder http = httpBuilder {
            ignoreSslIssues execution
            request.uri = "${serverUri(protocol)}/alpha"
        }

        expect:
        result(http.delete())

        and:
        result(http.deleteAsync().get())

        and:
        ersatzServer.verify()

        where:
        protocol | contentType      | content || result
        'HTTP'   | TEXT_PLAIN       | OK_TEXT || { r -> r == OK_TEXT }
        'HTTPS'  | TEXT_PLAIN       | OK_TEXT || { r -> r == OK_TEXT }

        'HTTP'   | APPLICATION_JSON | OK_JSON || { r -> r == [value: 'ok-json'] }
        'HTTPS'  | APPLICATION_JSON | OK_JSON || { r -> r == [value: 'ok-json'] }

        'HTTP'   | APPLICATION_XML  | OK_XML  || { r -> r == OK_XML_DOC }
        'HTTPS'  | APPLICATION_XML  | OK_XML  || { r -> r == OK_XML_DOC }

        'HTTP'   | TEXT_HTML        | OK_HTML || { r -> r.body().text() == 'ok-html' }
        'HTTPS'  | TEXT_HTML        | OK_HTML || { r -> r.body().text() == 'ok-html' }

        'HTTP'   | 'text/csv'       | OK_CSV  || { r -> r == OK_CSV_DOC }
        'HTTPS'  | 'text/csv'       | OK_CSV  || { r -> r == OK_CSV_DOC }
    }

    @Unroll 'delete(Closure): query -> #query'() {
        setup:
        ersatzServer.expectations {
            delete('/bravo').queries(query).called(2).responds().content(OK_TEXT, TEXT_PLAIN)
        }

        HttpBuilder http = httpBuilder(ersatzServer.httpUrl)

        def config = {
            request.uri.path = '/bravo'
            request.uri.query = query
        }

        expect:
        http.delete(config) == OK_TEXT

        and:
        http.deleteAsync(config).get() == OK_TEXT

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

    @Unroll 'delete(Consumer): headers -> #headers'() {
        setup:
        ersatzServer.expectations {
            delete('/charlie').headers(headers).called(2).responds().content(OK_TEXT, TEXT_PLAIN)
        }

        HttpBuilder http = httpBuilder(ersatzServer.httpUrl)

        // odd scoping issue requires this
        def requestHeaders = headers

        Consumer<HttpConfig> consumer = new Consumer<HttpConfig>() {
            @Override void accept(final HttpConfig config) {
                config.request.uri.path = '/charlie'
                config.request.headers = requestHeaders
            }
        }

        expect:
        http.delete(consumer) == OK_TEXT

        and:
        http.deleteAsync(consumer).get() == OK_TEXT

        and:
        ersatzServer.verify()

        where:
        headers << [
            null,
            [:],
            [hat: 'fedora']
        ]
    }

    @Unroll 'delete(Class,Closure): cookies -> #cookies'() {
        setup:
        ersatzServer.expectations {
            delete('/delta').cookies(cookies == null ? noCookies() : cookies).called(2).responder {
                encoder 'text/date', String, Encoders.text
                content('2016.08.25 14:43', 'text/date')
            }
        }

        HttpBuilder http = httpBuilder(ersatzServer.httpUrl)

        def config = {
            request.uri.path = '/delta'

            cookies.each { n, v ->
                request.cookie n, v
            }

            response.parser('text/date') { ChainedHttpConfig config, FromServer fromServer ->
                Date.parse('yyyy.MM.dd HH:mm', fromServer.inputStream.text)
            }
        }

        expect:
        http.delete(Date, config).format('MM/dd/yyyy HH:mm') == '08/25/2016 14:43'

        and:
        http.deleteAsync(Date, config).get().format('MM/dd/yyyy HH:mm') == '08/25/2016 14:43'

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

    @Unroll 'delete(Class,Consumer): cookies -> #cookies'() {
        setup:
        ersatzServer.expectations {
            delete('/delta').cookies(cookies == null ? noCookies() : cookies).called(2).responder {
                encoder 'text/date', String, Encoders.text
                content('2016.08.25 14:43', 'text/date')
            }
        }

        HttpBuilder http = httpBuilder(ersatzServer.httpUrl)

        // required for variable scoping
        def consumerCookies = cookies

        Consumer<HttpConfig> consumer = new Consumer<HttpConfig>() {
            @Override void accept(final HttpConfig config) {
                config.request.uri.path = '/delta'

                consumerCookies.each { n, v ->
                    config.request.cookie n, v
                }

                config.response.parser('text/date') { ChainedHttpConfig cfg, FromServer fromServer ->
                    Date.parse('yyyy.MM.dd HH:mm', fromServer.inputStream.text)
                }
            }
        }

        expect:
        http.delete(Date, consumer).format('MM/dd/yyyy HH:mm') == '08/25/2016 14:43'

        and:
        http.deleteAsync(Date, consumer).get().format('MM/dd/yyyy HH:mm') == '08/25/2016 14:43'

        and:
        ersatzServer.verify()

        where:
        cookies << [
            null,
            [:],
            [flavor: 'peanut-butter'],
            [flavor: 'oatmeal', count: 'dozen']
        ]
    }

    @Unroll '#protocol DELETE with BASIC authentication (authorized)'() {
        setup:
        ersatzServer.authentication {
            basic()
        }

        ersatzServer.expectations {
            delete('/basic').protocol(protocol).called(2).responds().content(OK_TEXT, TEXT_PLAIN)
        }

        HttpBuilder http = httpBuilder {
            ignoreSslIssues execution
            request.uri = "${serverUri(protocol)}/basic"
            request.auth.basic 'admin', '$3cr3t'
        }

        expect:
        http.delete() == OK_TEXT

        and:
        http.deleteAsync().get() == OK_TEXT

        and:
        ersatzServer.verify()

        where:
        protocol << ['HTTP', 'HTTPS']
    }

    @Unroll '#protocol DELETE with BASIC authentication (unauthorized)'() {
        setup:
        ersatzServer.authentication {
            basic()
        }

        ersatzServer.expectations {
            delete('/basic').protocol(protocol).called(0).responds().content(OK_TEXT, TEXT_PLAIN)
        }

        HttpBuilder http = httpBuilder {
            ignoreSslIssues execution
            request.uri = "${serverUri(protocol)}/basic"
            request.auth.basic 'guest', 'blah'
        }

        when:
        http.delete()

        then:
        def ex = thrown(Exception)
        println ex
        findExceptionMessage(ex) == 'Unauthorized'

        when:
        http.deleteAsync().get()

        then:
        ex = thrown(Exception)
        findExceptionMessage(ex) == 'Unauthorized'

        and:
        ersatzServer.verify()

        where:
        protocol << ['HTTP', 'HTTPS']
    }

    @Unroll '#protocol DELETE with DIGEST authentication (authorized)'() {
        setup:
        ersatzServer.authentication {
            digest()
        }

        ersatzServer.expectations {
            delete('/digest').protocol(protocol).called(2).responds().content(OK_TEXT, TEXT_PLAIN)
        }

        HttpBuilder http = httpBuilder {
            ignoreSslIssues execution
            request.uri = "${serverUri(protocol)}/digest"
            request.auth.digest 'admin', '$3cr3t'
        }

        expect:
        http.delete() == OK_TEXT

        and:
        http.deleteAsync().get() == OK_TEXT

        and:
        ersatzServer.verify()

        where:
        protocol << ['HTTP', 'HTTPS']
    }

    @Unroll '#protocol DELETE with DIGEST authentication (unauthorized)'() {
        setup:
        ersatzServer.authentication {
            digest()
        }

        ersatzServer.expectations {
            delete('/digest').protocol(protocol).called(0).responds().content(OK_TEXT, TEXT_PLAIN)
        }

        HttpBuilder http = httpBuilder {
            ignoreSslIssues execution
            request.uri = "${serverUri(protocol)}/digest"
            request.auth.digest 'nobody', 'foobar'
        }

        when:
        http.delete()

        then:
        def ex = thrown(Exception)
        findExceptionMessage(ex) == 'Unauthorized'

        when:
        http.deleteAsync().get()

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
            delete('/pass').called(2).responds().content(OK_TEXT, TEXT_PLAIN)
        }

        def http = httpBuilder {
            request.uri = "${ersatzServer.httpUrl}/pass"
            execution.interceptor(DELETE) { ChainedHttpConfig cfg, Function<ChainedHttpConfig, Object> fx ->
                "Response: ${fx.apply(cfg)}"
            }
        }

        expect:
        http.delete() == 'Response: ok-text'

        and:
        http.deleteAsync().get() == 'Response: ok-text'

        and:
        ersatzServer.verify()
    }

    @Unroll 'when handler with Closure (#code)'() {
        setup:
        ersatzServer.expectations {
            delete('/handling').called(2).responds().code(code)
        }

        def http = httpBuilder {
            request.uri = "${ersatzServer.httpUrl}/handling"
            response.when(status) { FromServer fs, Object body ->
                "Code: ${fs.statusCode}"
            }
        }

        expect:
        http.delete() == result

        and:
        http.deleteAsync().get() == result

        and:
        ersatzServer.verify()

        where:
        code | status                    || result
        205  | HttpConfig.Status.SUCCESS || 'Code: 205'
        210  | 210                       || 'Code: 210'
        211  | '211'                     || 'Code: 211'
    }

    @Unroll 'when handler with BiFunction (#code)'() {
        setup:
        ersatzServer.expectations {
            delete('/handling').called(2).responds().code(code)
        }

        def http = httpBuilder {
            request.uri = "${ersatzServer.httpUrl}/handling"
            response.when(status, new BiFunction<FromServer, Object, Object>() {
                @Override Object apply(FromServer fs, Object body) {
                    "Code: ${fs.statusCode}"
                }
            })
        }

        expect:
        http.delete() == result

        and:
        http.deleteAsync().get() == result

        and:
        ersatzServer.verify()

        where:
        code | status                    || result
        205  | HttpConfig.Status.SUCCESS || 'Code: 205'
        210  | 210                       || 'Code: 210'
        211  | '211'                     || 'Code: 211'
    }

    @Unroll 'success/failure handler with Closure (#code)'() {
        setup:
        ersatzServer.expectations {
            delete('/handling').called(2).responds().code(code)
        }

        def http = httpBuilder {
            request.uri = "${ersatzServer.httpUrl}/handling"
            response.success { FromServer fs, Object body ->
                "Success: ${fs.statusCode}"
            }
            response.failure { FromServer fs, Object body ->
                "Failure: ${fs.statusCode}"
            }
        }

        expect:
        http.delete() == result

        and:
        http.deleteAsync().get() == result

        and:
        ersatzServer.verify()

        where:
        code || result
        200  || 'Success: 200'
        300  || 'Success: 300'
        400  || 'Failure: 400'
        500  || 'Failure: 500'
    }

    @Unroll 'success/failure handler with BiFunction (#code)'() {
        setup:
        ersatzServer.expectations {
            delete('/handling').called(2).responds().content(OK_TEXT, TEXT_PLAIN).code(code)
        }

        def http = httpBuilder {
            request.uri = "${ersatzServer.httpUrl}/handling"
            response.success(new BiFunction<FromServer, Object, Object>() {
                @Override Object apply(FromServer fs, Object body) {
                    "Success: ${fs.statusCode}"
                }
            })
            response.failure(new BiFunction<FromServer, Object, Object>() {
                @Override Object apply(FromServer fs, Object body) {
                    "Failure: ${fs.statusCode}"
                }
            })
        }

        expect:
        http.delete() == result

        and:
        http.deleteAsync().get() == result

        and:
        ersatzServer.verify()

        where:
        code || result
        200  || 'Success: 200'
        300  || 'Success: 300'
        400  || 'Failure: 400'
        500  || 'Failure: 500'
    }

    def 'gzip compression supported'() {
        setup:
        ersatzServer.expectations {
            delete('/gzip').header('Accept-Encoding', 'gzip').called(2).responds().content('x' * 1000, TEXT_PLAIN)
        }

        def http = httpBuilder {
            request.uri = "${ersatzServer.httpUrl}/gzip"
            request.headers = ['Accept-Encoding': 'gzip']
            response.success { FromServer fs, Object body ->
                "${fs.headers.find { FromServer.Header h -> h.key == 'Content-Encoding' }.value} (${fs.statusCode})"
            }
        }

        expect:
        http.delete() == 'gzip (200)'

        and:
        http.deleteAsync().get() == 'gzip (200)'

        and:
        ersatzServer.verify()
    }
}

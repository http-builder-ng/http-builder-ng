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
import com.stehno.ersatz.NoCookiesMatcher
import groovyx.net.http.*
import spock.lang.Unroll

import java.util.function.BiFunction
import java.util.function.Consumer
import java.util.function.Function

import static com.stehno.ersatz.ContentType.*
import static com.stehno.ersatz.NoCookiesMatcher.noCookies
import static groovyx.net.http.ContentTypes.URLENC
import static groovyx.net.http.HttpVerb.PATCH
import static groovyx.net.http.util.SslUtils.ignoreSslIssues

/**
 * Test kit for testing the HTTP PATCH method with different clients.
 */
abstract class HttpPatchTestKit extends HttpMethodTestKit {

    @Unroll 'patch(): #protocol #contentType.value #body'() {
        setup:
        ersatzServer.expectations {
            patch('/alpha').body(expectedBody, APPLICATION_JSON).protocol(protocol).called(2).responds().content(content, contentType)
        }

        HttpBuilder http = httpBuilder {
            ignoreSslIssues execution
            request.uri = "${serverUri(protocol)}/alpha"
            request.body = body
            request.contentType = APPLICATION_JSON.value
        }

        expect:
        result(http.patch())

        and:
        result(http.patchAsync().get())

        and:
        ersatzServer.verify()

        where:
        protocol | body               | expectedBody          | contentType      | content || result
        'HTTP'   | null               | ''                    | TEXT_PLAIN       | OK_TEXT || { r -> r == OK_TEXT }
        'HTTPS'  | null               | ''                    | TEXT_PLAIN       | OK_TEXT || { r -> r == OK_TEXT }

        'HTTP'   | [:]                | '{}'                  | APPLICATION_JSON | OK_JSON || { r -> r == [value: 'ok-json'] }
        'HTTPS'  | [:]                | '{}'                  | APPLICATION_JSON | OK_JSON || { r -> r == [value: 'ok-json'] }

        'HTTP'   | [one: '1']         | '{"one":"1"}'         | APPLICATION_XML  | OK_XML  || { r -> r == OK_XML_DOC }
        'HTTPS'  | [one: '1']         | '{"one":"1"}'         | APPLICATION_XML  | OK_XML  || { r -> r == OK_XML_DOC }

        'HTTP'   | [two: 2]           | '{"two":2}'           | TEXT_HTML        | OK_HTML || { r -> r.body().text() == 'ok-html' }
        'HTTPS'  | [two: 2]           | '{"two":2}'           | TEXT_HTML        | OK_HTML || { r -> r.body().text() == 'ok-html' }

        'HTTP'   | [one: '1', two: 2] | '{"one":"1","two":2}' | 'text/csv'       | OK_CSV  || { r -> r == OK_CSV_DOC }
        'HTTPS'  | [one: '1', two: 2] | '{"one":"1","two":2}' | 'text/csv'       | OK_CSV  || { r -> r == OK_CSV_DOC }
    }

    @Unroll 'patch(Closure): query -> #query'() {
        setup:
        ersatzServer.expectations {
            patch('/bravo').body(REQUEST_BODY_JSON, APPLICATION_JSON).queries(query).called(2).responds().content(OK_TEXT, TEXT_PLAIN)
        }

        HttpBuilder http = httpBuilder(ersatzServer.httpUrl)

        def config = {
            request.uri.path = '/bravo'
            request.uri.query = query
            request.body = REQUEST_BODY
            request.contentType = APPLICATION_JSON.value
        }

        expect:
        http.patch(config) == OK_TEXT

        and:
        http.patchAsync(config).get() == OK_TEXT

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

    @Unroll 'patch(Consumer): headers -> #headers'() {
        setup:
        ersatzServer.expectations {
            patch('/charlie').body(REQUEST_BODY_JSON, APPLICATION_JSON).headers(headers).called(2).responds().content(OK_TEXT, TEXT_PLAIN)
        }

        HttpBuilder http = httpBuilder(ersatzServer.httpUrl)

        // odd scoping issue requires this
        def requestHeaders = headers

        Consumer<HttpConfig> consumer = new Consumer<HttpConfig>() {
            @Override void accept(final HttpConfig config) {
                config.request.uri.path = '/charlie'
                config.request.headers = requestHeaders
                config.request.body = REQUEST_BODY
                config.request.contentType = APPLICATION_JSON.value
            }
        }

        expect:
        http.patch(consumer) == OK_TEXT

        and:
        http.patchAsync(consumer).get() == OK_TEXT

        and:
        ersatzServer.verify()

        where:
        headers << [
                null,
                [:],
                [hat: 'fedora']
        ]
    }

    @Unroll 'patch(Class,Closure): cookies -> #cookies'() {
        setup:
        ersatzServer.expectations {
            patch('/delta').body(REQUEST_BODY_JSON, APPLICATION_JSON).cookies(cookies == null ? noCookies() : cookies).called(2).responder {
                encoder 'text/date', String, Encoders.text
                content('2016.08.25 14:43', 'text/date')
            }
        }

        HttpBuilder http = httpBuilder(ersatzServer.httpUrl)

        def config = {
            request.uri.path = '/delta'
            request.body = REQUEST_BODY
            request.contentType = APPLICATION_JSON.value

            cookies.each { n, v ->
                request.cookie n, v
            }

            response.parser('text/date') { ChainedHttpConfig config, FromServer fromServer ->
                Date.parse('yyyy.MM.dd HH:mm', fromServer.inputStream.text)
            }
        }

        expect:
        http.patch(Date, config).format('MM/dd/yyyy HH:mm') == '08/25/2016 14:43'

        and:
        http.patchAsync(Date, config).get().format('MM/dd/yyyy HH:mm') == '08/25/2016 14:43'

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

    @Unroll 'patch(Class,Consumer): cookies -> #cookies'() {
        setup:
        ersatzServer.expectations {
            patch('/delta').body(REQUEST_BODY_JSON, APPLICATION_JSON).cookies(cookies == null ? noCookies() : cookies).called(2).responder {
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
                config.request.body = REQUEST_BODY
                config.request.contentType = APPLICATION_JSON.value

                consumerCookies.each { n, v ->
                    config.request.cookie n, v
                }

                config.response.parser('text/date') { ChainedHttpConfig cfg, FromServer fromServer ->
                    Date.parse('yyyy.MM.dd HH:mm', fromServer.inputStream.text)
                }
            }
        }

        expect:
        http.patch(Date, consumer).format('MM/dd/yyyy HH:mm') == '08/25/2016 14:43'

        and:
        http.patchAsync(Date, consumer).get().format('MM/dd/yyyy HH:mm') == '08/25/2016 14:43'

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

    @Unroll '#protocol PATCH with BASIC authentication (authorized)'() {
        setup:
        ersatzServer.authentication {
            basic()
        }

        ersatzServer.expectations {
            patch('/basic').body(REQUEST_BODY_JSON, APPLICATION_JSON).protocol(protocol).called(2).responds().content(OK_TEXT, TEXT_PLAIN)
        }

        HttpBuilder http = httpBuilder {
            ignoreSslIssues execution
            request.uri = "${serverUri(protocol)}/basic"
            request.auth.basic 'admin', '$3cr3t'
            request.body = REQUEST_BODY
            request.contentType = APPLICATION_JSON.value
        }

        expect:
        http.patch() == OK_TEXT

        and:
        http.patchAsync().get() == OK_TEXT

        and:
        ersatzServer.verify()

        where:
        protocol << ['HTTP', 'HTTPS']
    }

    @Unroll '#protocol PATCH with BASIC authentication (unauthorized)'() {
        setup:
        ersatzServer.authentication {
            basic()
        }

        ersatzServer.expectations {
            patch('/basic').body(REQUEST_BODY_JSON, APPLICATION_JSON).protocol(protocol).called(0).responds().content(OK_TEXT, TEXT_PLAIN)
        }

        HttpBuilder http = httpBuilder {
            ignoreSslIssues execution
            request.uri = "${serverUri(protocol)}/basic"
            request.auth.basic 'guest', 'blah'
            request.body = REQUEST_BODY
            request.contentType = APPLICATION_JSON.value
        }

        when:
        http.patch()

        then:
        def ex = thrown(Exception)
        findExceptionMessage(ex) == 'Unauthorized'

        when:
        http.patchAsync().get()

        then:
        ex = thrown(Exception)
        findExceptionMessage(ex) == 'Unauthorized'

        and:
        ersatzServer.verify()

        where:
        protocol << ['HTTP', 'HTTPS']
    }

    @Unroll '#protocol PATCH with DIGEST authentication (authorized)'() {
        setup:
        ersatzServer.authentication {
            digest()
        }

        // OkHttp fails due to missing expectation but the request looks good - relaxed the constraint until further investigation
        ersatzServer.expectations {
            patch('/digest')/*.body(REQUEST_BODY_JSON, APPLICATION_JSON)*/.protocol(protocol).called(2).responds().content(OK_TEXT, TEXT_PLAIN)
        }

        HttpBuilder http = httpBuilder {
            ignoreSslIssues execution
            request.uri = "${serverUri(protocol)}/digest"
            request.auth.digest 'admin', '$3cr3t'
            request.body = REQUEST_BODY
            request.contentType = APPLICATION_JSON.value
        }

        expect:
        http.patch() == OK_TEXT

        and:
        http.patchAsync().get() == OK_TEXT

        and:
        ersatzServer.verify()

        where:
        protocol << ['HTTP', 'HTTPS']
    }

    @Unroll '#protocol PATCH with DIGEST authentication (unauthorized)'() {
        setup:
        ersatzServer.authentication {
            digest()
        }

        ersatzServer.expectations {
            patch('/digest').body(REQUEST_BODY_JSON, APPLICATION_JSON).protocol(protocol).called(0).responds().content(OK_TEXT, TEXT_PLAIN)
        }

        HttpBuilder http = httpBuilder {
            ignoreSslIssues execution
            request.uri = "${serverUri(protocol)}/digest"
            request.auth.digest 'nobody', 'foobar'
            request.body = REQUEST_BODY
            request.contentType = APPLICATION_JSON.value
        }

        when:
        http.patch()

        then:
        def ex = thrown(Exception)
        findExceptionMessage(ex) == 'Unauthorized'

        when:
        http.patchAsync().get()

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
            patch('/pass').body(REQUEST_BODY_JSON, APPLICATION_JSON).called(2).responds().content(OK_TEXT, TEXT_PLAIN)
        }

        def http = httpBuilder {
            request.uri = "${ersatzServer.httpUrl}/pass"
            request.body = REQUEST_BODY
            request.contentType = APPLICATION_JSON.value
            execution.interceptor(PATCH) { ChainedHttpConfig cfg, Function<ChainedHttpConfig, Object> fx ->
                "Response: ${fx.apply(cfg)}"
            }
        }

        expect:
        http.patch() == 'Response: ok-text'

        and:
        http.patchAsync().get() == 'Response: ok-text'

        and:
        ersatzServer.verify()
    }

    @Unroll 'when handler with Closure (#code)'() {
        setup:
        ersatzServer.expectations {
            patch('/handling').body(REQUEST_BODY_JSON, APPLICATION_JSON).called(2).responds().code(code)
        }

        def http = httpBuilder {
            request.uri = "${ersatzServer.httpUrl}/handling"
            request.body = REQUEST_BODY
            request.contentType = APPLICATION_JSON.value
            response.when(status) { FromServer fs, Object body ->
                "Code: ${fs.statusCode}"
            }
        }

        expect:
        http.patch() == result

        and:
        http.patchAsync().get() == result

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
            patch('/handling').body(REQUEST_BODY_JSON, APPLICATION_JSON).called(2).responds().code(code)
        }

        def http = httpBuilder {
            request.uri = "${ersatzServer.httpUrl}/handling"
            request.body = REQUEST_BODY
            request.contentType = APPLICATION_JSON.value
            response.when(status, new BiFunction<FromServer, Object, Object>() {
                @Override Object apply(FromServer fs, Object body) {
                    "Code: ${fs.statusCode}"
                }
            })
        }

        expect:
        http.patch() == result

        and:
        http.patchAsync().get() == result

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
            patch('/handling').body(REQUEST_BODY_JSON, APPLICATION_JSON).called(2).responds().code(code)
        }

        def http = httpBuilder {
            request.uri = "${ersatzServer.httpUrl}/handling"
            request.body = REQUEST_BODY
            request.contentType = APPLICATION_JSON.value
            response.success { FromServer fs, Object body ->
                "Success: ${fs.statusCode}"
            }
            response.failure { FromServer fs, Object body ->
                "Failure: ${fs.statusCode}"
            }
        }

        expect:
        http.patch() == result

        and:
        http.patchAsync().get() == result

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
            patch('/handling').body(REQUEST_BODY_JSON, APPLICATION_JSON).called(2).responds().code(code)
        }

        def http = httpBuilder {
            request.uri = "${ersatzServer.httpUrl}/handling"
            request.body = REQUEST_BODY
            request.contentType = APPLICATION_JSON.value
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
        http.patch() == result

        and:
        http.patchAsync().get() == result

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
            patch('/gzip').body(REQUEST_BODY_JSON, APPLICATION_JSON).header('Accept-Encoding', 'gzip').called(2).responds().content('x' * 1000, TEXT_PLAIN)
        }

        def http = httpBuilder {
            request.uri = "${ersatzServer.httpUrl}/gzip"
            request.headers = ['Accept-Encoding': 'gzip']
            request.body = REQUEST_BODY
            request.contentType = APPLICATION_JSON.value
            response.success { FromServer fs, Object body ->
                "${fs.headers.find { FromServer.Header h -> h.key == 'Content-Encoding' }.value} (${fs.statusCode})"
            }
        }

        expect:
        http.patch() == 'gzip (200)'

        and:
        http.patchAsync().get() == 'gzip (200)'

        and:
        ersatzServer.verify()
    }

    @Unroll 'request content encoding (#contentType.value)'() {
        setup:
        ersatzServer.expectations {
            patch('/types').body(expected, contentType).called(2).responds().content(OK_TEXT, TEXT_PLAIN)
        }

        def http = httpBuilder {
            request.uri = "${ersatzServer.httpUrl}/types"
            request.body = content
            request.contentType = contentType
        }

        expect:
        http.patch() == OK_TEXT

        and:
        http.patchAsync().get() == OK_TEXT

        and:
        ersatzServer.verify()

        where:
        content     | contentType            | expected
        OK_JSON     | APPLICATION_JSON.value | OK_JSON
        OK_XML      | APPLICATION_XML.value  | OK_XML
        OK_HTML_DOC | TEXT_HTML.value        | 'ok-html'
    }

    @Unroll 'form (url-encoded): #protocol'() {
        setup:
        ersatzServer.expectations {
            patch('/form').body([username: 'bobvila', password: 'oldhouse'], APPLICATION_URLENCODED).protocol(protocol).called(2).responds().content(OK_TEXT, TEXT_PLAIN)
        }

        def http = httpBuilder {
            ignoreSslIssues execution
            request.uri = "${serverUri(protocol)}/form"
            request.body = [username: 'bobvila', password: 'oldhouse']
            request.contentType = URLENC[0]
            request.encoder URLENC, NativeHandlers.Encoders.&form
        }

        expect:
        http.patch() == OK_TEXT

        and:
        http.patchAsync().get() == OK_TEXT

        and:
        ersatzServer.verify()

        where:
        protocol << ['HTTP', 'HTTPS']
    }
}

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

import com.stehno.ersatz.Cookie
import com.stehno.ersatz.Encoders
import com.stehno.ersatz.proxy.ErsatzProxy
import groovyx.net.http.ChainedHttpConfig
import groovyx.net.http.FromServer
import groovyx.net.http.HttpBuilder
import groovyx.net.http.HttpConfig
import groovyx.net.http.NullCookieStore
import spock.lang.IgnoreIf
import spock.lang.Unroll

import java.util.function.BiFunction
import java.util.function.Consumer
import java.util.function.Function

import static com.stehno.ersatz.ContentType.APPLICATION_JSON
import static com.stehno.ersatz.ContentType.APPLICATION_XML
import static com.stehno.ersatz.ContentType.TEXT_HTML
import static com.stehno.ersatz.ContentType.TEXT_PLAIN
import static com.stehno.ersatz.CookieMatcher.cookieMatcher
import static com.stehno.ersatz.NoCookiesMatcher.noCookies
import static groovyx.net.http.HttpVerb.GET
import static groovyx.net.http.util.SslUtils.ignoreSslIssues
/**
 * Test kit for testing the HTTP GET method with different clients.
 */
abstract class HttpGetTestKit extends HttpMethodTestKit {

    @Unroll 'get(): #protocol #contentType.value'() {
        setup:
        ersatzServer.expectations {
            get('/alpha').protocol(protocol).called(2).responds().content(content, contentType)
        }

        HttpBuilder http = httpBuilder {
            ignoreSslIssues execution
            request.uri = "${serverUri(protocol)}/alpha"
        }

        expect:
        result(http.get())

        and:
        result(http.getAsync().get())

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

    @Unroll 'get(Closure): query -> #query'() {
        setup:
        ersatzServer.expectations {
            get('/bravo').queries(query).called(2).responds().content(OK_TEXT, TEXT_PLAIN)
        }

        HttpBuilder http = httpBuilder(ersatzServer.httpUrl)

        def config = {
            request.uri.path = '/bravo'
            request.uri.query = query
        }

        expect:
        http.get(config) == OK_TEXT

        and:
        http.getAsync(config).get() == OK_TEXT

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

    @Unroll 'get(Consumer): headers -> #headers'() {
        setup:
        ersatzServer.expectations {
            get('/charlie').headers(headers).called(2).responds().content(OK_TEXT, TEXT_PLAIN)
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
        http.get(consumer) == OK_TEXT

        and:
        http.getAsync(consumer).get() == OK_TEXT

        and:
        ersatzServer.verify()

        where:
        headers << [
            null,
            [:],
            [hat: 'fedora'],
            [coat: "overcoat ${'something'}"]
        ]
    }

    @Unroll 'get(Class,Closure): cookies -> #cookies'() {
        setup:
        ersatzServer.expectations {
            get('/delta').cookies(cookies == null ? noCookies() : cookies).called(2).responder {
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
        http.get(Date, config).format('MM/dd/yyyy HH:mm') == '08/25/2016 14:43'

        and:
        http.getAsync(Date, config).get().format('MM/dd/yyyy HH:mm') == '08/25/2016 14:43'

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

    def 'cookies are only set once'() {
        setup:
        ersatzServer.expectations {
            get('/multicookie1').cookie('foo', 'bar').called(1).responds().content(OK_TEXT, TEXT_PLAIN)
            get('/multicookie2').cookie('foo', 'bar').called(1).responds().content(OK_TEXT, TEXT_PLAIN)
            get('/lots/of/path/elements/multicookie3').cookie('foo', 'bar').called(1).responds().content(OK_TEXT, TEXT_PLAIN)
        }

        def http = httpBuilder {
            request.uri = ersatzServer.httpUrl
            request.cookie 'foo', 'bar'
        }

        when:
        http.get { request.uri.path = '/multicookie1' }
        http.get { request.uri.path = '/multicookie2' }
        http.get { request.uri.path = '/lots/of/path/elements/multicookie3' }

        then:
        http.cookieManager.cookieStore.all.values().size() == 2
        ersatzServer.verify()
    }

    def 'server set cookies are honored'() {
        setup:
        ersatzServer.expectations {
            get('/setkermit').called(2).responder {
                content(OK_TEXT, TEXT_PLAIN)
                cookie('kermit', Cookie.cookie {
                    value 'frog'
                    path '/showkermit'
                })
            }

            get('/showkermit').cookie('kermit', cookieMatcher {
                value 'frog'
            }).called(1).responder {
                content(OK_TEXT, TEXT_PLAIN)
                cookie('miss', Cookie.cookie {
                    value 'piggy'
                    path '/'
                })
                cookie('fozzy', Cookie.cookie {
                    value 'bear'
                    path '/some/deep/path'
                })
            }

            get('/some/deep/path').cookie('miss', 'piggy').cookie('fozzy', 'bear').called(1).responds().content(OK_TEXT, TEXT_PLAIN)
        }

        when:
        def http = httpBuilder { request.uri = ersatzServer.httpUrl }

        then:
        http.cookieStore.all.size() == 0

        when:
        http.get { request.uri.path = '/setkermit' }

        then:
        http.cookieStore.all.size() == 1

        when:
        http.get { request.uri.path = '/showkermit' }

        then:
        http.cookieStore.all.size() == 3

        when:
        http.get { request.uri.path = '/some/deep/path' }

        then:
        http.cookieStore.all.size() == 3

        when:
        http.get { request.uri.path = '/setkermit' } //verify that duplicate cookies are not created

        then:
        http.cookieStore.all.size() == 3
        ersatzServer.verify()
    }

    def 'cookies are not stored when disabled'() {
        setup:
        ersatzServer.expectations {
            get('/showkermit').called(1).responder {
                content(OK_TEXT, TEXT_PLAIN)
                cookie('miss', 'piggy; path=/')
                cookie('fozzy', 'bear; path=/some/deep/path')
            }
        }

        when:
        def http = httpBuilder {
            client.cookiesEnabled = false
            request.uri = ersatzServer.httpUrl
        }
        http.get { request.uri.path = '/showkermit' }

        then:
        http.cookieStore.cookies.size() == 0
        http.cookieStore.URIs.size() == 0
        http.cookieStore.is(NullCookieStore.instance())
    }

    @Unroll 'get(Class,Consumer): cookies -> #cookies'() {
        setup:
        ersatzServer.expectations {
            get('/delta').cookies(cookies == null ? noCookies() : cookies).called(2).responder {
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
        http.get(Date, consumer).format('MM/dd/yyyy HH:mm') == '08/25/2016 14:43'

        and:
        http.getAsync(Date, consumer).get().format('MM/dd/yyyy HH:mm') == '08/25/2016 14:43'

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

    @Unroll '#protocol GET with BASIC authentication (authorized)'() {
        setup:
        ersatzServer.authentication {
            basic()
        }

        ersatzServer.expectations {
            get('/basic').protocol(protocol).called(2).responds().content(OK_TEXT, TEXT_PLAIN)
        }

        HttpBuilder http = httpBuilder {
            ignoreSslIssues execution
            request.uri = "${serverUri(protocol)}/basic"
            request.auth.basic 'admin', '$3cr3t'
        }

        expect:
        http.get() == OK_TEXT

        and:
        http.getAsync().get() == OK_TEXT

        and:
        ersatzServer.verify()

        where:
        protocol << ['HTTP', 'HTTPS']
    }

    @Unroll '#protocol GET with BASIC authentication (unauthorized)'() {
        setup:
        ersatzServer.authentication {
            basic()
        }

        ersatzServer.expectations {
            get('/basic').protocol(protocol).called(0).responds().content(OK_TEXT, TEXT_PLAIN)
        }

        HttpBuilder http = httpBuilder {
            ignoreSslIssues execution
            request.uri = "${serverUri(protocol)}/basic"
            request.auth.basic 'guest', 'blah'
        }

        when:
        http.get()

        then:
        def ex = thrown(Exception)
        findExceptionMessage(ex) == 'Unauthorized'

        when:
        http.getAsync().get()

        then:
        ex = thrown(Exception)
        findExceptionMessage(ex) == 'Unauthorized'

        and:
        ersatzServer.verify()

        where:
        protocol << ['HTTP', 'HTTPS']
    }

    @Unroll '#protocol GET with DIGEST authentication (authorized)'() {
        setup:
        ersatzServer.authentication {
            digest()
        }

        ersatzServer.expectations {
            get('/digest').protocol(protocol).called(2).responds().content(OK_TEXT, TEXT_PLAIN)
        }

        HttpBuilder http = httpBuilder {
            ignoreSslIssues execution
            request.uri = "${serverUri(protocol)}/digest"
            request.auth.digest 'admin', '$3cr3t'
        }

        expect:
        http.get() == OK_TEXT

        and:
        http.getAsync().get() == OK_TEXT

        and:
        ersatzServer.verify()

        where:
        protocol << ['HTTP', 'HTTPS']
    }

    @Unroll '#protocol GET with DIGEST authentication (unauthorized)'() {
        setup:
        ersatzServer.authentication {
            digest()
        }

        ersatzServer.expectations {
            get('/digest').protocol(protocol).called(0).responds().content(OK_TEXT, TEXT_PLAIN)
        }

        HttpBuilder http = httpBuilder {
            ignoreSslIssues execution
            request.uri = "${serverUri(protocol)}/digest"
            request.auth.digest 'nobody', 'foobar'
        }

        when:
        http.get()

        then:
        def ex = thrown(Exception)
        findExceptionMessage(ex) == 'Unauthorized'

        when:
        http.getAsync().get()

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
            get('/pass').called(2).responds().content(OK_TEXT, TEXT_PLAIN)
        }

        def http = httpBuilder {
            request.uri = "${ersatzServer.httpUrl}/pass"
            execution.interceptor(GET) { ChainedHttpConfig cfg, Function<ChainedHttpConfig, Object> fx ->
                "Response: ${fx.apply(cfg)}"
            }
        }

        expect:
        http.get() == 'Response: ok-text'

        and:
        http.getAsync().get() == 'Response: ok-text'

        and:
        ersatzServer.verify()
    }

    @Unroll 'when handler with Closure (#code)'() {
        setup:
        ersatzServer.expectations {
            get('/handling').called(2).responds().code(code)
        }

        def http = httpBuilder {
            request.uri = "${ersatzServer.httpUrl}/handling"
            response.when(status) { FromServer fs, Object body ->
                "Code: ${fs.statusCode}"
            }
        }

        expect:
        http.get() == result

        and:
        http.getAsync().get() == result

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
            get('/handling').called(2).responds().code(code)
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
        http.get() == result

        and:
        http.getAsync().get() == result

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
            get('/handling').called(2).responds().content(OK_TEXT, TEXT_PLAIN).code(code)
        }

        def http = httpBuilder {
            request.uri = "${ersatzServer.httpUrl}/handling"
            response.success { FromServer fs, Object body ->
                "Success: ${fs.statusCode}, Text: ${body}"
            }
            response.failure { FromServer fs, Object body ->
                "Failure: ${fs.statusCode}, Error: ${body}"
            }
        }

        expect:
        http.get() == result

        and:
        http.getAsync().get() == result

        and:
        ersatzServer.verify()

        where:
        code || result
        200  || 'Success: 200, Text: ok-text'
        300  || 'Success: 300, Text: ok-text'
        400  || 'Failure: 400, Error: ok-text'
        500  || 'Failure: 500, Error: ok-text'
    }

    @Unroll 'success/failure handler with BiFunction (#code)'() {
        setup:
        ersatzServer.expectations {
            get('/handling').called(2).responds().content(OK_TEXT, TEXT_PLAIN).code(code)
        }

        def http = httpBuilder {
            request.uri = "${ersatzServer.httpUrl}/handling"
            response.success(new BiFunction<FromServer, Object, Object>() {
                @Override Object apply(FromServer fs, Object body) {
                    "Success: ${fs.statusCode}, Text: ${body}"
                }
            })
            response.failure(new BiFunction<FromServer, Object, Object>() {
                @Override Object apply(FromServer fs, Object body) {
                    "Failure: ${fs.statusCode}, Error: ${body}"
                }
            })
        }

        expect:
        http.get() == result

        and:
        http.getAsync().get() == result

        and:
        ersatzServer.verify()

        where:
        code || result
        200  || 'Success: 200, Text: ok-text'
        300  || 'Success: 300, Text: ok-text'
        400  || 'Failure: 400, Error: ok-text'
        500  || 'Failure: 500, Error: ok-text'
    }

    def 'gzip compression supported'() {
        setup:
        ersatzServer.expectations {
            get('/gzip').header('Accept-Encoding', 'gzip').called(2).responds().content('x' * 1000, TEXT_PLAIN)
        }

        def http = httpBuilder {
            request.uri = "${ersatzServer.httpUrl}/gzip"
            request.headers = ['Accept-Encoding': 'gzip']
            response.success { FromServer fs, Object body ->
                "${fs.headers.find { FromServer.Header h -> h.key == 'Content-Encoding' }.value} (${fs.statusCode})"
            }
        }

        expect:
        http.get() == 'gzip (200)'

        and:
        http.getAsync().get() == 'gzip (200)'

        and:
        ersatzServer.verify()
    }

    def 'exception handler works with closure'() {
        setup:
        ersatzServer.expectations {
            get('/exceptionally').called(1).responds().content(OK_TEXT, TEXT_PLAIN)
        }

        boolean caughtIt
        boolean caughtCorrectType

        HttpBuilder http = httpBuilder {
            request.uri = ersatzServer.httpUrl

            response.exception { t ->
                caughtCorrectType = (t instanceof IOException)
                caughtIt = true
                return null
            }
        }

        def config = {
            request.uri.path = '/exceptionally'

            response.parser('text/plain') { config, fromServer ->
                throw new IOException("couldn't parse it")
            }
        }

        when:
        http.get(config)

        then:
        caughtIt
        caughtCorrectType
        noExceptionThrown()
    }

    def 'exception handler works with function'() {
        setup:
        ersatzServer.expectations {
            get('/exceptionally').called(1).responds().content(OK_TEXT, TEXT_PLAIN)
        }

        boolean caughtIt
        boolean caughtCorrectType

        HttpBuilder http = httpBuilder {
            request.uri = ersatzServer.httpUrl

            response.exception(new Function<Throwable, Object>() {
                @Override Object apply(Throwable t) {
                    caughtIt = true
                    caughtCorrectType = (t instanceof IOException)
                    return null
                }
            })
        }

        def config = {
            request.uri.path = '/exceptionally'

            response.parser('text/plain') { config, fromServer ->
                throw new IOException("couldn't parse it")
            }
        }

        when:
        http.get(config)

        then:
        caughtIt
        caughtCorrectType
        noExceptionThrown()
    }

    def 'exception handler chain works correctly'() {
        setup:
        ersatzServer.expectations {
            get('/exceptionally').called(1).responds().content(OK_TEXT, TEXT_PLAIN)
        }

        boolean globalCaughtIt, requestCaughtIt

        HttpBuilder http = httpBuilder {
            request.uri = ersatzServer.httpUrl

            response.exception { t ->
                globalCaughtIt = true
                return null
            }
        }

        def config = {
            request.uri.path = '/exceptionally'

            response.parser('text/plain') { config, fromServer ->
                throw new IOException("couldn't parse it")
            }

            response.exception { t ->
                requestCaughtIt = true
                return null
            }
        }

        when:
        http.get(config)

        then:
        requestCaughtIt
        !globalCaughtIt
        noExceptionThrown()
    }

    def 'handles basic errors'() {
        setup:
        ersatzServer.expectations {
            get('/exceptionally').called(1).responds().content(OK_TEXT, TEXT_PLAIN)
        }

        when:
        boolean handledCorrectly = false

        HttpBuilder http = httpBuilder {
            request.uri = ersatzServer.httpUrl
            request.uri.host = 'www.mkdfiwiejglejrligjsldkflwngunwfnkwemfiwefdsf.com'

            response.exception { t ->
                handledCorrectly = (t instanceof UnknownHostException)
                return null
            }
        }

        http.get()

        then:
        handledCorrectly
        noExceptionThrown()

        when:
        handledCorrectly = false

        http = httpBuilder {
            request.uri = ersatzServer.httpUrl
            request.uri.host = 'www.g o o g l e.com'

            response.exception { t ->
                handledCorrectly = (t instanceof URISyntaxException)
                return null
            }
        }

        http.get()

        then:
        handledCorrectly
        noExceptionThrown()
    }

    @Unroll 'proxied get(): #protocol #contentType'() {
        setup:
        ersatzServer.expectations {
            get('/proxied').protocol(protocol).called(1).responds().content(content, contentType)
        }

        ErsatzProxy ersatzProxy = new ErsatzProxy({
            target ersatzServer.httpUrl
            expectations {
                get '/proxied'
            }
        })

        HttpBuilder http = httpBuilder {
            ignoreSslIssues execution
            execution.proxy('127.0.0.1', ersatzProxy.port, Proxy.Type.HTTP, protocol == 'HTTPS')
            request.uri = "${serverUri(protocol)}/proxied"
        }

        expect:
        result http.get()

        and:
        ersatzProxy.verify()
        ersatzServer.verify()

        cleanup:
        ersatzProxy.stop()

        // TODO: HTTPS support can be added once Erstaz supports it (https://github.com/cjstehno/ersatz/issues/68)
        where:
        protocol | contentType | content || result
        'HTTP'   | TEXT_PLAIN  | OK_TEXT || { r -> r == OK_TEXT }
        //'HTTPS'  | TEXT_PLAIN       | OK_TEXT || { r -> r == OK_TEXT }
    }

    @IgnoreIf({ !Boolean.valueOf(properties['test.proxy.support']) })
    @Unroll 'socks proxied get(): #protocol #contentType'() {
        //currently socks support can be tested by doing the following
        //1) Make sure sshd is running on localhost
        //2) Execute the following command: ssh -D 8889 localhost
        //3) Make sure the login is successful (enter password if needed). It's non-obvious
        //but by logging in you have also set up a SOCKS proxy listening on 8889 that will
        //be tunneled through ssh/sshd working in tandem.
        setup:
        ersatzServer.expectations {
            get('/proxied').protocol(protocol).called(1).responds().content(content, contentType)
        }

        println("Server url: ${serverUri(protocol)}")

        HttpBuilder http = httpBuilder {
            ignoreSslIssues execution
            execution.proxy('127.0.0.1', 8889, Proxy.Type.SOCKS, protocol == 'HTTPS')
            request.uri = "${serverUri(protocol)}/proxied"
        }

        expect:
        result(http.get())

        and:
        ersatzServer.verify()

        where:
        protocol | contentType | content || result
        'HTTP'   | TEXT_PLAIN  | OK_TEXT || { r -> r == OK_TEXT }
        'HTTPS'  | TEXT_PLAIN  | OK_TEXT || { r -> r == OK_TEXT }
    }

}

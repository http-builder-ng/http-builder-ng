/*
 * Copyright (C) 2016 David Clark
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

import com.stehno.ersatz.Decoders
import groovyx.net.http.ChainedHttpConfig
import groovyx.net.http.FromServer
import groovyx.net.http.NativeHandlers
import spock.lang.Unroll

import static com.stehno.ersatz.ContentType.APPLICATION_JSON
import static groovyx.net.http.ContentTypes.JSON
import static groovyx.net.http.ContentTypes.TEXT

/**
 * Test kit for testing the HTTP PUT method with different clients.
 */
abstract class HttpPutTestKit extends HttpMethodTestKit {

    private static final String DATE_STRING = '2016.08.25 14:43'
    private static final String BODY_STRING = 'Something Interesting'
    private static final String HTML_CONTENT = htmlContent('Something Cool')
    private static final String JSON_STRING = '{ "name":"Chuck", "age":56 }'
    private static final String JSON_CONTENT = '{ "accepted":false, "id":123 }'

    def 'PUT /: returns content'() {
        setup:
        ersatzServer.expectations {
            put('/').responds().content(htmlContent(), 'text/plain')
        }.start()

        expect:
        httpBuilder(ersatzServer.port).put() == htmlContent()

        and:
        httpBuilder(ersatzServer.port).putAsync().get() == htmlContent()
    }

    @Unroll 'PUT /foo (#contentType): returns content'() {
        given:
        ersatzServer.expectations {
            put('/foo').decoders(commonDecoders).body(BODY_STRING, TEXT[0]).responds().content(HTML_CONTENT, TEXT[0])
            put('/foo').decoders(commonDecoders).decoder(APPLICATION_JSON, Decoders.parseJson).body([name: 'Chuck', age: 56], JSON[0]).responds().content(JSON_CONTENT, JSON[0])
        }.start()

        def config = {
            request.uri.path = '/foo'
            request.body = content
            request.contentType = contentType[0]
            response.parser contentType, parser
        }

        expect:
        httpBuilder(ersatzServer.port).put(config) == result

        and:
        httpBuilder(ersatzServer.port).putAsync(config).get() == result

        where:
        content     | contentType | parser                               || result
        BODY_STRING | TEXT        | NativeHandlers.Parsers.&textToString || HTML_CONTENT
        JSON_STRING | JSON        | NativeHandlers.Parsers.&json         || [accepted: false, id: 123]
    }

    def 'PUT /foo (cookie): returns content'() {
        given:
        ersatzServer.expectations {
            put('/foo').decoders(commonDecoders).body(BODY_STRING, TEXT[0]).cookie('userid', 'spock').responds().content(htmlContent(), TEXT[0])
        }.start()

        def config = {
            request.uri.path = '/foo'
            request.body = BODY_STRING
            request.contentType = TEXT[0]
            request.cookie 'userid', 'spock'
        }

        expect:
        httpBuilder(ersatzServer.port).put(config) == htmlContent()

        and:
        httpBuilder(ersatzServer.port).putAsync(config).get() == htmlContent()
    }

    def 'PUT /foo (query string): returns content'() {
        given:
        ersatzServer.expectations {
            put('/foo').decoders(commonDecoders).body(BODY_STRING, TEXT[0]).query('action', 'login').responds().content(htmlContent(), TEXT[0])
        }.start()

        def config = {
            request.uri.path = '/foo'
            request.uri.query = [action: 'login']
            request.body = BODY_STRING
            request.contentType = TEXT[0]
        }

        expect:
        httpBuilder(ersatzServer.port).put(config) == htmlContent()

        and:
        httpBuilder(ersatzServer.port).putAsync(config).get() == htmlContent()
    }

    def 'PUT /date: returns content as Date'() {
        given:
        ersatzServer.expectations {
            put('/date').decoders(commonDecoders).body(BODY_STRING, TEXT[0]).responds().content(DATE_STRING, 'text/date')
        }.start()

        def config = {
            request.uri.path = '/date'
            request.body = BODY_STRING
            request.contentType = TEXT[0]
            response.parser('text/date') { ChainedHttpConfig config, FromServer fromServer ->
                Date.parse('yyyy.MM.dd HH:mm', fromServer.inputStream.text)
            }
        }

        expect:
        httpBuilder(ersatzServer.port).put(Date, config).format('yyyy.MM.dd HH:mm') == DATE_STRING

        and:
        httpBuilder(ersatzServer.port).putAsync(Date, config).get().format('yyyy.MM.dd HH:mm') == DATE_STRING
    }
}

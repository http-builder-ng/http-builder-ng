/*
 * Copyright (C) 2017 David Clark
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

import com.stehno.ersatz.feat.BasicAuthFeature
import groovyx.net.http.ChainedHttpConfig
import groovyx.net.http.FromServer

import static groovyx.net.http.ContentTypes.TEXT

/**
 * Test kit for testing the HTTP DELETE method with different clients.
 */
abstract class HttpDeleteTestKit extends HttpMethodTestKit {

    private static final String DATE_STRING = '2016.08.25 14:43'

    def 'DELETE /: returns content'() {
        setup:
        ersatzServer.expectations {
            delete('/').responds().content(htmlContent(), TEXT[0])
        }.start()

        expect:
        httpBuilder(ersatzServer.port).delete() == htmlContent()

        and:
        httpBuilder(ersatzServer.port).deleteAsync().get() == htmlContent()
    }

    def 'DELETE /foo: returns content'() {
        given:
        ersatzServer.expectations {
            delete('/foo').responds().content(htmlContent(), TEXT[0])
        }.start()

        def config = {
            request.uri.path = '/foo'
        }

        expect:
        httpBuilder(ersatzServer.port).delete(config) == htmlContent()

        and:
        httpBuilder(ersatzServer.port).deleteAsync(config).get() == htmlContent()
    }

    def 'DELETE /foo (cookie): returns content'() {
        given:
        ersatzServer.expectations {
            delete('/foo').cookie('userid', 'spock').responds().content(htmlContent(), TEXT[0])
        }.start()

        def config = {
            request.uri.path = '/foo'
            request.cookie 'userid', 'spock'
        }

        expect:
        httpBuilder(ersatzServer.port).delete(config) == htmlContent()

        and:
        httpBuilder(ersatzServer.port).deleteAsync(config).get() == htmlContent()
    }

    def 'DELETE /foo (query string): returns content'() {
        given:
        ersatzServer.expectations {
            delete('/foo').query('action', 'login').responds().content(htmlContent(), TEXT[0])
        }.start()

        def config = {
            request.uri.path = '/foo'
            request.uri.query = [action: 'login']
            request.contentType = TEXT[0]
        }

        expect:
        httpBuilder(ersatzServer.port).delete(config) == htmlContent()

        and:
        httpBuilder(ersatzServer.port).deleteAsync(config).get() == htmlContent()
    }

    def 'DELETE /date: returns content as Date'() {
        given:
        ersatzServer.expectations {
            delete('/date').responds().content(DATE_STRING, 'text/date')
        }.start()

        def config = {
            request.uri.path = '/date'
            response.parser('text/date') { ChainedHttpConfig config, FromServer fromServer ->
                Date.parse('yyyy.MM.dd HH:mm', fromServer.inputStream.text)
            }
        }

        expect:
        httpBuilder(ersatzServer.port).delete(Date, config).format('yyyy.MM.dd HH:mm') == DATE_STRING

        and:
        httpBuilder(ersatzServer.port).deleteAsync(Date, config).get().format('yyyy.MM.dd HH:mm') == DATE_STRING
    }

    def 'DELETE (BASIC) /basic: returns content'() {
        given:
        ersatzServer.addFeature new BasicAuthFeature()

        ersatzServer.expectations {
            delete('/basic').responds().content(htmlContent(), TEXT[0])
        }.start()

        def config = {
            request.uri.path = '/basic'
            request.auth.basic 'admin', '$3cr3t'
        }

        expect:
        httpBuilder(ersatzServer.port).delete(config) == htmlContent()

        and:
        httpBuilder(ersatzServer.port).deleteAsync(config).get() == htmlContent()
    }
}

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
package groovyx.net.http

import groovyx.net.http.tk.HttpBuilderTestKit
import spock.lang.Issue
import spock.lang.Requires

import java.util.function.Function

@Requires(HttpBin)
class ApacheHttpBuilderSpec extends HttpBuilderTestKit {

    def singleThreadHttpBin

    def setup() {
        clientFactory = { c -> new ApacheHttpBuilder(c) } as Function

        init()

        singleThreadHttpBin = HttpBuilder.configure(clientFactory) {
            request.uri = 'http://httpbin.org/'
        }
    }

    @Issue('https://github.com/http-builder-ng/http-builder-ng/issues/49')
    def "Test Set Cookies"() {
        expect:
        singleThreadHttpBin.get {
            request.uri.path = '/cookies'
            request.cookie('foocookie', 'barcookie')
        }.with {
            cookies.foocookie == 'barcookie'
        }

        singleThreadHttpBin.get {
            request.uri.path = '/cookies'
            request.cookie('requestcookie', '12345')
        }.with {
            cookies.foocookie == 'barcookie' && cookies.requestcookie == '12345'
        }
    }
}

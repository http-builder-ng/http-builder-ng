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
package groovyx.net.http

import groovyx.net.http.tk.HttpGetTestKit

import static com.stehno.ersatz.ContentType.TEXT_PLAIN

class JavaHttpGetSpec extends HttpGetTestKit implements UsesJavaClient {

//    def 'ssl request (ignoring issues / alternate config)'() {
//        setup:
//        ersatzServer.expectations {
//            get('/secure').protocol('https').responds().content('ok', TEXT_PLAIN)
//        }.start()
//
//        when:
//        def result = httpBuilder {
//            execution.sslContext =
//            request.uri = ersatzServer.httpsUrl
//        }.get {
//            request.uri.path = '/secure'
//        }
//
//        then:
//        result == 'ok'
//    }
}

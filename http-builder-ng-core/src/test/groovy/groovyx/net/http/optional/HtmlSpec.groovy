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
package groovyx.net.http.optional

import com.stehno.ersatz.ErsatzServer
import groovyx.net.http.HttpBuilder
import spock.lang.AutoCleanup
import spock.lang.Specification

class HtmlSpec extends Specification {

    @AutoCleanup('stop') private final ErsatzServer ersatzServer = new ErsatzServer()
    private static final String HTML_CONTENT = '<html><body><p>This is HTML!</p></body></html>'
    private static final String CONTENT_TYPE = 'web/content'

    def setup() {
        ersatzServer.expectations {
            get('/html').responds().content(HTML_CONTENT, CONTENT_TYPE)
        }.start()
    }

    // FIXME: these both pass, but they are not using the expected parsers
    // - these need to be addressed during the parser/encoder refactoring.

    def 'use necko parser'() {
        given:
        def http = HttpBuilder.configure {
            request.uri = "${ersatzServer.httpUrl}/html"
            response.parser([CONTENT_TYPE], Html.&neckoParse)
        }

        expect:
        http.get().text() == 'This is HTML!'
    }

    def 'use jsoup parser'() {
        given:
        def http = HttpBuilder.configure {
            request.uri = "${ersatzServer.httpUrl}/html"
            response.parser([CONTENT_TYPE], Html.&jsoupParse)
        }

        expect:
        http.get().getElementsByTag('p').text() == 'This is HTML!'
    }
}

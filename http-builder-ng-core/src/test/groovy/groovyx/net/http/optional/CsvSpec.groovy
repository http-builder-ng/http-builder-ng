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

import static com.stehno.ersatz.ContentType.TEXT_PLAIN
import static groovyx.net.http.optional.Csv.toCsv

class CsvSpec extends Specification {

    @AutoCleanup('stop') private final ErsatzServer ersatzServer = new ErsatzServer()

    // TODO: more testing needed here

    def "Robots.txt as CSV"() {
        setup:
        ersatzServer.expectations {
            get('/robots.txt').responds().content('User-agent: *\nDisallow: /deny\n', TEXT_PLAIN)
        }.start()

        when:
        def result = HttpBuilder.configure {
            request.uri = "${ersatzServer.httpUrl}/robots.txt"
            toCsv(delegate, 'text/plain', ':' as Character, null)
        }.get()

        then:
        result.size() == 2
        result[0][0] == 'User-agent'
        result[0][1].trim() == '*'
        result[1][0] == 'Disallow'
        result[1][1].trim() == '/deny'
    }
}

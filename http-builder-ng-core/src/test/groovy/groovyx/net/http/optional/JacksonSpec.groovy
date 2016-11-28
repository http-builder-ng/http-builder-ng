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
package groovyx.net.http.optional

import com.fasterxml.jackson.databind.ObjectMapper
import groovyx.net.http.HttpBuilder
import groovyx.net.http.MockWebServerRule
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Rule
import spock.lang.Specification

import static groovyx.net.http.ContentTypes.JSON

class JacksonSpec extends Specification {

    @Rule MockWebServerRule serverRule = new MockWebServerRule()

    private static final String CONTENT = '{"alpha":"bravo","charlie":42}'
    private static final String CONTENT_TYPE = 'jackson/json'
    private final ObjectMapper objectMapper = new ObjectMapper()

    def setup() {
        serverRule.dispatcher { RecordedRequest request ->
            if (request.method == 'GET' && request.path == '/jackson') {
                return new MockResponse().setHeader('Content-Type', CONTENT_TYPE).setBody(CONTENT)
            } else if (request.method == 'GET' && request.path == '/json') {
                return new MockResponse().setHeader('Content-Type', JSON[0]).setBody(CONTENT)
            }
            return new MockResponse().setResponseCode(404)
        }
    }

    def 'use with context config (alternate content type)'() {
        given:
        def http = HttpBuilder.configure {
            request.uri = "${serverRule.serverUrl}/jackson"
            request.contentType = CONTENT_TYPE
            context CONTENT_TYPE, Jackson.OBJECT_MAPPER_ID, objectMapper
        }

        when:
        def result = http.get {
            Jackson.use(delegate, [CONTENT_TYPE])
        }

        then:
        result == [alpha: 'bravo', charlie: 42]
    }

    def 'use with context config (default content type)'() {
        given:
        def http = HttpBuilder.configure {
            request.uri = "${serverRule.serverUrl}/json"
            request.contentType = JSON[0]
            context JSON, Jackson.OBJECT_MAPPER_ID, objectMapper
        }

        when:
        def result = http.get {
            Jackson.use(delegate)
        }

        then:
        result == [alpha: 'bravo', charlie: 42]
    }

    def 'use with mapper config (alternate content type)'() {
        given:
        def http = HttpBuilder.configure {
            request.uri = "${serverRule.serverUrl}/jackson"
            request.contentType = CONTENT_TYPE
            Jackson.mapper(delegate, objectMapper, [CONTENT_TYPE])
        }

        when:
        def result = http.get {
            Jackson.use(delegate, [CONTENT_TYPE])
        }

        then:
        result == [alpha: 'bravo', charlie: 42]
    }

    def 'use with mapper config (default content type)'() {
        given:
        def http = HttpBuilder.configure {
            request.uri = "${serverRule.serverUrl}/json"
            request.contentType = JSON[0]
            Jackson.mapper(delegate, objectMapper)
        }

        when:
        def result = http.get {
            Jackson.use(delegate)
        }

        then:
        result == [alpha: 'bravo', charlie: 42]
    }
}

/**
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

import groovyx.net.http.HttpBuilder
import groovyx.net.http.MockWebServerRule
import okhttp3.mockwebserver.MockResponse
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static groovyx.net.http.ContentTypes.TEXT
import static groovyx.net.http.optional.Download.*

class DownloadSpec extends Specification {

    @Rule MockWebServerRule serverRule = new MockWebServerRule()
    @Rule TemporaryFolder folder = new TemporaryFolder()

    private static final String CONTENT = "This is some file content."
    private HttpBuilder http

    def setup() {
        http = HttpBuilder.configure {
            request.uri = "${serverRule.serverUrl}/download"
        }
    }

    def 'toTempFile'() {
        given:
        serverRule.dispatcher('GET', '/download', new MockResponse().setBody(CONTENT))

        when:
        File file = http.get { toTempFile(delegate) }

        then:
        file.exists()
        file.text == CONTENT
    }

    def 'toTempFile with contentType'() {
        given:
        serverRule.dispatcher('GET', '/download', new MockResponse().setHeader('Content-Type', TEXT[0]).setBody(CONTENT))

        when:
        File file = http.get { toTempFile(delegate, TEXT[0]) }

        then:
        file.exists()
        file.text == CONTENT
    }

    def 'toFile'() {
        given:
        serverRule.dispatcher('GET', '/download', new MockResponse().setBody(CONTENT))

        File saved = folder.newFile()

        when:
        File file = http.get { toFile(delegate, saved) }

        then:
        file.exists()
        saved == file
        file.text == CONTENT
    }

    def 'toFile with contentType'() {
        given:
        serverRule.dispatcher('GET', '/download', new MockResponse().setHeader('Content-Type', TEXT[0]).setBody(CONTENT))

        File saved = folder.newFile()

        when:
        File file = http.get { toFile(delegate, TEXT[0], saved) }

        then:
        file.exists()
        saved == file
        file.text == CONTENT
    }

    def 'toStream'() {
        given:
        serverRule.dispatcher('GET', '/download', new MockResponse().setBody(CONTENT))

        when:
        ByteArrayOutputStream stream = http.get { toStream(delegate, new ByteArrayOutputStream()) }

        then:
        stream.toByteArray() == CONTENT.bytes
    }

    def 'toStream with contentType'() {
        given:
        serverRule.dispatcher('GET', '/download', new MockResponse().setHeader('Content-Type', TEXT[0]).setBody(CONTENT))

        when:
        ByteArrayOutputStream stream = http.get { toStream(delegate, TEXT[0], new ByteArrayOutputStream()) }

        then:
        stream.toByteArray() == CONTENT.bytes
    }
}

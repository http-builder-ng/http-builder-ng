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
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.AutoCleanup
import spock.lang.Specification

import static groovyx.net.http.ContentTypes.TEXT
import static groovyx.net.http.HttpBuilder.configure
import static groovyx.net.http.optional.Download.*

class DownloadSpec extends Specification {

    @Rule TemporaryFolder folder = new TemporaryFolder()

    @AutoCleanup('stop') private final ErsatzServer ersatzServer = new ErsatzServer()
    private static final String CONTENT = "This is some file content."

    def 'toTempFile'() {
        given:
        ersatzServer.expectations {
            get('/download').responds().content(CONTENT, 'text/plain')
        }.start()

        when:
        File file = configure {
            request.uri = "${ersatzServer.httpUrl}/download"
        }.get { toTempFile(delegate) }

        then:
        file.exists()
        file.text == CONTENT
    }

    def 'toTempFile with contentType'() {
        given:
        ersatzServer.expectations {
            get('/download').responds().content(CONTENT, TEXT[0])
        }.start()

        when:
        File file = configure {
            request.uri = "${ersatzServer.httpUrl}/download"
        }.get { toTempFile(delegate, TEXT[0]) }

        then:
        file.exists()
        file.text == CONTENT
    }

    def 'toFile'() {
        given:
        ersatzServer.expectations {
            get('/download').responds().content(CONTENT, TEXT[0])
        }.start()

        File saved = folder.newFile()

        when:
        File file = configure {
            request.uri = "${ersatzServer.httpUrl}/download"
        }.get { toFile(delegate, saved) }

        then:
        file.exists()
        saved == file
        file.text == CONTENT
    }

    def 'toFile with contentType'() {
        given:
        ersatzServer.expectations {
            get('/download').responds().content(CONTENT, TEXT[0])
        }.start()

        File saved = folder.newFile()

        when:
        File file = configure {
            request.uri = "${ersatzServer.httpUrl}/download"
        }.get { toFile(delegate, TEXT[0], saved) }

        then:
        file.exists()
        saved == file
        file.text == CONTENT
    }

    def 'toStream'() {
        given:
        ersatzServer.expectations {
            get('/download').responds().content(CONTENT, TEXT[0])
        }.start()

        when:
        ByteArrayOutputStream stream = configure {
            request.uri = "${ersatzServer.httpUrl}/download"
        }.get { toStream(delegate, new ByteArrayOutputStream()) }

        then:
        stream.toByteArray() == CONTENT.bytes
    }

    def 'toStream with contentType'() {
        given:
        ersatzServer.expectations {
            get('/download').responds().content(CONTENT, TEXT[0])
        }.start()

        when:
        ByteArrayOutputStream stream = configure {
            request.uri = "${ersatzServer.httpUrl}/download"
        }.get { toStream(delegate, TEXT[0], new ByteArrayOutputStream()) }

        then:
        stream.toByteArray() == CONTENT.bytes
    }
}

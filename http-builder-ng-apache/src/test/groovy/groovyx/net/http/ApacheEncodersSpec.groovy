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

import com.stehno.ersatz.ErsatzServer
import com.stehno.ersatz.MultipartContentMatcher
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.AutoCleanup
import spock.lang.Specification

import static com.stehno.ersatz.ContentType.TEXT_PLAIN
import static com.stehno.ersatz.Verifiers.once
import static groovyx.net.http.ContentTypes.MULTIPART_FORMDATA
import static groovyx.net.http.ContentTypes.TEXT

class ApacheEncodersSpec extends Specification {

    // TODO: there is duplication in these encoder tests - maybe another testkit for shared testing?

    @Rule TemporaryFolder folder = new TemporaryFolder()
    @AutoCleanup('stop') private final ErsatzServer ersatzServer = new ErsatzServer()
    private HttpBuilder http

    def setup() {
        http = ApacheHttpBuilder.configure {
            request.encoder(MULTIPART_FORMDATA, ApacheEncoders.&multipart)
            request.contentType = MULTIPART_FORMDATA[0]
        }
    }

    def 'multipart: field'() {
        setup:
        ersatzServer.expectations {
            post('/multi') {
                condition MultipartContentMatcher.multipart {
                    field(0, 'alpha', 'one') && field(1, 'bravo', 'two')
                }
                verifier(once())
                responds().content('ok', TEXT_PLAIN)
            }
        }.start()

        expect:
        http.post {
            request.uri = "${ersatzServer.serverUrl}/multi"
            request.body = MultipartContent.multipart {
                field 'alpha', 'one'
                field 'bravo', 'two'
            }
        } == 'ok'

        and:
        ersatzServer.verify()
    }

    def 'multipart: file (path)'() {
        setup:
        def (File fileA, File fileB) = setupMultipartFileExpectations()

        expect:
        http.post {
            request.uri = "${ersatzServer.serverUrl}/multi"
            request.body = MultipartContent.multipart {
                file 'filea', fileA.name, TEXT[0], fileA.toPath()
                file 'fileb', fileB.name, TEXT[0], fileB.toPath()
            }
        } == 'ok'

        and:
        ersatzServer.verify()
    }

    def 'multipart: file (bytes)'() {
        setup:
        def (File fileA, File fileB) = setupMultipartFileExpectations()

        expect:
        http.post {
            request.uri = "${ersatzServer.serverUrl}/multi"
            request.body = MultipartContent.multipart {
                file 'filea', fileA.name, TEXT[0], fileA.bytes
                file 'fileb', fileB.name, TEXT[0], fileB.bytes
            }
        } == 'ok'

        and:
        ersatzServer.verify()
    }

    def 'multipart: file (string)'() {
        setup:
        def (File fileA, File fileB) = setupMultipartFileExpectations()

        expect:
        http.post {
            request.uri = "${ersatzServer.serverUrl}/multi"
            request.body = MultipartContent.multipart {
                file 'filea', fileA.name, TEXT[0], fileA.text
                file 'fileb', fileB.name, TEXT[0], fileB.text
            }
        } == 'ok'

        and:
        ersatzServer.verify()
    }

    def 'multipart: file (stream)'() {
        setup:
        def (File fileA, File fileB) = setupMultipartFileExpectations()

        expect:
        http.post {
            request.uri = "${ersatzServer.serverUrl}/multi"
            request.body = MultipartContent.multipart {
                file 'filea', fileA.name, TEXT[0], fileA.newInputStream()
                file 'fileb', fileB.name, TEXT[0], fileB.newInputStream()
            }
        } == 'ok'

        and:
        ersatzServer.verify()
    }

    private setupMultipartFileExpectations() {
        File fileA = folder.newFile('file-a.txt')
        fileA.text = 'some-a-content'

        File fileB = folder.newFile('file-b.xtx')
        fileB.text = 'some-b-content'

        ersatzServer.expectations {
            post('/multi') {
                condition MultipartContentMatcher.multipart {
                    file(0, 'filea', 'file-a.txt', TEXT_PLAIN.value, 'some-a-content') &&
                        file(1, 'fileb', 'file-b.xtx', TEXT_PLAIN.value, 'some-b-content')
                }
                verifier(once())
                responds().content('ok', TEXT_PLAIN)
            }
        }.start()

        [fileA, fileB]
    }
}

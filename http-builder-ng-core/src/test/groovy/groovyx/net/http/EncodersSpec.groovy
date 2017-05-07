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
package groovyx.net.http

import com.stehno.ersatz.ContentType
import com.stehno.ersatz.Decoders
import com.stehno.ersatz.ErsatzServer
import com.stehno.ersatz.MultipartRequestContent
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.AutoCleanup
import spock.lang.Specification

import static com.stehno.ersatz.ContentType.MULTIPART_MIXED
import static com.stehno.ersatz.ContentType.TEXT_PLAIN
import static groovyx.net.http.ContentTypes.MULTIPART_FORMDATA
import static groovyx.net.http.ContentTypes.MULTIPART_MIXED
import static groovyx.net.http.ContentTypes.TEXT
import static org.hamcrest.Matchers.equalTo

class EncodersSpec extends Specification {

    @Rule TemporaryFolder folder = new TemporaryFolder()
    @AutoCleanup('stop') private final ErsatzServer ersatzServer = new ErsatzServer()
    private HttpBuilder http

    def 'multipart'() {
        setup:
        http = JavaHttpBuilder.configure {
            request.encoder(MULTIPART_FORMDATA, CoreEncoders.&multipart)
            request.contentType = MULTIPART_FORMDATA[0]
        }

        ersatzServer.expectations {
            post('/multi') {
                decoder ContentType.MULTIPART_MIXED, Decoders.multipart
                decoder TEXT_PLAIN, Decoders.utf8String
                body MultipartRequestContent.multipart {
                    part 'alpha', 'one'
                    part 'bravo', 'two'
                }, ContentType.MULTIPART_MIXED
                called 1
                responds().content('ok', TEXT_PLAIN)
            }
        }.start()

        expect:
        http.post {
            request.uri = "${ersatzServer.httpUrl}/multi"
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
        http = JavaHttpBuilder.configure {
            request.encoder(MULTIPART_MIXED, CoreEncoders.&multipart)
            request.contentType = MULTIPART_MIXED[0]
        }

        def (File fileA, File fileB) = setupMultipartFileExpectations()

        expect:
        http.post {
            request.uri = "${ersatzServer.httpUrl}/multi"
            request.body = MultipartContent.multipart {
                part 'filea', fileA.name, TEXT[0], fileA.toPath()
                part 'fileb', fileB.name, TEXT[0], fileB.toPath()
            }
        } == 'ok'

        and:
        ersatzServer.verify()
    }

    def 'multipart: file (bytes)'() {
        setup:
        http = JavaHttpBuilder.configure {
            request.encoder(MULTIPART_MIXED, CoreEncoders.&multipart)
            request.contentType = MULTIPART_MIXED[0]
        }

        def (File fileA, File fileB) = setupMultipartFileExpectations()

        expect:
        http.post {
            request.uri = "${ersatzServer.httpUrl}/multi"
            request.body = MultipartContent.multipart {
                part 'filea', fileA.name, TEXT[0], fileA.bytes
                part 'fileb', fileB.name, TEXT[0], fileB.bytes
            }
        } == 'ok'

        and:
        ersatzServer.verify()
    }

    def 'multipart: file (string)'() {
        setup:
        http = JavaHttpBuilder.configure {
            request.encoder(MULTIPART_MIXED, CoreEncoders.&multipart)
            request.contentType = MULTIPART_MIXED[0]
        }

        def (File fileA, File fileB) = setupMultipartFileExpectations()

        expect:
        http.post {
            request.uri = "${ersatzServer.httpUrl}/multi"
            request.body = MultipartContent.multipart {
                part 'filea', fileA.name, TEXT[0], fileA.text
                part 'fileb', fileB.name, TEXT[0], fileB.text
            }
        } == 'ok'

        and:
        ersatzServer.verify()
    }

    def 'multipart: file (stream)'() {
        setup:
        http = JavaHttpBuilder.configure {
            request.encoder(MULTIPART_MIXED, CoreEncoders.&multipart)
            request.contentType = MULTIPART_MIXED[0]
        }

        def (File fileA, File fileB) = setupMultipartFileExpectations()

        expect:
        http.post {
            request.uri = "${ersatzServer.httpUrl}/multi"
            request.body = MultipartContent.multipart {
                part 'filea', fileA.name, TEXT[0], fileA.newInputStream()
                part 'fileb', fileB.name, TEXT[0], fileB.newInputStream()
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
                decoder ContentType.MULTIPART_MIXED, Decoders.multipart
                decoder TEXT_PLAIN, Decoders.utf8String
                body MultipartRequestContent.multipart {
                    part 'filea', 'file-a.txt', TEXT_PLAIN, 'some-a-content'
                    part 'fileb', 'file-b.xtx', TEXT_PLAIN, 'some-b-content'
                }, ContentType.MULTIPART_MIXED
                called equalTo(1)
                responds().content('ok', TEXT_PLAIN)
            }
        }.start()

        [fileA, fileB]
    }
}

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
package groovyx.net.http.tk

import com.stehno.ersatz.*
import groovyx.net.http.ChainedHttpConfig
import groovyx.net.http.HttpBuilder
import groovyx.net.http.MultipartContent
import groovyx.net.http.ToServer
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.util.function.BiConsumer
import java.util.function.Function

import static com.stehno.ersatz.ContentType.TEXT_PLAIN
import static groovyx.net.http.ContentTypes.MULTIPART_FORMDATA
import static groovyx.net.http.ContentTypes.TEXT
import static org.hamcrest.Matchers.equalTo

abstract class EncoderTestKit extends Specification {

    @Rule TemporaryFolder folder = new TemporaryFolder()
    @AutoCleanup('stop') final ErsatzServer ersatzServer = new ErsatzServer()
    private HttpBuilder http

    abstract Function getClientFactory()

    abstract BiConsumer<ChainedHttpConfig, ToServer> getEncoder()

    def setup() {
        http = HttpBuilder.configure(clientFactory) {
            request.encoder(MULTIPART_FORMDATA, encoder)
            request.contentType = MULTIPART_FORMDATA[0]
        }
    }

    def 'multipart: field'() {
        setup:
        ersatzServer.expectations {
            post('/multi') {
                decoder ContentType.MULTIPART_FORMDATA, Decoders.multipart
                decoder TEXT_PLAIN, Decoders.utf8String
                body MultipartRequestContent.multipart {
                    part 'alpha', 'one'
                    part 'bravo', 'two'
                }, ContentType.MULTIPART_FORMDATA
                called equalTo(1)
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
                decoder ContentType.MULTIPART_FORMDATA, Decoders.multipart
                decoder TEXT_PLAIN, Decoders.utf8String
                body MultipartRequestContent.multipart {
                    part 'filea', 'file-a.txt', TEXT_PLAIN.value, 'some-a-content'
                    part 'fileb', 'file-b.xtx', TEXT_PLAIN.value, 'some-b-content'
                }, ContentType.MULTIPART_FORMDATA
                called 1
                responds().content('ok', TEXT_PLAIN)
            }
        }.start()

        [fileA, fileB]
    }
}

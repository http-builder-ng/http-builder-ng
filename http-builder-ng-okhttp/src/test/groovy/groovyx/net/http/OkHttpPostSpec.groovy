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
import com.stehno.ersatz.MultipartRequestContent
import groovyx.net.http.tk.HttpPostTestKit
import spock.lang.Ignore
import spock.lang.Unroll

import static com.stehno.ersatz.ContentType.MULTIPART_MIXED
import static com.stehno.ersatz.ContentType.MULTIPART_MIXED
import static com.stehno.ersatz.ContentType.TEXT_PLAIN
import static groovyx.net.http.ContentTypes.MULTIPART_FORMDATA
import static groovyx.net.http.MultipartContent.multipart
import static groovyx.net.http.util.SslUtils.ignoreSslIssues

class OkHttpPostSpec extends HttpPostTestKit implements UsesOkClient {

    @Unroll 'multipart request #proto'() {
        setup:
        ersatzServer.expectations {
            post('/upload') {
                body MultipartRequestContent.multipart {
                    decoder ContentType.MULTIPART_FORMDATA, Decoders.multipart
                    decoder ContentType.TEXT_PLAIN, Decoders.utf8String
                    called(2)
                    protocol(proto)
                    part 'alpha', 'some data'
                    part 'bravo', 'bravo.txt', 'text/plain', 'This is bravo content'
                }, ContentType.MULTIPART_FORMDATA
                responds().content(OK_TEXT, TEXT_PLAIN)
            }
        }.start()

        def http = httpBuilder {
            ignoreSslIssues execution
            request.uri = "${serverUri(proto)}/upload"
            request.contentType = MULTIPART_FORMDATA[0]
            request.body = multipart {
                field 'alpha', 'some data'
                part 'bravo', 'bravo.txt', 'text/plain', 'This is bravo content'
            }
            request.encoder(MULTIPART_FORMDATA, OkHttpEncoders.&multipart)
        }

        expect:
        http.post() == OK_TEXT

        and:
        http.postAsync().get() == OK_TEXT

        and:
        ersatzServer.verify()

        where:
        proto << ['HTTP', 'HTTPS']
    }

    @Ignore('OkHttp has an issue with the generic encoder - just use the client-specific one for now')
    @Unroll 'multipart request #proto (core encoder)'() {
        setup:
        ersatzServer.expectations {
            post('/upload') {
                decoder MULTIPART_MIXED, Decoders.multipart
                decoder TEXT_PLAIN, Decoders.utf8String

                called(2)
                protocol(proto)
                body MultipartRequestContent.multipart {
                    part 'alpha', 'some data'
                    part 'bravo', 'bravo.txt', 'text/plain', 'This is bravo content'
                }, MULTIPART_MIXED
                responds().content(OK_TEXT, TEXT_PLAIN)
            }
        }

        def http = httpBuilder {
            ignoreSslIssues execution
            request.uri = "${serverUri(proto)}/upload"
            request.contentType = MULTIPART_FORMDATA[0]
            request.body = multipart {
                field 'alpha', 'some data'
                part 'bravo', 'bravo.txt', 'text/plain', 'This is bravo content'
            }
            request.encoder(MULTIPART_FORMDATA, CoreEncoders.&multipart)
        }

        expect:
        http.post() == OK_TEXT

        and:
        http.postAsync().get() == OK_TEXT

        and:
        ersatzServer.verify()

        where:
        proto << ['HTTP', 'HTTPS']
    }
}

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

import com.stehno.ersatz.ContentType
import com.stehno.ersatz.Decoders
import com.stehno.ersatz.MultipartRequestContent
import groovyx.net.http.tk.HttpPutTestKit

import static com.stehno.ersatz.ContentType.TEXT_PLAIN
import static groovyx.net.http.ContentTypes.MULTIPART_FORMDATA
import static groovyx.net.http.MultipartContent.multipart

class ApacheHttpPutSpec extends HttpPutTestKit implements UsesApacheClient {

    def 'PUT /upload (multipart)'() {
        setup:
        ersatzServer.expectations {
            put('/upload') {
                body MultipartRequestContent.multipart {
                    decoder ContentType.MULTIPART_FORMDATA, Decoders.multipart
                    decoder ContentType.TEXT_PLAIN, Decoders.utf8String
                    part 'alpha', 'some data'
                    part 'bravo', 'bravo.txt', 'text/plain', 'This is bravo content'
                }, ContentType.MULTIPART_FORMDATA
                responds().content('ok', TEXT_PLAIN)
            }
        }.start()

        def config = {
            request.uri.path = '/upload'
            request.contentType = MULTIPART_FORMDATA[0]
            request.body = multipart {
                field 'alpha', 'some data'
                part 'bravo', 'bravo.txt', 'text/plain', 'This is bravo content'
            }
            request.encoder(MULTIPART_FORMDATA, ApacheEncoders.&multipart)
        }

        expect:
        httpBuilder(ersatzServer.port).put(config) == 'ok'

        and:
        httpBuilder(ersatzServer.port).putAsync(config).get() == 'ok'
    }
}

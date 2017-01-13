/*
 * Copyright (C) 2017 David Clark
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

import com.stehno.ersatz.Decoders
import com.stehno.ersatz.ErsatzServer
import com.stehno.ersatz.RequestDecoders

import static com.stehno.ersatz.ContentType.APPLICATION_JSON
import static com.stehno.ersatz.ContentType.APPLICATION_URLENCODED
import static com.stehno.ersatz.ContentType.TEXT_PLAIN

/**
 * Base test kit for testing HTTP method handling by different client implementations.
 */
abstract class HttpMethodTestKit extends TestKit {

    protected final ErsatzServer ersatzServer = new ErsatzServer()
    protected final RequestDecoders commonDecoders = new RequestDecoders({
        register TEXT_PLAIN, Decoders.utf8String
        register APPLICATION_URLENCODED, Decoders.urlEncoded
        register APPLICATION_JSON, Decoders.parseJson
    })

    def cleanup() {
        ersatzServer.stop()
    }

    static String htmlContent(String text = 'Nothing special') {
        "<html><body><!-- a bunch of really interesting content that you would be sorry to miss -->$text</body></html>" as String
    }

    static String xmlContent(String text = 'Nothing special') {
        "<?xml version=\"1.0\"?><root><child><elt name='foo' /><text>$text</text></child></root>" as String
    }

    static String jsonContent(String text = 'Nothing special') {
        """
            {
                "items":[
                    {
                        "name":"alpha",
                        "score":123,
                        "text": "${text}"
                    }
                ]
            }
        """.stripIndent()
    }
}

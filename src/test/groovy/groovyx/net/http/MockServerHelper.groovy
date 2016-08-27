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

import groovyx.net.http.optional.ApacheHttpBuilder
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse

import java.util.function.Function

class MockServerHelper {
    // TODO: rename to something more meaningful

    static final Function apacheClientFactory = { c -> new ApacheHttpBuilder(c); } as Function
    static final Function javaClientFactory = { c -> new JavaHttpBuilder(c); } as Function

    private static HttpBuilder httpBuilder(final HttpClientType clientType, Closure config) {
        HttpBuilder.configure(clientType == HttpClientType.APACHE ? apacheClientFactory : javaClientFactory, config)
    }

    static HttpRequest get(final String path) {
        HttpRequest.request().withMethod('GET').withPath(path)
    }

    static HttpRequest post(final String path) {
        HttpRequest.request().withMethod('POST').withPath(path)
    }

    static HttpRequest post(final String path, final String body, final String contentType = 'text/plain') {
        HttpRequest.request().withMethod('POST').withPath(path).withBody(body).withHeader('Content-Type', contentType)
    }

    static HttpRequest head(final String path) {
        HttpRequest.request().withMethod('HEAD').withPath(path)
    }

    static HttpResponse responseContent(final String content, final String type = 'text/plain') {
        HttpResponse.response().withBody(content).withStatusCode(200).withHeader('Content-Type', type)
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

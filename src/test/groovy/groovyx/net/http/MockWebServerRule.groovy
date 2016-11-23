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

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.rules.ExternalResource

/**
 * JUnit Rule used to manage a mock web server for testing.
 */
class MockWebServerRule extends ExternalResource {

    private MockWebServer server

    @Override
    protected void before() throws Throwable {
        server = new MockWebServer()
        server.start()
    }

    void dispatcher(final Closure<MockResponse> closure) {
        server.dispatcher = closure as Dispatcher
    }

    void dispatcher(final String method, final String path, final MockResponse response) {
        dispatcher { RecordedRequest request ->
            if (request.method == method && request.path == path) {
                return response
            }
            return new MockResponse().setResponseCode(404)
        }
    }

    String getServerUrl() {
        "http://${server.hostName}:${server.port}"
    }

    int getServerPort() {
        server.port
    }

    @Override
    protected void after() {
        server.shutdown()
    }
}

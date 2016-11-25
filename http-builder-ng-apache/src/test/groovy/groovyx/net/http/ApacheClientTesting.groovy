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

import java.util.function.Function

/**
 * Helpers for testing with the Apache client HttpBuilder.
 */
class ApacheClientTesting {

    static final Function clientFactory = { c -> new ApacheHttpBuilder(c); } as Function

    static HttpBuilder httpBuilder(Closure config) {
        HttpBuilder.configure(clientFactory, config)
    }

    static HttpBuilder httpBuilder(String uri) {
        httpBuilder {
            request.uri = uri
        }
    }

    static HttpBuilder httpBuilder(int port) {
        httpBuilder {
            request.uri = "http://localhost:${port}"
        }
    }
}

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

import groovyx.net.http.HttpBuilder
import groovyx.net.http.HttpConfig
import groovyx.net.http.HttpObjectConfig
import spock.lang.Specification

import java.util.function.Function

/**
 * Base functionality for a shared test kit allowing the same tests to be executed using different client implementations.
 *
 * Options may be provided via the `options` property as name/value mappings. These options will be used by various tests
 * to manage the testing of features for clients which may not support certain tested features.
 */
abstract class TestKit extends Specification {

    Function clientFactory

    protected HttpBuilder httpBuilder(@DelegatesTo(HttpObjectConfig) Closure config) {
        HttpBuilder.configure(clientFactory, config)
    }

    protected HttpBuilder httpBuilder(String uri) {
        httpBuilder {
            request.uri = uri
        }
    }
}

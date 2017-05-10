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

import spock.lang.Specification
import spock.lang.Unroll

import static groovyx.net.http.ContentTypes.*

class ContentTypesSpec extends Specification {

    @Unroll def 'fromValue(#string)'() {
        expect:
        fromValue(string) == value

        where:
        string                              || value
        '*/*'                               || ANY
        'text/plain'                        || TEXT
        'application/json'                  || JSON
        'application/javascript'            || JSON
        'text/javascript'                   || JSON
        'application/xml'                   || XML
        'text/xml'                          || XML
        'application/xhtml+xml'             || XML
        'application/atom+xml'              || XML
        'text/html'                         || HTML
        'application/x-www-form-urlencoded' || URLENC
        'application/octet-stream'          || BINARY
        'nothing/nohow'                     || null
    }

    @Unroll def 'getAt(#index) == #value'() {
        expect:
        type[index] == value

        where:
        type   | index || value
        ANY    | 0     || '*/*'
        TEXT   | 0     || 'text/plain'
        JSON   | 0     || 'application/json'
        JSON   | 1     || 'application/javascript'
        JSON   | 2     || 'text/javascript'
        XML    | 0     || 'application/xml'
        XML    | 1     || 'text/xml'
        XML    | 2     || 'application/xhtml+xml'
        XML    | 3     || 'application/atom+xml'
        HTML   | 0     || 'text/html'
        URLENC | 0     || 'application/x-www-form-urlencoded'
        BINARY | 0     || 'application/octet-stream'
    }

    def 'iterator'() {
        expect:
        XML.iterator().collect() == ['application/xml', 'text/xml', 'application/xhtml+xml', 'application/atom+xml']
    }

    def 'forEach'() {
        when:
        def values = []

        JSON.forEach { value->
            values << value
        }

        then:
        values == ['application/json', 'application/javascript', 'text/javascript']
    }
}

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

import spock.lang.*
import groovyx.net.http.FromServer.Header
import static groovyx.net.http.FromServer.Header.full
import java.time.ZoneOffset
import java.time.ZonedDateTime

class HeadersSpec extends Specification {

    static Map all
    
    def setupSpec() {
        def tmp = [:]
        tmp = HeadersSpec.classLoader.getResourceAsStream('headers.csv').eachLine('UTF-8') { line ->
            def ary = line.split('~')
            tmp[ary[0]] = ary[1]
            return tmp
        }
        
        all = tmp.asImmutable()
    }

    def "header parsing gives something back"() {
        expect:
        all.every { key, raw -> FromServer.Header.full(raw) instanceof Header }
    }

    def "correct types"() {
        expect:
        full(all['Connection']) instanceof FromServer.Header.ValueOnly
        full(all["Accept-Patch"]) instanceof FromServer.Header.CombinedMap
        full(all['Cache-Control']) instanceof FromServer.Header.MapPairs
        full(all['Content-Length']) instanceof FromServer.Header.SingleLong
        full(all['Allow']) instanceof FromServer.Header.CsvList
        full(all['Date']) instanceof FromServer.Header.HttpDate
    }

    def "parse returns correct type"() {
        expect:
        full(all['Connection']).parsed instanceof String
        full(all["Accept-Patch"]).parsed instanceof Map
        full(all['Cache-Control']).parsed instanceof Map
        full(all['Content-Length']).parsed instanceof Number
        full(all['Allow']).parsed instanceof List
        full(all['Date']).parsed instanceof ZonedDateTime
    }

    def "correct values"() {
        expect:
        full(all['Connection']).parsed == 'close'
        full(all['Accept-Patch']).parsed == [ 'Accept-Patch': 'text/example', charset: 'utf-8' ]
        full(all['Alt-Svc']).parsed == [ h2: "http2.example.com:443", ma: '7200' ]
        full(all['Content-Length']).parsed == 348L
        full(all['Allow']).parsed == [ 'GET', 'POST' ]
        // Tue, 15 Nov 1994 08:12:31 GMT
        full(all['Date']).parsed == ZonedDateTime.of(1994, 11, 15, 8, 12, 31, 0, ZoneOffset.UTC)
    }
}

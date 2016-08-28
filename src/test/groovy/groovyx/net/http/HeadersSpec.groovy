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
package groovyx.net.http;

import spock.lang.*;
import groovyx.net.http.Header;
import static groovyx.net.http.Header.header;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

class HeadersSpec extends Specification {

    static Map all;
    
    def setupSpec() {
        def tmp = [:]
        tmp = HeadersSpec.classLoader.getResourceAsStream('headers.csv').eachLine('UTF-8') { line ->
            def ary = line.split('~');
            tmp[ary[0]] = ary[1];
            return tmp;
        }
        
        all = tmp.asImmutable();
    }

    def "header parsing gives something back"() {
        expect:
        all.every { key, raw -> Header.header(raw) instanceof Header; }
    }

    def "correct types"() {
        expect:
        header(all['Connection']) instanceof Header.ValueOnly;
        header(all["Accept-Patch"]) instanceof Header.CombinedMap;
        header(all['Cache-Control']) instanceof Header.MapPairs;
        header(all['Content-Length']) instanceof Header.SingleLong;
        header(all['Allow']) instanceof Header.CsvList;
        header(all['Date']) instanceof Header.HttpDate;
    }

    def "parse returns correct type"() {
        expect:
        header(all['Connection']).parsed instanceof String
        header(all["Accept-Patch"]).parsed instanceof Map
        header(all['Cache-Control']).parsed instanceof Map
        header(all['Content-Length']).parsed instanceof Number
        header(all['Allow']).parsed instanceof List
        header(all['Date']).parsed instanceof ZonedDateTime
    }

    def "correct values"() {
        expect:
        header(all['Connection']).parsed == 'close'
        header(all['Accept-Patch']).parsed == [ 'Accept-Patch': 'text/example', charset: 'utf-8' ]
        header(all['Alt-Svc']).parsed == [ h2: "http2.example.com:443", ma: '7200' ];
        header(all['Content-Length']).parsed == 348L;
        header(all['Allow']).parsed == [ 'GET', 'POST' ]
        // Tue, 15 Nov 1994 08:12:31 GMT
        header(all['Date']).parsed == ZonedDateTime.of(1994, 11, 15, 8, 12, 31, 0, ZoneOffset.UTC)
    }

    def "populate headers object"() {
        setup:
        def set = new HashSet();
        all.each { key, value -> set.add(header(value)); };
        def headers = new Headers(set);
        
        expect:
        all.size() == set.size();
        headers.headerSet().every { h -> h.parsed == headers.parsed(h.key); }
    }
}

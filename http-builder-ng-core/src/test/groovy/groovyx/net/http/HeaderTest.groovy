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
package groovyx.net.http;

import spock.lang.*;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import static groovyx.net.http.FromServer.*;
import static groovyx.net.http.FromServer.Header.*;

class HeaderTest extends Specification {

    def "Accept Header"() {
        setup:
        def h = FromServer.Header.full('Accept: text/plain');
        
        expect:
        h.key == 'Accept';
        h.value == 'text/plain';
        h.parsedType == String
    }

    def "Host Header"() {
        setup:
        def h = FromServer.Header.full('Host: en.wikipedia.org:8080');

        expect:
        h.key == 'Host';
        h.value == 'en.wikipedia.org:8080';
        h.parsedType == String;
    }

    def "ETag Header"() {
        setup:
        def h = FromServer.Header.full('ETag: "737060cd8c284d8af7ad3082f209582d"');
        
        expect:
        h.key == 'ETag';
        h.value == '737060cd8c284d8af7ad3082f209582d';
        h.parsedType == String
    }

    def "Date Header"() {
        setup:
        def h = FromServer.Header.full('Date: Tue, 15 Nov 1994 08:12:31 GMT');

        expect:
        h.key == 'Date';
        h.value == 'Tue, 15 Nov 1994 08:12:31 GMT';
        h.parsed == ZonedDateTime.of(1994, 11, 15, 8, 12, 31, 0, ZoneOffset.UTC)
    }

    def "Set Cookie Header"() {
        setup:
        def h = FromServer.Header.full('Set-Cookie2: UserID=JohnDoe; Max-Age=3600; Version=1');
        def listCookies = h.parsed;
        
        expect:
        h.key == 'Set-Cookie2';
        h.value == 'UserID=JohnDoe; Max-Age=3600; Version=1';
        listCookies.size() == 1;
        listCookies[0].name == 'UserID';
        listCookies[0].value == 'JohnDoe';
        listCookies[0].version == 1;
    }

    def "Find Header"() {
        setup:
        def headers = [ full('Accept: text/plain'),
                        full('Host: en.wikipedia.org:8080'),
                        full('ETag: "737060cd8c284d8af7ad3082f209582d"'),
                        full('Date: Tue, 15 Nov 1994 08:12:31 GMT'),
                        full('Set-Cookie: UserID=JohnDoe; Max-Age=3600; Version=1') ];

        expect:
        find(headers, 'Set-Cookie');
        find(headers, 'date');
        find(headers, 'eTaG');
        !find(headers, 'Content-Language');
    }

    def "Content-Type And Charset"() {
        setup:
        def h = full('Content-Type: text/html; charset=utf-8');

        expect:
        h.parsed['Content-Type'] == 'text/html';
        h.parsed['charset'] == 'utf-8';
    }
}


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

import spock.lang.Ignore
import spock.lang.Issue
import spock.lang.Specification

class FromServerSpec extends Specification {

    def 'Header.full()'() {
        when:
        def header = FromServer.Header.full('Content-Type:text/plain')

        then:
        header.key == 'Content-Type'
        header.value == 'text/plain'
        header.parsedType == Map
        header.toString() == 'Content-Type: text/plain'
    }

    def 'Header.keyValue()'() {
        when:
        def header = FromServer.Header.keyValue('Accept', 'image/jpeg')

        then:
        header.key == 'Accept'
        header.value == 'image/jpeg'
        header.parsedType == String
        header.toString() == 'Accept: image/jpeg'
    }

    @Ignore @Issue('https://github.com/dwclark/http-builder-ng/issues/9')
    def 'Header.keysValues'() {
        when:
        def header = FromServer.Header.keyValue('Forwarded', 'for=192.0.2.60;proto=http;by=203.0.113.43')

        then:
        header.key == 'Forwarded'
        header.value == 'for=192.0.2.60;proto=http;by=203.0.113.43'
        header.multiValued

        // FIXME: figure out if this is the expected format (and why the test fails even when it appears to be the expected output)
        header.keysValues == [
            Forwarded: [],
            by       : ['203.0.113.43'],
            for      : ['192.0.2.60'],
            proto    : ['http']
        ] as Map<String,List<String>>
    }
}

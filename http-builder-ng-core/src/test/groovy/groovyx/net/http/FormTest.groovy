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
import static java.nio.charset.StandardCharsets.UTF_8;
import static groovyx.net.http.Form.*;

class FormTest extends Specification {

    def "Decoding"() {
        setup:
        def query = "foo=bar&baz="

        def shouldBe = [ foo: ['bar'], baz: []]

        expect:
        shouldBe == decode(new ByteArrayInputStream(query.getBytes(UTF_8)), UTF_8)
    }

    def "Back and Forth"() {
        setup:
        def one = [ foo: ['bar'], baz: ['floppy'], empty: [] ]
        def two = [ 'key&&=': [ 'one', 'two', 'three' ],
                    key2: [ 'one&&', 'two&&', 'three&&' ] ]

        expect:
        one == decode(new ByteArrayInputStream(encode(one, UTF_8).getBytes(UTF_8)), UTF_8)
        two == decode(new ByteArrayInputStream(encode(two, UTF_8).getBytes(UTF_8)), UTF_8)
    }

    def "Encoding"() {
        setup:
        def map = [ custname: 'bar&&', custtel: '111==', custemail: [],
                    delivery: [], comments: null ];
        def shouldBe = 'custname=bar%26%26&custtel=111%3D%3D&custemail=&delivery=&comments='

        expect:
        shouldBe == encode(map, UTF_8)
    }

    def "Round Trip All Types"() {
        setup:
        def map = [ custname: 'bar&&', custtel: '111==', custemail: [],
                    delivery: '', comments: null ]
        def shouldBe =  [ custname: ['bar&&'], custtel: ['111=='], custemail: [],
                          delivery: [], comments: [] ]

        def returned = decode(new ByteArrayInputStream(encode(map, UTF_8).getBytes(UTF_8)), UTF_8)

        expect:
        returned == shouldBe
    }
}

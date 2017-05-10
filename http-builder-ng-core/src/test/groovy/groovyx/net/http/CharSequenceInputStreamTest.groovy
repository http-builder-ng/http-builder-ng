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
import java.nio.charset.StandardCharsets;

class CharSequenceInputStreamTest extends Specification {

    def "Basic Roundtrip"() {
        setup:
        def istream = new CharSequenceInputStream("foobarbaz", StandardCharsets.UTF_8);
        def baos = new ByteArrayOutputStream();
        baos << istream;
        def str = new String(baos.toByteArray(), StandardCharsets.UTF_8);

        expect:
        str == "foobarbaz";
    }

    def "Something Large"() {
        setup:
        def text = CharSequenceInputStreamTest.classLoader.getResourceAsStream('rss_1_0_validator.xml').getText('UTF-8');
        def istream = new CharSequenceInputStream(text, StandardCharsets.UTF_8)
        def baos = new ByteArrayOutputStream();
        baos << istream;
        def str = new String(baos.toByteArray(), StandardCharsets.UTF_8);

        expect:
        str == text;
    }

    def "Something Random"() {
        setup:
        def text = ReaderInputStreamTest.randomAscii(4096);
        def istream = new CharSequenceInputStream(text, StandardCharsets.UTF_8);
        def baos = new ByteArrayOutputStream();
        baos << istream;
        def str = new String(baos.toByteArray(), StandardCharsets.UTF_8);

        expect:
        str == text;
    }
}

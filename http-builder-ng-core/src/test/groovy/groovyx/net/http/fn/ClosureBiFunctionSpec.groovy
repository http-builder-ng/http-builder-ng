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
package groovyx.net.http.fn

import spock.lang.Specification

class ClosureBiFunctionSpec extends Specification {

    private static final Object IN_A = new Object()
    private static final Object IN_B = new Object()
    private static final Object OUT = new Object()

    def 'wraps a zero-arg closure'() {
        given:
        def closure = { OUT }

        when:
        def output = new ClosureBiFunction<>(closure).apply(IN_A, IN_B)

        then:
        output == OUT
    }

    def 'wraps a one-arg closure'() {
        given:
        def arg
        def closure = { a ->
            arg = a
            OUT
        }

        when:
        def output = new ClosureBiFunction<>(closure).apply(IN_A, IN_B)

        then:
        arg == IN_A
        output == OUT
    }

    def 'wraps a two-arg closure'() {
        given:
        def args = []
        def closure = { a, b ->
            args = [a, b]
            OUT
        }

        when:
        def output = new ClosureBiFunction<>(closure).apply(IN_A, IN_B)

        then:
        args[0] == IN_A
        args[1] == IN_B
        output == OUT
    }
}

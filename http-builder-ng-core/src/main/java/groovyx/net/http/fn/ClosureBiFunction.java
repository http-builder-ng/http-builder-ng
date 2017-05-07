/**
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
package groovyx.net.http.fn;

import groovy.lang.Closure;

import java.util.function.BiFunction;

/**
 * Utility `BiFunction` implementation used to wrap a Groovy `Closure` in a Java `BiFunction` interface.
 *
 * The wrapped closure must accept 0, 1 or 2 arguments, and should return the specified `OUT` type.
 *
 * @param <IN_0> the type of the first input parameter
 * @param <IN_1> the type of the second input parameter
 * @param <OUT> the type of the return value
 */
public class ClosureBiFunction<IN_0, IN_1, OUT> implements BiFunction<IN_0, IN_1, OUT> {

    private final Closure<OUT> closure;

    public ClosureBiFunction(final Closure<OUT> closure) {
        this.closure = closure;
    }

    public Closure<OUT> getClosure() {
        return closure;
    }

    @Override
    public OUT apply(IN_0 in_0, IN_1 in_1) {
        return closure.call(closureArgs(in_0, in_1));
    }

    private Object[] closureArgs(final IN_0 in_0, final IN_1 in_1) {
        final int size = closure.getMaximumNumberOfParameters();
        final Object[] args = new Object[size];

        if (size >= 1) {
            args[0] = in_0;
        }

        if (size >= 2) {
            args[1] = in_1;
        }

        return args;
    }
}

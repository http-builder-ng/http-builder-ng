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

import java.util.function.Function;

/**
 * Utility `Function` implementation used to wrap a Groovy `Closure` in a Java `Function` interface.
 *
 * The wrapped closure must accept 1 argument, and should return the specified `OUT` type.
 *
 * @param <IN_0> the type of the input parameter
 * @param <OUT> the type of the return value
 */
public class ClosureFunction<IN_0, OUT> implements Function<IN_0, OUT> {

    private final Closure<OUT> closure;
    private final int size;
    
    public ClosureFunction(final Closure<OUT> closure) {
        this.closure = closure;
        this.size = closure.getMaximumNumberOfParameters();
        if(size != 1) {
            throw new IllegalArgumentException("Closure needs to accept a single argument");
        }
    }

    public Closure<OUT> getClosure() {
        return closure;
    }

    @Override
    public OUT apply(final IN_0 in_0) {
        return closure.call(in_0);
    }
}

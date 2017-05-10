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
package groovyx.net.http;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public class Traverser {

    public static <T,V> V traverse(final T target, final Function<T,T> next,
                                   final Function<T,V> getValue, final Predicate<V> testValue) {
        final V v = getValue.apply(target);
        if(testValue.test(v)) {
            return v;
        }

        final T nextTarget = next.apply(target);
        if(nextTarget != null) {
            return traverse(nextTarget, next, getValue, testValue);
        }

        return null;
    }

    public static <V> boolean notNull(final V v) { return v != null; }
    public static boolean nonEmptyMap(final Map<?,?> v) { return v != null && !v.isEmpty(); }
    public static <V> Predicate<V> notValue(final V v) { return (toTest) -> !v.equals(toTest); }
}

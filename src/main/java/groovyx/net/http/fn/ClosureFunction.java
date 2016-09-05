package groovyx.net.http.fn;


import groovy.lang.Closure;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * FIXME: document
 */
public class ClosureFunction<IN, OUT> implements Function<IN, OUT> {

    private Closure<OUT> closure;

    public ClosureFunction(final Closure<OUT> closure) {
        this.closure = closure;
    }

    @Override
    public OUT apply(IN in) {
        return closure.call(in);
    }
}


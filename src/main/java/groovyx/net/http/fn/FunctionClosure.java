package groovyx.net.http.fn;

import groovy.lang.Closure;

import java.util.function.Function;

/**
 * FIXME: document
 */
public abstract class FunctionClosure<IN, OUT> extends Closure<OUT> implements Function<IN, OUT> {

    public FunctionClosure() {
        super(null);
    }

    OUT doCall(IN input) {
        return apply(input);
    }

    @Override
    abstract public OUT apply(IN in);

    @Override
    public int getMaximumNumberOfParameters() {
        return 1;
    }
}

package groovyx.net.http.fn;

import java.util.function.Function;

/**
 * FIXME: document
 */
public class FunctionClosureAdapter<IN, OUT> extends FunctionClosure<IN, OUT> {

    private final Function<IN, OUT> function;

    public FunctionClosureAdapter(final Function<IN, OUT> function) {
        this.function = function;
    }

    @Override
    public OUT apply(IN in) {
        return function.apply(in);
    }
}

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

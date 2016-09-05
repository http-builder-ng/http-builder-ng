package groovyx.net.http

import spock.lang.Specification

import java.util.function.Function

class HttpConfigsSpec extends Specification {


    def 'BasicResponse: success closure'() {
        setup:
        ChainedHttpConfig.ChainedResponse parent = Mock(ChainedHttpConfig.ChainedResponse)
        FromServer fromServer = Mock(FromServer)

        def response = new HttpConfigs.BasicResponse(parent)

        when:
        response.success { FromServer from -> 42 }

        then:
        response.success(fromServer) == 42

        and:
        response.when(200).call(fromServer) == 42
    }

    def 'BasicResponse: success function'() {
        setup:
        ChainedHttpConfig.ChainedResponse parent = Mock(ChainedHttpConfig.ChainedResponse)
        FromServer fromServer = Mock(FromServer)

        def response = new HttpConfigs.BasicResponse(parent)

        Function<FromServer, ?> function = new Function<FromServer, Integer>() {
            @Override
            Integer apply(FromServer from) {
                42
            }
        }

        when:
        response.success function

        then:
        response.success(fromServer) == 42

        and:
        response.when(200).call(fromServer) == 42
    }
}

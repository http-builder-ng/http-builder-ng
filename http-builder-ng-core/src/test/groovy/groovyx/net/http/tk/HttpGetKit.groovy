package groovyx.net.http.tk

import groovy.transform.TupleConstructor
import groovyx.net.http.HttpBuilder
import groovyx.net.http.HttpObjectConfig

import java.util.function.Function

/**
 * Created by cjstehno on 11/25/16.
 */
@TupleConstructor
class HttpGetKit {

    final Function<HttpObjectConfig, ? extends HttpBuilder> clientFactory



}

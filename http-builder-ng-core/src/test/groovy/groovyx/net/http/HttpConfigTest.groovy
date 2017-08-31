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
package groovyx.net.http

import spock.lang.Specification

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.function.BiConsumer
import java.util.function.BiFunction

import static groovy.lang.Closure.DELEGATE_FIRST

class HttpConfigTest extends Specification {

    def "Basic Config"() {
        setup:
        BiConsumer jsonEncoder = NativeHandlers.Encoders.&json;
        BiFunction jsonParser = NativeHandlers.Parsers.&json;

        HttpConfig http = chainedConfig(HttpConfigs.threadSafe()) {
            request.charset = StandardCharsets.UTF_8;
            request.contentType = "application/json";
            request.uri = 'http://www.google.com';
            request.body = [one: 1, two: 2];

            def JSON = ["application/json", "application/javascript", "text/javascript"];
            request.encoder JSON, jsonEncoder
            response.parser JSON, jsonParser
        }

        expect:
        http.request.encoder("application/json") == jsonEncoder;
        http.response.parser("text/javascript") == jsonParser;
        http.request.body == [one: 1, two: 2];
    }

    def Chaining() {
        setup:
        BiConsumer xmlEncoder = NativeHandlers.Encoders.&xml;
        BiFunction xmlParser = NativeHandlers.Parsers.&xml;
        Closure success = { res, o -> println(o); }
        Closure failure = { res -> println("failed"); }
        Closure on404 = { res -> println('why u 404?'); }

        def theBody = { ->
            root {
                person {
                    firstName 'Fred'
                    lastName 'Flinstone'
                };
            };
        };
        String contentType = 'application/xml';
        Charset charset = StandardCharsets.UTF_8;
        URI myURI = new URI('http://www.yahoo.com/likes/xml')
        def XML = ["application/xml", "text/xml", "application/xhtml+xml", "application/atom+xml"];

        def root = chainedConfig(HttpConfigs.threadSafe()) {
            request.charset = charset
            request.encoder XML, xmlEncoder
            response.success success;
            response.failure failure;
        };

        def intermediate = chainedConfig(HttpConfigs.threadSafe(root)) {
            request.contentType = contentType
            request.uri = myURI
            response.parser XML, xmlParser
        };

        def end = chainedConfig(HttpConfigs.basic(intermediate)) {
            request.body = theBody;
            response.when 404, on404;
        };

        expect:
        end.request.actualEncoder(contentType) == xmlEncoder;
        end.response.actualParser(contentType) == xmlParser;
        end.request.actualBody() == theBody;
        end.request.actualContentType() == contentType;
        end.request.actualCharset() == charset;
        end.request.uri.toURI() == myURI;

        end.response.actualAction(200).closure == success;
        end.response.actualAction(400).closure == failure;
        end.response.actualAction(404).closure == on404;
        intermediate.response.actualAction(404).closure == failure;
    }

    private ChainedHttpConfig chainedConfig(ChainedHttpConfig chc, @DelegatesTo(HttpConfig.class) final Closure closure) {
        closure.setDelegate(chc)
        closure.setResolveStrategy(DELEGATE_FIRST)
        closure.call()
        chc
    }
}

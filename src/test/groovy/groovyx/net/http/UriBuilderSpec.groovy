/*
 * Copyright (C) 2016 David Clark
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

import static groovyx.net.http.UriBuilder.basic
import static groovyx.net.http.UriBuilder.root;

class UriBuilderSpec extends Specification {

    def 'root yields empty URI'() {
        expect:
        root().toURI() == new URI('')
    }

    def 'builder with parent'(){
        given:
        def builderA = basic(root()).setFull('http://localhost:8080/info')

        expect:
        basic(builderA).toURI() == new URI('http://localhost:8080/info')

        and:
        basic(builderA).setPath('/bar').toURI() == new URI('http://localhost:8080/bar')
    }

    def 'basic from root'() {
        given:
        UriBuilder builder = basic(root())

        expect:
        builder.toURI() == new URI('')

        and:
        builder.setFull('http://localhost:10101').toURI() == new URI('http://localhost:10101')

        and:
        builder.setPath('/foo').toURI() == new URI('http://localhost:10101/foo')

        and:
        builder.setScheme('https').toURI() == new URI('https://localhost:10101/foo')

        and:
        builder.setPort(9191).toURI() ==  new URI('https://localhost:9191/foo')

        and:
        builder.setHost('nohost').toURI() ==  new URI('https://nohost:9191/foo')

        and:
        builder.setQuery(alpha:'1', bravo:100).toURI() ==  new URI('https://nohost:9191/foo?alpha=1&bravo=100')

        and:
        builder.setFragment('horse').toURI() ==  new URI('https://nohost:9191/foo?alpha=1&bravo=100#horse')

        and:
        builder.setUserInfo('dog').toURI() ==  new URI('https://dog@nohost:9191/foo?alpha=1&bravo=100#horse')
    }

    def 'threadSafe from root'() {
        given:
        UriBuilder builder = UriBuilder.threadSafe(root())

        expect:
        builder.toURI() == new URI('')

        and:
        builder.setFull('http://localhost:10101').toURI() == new URI('http://localhost:10101')

        and:
        builder.setPath('/foo').toURI() == new URI('http://localhost:10101/foo')

        and:
        builder.setScheme('https').toURI() == new URI('https://localhost:10101/foo')

        and:
        builder.setPort(9191).toURI() ==  new URI('https://localhost:9191/foo')

        and:
        builder.setHost('nohost').toURI() ==  new URI('https://nohost:9191/foo')

        and:
        builder.setQuery(alpha:'1', bravo:100).toURI() ==  new URI('https://nohost:9191/foo?bravo=100&alpha=1')

        and:
        builder.setFragment('horse').toURI() ==  new URI('https://nohost:9191/foo?bravo=100&alpha=1#horse')

        and:
        builder.setUserInfo('dog').toURI() ==  new URI('https://dog@nohost:9191/foo?bravo=100&alpha=1#horse')
    }
}
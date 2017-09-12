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

import com.stehno.ersatz.ContentType
import com.stehno.ersatz.Encoders
import com.stehno.ersatz.ErsatzServer
import spock.lang.Specification

import static com.stehno.ersatz.ContentType.TEXT_PLAIN
import static groovyx.net.http.UriBuilder.basic
import static groovyx.net.http.UriBuilder.root;

class UriBuilderSpec extends Specification {

    def 'root yields empty URI'() {
        expect:
        root().toURI() == new URI('')
    }

    def 'builder with parent'() {
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
        builder.setPort(9191).toURI() == new URI('https://localhost:9191/foo')

        and:
        builder.setHost('nohost').toURI() == new URI('https://nohost:9191/foo')

        and:
        builder.setQuery(alpha: '1', bravo: 100).toURI() == new URI('https://nohost:9191/foo?alpha=1&bravo=100')

        and:
        builder.setQuery(alpha: '1', bravo: [100, 200]).toURI() == new URI('https://nohost:9191/foo?alpha=1&bravo=100&bravo=200')

        and:
        builder.setFragment('horse').toURI() == new URI('https://nohost:9191/foo?alpha=1&bravo=100&bravo=200#horse')

        and:
        builder.setUserInfo('dog').toURI() == new URI('https://dog@nohost:9191/foo?alpha=1&bravo=100&bravo=200#horse')
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
        builder.setPort(9191).toURI() == new URI('https://localhost:9191/foo')

        and:
        builder.setHost('nohost').toURI() == new URI('https://nohost:9191/foo')

        and:
        builder.setQuery(alpha: '1', bravo: 100).toURI() == new URI('https://nohost:9191/foo?bravo=100&alpha=1')

        and:
        builder.setQuery(alpha: '1', bravo: [100, 300]).toURI() == new URI('https://nohost:9191/foo?bravo=100&bravo=300&alpha=1')

        and:
        builder.setFragment('horse').toURI() == new URI('https://nohost:9191/foo?bravo=100&bravo=300&alpha=1#horse')

        and:
        builder.setUserInfo('dog').toURI() == new URI('https://dog@nohost:9191/foo?bravo=100&bravo=300&alpha=1#horse')
    }

    def 'commas allowed in query string'() {
        given:
        UriBuilder tsafe = UriBuilder.threadSafe(root())
        UriBuilder tunsafe = basic(root())

        expect:
        tsafe.setFull('http://foo.com/endpoint').setQuery(ids: '1,2,3').toURI() == new URI('http://foo.com/endpoint?ids=1,2,3')
        tunsafe.setFull('http://foo.com/endpoint').setQuery(ids: '1,2,3').toURI() == new URI('http://foo.com/endpoint?ids=1,2,3')
    }

    def 'full uri with path'() {
        setup:
        UriBuilder builder = basic(root())

        when:
        builder.full = 'http://localhost:1234/something/else'

        then:
        builder.toURI() == 'http://localhost:1234/something/else'.toURI()

        when:
        builder.path = '/foo'

        then:
        builder.toURI() == 'http://localhost:1234/foo'.toURI()

        when:
        builder.path = '/foo%2Fbar'

        then:
        builder.toURI().toString() == 'http://localhost:1234/foo%2Fbar'

        when:
        builder.path = '/foo%2Fbar%25baz%26qux'

        then:
        builder.toURI() == 'http://localhost:1234/foo%2Fbar%25baz%26qux'.toURI()
    }

    def 'uri full specified with query string'() {
        setup:
        UriBuilder builder = basic(root())

        when:
        builder.full = 'http://test.com/a?b=c&d=e'
        URI uri = builder.toURI()

        then:
        uri == 'http://test.com/a?b=c&d=e'.toURI()
    }

    def 'uri full specified with duplicates in query string'() {
        setup:
        UriBuilder builder = basic(root())

        when:
        builder.full = 'http://test.com/a?b=c&d=e&b=f'
        URI uri = builder.toURI()

        then:
        uri == 'http://test.com/a?b=c&b=f&d=e'.toURI()
    }

    def 'uri with query in configuration and empty verb'() {
        setup:
        def server = new ErsatzServer({
            encoder TEXT_PLAIN, String, Encoders.text
            expects().get('/something').query('foo', 'bar').responds().content('ok', TEXT_PLAIN)
        })
        server.start()

        when:
        def http = JavaHttpBuilder.configure {
            request.uri = "${server.httpUrl}/something?foo=bar"
        }.get()

        then:
        http == 'ok'

        cleanup:
        server.stop()
    }
}

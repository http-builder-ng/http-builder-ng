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

import groovy.transform.Canonical
import org.jsoup.nodes.Document
import spock.lang.Specification

import java.time.ZonedDateTime
import java.util.function.Function

import static groovyx.net.http.ContentTypes.JSON
import static groovyx.net.http.FromServer.Header.find
import static groovyx.net.http.HttpBuilder.NO_OP
import static groovyx.net.http.HttpBuilder.configure
import static groovyx.net.http.HttpVerb.GET
import static java.time.format.DateTimeFormatter.ofPattern

/**
 * These are not really tests, but examples, therefore they should never fail and they only print out results.
 */
class ExamplesSpec extends Specification {

    def 'Resource Last Modified (HEAD)'() {
        when:
        Date lastModified = configure {
            request.uri = 'http://central.maven.org/maven2/org/codehaus/groovy/groovy-all/2.4.7/groovy-all-2.4.7.jar'
        }.head(Date) {
            response.success { FromServer resp ->
                String value = find(resp.headers, 'Last-Modified')?.value
                value ? Date.parse('EEE, dd MMM yyyy  H:mm:ss zzz', value) : null
            }
        }

        then:
        println "Groovy 2.4.7 (jar) was last modified on ${lastModified.format('MM/dd/yyyy HH:mm')}"
    }

    def 'Resource Last Modified (HEAD) - java.time'() {
        when:
        ZonedDateTime lastModified = configure {
            request.uri = 'http://central.maven.org/maven2/org/codehaus/groovy/groovy-all/2.4.7/groovy-all-2.4.7.jar'
        }.head(ZonedDateTime) {
            response.success { FromServer resp ->
                resp.headers.find { h-> h.key == 'Last-Modified' }?.parse(ofPattern('EEE, dd MMM yyyy  H:mm:ss zzz'))
            }
        }

        then:
        println "Groovy 2.4.7 (jar) was last modified on ${lastModified.format(ofPattern('MM/dd/yyyy HH:mm'))}"
    }

    def 'Scraping Web Content (GET)'() {
        when:
        Document page = configure {
            request.uri = 'https://mvnrepository.com/artifact/org.codehaus.groovy/groovy-all'
        }.get()

        String license = page.select('span.b.lic').collect { it.text() }.join(', ')

        then:
        println "Groovy is licensed under: ${license}"
    }

    def 'Sending/Receiving JSON Data (POST)'() {
        when:
        ItemScore itemScore = configure {
            request.uri = 'http://httpbin.org'
            request.contentType = JSON[0]
            response.parser(JSON[0]) { config, resp ->
                new ItemScore(NativeHandlers.Parsers.json(config, resp).json)
            }
        }.post(ItemScore) {
            request.uri.path = '/post'
            request.body = new ItemScore('ASDFASEACV235', 90786)
        }

        then:
        println "Your score for item (${itemScore.item}) was (${itemScore.score})."
    }

    def 'Using Interceptors'() {
        when:
        long elapsed = configure {
            request.uri = 'https://mvnrepository.com/artifact/org.codehaus.groovy/groovy-all'
            execution.interceptor(GET) { ChainedHttpConfig cfg, Function<ChainedHttpConfig, Object> fx ->
                long started = System.currentTimeMillis()
                fx.apply(cfg)
                System.currentTimeMillis() - started
            }
        }.get(Long, NO_OP)

        then:
        println "Elapsed time for request: $elapsed ms"
    }

    @Canonical
    static class ItemScore {
        String item
        Long score
    }
}

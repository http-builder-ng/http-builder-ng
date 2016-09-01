package groovyx.net.http

import org.jsoup.nodes.Document
import spock.lang.Specification

import static groovyx.net.http.FromServer.Header.find
import static groovyx.net.http.HttpBuilder.configure

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

    def 'Scraping Web Content (GET)'() {
        when:
        Document page = configure {
            request.uri = 'https://mvnrepository.com/artifact/org.codehaus.groovy/groovy-all'
        }.get()

        String license = page.select('span.b.lic').collect { it.text() }.join(', ')

        then:
        println "Groovy is licensed under: ${license}"
    }
}

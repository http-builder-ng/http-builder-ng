package groovyx.net.http.optional

import com.stehno.ersatz.ErsatzServer
import groovyx.net.http.HttpBuilder
import spock.lang.AutoCleanup
import spock.lang.Specification

import static com.stehno.ersatz.ContentType.TEXT_PLAIN
import static groovyx.net.http.optional.Csv.toCsv

class CsvSpec extends Specification {

    @AutoCleanup('stop') private final ErsatzServer ersatzServer = new ErsatzServer()

    // TODO: more testing needed here

    def "Robots.txt as CSV"() {
        setup:
        ersatzServer.expectations {
            get('/robots.txt').responds().content('User-agent: *\nDisallow: /deny\n', TEXT_PLAIN)
        }.start()

        when:
        def result = HttpBuilder.configure {
            request.uri = "${ersatzServer.httpUrl}/robots.txt"
            toCsv(delegate, 'text/plain', ':' as Character, null)
        }.get()

        then:
        result.size() == 2
        result[0][0] == 'User-agent'
        result[0][1].trim() == '*'
        result[1][0] == 'Disallow'
        result[1][1].trim() == '/deny'
    }
}

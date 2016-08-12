package groovyx.net.http;

import spock.lang.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

class ReaderInputStreamTest extends Specification {

    static String randomAscii(int max) {
        def randSize = ThreadLocalRandom.current().nextInt(max);
        StringBuilder sb = new StringBuilder(randSize);
        randSize.times {
            sb.append((char) (ThreadLocalRandom.current().nextInt(126-33) + 33));
        }
        
        sb.toString()
    }

    def "Basic Roundtrip"() {
        setup:
        def istream = new ReaderInputStream(new StringReader("foobarbaz"), StandardCharsets.UTF_8);
        def baos = new ByteArrayOutputStream();
        baos << istream;
        def str = new String(baos.toByteArray(), StandardCharsets.UTF_8);

        expect:
        str == "foobarbaz";
    }

    def "Something Large"() {
        setup:
        def text = ReaderInputStreamTest.classLoader.getResourceAsStream('rss_1_0_validator.xml').getText('UTF-8');
        def istream = new ReaderInputStream(new StringReader(text), StandardCharsets.UTF_8)
        def baos = new ByteArrayOutputStream();
        baos << istream;
        def str = new String(baos.toByteArray(), StandardCharsets.UTF_8);

        expect:
        str == text;
    }

    def "Something Random"() {
        setup:
        def text = randomAscii(4096);
        def istream = new ReaderInputStream(new StringReader(text), StandardCharsets.UTF_8);
        def baos = new ByteArrayOutputStream();
        baos << istream;
        def str = new String(baos.toByteArray(), StandardCharsets.UTF_8);
        
        expect:
        str == text;
    }
}

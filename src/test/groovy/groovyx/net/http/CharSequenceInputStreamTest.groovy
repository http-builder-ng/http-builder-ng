package groovyx.net.http;

import spock.lang.*;
import java.nio.charset.StandardCharsets;

class CharSequenceInputStreamTest extends Specification {

    def "Basic Roundtrip"() {
        setup:
        def istream = new CharSequenceInputStream("foobarbaz", StandardCharsets.UTF_8);
        def baos = new ByteArrayOutputStream();
        baos << istream;
        def str = new String(baos.toByteArray(), StandardCharsets.UTF_8);

        expect:
        str == "foobarbaz";
    }

    def "Something Large"() {
        setup:
        def text = CharSequenceInputStreamTest.classLoader.getResourceAsStream('rss_1_0_validator.xml').getText('UTF-8');
        def istream = new CharSequenceInputStream(text, StandardCharsets.UTF_8)
        def baos = new ByteArrayOutputStream();
        baos << istream;
        def str = new String(baos.toByteArray(), StandardCharsets.UTF_8);

        expect:
        str == text;
    }
}

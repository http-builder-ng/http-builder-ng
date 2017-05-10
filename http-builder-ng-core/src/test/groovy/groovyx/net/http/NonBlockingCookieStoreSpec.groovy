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
package groovyx.net.http;

import spock.lang.*;
import java.net.CookieStore;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import java.util.concurrent.Executor;

class NonBlockingCookieStoreSpec extends Specification {

    @Rule TemporaryFolder tmp;
    
    CookieManager defaultManager;
    CookieStore defaultStore;
    CookieStore fileBacked;
    
    CookieStore nonBlocking;
    CookieManager nonBlockingManager;
    CookieManager fileBackedManager;
    
    URI yahoo = new URI('http://www.yahoo.com');
    URI google = new URI('http://google.com');
    URI slashdot = new URI('http://slashdot.org');

    File directory;

    int theCount = 0;

    def addCount = { File f ->
        if(f.name.endsWith('.properties')) {
            ++theCount;
        }
    }

    static Executor executor = new Executor() {
        public void execute(Runnable r) {
            r.run();
        }
    }
    
    def setup() {
        defaultManager = new CookieManager();
        defaultManager.cookiePolicy = CookiePolicy.ACCEPT_ALL;
        defaultStore = defaultManager.cookieStore;
        
        nonBlocking = new NonBlockingCookieStore();
        nonBlockingManager = new CookieManager(nonBlocking, CookiePolicy.ACCEPT_ALL);

        directory = tmp.newFolder();
        fileBacked = new FileBackedCookieStore(directory, executor);
        fileBackedManager = new CookieManager(fileBacked, CookiePolicy.ACCEPT_ALL);
    }

    static List<HttpCookie> randomCookies(int num, Closure closure) {
        (0..<num).collect { count ->
            HttpCookie cookie = new HttpCookie("random${count}", UUID.randomUUID().toString());
            closure.setDelegate(cookie);
            closure.setResolveStrategy(Closure.DELEGATE_FIRST);
            closure.call();
            return cookie; };
    }

    final static CMP = { c1, c2 -> c1.name <=> c2.name; };

    def 'no path with domain'() {
        setup:
        [ yahoo, google ].each { uri ->
            randomCookies(10, { domain = uri.host;}).each { cookie ->
                defaultStore.add(uri, cookie);
                fileBacked.add(uri, cookie);
                nonBlocking.add(uri, cookie); }; };
        
        expect:
        println(defaultStore.get(yahoo).sort(CMP));
        println(nonBlocking.get(yahoo).sort(CMP));
        
        defaultStore.get(yahoo).sort(CMP) == nonBlocking.get(yahoo).sort(CMP);
        defaultStore.get(yahoo).sort(CMP) == fileBacked.get(yahoo).sort(CMP);
        
        defaultStore.get(google).sort(CMP) == nonBlocking.get(google).sort(CMP);
        defaultStore.get(yahoo).sort(CMP) == fileBacked.get(yahoo).sort(CMP);
        
        defaultStore.get(slashdot) == nonBlocking.get(slashdot);
        defaultStore.get(slashdot) == fileBacked.get(slashdot);
    }

    def 'no domain info'() {
        setup:
        randomCookies(10, { -> }).each { cookie ->
            [ defaultStore, nonBlocking, fileBacked ].each { store ->
                store.add(yahoo, cookie); }; };
        
        expect:
        defaultStore.get(yahoo).sort(CMP) == nonBlocking.get(yahoo).sort(CMP);
        defaultStore.get(yahoo).sort(CMP) == fileBacked.get(yahoo).sort(CMP);
        
        defaultStore.get(google).sort(CMP) == nonBlocking.get(google).sort(CMP);
        defaultStore.get(google).sort(CMP) == fileBacked.get(google).sort(CMP);
        
        defaultStore.get(slashdot) == nonBlocking.get(slashdot);
        defaultStore.get(slashdot) == fileBacked.get(slashdot);
    }

    def 'domain and path info'() {
        setup:
        def base = new URI('http://www.yahoo.com');
        def uri1 = new URI('http://www.yahoo.com/foo');
        def uri1Cookies = randomCookies(3, { domain = 'www.yahoo.com'; path = '/foo'; })
        def uri2 = new URI('http://www.yahoo.com/foo/bar');
        def uri2Cookies = randomCookies(3, { domain = 'www.yahoo.com'; path = '/foo/bar'; })
        def uri3 = new URI('http://www.yahoo.com/foo/bar/baz');
        def uri3Cookies = randomCookies(3, { domain = 'www.yahoo.com'; path = '/foo/bar/baz'; });
        
        [ uri1Cookies, uri2Cookies, uri3Cookies ].flatten().each { cookie ->
            defaultStore.add(base, cookie);
            nonBlocking.add(base, cookie);
            fileBacked.add(base, cookie); };
            
        expect:
        (uri1Cookies + uri2Cookies +  uri3Cookies).every { c ->
            String lookFor = "${c.name}=\"${c.value}\"";
            String found1 = defaultManager.get(uri3, [:])['Cookie'] as String;
            String found2 = nonBlockingManager.get(uri3, [:])['Cookie'] as String;
            String found3 = fileBackedManager.get(uri3, [:])['Cookie'] as String;
            (found1.indexOf(lookFor) != -1 &&
             found2.indexOf(lookFor) != -1 &&
             found3.indexOf(lookFor) != -1);
        };

        (uri1Cookies + uri2Cookies).every { c ->
            String lookFor = "${c.name}=\"${c.value}\"";
            String found1 = defaultManager.get(uri2, [:])['Cookie'] as String;
            String found2 = nonBlockingManager.get(uri2, [:])['Cookie'] as String;
            String found3 = fileBackedManager.get(uri2, [:])['Cookie'] as String;
            (found1.indexOf(lookFor) != -1 &&
             found2.indexOf(lookFor) != -1 &&
             found3.indexOf(lookFor) != -1);
        };

        (uri1Cookies).every { c ->
            String lookFor = "${c.name}=\"${c.value}\"";
            String found1 = defaultManager.get(uri1, [:])['Cookie'] as String;
            String found2 = nonBlockingManager.get(uri1, [:])['Cookie'] as String;
            String found3 = fileBackedManager.get(uri1, [:])['Cookie'] as String;
            (found1.indexOf(lookFor) != -1 &&
             found2.indexOf(lookFor) != -1 &&
             found3.indexOf(lookFor) != -1);
        }
    }

    def 'secured only'() {
        setup:
        URI secureURI = new URI('https://www.yahoo.com');
        randomCookies(10, { domain = yahoo.host; secure = true; }).each { cookie ->
            defaultStore.add((URI) null, cookie);
            nonBlocking.add((URI) null, cookie);
            fileBacked.add((URI) null, cookie);
        };

        expect:
        !defaultStore.get(yahoo);
        !nonBlocking.get(yahoo);
        !fileBacked.get(yahoo);
        defaultStore.get(secureURI);
        defaultStore.get(secureURI).sort(CMP) == nonBlocking.get(secureURI).sort(CMP);
        defaultStore.get(secureURI).sort(CMP) == fileBacked.get(secureURI).sort(CMP);
    }

    def 'get uris non blocking'() {
        //The default cookie store returns different values, but
        //the cookie manager doesn't appear to call it, so a different
        //implementation is fine for now.
        setup:
        [ yahoo, google ].each { uri ->
            randomCookies(10, { -> }).each { cookie ->
                nonBlocking.add(uri, cookie); }; };

        expect:
        nonBlocking.URIs.size() == 2;
    }

    def 'remove cookie non blocking'() {
        setup:
        def cookies = randomCookies(10, { domain = 'www.yahoo.com';});

        when:
        cookies.each { cookie -> nonBlocking.add((URI) null, cookie); }

        then:
        nonBlocking.cookies.size() == 10;

        when:
        nonBlocking.remove(null, cookies[0]);
        nonBlocking.remove(new URI('http://www.yahoo.com'), cookies[1]);
        nonBlocking.remove(null, cookies[2]);

        then:
        nonBlocking.cookies.size() == 7;

        when:
        nonBlocking.removeAll();

        then:
        !nonBlocking.cookies
    }

    def 'store on file system and retrieve'() {
        setup:
        def theCookies = randomCookies(20) { ->
            domain = yahoo.host;
            secure = true;
            comment = 'my comment';
            maxAge = 1000L;
            path = '/foo';
            discard = true;
            version = 1;
            commentURL = 'http://foo.com'
        }

        theCookies.each { cookie -> fileBacked.add((URI) null, cookie); };
        fileBacked.shutdown();

        def reRead = new FileBackedCookieStore(directory, executor);

        expect:
        reRead.cookies.size() == 20;
        reRead.cookies.every { cookie ->
            theCookies.find { c ->
                (c.name == cookie.name &&
                 c.domain == cookie.domain &&
                 c.value == cookie.value &&
                 c.secure &&
                 c.comment == 'my comment' &&
                 c.path == '/foo' &&
                 c.discard &&
                 c.version == 1 &&
                 c.commentURL == 'http://foo.com');
            }; };
    }

    def 'old spec'() {
        setup:
        [ yahoo, google ].each { uri ->
            randomCookies(10, { domain = uri.host; version = 0; }).each { cookie ->
                defaultStore.add((URI) null, cookie);
                fileBacked.add((URI) null, cookie);
                nonBlocking.add((URI) null, cookie); }; };
        
        expect:
        defaultStore.get(yahoo).sort(CMP) == nonBlocking.get(yahoo).sort(CMP);
        defaultStore.get(yahoo).sort(CMP) == fileBacked.get(yahoo).sort(CMP);
        
        defaultStore.get(google).sort(CMP) == nonBlocking.get(google).sort(CMP);
        defaultStore.get(yahoo).sort(CMP) == fileBacked.get(yahoo).sort(CMP);
        
        defaultStore.get(slashdot) == nonBlocking.get(slashdot);
        defaultStore.get(slashdot) == fileBacked.get(slashdot);
    }

    def 'remove all files'() {
        setup:
        def theCookies = randomCookies(20) { ->
            domain = yahoo.host;
            secure = true;
            comment = 'my comment';
            maxAge = 1000L;
            path = '/foo';
            discard = true;
            version = 1;
            commentURL = 'http://foo.com'
        }

        theCookies.each { cookie -> fileBacked.add((URI) null, cookie); };

        when:
        directory.listFiles().each(addCount);
        
        then:
        theCount == 20;

        when:
        theCount = 0;
        (0..<10).each { num -> fileBacked.remove((URI) null, theCookies[num]); }
        directory.listFiles().each(addCount);
        
        then:
        theCount == 10;
        
        when:
        theCount = 0;
        fileBacked.removeAll();
        directory.listFiles().each(addCount);

        then:
        theCount == 0;
    }

    def 'time outs'() {
        setup:
        def cookie = new HttpCookie('foo', 'bar');
        cookie.maxAge = 1L;
        cookie.domain = 'www.blah.com';
        
        nonBlocking.add((URI) null, cookie);
        fileBacked.add((URI) null, cookie);

        when:
        sleep(2000L);
        
        then:
        !nonBlocking.cookies
        !fileBacked.cookies;

        when:
        directory.listFiles().each(addCount);

        then:
        theCount == 0;
    }
}

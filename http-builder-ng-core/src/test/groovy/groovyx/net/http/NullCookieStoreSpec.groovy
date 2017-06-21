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
import java.net.URI;

class NullCookieStoreSpec extends Specification {

    def 'test that null cookie store does nothing'() {
        setup:
        def yahoo = new URI('http://www.yahoo.com');
        def yahooCookie = new HttpCookie('yahoocookie', '!!!')
        def fooCookie = new HttpCookie('foo', 'bar')
        
        def store = NullCookieStore.instance();
        store.add(null, fooCookie);
        store.add(yahoo, yahooCookie);
        
        expect:
        !store.get(yahoo);
        !store.cookies;
        !store.URIs;
        !store.remove(yahoo, yahooCookie);
        !store.removeAll();
    }
}

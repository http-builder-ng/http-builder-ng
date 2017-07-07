/**
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

import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.List;
import java.util.Collections;

class NullCookieStore implements CookieStore {

    private NullCookieStore() { }

    private static final NullCookieStore _instance = new NullCookieStore();

    public static NullCookieStore instance() {
        return _instance;
    }
    
    public void add(final URI uri, final HttpCookie cookie) {
        return;
    }

    public List<HttpCookie> get(final URI uri) {
        return Collections.emptyList();
    }

    public List<HttpCookie> getCookies() {
        return Collections.emptyList();
    }

    public List<URI> getURIs() {
        return Collections.emptyList();
    }

    public boolean remove(final URI uri, final HttpCookie cookie) {
        return false;
    }

    public boolean removeAll() {
        return false;
    }
}

/**
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
package groovyx.net.http;

import java.util.Map;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.List;
import java.util.Set;
import java.util.Iterator;
import java.util.AbstractSet;
import static java.util.Collections.unmodifiableSet;

public class Headers extends AbstractMap<String,String> {

    private final Set<Header> headers;
    
    public Headers(final Set<Header> headers) {
        this.headers = unmodifiableSet(headers);
    }

    //NOTE: HeaderIterator and EntrySet only exist to bridge the gap between
    //Entry<String,String> and Header. The return type of entrySet() should be
    //? extends Map<K,V>, but that might not even be possible in Java. I'm not
    //sufficiently versed in Java generics to know this (and frankly I don't care).
    private class HeaderIterator implements Iterator<Entry<String,String>> {
        final Iterator<Header> myIter = headers.iterator();
        public boolean hasNext() { return myIter.hasNext(); }
        public Entry<String,String> next() { return myIter.next(); }
    }

    private class EntrySet extends AbstractSet<Entry<String,String>> {
        public int size() { return headers.size(); }
        public Iterator<Entry<String,String>> iterator() { return new HeaderIterator(); }
    }

    public Set<Entry<String,String>> entrySet() {
        return new EntrySet();
    }

    public Set<Header> headerSet() {
        return headers;
    }

    public Object parsed(final String key) {
        for(Header h : headerSet()) {
            if(h.getKey().equals(key)) {
                return h.getParsed();
            }
        }

        return null;
    }
}

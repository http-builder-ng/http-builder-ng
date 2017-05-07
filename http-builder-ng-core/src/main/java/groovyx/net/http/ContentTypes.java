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

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

/**
 * Collection of Content-Type header values grouped together by their overall type. Generally the first element in the list is the most common
 * content type value for the type.
 */
public enum ContentTypes implements Iterable<String> {

    ANY("*/*"),
    TEXT("text/plain"),
    JSON("application/json", "application/javascript", "text/javascript"),
    XML("application/xml", "text/xml", "application/xhtml+xml", "application/atom+xml"),
    HTML("text/html"),
    URLENC("application/x-www-form-urlencoded"),
    BINARY("application/octet-stream"),
    MULTIPART_FORMDATA("multipart/form-data"),
    MULTIPART_MIXED("multipart/mixed");

    private final List<String> values;

    ContentTypes(final String... values) {
        this.values = unmodifiableList(asList(values));
    }

    public static ContentTypes fromValue(final String value) {
        for (ContentTypes type : ContentTypes.values()) {
            if (type.values.contains(value)) {
                return type;
            }
        }
        return null;
    }

    public String getAt(final int index){
        return values.get(index);
    }

    @Override public Iterator<String> iterator() {
        return values.iterator();
    }

    @Override public void forEach(Consumer<? super String> action) {
        values.forEach(action);
    }

    @Override public Spliterator<String> spliterator() {
        return values.spliterator();
    }
}

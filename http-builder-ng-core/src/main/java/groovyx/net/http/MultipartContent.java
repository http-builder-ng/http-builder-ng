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

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Collections.unmodifiableList;

/**
 * Multipart request content object used to define the multipart data. An example would be:
 *
 * [source,groovy]
 * ----
 * request.contentType = 'multipart/form-data'
 * request.body = multipart {
 *     field 'userid','someuser'
 *     file 'icon','user-icon.jpg', 'image/jpeg', imageFile
 * }
 * ----
 *
 * which would define a `multipart/form-data` request with a field part and a file part with the specified properties.
 */
public class MultipartContent {

    private final List<MultipartEntry> entries = new LinkedList<>();

    /**
     * Configures multipart request content using a Groovy closure (delegated to {@link MultipartContent}).
     *
     * @param closure the configuration closure
     * @return a configured instance of {@link MultipartContent}
     */
    public static MultipartContent multipart(@DelegatesTo(MultipartContent.class) Closure closure) {
        MultipartContent content = new MultipartContent();
        closure.setDelegate(content);
        closure.call();
        return content;
    }

    /**
     * Configures multipart request content using a {@link Consumer} which will have an instance of {@link MultipartContent} passed into it for
     * configuring the multipart content data.
     *
     * @param config the configuration {@link Consumer}
     * @return a configured instance of {@link MultipartContent}
     */
    public static MultipartContent multipart(final Consumer<MultipartContent> config) {
        MultipartContent content = new MultipartContent();
        config.accept(content);
        return content;
    }

    /**
     * Adds a text field part with the specified name and value.
     *
     * @param fieldName the field name
     * @param value the text value
     * @return a reference to this {@link MultipartContent} instance
     */
    public MultipartContent field(String fieldName, String value) {
        entries.add(new MultipartEntry(fieldName, null, null, value));
        return this;
    }

    /**
     * Adds a file part with the specified properties.
     *
     * @param fieldName the field name
     * @param fileName the file name
     * @param contentType the content type of the part
     * @param content the text content of the part
     * @return a reference to this {@link MultipartContent} instance
     */
    public MultipartContent file(String fieldName, String fileName, String contentType, String content) {
        entries.add(new MultipartEntry(fieldName, fileName, contentType, content));
        return this;
    }

    /**
     * Adds a file part with the specified properties.
     *
     * @param fieldName the field name
     * @param fileName the file name
     * @param contentType the content type of the part
     * @param content the content of the part
     * @return a reference to this {@link MultipartContent} instance
     */
    public MultipartContent file(String fieldName, String fileName, String contentType, Path content) {
        entries.add(new MultipartEntry(fieldName, fileName, contentType, content));
        return this;
    }

    /**
     * Adds a file part with the specified properties. The {@link InputStream} content will be read and transferred to the outgoing request body.
     *
     * @param fieldName the field name
     * @param fileName the file name
     * @param contentType the content type of the part
     * @param stream the content of the part as an {@link InputStream}
     * @return a reference to this {@link MultipartContent} instance
     */
    public MultipartContent file(String fieldName, String fileName, String contentType, InputStream stream){
        entries.add(new MultipartEntry(fieldName, fileName, contentType, stream));
        return this;
    }

    /**
     * Adds a file part with the specified properties.
     *
     * @param fieldName the field name
     * @param fileName the file name
     * @param contentType the content type of the part
     * @param content the content of the part
     * @return a reference to this {@link MultipartContent} instance
     */
    public MultipartContent file(String fieldName, String fileName, String contentType, byte[] content) {
        entries.add(new MultipartEntry(fieldName, fileName, contentType, content));
        return this;
    }

    Iterable<MultipartEntry> entries() {
        return unmodifiableList(entries);
    }

    /**
     * Represents a single multipart part.
     */
    static class MultipartEntry {

        private final String fieldName;
        private final String fileName;
        private final String contentType;
        private final Object content;

        private MultipartEntry(String fieldName, String fileName, String contentType, Object content) {
            this.fieldName = fieldName;
            this.fileName = fileName;
            this.contentType = contentType;
            this.content = content;
        }

        public boolean isField() {
            return fileName == null;
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getFileName() {
            return fileName;
        }

        public String getContentType() {
            return contentType;
        }

        public Object getContent() {
            return content;
        }
    }
}

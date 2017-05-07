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

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Collections.unmodifiableList;
import static groovyx.net.http.util.Misc.randomString;
/**
 * Multipart request content object used to define the multipart data. An example would be:
 *
 * [source,groovy]
 * ----
 * request.contentType = 'multipart/form-data'
 * request.body = multipart {
 *     field 'userid','someuser'
 *     part 'icon', 'user-icon.jpg', 'image/jpeg', imageFile
 * }
 * ----
 *
 * which would define a `multipart/form-data` request with a field part and a file part with the specified properties.
 */
public class MultipartContent {
    
    private final List<MultipartPart> entries = new LinkedList<>();
    private final String boundary = randomString(16);
    
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
     * Configures a field part with the given field name and value.
     *
     * @param fieldName the field name
     * @param value the value
     * @return a reference to this {@link MultipartContent} instance
     */
    public MultipartContent field(String fieldName, String value) {
        return part(fieldName, value);
    }

    /**
     * Configures a field part with the given field name and value (of the specified content type).
     *
     * @param fieldName the field name
     * @param contentType the value content type
     * @param value the value
     * @return a reference to this {@link MultipartContent} instance
     */
    public MultipartContent field(String fieldName, String contentType, String value) {
        return part(fieldName, contentType, value);
    }

    /**
     * Configures a field part with the given field name and value.
     *
     * @param fieldName the field name
     * @param value the value
     * @return a reference to this {@link MultipartContent} instance
     */
    public MultipartContent part(String fieldName, String value) {
        return part(fieldName, null, ContentTypes.TEXT.getAt(0), value);
    }

    /**
     * Configures a field part with the given field name and value (of the specified content type).
     *
     * @param fieldName the field name
     * @param contentType the value content type
     * @param value the value
     * @return a reference to this {@link MultipartContent} instance
     */
    public MultipartContent part(String fieldName, String contentType, String value) {
        return part(fieldName, null, contentType, value);
    }

    /**
     * Configures a file part with the specified properties. Encoders must be configured on the {@link HttpBuilder} to handle the content type
     * of each configured part.
     *
     * @param fieldName the field name
     * @param fileName the file name
     * @param contentType the part content type
     * @param content the part content (encoders must be configured)
     * @return a reference to this {@link MultipartContent} instance
     */
    public MultipartContent part(String fieldName, String fileName, String contentType, Object content) {
        entries.add(new MultipartPart(fieldName, fileName, contentType, content));
        return this;
    }

    /**
     * Iterates over the configured parts.
     *
     * @return an {@link Iterable} view of the configured parts.
     */
    Iterable<MultipartPart> parts() {
        return unmodifiableList(entries);
    }

    /**
     * Used to retrieve the multipart content boundary used by this content. Encoders may provide their own boundary implementation or use
     * the one provided by this method.
     *
     * @return a multipart boundary string for this content.
     */
    String boundary() {
        return boundary;
    }

    /**
     * Represents a single multipart part.
     */
    static class MultipartPart {

        private final String fieldName;
        private final String fileName;
        private final String contentType;
        private final Object content;

        private MultipartPart(String fieldName, String fileName, String contentType, Object content) {
            this.fieldName = fieldName;
            this.fileName = fileName;
            this.contentType = contentType != null ? contentType : ContentTypes.TEXT.getAt(0);
            this.content = content;
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

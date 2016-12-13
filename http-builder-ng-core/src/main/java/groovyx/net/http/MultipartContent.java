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
 * FIXME: document
 */
public class MultipartContent {

    private final List<MultipartEntry> entries = new LinkedList<>();

    public static MultipartContent multipart(@DelegatesTo(MultipartContent.class) Closure closure) {
        MultipartContent content = new MultipartContent();
        closure.setDelegate(content);
        closure.call();
        return content;
    }

    public static MultipartContent multipart(final Consumer<MultipartContent> config) {
        MultipartContent content = new MultipartContent();
        config.accept(content);
        return content;
    }

    public MultipartContent field(String fieldName, String value) {
        entries.add(new MultipartEntry(fieldName, null, null, value));
        return this;
    }

    public MultipartContent file(String fieldName, String fileName, String contentType, String content) {
        entries.add(new MultipartEntry(fieldName, fileName, contentType, content));
        return this;
    }

    public MultipartContent file(String fieldName, String fileName, String contentType, Path content) {
        entries.add(new MultipartEntry(fieldName, fileName, contentType, content));
        return this;
    }

    public MultipartContent file(String fieldName, String fileName, String contentType, InputStream content) {
        entries.add(new MultipartEntry(fieldName, fileName, contentType, content));
        return this;
    }

    public MultipartContent file(String fieldName, String fileName, String contentType, byte[] content) {
        entries.add(new MultipartEntry(fieldName, fileName, contentType, content));
        return this;
    }

    public Iterable<MultipartEntry> entries() {
        return unmodifiableList(entries);
    }

    public static class MultipartEntry {

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

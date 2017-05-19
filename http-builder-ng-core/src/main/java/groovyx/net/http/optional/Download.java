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
package groovyx.net.http.optional;

import groovyx.net.http.ChainedHttpConfig;
import groovyx.net.http.ContentTypes;
import groovyx.net.http.FromServer;
import groovyx.net.http.HttpConfig;
import groovyx.net.http.TransportingException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static groovyx.net.http.util.IoUtils.transfer;

/**
 * Helper methods used to assist in downloading remote content.
 *
 * [source,groovy]
 * ----
 * def http = HttpBuilder.configure {
 *     request.uri = 'http://example.com/download/foo.zip
 * }
 * File file = http.get { Download.toTempFile(delegate) }
 * ----
 *
 * The example will use a GET request to download the content as a temporary file and return the handle. The `delegate` property is a reference to the
 * `Closure` delegate, which in our case is an instance of `HttpConfig`.
 *
 * Any other configuration of the builder must be done in the `configure` closure.
 */
public class Download {

    private static final String ID = "LdazOKMfPTGymyyz5eLb/djgY3A=";

    /**
     * Downloads the content to a temporary file (*.tmp in the system temp directory).
     *
     * @param config the `HttpConfig` instance
     */
    public static void toTempFile(final HttpConfig config) {
        toTempFile(config, ContentTypes.ANY.getAt(0));
    }

    /**
     * Downloads the content to a temporary file (*.tmp in the system temp directory) with the specified content type.
     *
     * @param config the `HttpConfig` instance
     * @param contentType the content type
     */
    public static void toTempFile(final HttpConfig config, final String contentType) {
        try {
            toFile(config, contentType, File.createTempFile("tmp", ".tmp"));
        }
        catch(IOException ioe) {
            throw new TransportingException(ioe);
        }
    }

    /**
     * Downloads the content to a specified file.
     *
     * @param config the `HttpConfig` instance
     * @param file the file where content will be downloaded
     */
    public static void toFile(final HttpConfig config, final File file) {
        toFile(config, ContentTypes.ANY.getAt(0), file);
    }

    /**
     * Downloads the content to a specified file with the specified content type.
     *
     * @param config the `HttpConfig` instance
     * @param file the file where content will be downloaded
     * @param contentType the content type
     */
    public static void toFile(final HttpConfig config, final String contentType, final File file) {
        config.context(contentType, ID, file);
        config.getResponse().parser(contentType, Download::fileParser);
    }

    /**
     * Downloads the content into an `OutputStream`.
     *
     * @param config the `HttpConfig` instance
     * @param ostream the `OutputStream` to contain the content.
     */
    public static void toStream(final HttpConfig config, final OutputStream ostream) {
        toStream(config, ContentTypes.ANY.getAt(0), ostream);
    }

    /**
     * Downloads the content into an `OutputStream` with the specified content type.
     *
     * @param config the `HttpConfig` instance
     * @param ostream the `OutputStream` to contain the content.
     * @param contentType the content type
     */
    public static void toStream(final HttpConfig config, final String contentType, final OutputStream ostream) {
        config.context(contentType, ID, ostream);
        config.getResponse().parser(contentType, Download::streamParser);
    }

    private static File fileParser(final ChainedHttpConfig config, final FromServer fs) {
        try {
            final File file = (File) config.actualContext(fs.getContentType(), ID);
            transfer(fs.getInputStream(), new FileOutputStream(file), true);
            return file;
        } catch (IOException e) {
            throw new TransportingException(e);
        }
    }

    private static OutputStream streamParser(final ChainedHttpConfig config, final FromServer fs) {
        final OutputStream ostream = (OutputStream) config.actualContext(fs.getContentType(), ID);
        transfer(fs.getInputStream(), ostream, false);
        return ostream;
    }
}

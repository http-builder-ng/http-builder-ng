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
package groovyx.net.http.optional;

import groovyx.net.http.*;
import static groovyx.net.http.NativeHandlers.Parsers.transfer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Download {

    public static final String ID = "LdazOKMfPTGymyyz5eLb/djgY3A=";
    
    public static File fileParser(final ChainedHttpConfig config, final FromServer fs) {
        try {
            final File file = (File) config.actualContext(fs.getContentType(), ID);
            transfer(fs.getInputStream(), new FileOutputStream(file), true);
            return file;
        }
        catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static OutputStream streamParser(final ChainedHttpConfig config, final FromServer fs) {
        final OutputStream ostream = (OutputStream) config.actualContext(fs.getContentType(), ID);
        transfer(fs.getInputStream(), ostream, false);
        return ostream;
    }

    public static void toFile(final HttpConfig config, final String contentType, final File file) {
        config.context(contentType, ID, file);
        config.getResponse().parser(contentType, Download::fileParser);
    }

    public static void toTempFile(final HttpConfig config, final String contentType) {
        try {
            toFile(config, contentType, File.createTempFile("tmp", ".tmp"));
        }
        catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void toFile(final HttpConfig config, final File file) {
        toFile(config, ContentTypes.ANY.getAt(0), file);
    }

    public static void toTempFile(final HttpConfig config) {
        try {
            toFile(config, File.createTempFile("tmp", ".tmp"));
        }
        catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void toStream(final HttpConfig config, final String contentType, final OutputStream ostream) {
        config.context(contentType, ID, ostream);
        config.getResponse().parser(contentType, Download::streamParser);
    }

    public static void toStream(final HttpConfig config, final OutputStream ostream) {
        toStream(config, ContentTypes.ANY.getAt(0), ostream);
    }
}

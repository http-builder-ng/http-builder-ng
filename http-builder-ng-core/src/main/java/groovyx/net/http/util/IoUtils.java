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
package groovyx.net.http.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Shared IO utility operations.
 */
public class IoUtils {

    /**
     * Reads all bytes from the stream into a byte array.
     *
     * @param inputStream the {@link InputStream}
     * @return the array of bytes from the stream
     * @throws IOException if there is a problem reading the stream
     */
    public static byte[] streamToBytes(final InputStream inputStream) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            final byte[] bytes = new byte[2_048];

            int read;
            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }

            return outputStream.toByteArray();

        } finally {
            inputStream.close();
        }
    }
}


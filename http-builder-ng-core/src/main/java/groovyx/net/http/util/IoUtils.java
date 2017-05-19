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
package groovyx.net.http.util;

import groovyx.net.http.TransportingException;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
        return streamToBytes(inputStream, true);
    }

    public static byte[] streamToBytes(final InputStream inputStream, boolean close) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            final byte[] bytes = new byte[2_048];

            int read;
            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }

            return outputStream.toByteArray();

        } finally {
            if (close) inputStream.close();
        }
    }

    /**
     * Safely copies the contents of the {@link BufferedInputStream} to a {@link String} and resets the stream.
     * This method is generally only useful for testing and logging purposes.
     *
     * @param inputStream the BufferedInputStream
     * @return a String copy of the stream contents
     * @throws IOException           if something goes wrong with the stream
     * @throws IllegalStateException if the stream cannot be reset
     */
    public static String copyAsString(final BufferedInputStream inputStream) throws IOException, IllegalStateException {
        if (inputStream == null) return null;

        try {
            inputStream.mark(Integer.MAX_VALUE);
            return new String(streamToBytes(inputStream, false));
        } finally {
            try {
                inputStream.reset();
            } catch (IOException ioe) {
                throw new IllegalStateException("Unable to reset stream - original stream content may be corrupted");
            }
        }
    }

    /**
     * Transfers the contents of the {@link InputStream} into the {@link OutputStream}, optionally closing the stream.
     *
     * @param istream the input stream
     * @param ostream the output stream
     * @param close   whether or not to close the output stream
     */
    public static void transfer(final InputStream istream, final OutputStream ostream, final boolean close) {
        try {
            final byte[] bytes = new byte[2_048];
            int read;
            while ((read = istream.read(bytes)) != -1) {
                ostream.write(bytes, 0, read);
            }
        } catch (IOException e) {
            throw new TransportingException(e);
        } finally {
            if (close) {
                try {
                    ostream.close();
                } catch (IOException ioe) {
                    throw new TransportingException(ioe);
                }
            }
        }
    }
}


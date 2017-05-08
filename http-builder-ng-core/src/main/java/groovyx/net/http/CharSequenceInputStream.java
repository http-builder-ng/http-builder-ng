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

import java.io.InputStream;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

public class CharSequenceInputStream extends InputStream {

    private final Charset charset;
    private final CharsetEncoder encoder;
    private final CharBuffer inputBuffer;
    private final ByteBuffer outputBuffer;
    private CoderResult lastResult = CoderResult.OVERFLOW;
    private int written = 0;

    public CharSequenceInputStream(final CharSequence sequence, final Charset charset) {
        this.charset = charset;
        this.encoder = charset.newEncoder();
        this.inputBuffer = CharBuffer.wrap(sequence);
        this.outputBuffer = ByteBuffer.wrap(new byte[1_024]);
        encodeNext();
    }

    @Override
    public int available() throws IOException {
        final int estimated = inputBuffer.capacity() * (int) encoder.averageBytesPerChar();
        return Math.max(estimated - written, 0);
    }

    @Override
    public void close() throws IOException { }

    private void encodeNext() {
        if(lastResult == null) {
            return;
        }
        
        outputBuffer.clear();
        if(CoderResult.OVERFLOW == lastResult) {
            lastResult = encoder.encode(inputBuffer, outputBuffer, false);
        }
        else if(CoderResult.UNDERFLOW == lastResult) {
            encoder.encode(inputBuffer, outputBuffer, true);
            encoder.flush(outputBuffer);
            lastResult = null;
        }

        outputBuffer.flip();
    }

    private void ensure() {
        if(outputBuffer.remaining() == 0 && lastResult != null) {
            encodeNext();
        }
    }

    @Override
    public int read() throws IOException {
        ensure();
        if(outputBuffer.remaining() == 0) {
            return -1;
        }

        ++written;
        return outputBuffer.get();
    }

    @Override
    public int read(final byte[] ary) {
        return read(ary, 0, ary.length);
    }

    @Override
    public int read(final byte[] ary, final int offset, final int length) {
        ensure();
        if(outputBuffer.remaining() == 0) {
            return -1;
        }

        int total = 0;
        int index = offset;
        int leftAry = length;
        while(outputBuffer.remaining() > 0 && leftAry > 0) {
            final int toCopy = Math.min(outputBuffer.remaining(), leftAry);
            outputBuffer.get(ary, index, toCopy);
            index += toCopy;
            leftAry -= toCopy;
            total += toCopy;
            
            if(leftAry > 0) {
                encodeNext();
            }
        }

        written += total;
        return total;
    }
}

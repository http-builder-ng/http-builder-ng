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

import java.io.Reader;
import java.io.InputStream;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

public class ReaderInputStream extends InputStream {

    private final Charset charset;
    private final Reader reader;
    private final CharBuffer inputBuffer;
    private final ByteBuffer outputBuffer;
    private final CharsetEncoder encoder;
    private int lastRead = 0;
    private boolean finished = false;
    
    public ReaderInputStream(final Reader reader, final Charset charset) throws IOException {
        this.reader = reader;
        this.charset = charset;
        this.encoder = charset.newEncoder();
        this.inputBuffer = CharBuffer.wrap(new char[1_024]);
        this.inputBuffer.flip();
        this.outputBuffer = ByteBuffer.wrap(new byte[1_024 * (int) encoder.averageBytesPerChar()]);
        this.outputBuffer.flip();
        readOp();
    }

    private void encodeOp() {
        if(lastRead == -1) {
            encoder.encode(inputBuffer, outputBuffer, true);
            if(CoderResult.UNDERFLOW == encoder.flush(outputBuffer)) {
                finished = true;
            }
        }
        else {
            encoder.encode(inputBuffer, outputBuffer, false);
        }

        outputBuffer.flip();
    }
    
    private void readOp() throws IOException {
        //case #1, input buffer is exhausted, output buffer is exhausted
        if(inputBuffer.remaining() == 0 && outputBuffer.remaining() == 0) {
            inputBuffer.clear();
            outputBuffer.clear();
            lastRead = reader.read(inputBuffer);
            inputBuffer.flip();
            encodeOp();
            return;
        }
        
        //case #2, input buffer has remaining, output buffer needs compaction and re-encoding
        if(inputBuffer.remaining() > 0) {
            outputBuffer.compact();
            outputBuffer.flip();
            encodeOp();
            return;
        }
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    private void ensure() throws IOException {
        if(outputBuffer.remaining() == 0 && !finished) {
            readOp();
        }
    }

    @Override
    public int read() throws IOException {
        ensure();
        if(outputBuffer.remaining() == 0) {
            return -1;
        }

        return outputBuffer.get();
    }

    @Override
    public int read(final byte[] ary) throws IOException {
        return read(ary, 0, ary.length);
    }

    @Override
    public int read(final byte[] ary, final int offset, final int length) throws IOException {
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
                readOp();
            }
        }

        return total;
    }
}

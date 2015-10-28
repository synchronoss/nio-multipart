/*
 * Copyright (C) 2015 Synchronoss Technologies
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.synchronoss.cloud.nio.multipart.buffer;

import java.io.ByteArrayOutputStream;

/**
 * <p>
 *     A {@link ByteArrayOutputStream} with a limited capacity.
 *     If the data about to be written does not fir the remaining size an {@link IllegalStateException} will be thrown
 * </p>
 *
 * @author Silvano Riz.
 */
public class FixedSizeByteArrayOutputStream extends ByteArrayOutputStream {

    volatile private int remaining;

    /**
     * <p>
     *     Constructs a {@link ByteArrayOutputStream} that has a maximum memory footprint.
     * </p>
     *
     * @param maxSize The max size in bytes.
     */
    public FixedSizeByteArrayOutputStream(final int maxSize) {
        super(maxSize);
        this.remaining = maxSize;
    }

    @Override
    public void write(int b) {
        if (remaining < 1){
            throw new IllegalStateException("Cannot write. Output Stream is full. OutputStream size: " + super.buf.length);
        }
        super.write(b);
        remaining--;
    }

    @Override
    public void write(byte[] b, int off, int len) {
        if (remaining < len){
            throw new IllegalStateException("Cannot write. Not enough space in the OutputStream. Available space " + remaining + "/" + super.buf.length);
        }
        super.write(b, off, len);
        remaining = remaining - len;
    }

    @Override
    public synchronized void reset() {
        super.reset();
        remaining = super.buf.length;
    }
}

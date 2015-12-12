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

package org.synchronoss.cloud.nio.multipart.io;

import java.io.ByteArrayOutputStream;

/**
 * <p> A {@code ByteArrayOutputStream} with a limited capacity.
 *     If the data about to be written does not fit the remaining capacity an {@code IllegalStateException} will be thrown
 *
 *
 * @author Silvano Riz.
 */
public class FixedSizeByteArrayOutputStream extends ByteArrayOutputStream {

    volatile private int remaining;

    /**
     * <p> Constructor.
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

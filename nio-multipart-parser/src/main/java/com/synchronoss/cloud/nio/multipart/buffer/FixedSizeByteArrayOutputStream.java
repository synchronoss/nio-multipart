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
import java.io.IOException;

/**
 * <p>
 *     A {@link ByteArrayOutputStream} with a limited capacity.
 * </p>
 * @author Silvano Riz.
 */
public class FixedSizeByteArrayOutputStream extends ByteArrayOutputStream {

    private final int maxSize;

    public FixedSizeByteArrayOutputStream(final int maxSize) {
        super(maxSize);
        this.maxSize = maxSize;
    }

    @Override
    public void write(int b) {
        if (size() >= maxSize){
            throw new IllegalStateException("Output Stream is full. Size: " + maxSize);
        }
        super.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        if (len - off > maxSize - size()){
            throw new IllegalStateException("Data too long. It cannot be written to the stream. Size: " + maxSize);
        }
        super.write(b, off, len);
    }

    @Override
    public void write(byte[] b) throws IOException {
        if (b.length > maxSize - size()){
            throw new IllegalStateException("Data too long. It cannot be written to the stream. Size: " + maxSize);
        }
        super.write(b);
    }
}

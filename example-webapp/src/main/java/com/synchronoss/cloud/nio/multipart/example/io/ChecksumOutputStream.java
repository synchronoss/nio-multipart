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
package com.synchronoss.cloud.nio.multipart.example.io;

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;

/**
 * @author Silvano Riz.
 */
public class ChecksumOutputStream extends OutputStream {

    final MessageDigest digest;
    final OutputStream target;
    long writtenBytes = 0;

    public ChecksumOutputStream(final OutputStream target, final String algorithm) {
        try{
            this.digest = MessageDigest.getInstance(algorithm);
            this.target = target;
        }catch (Exception e){
            throw new IllegalArgumentException("Cannot create the ChecksumOutputStream", e);
        }

    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        target.write(b, off, len);
        digest.update(b, off, len);
        writtenBytes +=len-off;
    }

    @Override
    public void write(int b) throws IOException {
        target.write(b);
        digest.update((byte)b);
        writtenBytes++;
    }

    @Override
    public void flush() throws IOException {
        target.flush();
    }

    @Override
    public void close() throws IOException {
        target.close();
    }

    public byte[] getDigest(){
        return digest.digest();
    }

    public long getWrittenBytes(){
        return writtenBytes;
    }

}
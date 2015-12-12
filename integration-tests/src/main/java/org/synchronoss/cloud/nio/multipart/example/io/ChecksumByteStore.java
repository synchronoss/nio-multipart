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

package org.synchronoss.cloud.nio.multipart.example.io;

import org.synchronoss.cloud.nio.multipart.io.DeferredFileByteStore;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

/**
 * <p> Extension of {@code DeferredFileByteStore} that is capable of computing the checksum and size of the data going through the streams.
 *
 * @author Silvano Riz
 */
public class ChecksumByteStore extends DeferredFileByteStore {

    final MessageDigest digest;
    long writtenBytes = 0;

    public ChecksumByteStore(final File file, int threshold, final boolean purgeFileAfterReadComplete, final String checksumAlgorithm) {
        super(file, threshold, purgeFileAfterReadComplete);
        try {
            this.digest = MessageDigest.getInstance(checksumAlgorithm);
        }catch (Exception e){
            throw new IllegalStateException("Error creating an instance of ChecksumByteStore", e);
        }
    }

    @Override
    public void write(int b) throws IOException {
        super.write(b);
        digest.update((byte)b);
        writtenBytes++;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        super.write(b, off, len);
        digest.update(b, off, len);
        writtenBytes += len;
    }

    @Override
    public void write(byte[] b) throws IOException {
        super.write(b);
        digest.update(b);
        writtenBytes += b.length;
    }

    @Override
    public void flush() throws IOException {
        super.flush();
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

    @Override
    public InputStream getInputStream() {
        InputStream inputStream = super.getInputStream();
        return new ChecksumInputStream(inputStream, digest.getAlgorithm());
    }

    public byte[] getChecksum(){
        return digest.digest();
    }

    public long getWrittenBytes(){
        return writtenBytes;
    }

    public static class ChecksumInputStream extends InputStream {

        final MessageDigest digest;
        final InputStream target;
        int readBytes = 0;

        public ChecksumInputStream(final InputStream target, final String algorithm) {
            try{
                this.digest = MessageDigest.getInstance(algorithm);
                this.target = target;
            }catch (Exception e){
                throw new IllegalArgumentException("Cannot create the ChecksumInputStream", e);
            }

        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int read = target.read(b, off, len);
            if (read !=-1 ) {
                digest.update(b, off, read);
                readBytes += read;
            }
            return read;
        }

        @Override
        public int read() throws IOException {
            int read = target.read();
            if (read !=-1 ) {
                digest.update((byte) read);
                readBytes++;
            }
            return read;
        }

        @Override
        public long skip(long n) throws IOException {
            return target.skip(n);
        }

        @Override
        public int available() throws IOException {
            return target.available();
        }

        @Override
        public void close() throws IOException {
            target.close();
        }

        @Override
        public synchronized void mark(int readlimit) {
            target.mark(readlimit);
        }

        @Override
        public synchronized void reset() throws IOException {
            target.reset();
        }

        @Override
        public boolean markSupported() {
            return target.markSupported();
        }

        public byte[] getChecksum(){
            return digest.digest();
        }

        public long getReadBytes(){
            return readBytes;
        }

    }

}

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

import com.synchronoss.cloud.nio.multipart.example.io.ChecksumByteStore.ChecksumInputStream;
import com.synchronoss.cloud.nio.multipart.util.IOUtils;
import java.io.InputStream;

/**
 * @author Silvano Riz.
 */
public class ChecksumStreamUtils {

    private static final char[] hexDigits = "0123456789abcdef".toCharArray();

    public static String digestAsHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            sb.append(hexDigits[(b >> 4) & 0xf]).append(hexDigits[b & 0xf]);
        }
        return sb.toString();
    }

    public static class ChecksumAndReadBytes{
        final String checksum;
        final long readBytes;

        public ChecksumAndReadBytes(String checksum, long readBytes) {
            this.checksum = checksum;
            this.readBytes = readBytes;
        }

        public String getChecksum() {
            return checksum;
        }

        public long getReadBytes() {
            return readBytes;
        }
    }

    public static ChecksumAndReadBytes computeChecksumAndReadBytes(final ChecksumInputStream checksumInputStream) {
        try {
            byte[] buffer = new byte[5000];
            while (-1 != checksumInputStream.read(buffer)) {
                // Do nothing...
            }
            return new ChecksumAndReadBytes(digestAsHexString(checksumInputStream.getChecksum()), checksumInputStream.getReadBytes());
        }catch (Exception e){
            throw new IllegalStateException("Unable to compute the hash for of the part input stream", e);
        }finally {
            IOUtils.closeQuietly(checksumInputStream);
        }
    }

    public static ChecksumAndReadBytes computeChecksumAndReadBytes(final InputStream inputStream) {
        if (inputStream instanceof ChecksumInputStream) {
            return computeChecksumAndReadBytes((ChecksumInputStream)inputStream);
        }else{
            throw new IllegalStateException("Input stream is not a ChecksumInputStream");
        }
    }

    public static ChecksumAndReadBytes computeChecksumAndReadBytes(final InputStream inputStream, final String algorithm) {
        ChecksumInputStream checksumInputStream = new ChecksumInputStream(inputStream, algorithm);
        return computeChecksumAndReadBytes(checksumInputStream);
    }

}

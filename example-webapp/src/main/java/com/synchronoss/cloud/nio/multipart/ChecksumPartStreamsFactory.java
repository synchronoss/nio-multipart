/*
 * Copyright 2015 Synchronoss Technologies
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

package com.synchronoss.cloud.nio.multipart;

import org.apache.commons.io.IOUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *     Extension of {@link DefaultPartStreamsFactory} that provides {@link com.synchronoss.cloud.nio.multipart.PartStreamsFactory.PartStreams} capable of computing a data hash.
 *     This class is an example of how the NIO Multipart parser can be extended via a custom {@link PartStreamsFactory} and it is also used for testing purposes inside the example-webapp
 * </p>
 * @author Silvano Riz.
 */
public class ChecksumPartStreamsFactory extends DefaultPartStreamsFactory {

    final String checksumAlgorithm;

    public ChecksumPartStreamsFactory(String tempFolderPath, int maxSizeThreshold, final String checksumAlgorithm) {
        super(tempFolderPath, maxSizeThreshold);
        this.checksumAlgorithm = checksumAlgorithm;
    }

    public ChecksumPartStreamsFactory(String tempFolderPath, final String checksumAlgorithm) {
        super(tempFolderPath);
        this.checksumAlgorithm = checksumAlgorithm;
    }

    public ChecksumPartStreamsFactory(int maxSizeThreshold, final String checksumAlgorithm) {
        super(maxSizeThreshold);
        this.checksumAlgorithm = checksumAlgorithm;
    }

    public ChecksumPartStreamsFactory(final String checksumAlgorithm) {
        super();
        this.checksumAlgorithm = checksumAlgorithm;
    }

    @Override
    public PartStreams newPartStreams(Map<String, List<String>> headers, int partIndex) {
        try{
            final PartStreams partStreams = super.newPartStreams(headers, partIndex);
            return new ChecksumPartStreams(partStreams, checksumAlgorithm);
        }catch (Exception e){
            throw new IllegalStateException("Unable to create the temporary file where to store the body", e);
        }
    }

    /**
     * {@link PartStreams} that returns {@link ChecksumOutputStream} and {@link ChecksumInputStream}
     */
    public static class ChecksumPartStreams implements PartStreams {

        private static final char[] hexDigits = "0123456789abcdef".toCharArray();
        final String checksumAlgorithm;
        final PartStreams wrapped;

        String outputStreamDigest = null;
        Long outputStreamWrittenBytes = null;
        String inputStreamDigest = null;
        Long inputStreamReadBytes = null;
        ChecksumOutputStream servedOutputStream = null;

        public ChecksumPartStreams(final PartStreams wrapped, final String checksumAlgorithm) throws FileNotFoundException {
            this.wrapped = wrapped;
            this.checksumAlgorithm = checksumAlgorithm;
        }

        @Override
        public OutputStream getPartOutputStream() {
            servedOutputStream = new ChecksumOutputStream(wrapped.getPartOutputStream(), checksumAlgorithm);
            return servedOutputStream;

        }

        @Override
        public InputStream getPartInputStream() {
           return new ChecksumInputStream(wrapped.getPartInputStream(), checksumAlgorithm);
        }

        public synchronized String getOutputStreamDigest(){

            if (outputStreamDigest != null){
                return outputStreamDigest;
            }

            if (servedOutputStream == null){
                throw new IllegalStateException("Cannot compute the output stream digest because the output stream has not been served yet");
            }

            outputStreamDigest = digestAsHexString(servedOutputStream.getDigest());
            return outputStreamDigest;
        }

        public synchronized long getOutputStreamWrittenBytes(){
            if (outputStreamWrittenBytes != null){
                return outputStreamWrittenBytes;
            }

            if (servedOutputStream == null){
                throw new IllegalStateException("Cannot compute the output stream written bytes because the output stream has not been served yet");
            }

            outputStreamWrittenBytes = servedOutputStream.getWrittenBytes();
            return outputStreamWrittenBytes;
        }

        public synchronized String getInputStreamDigest(){

            if (inputStreamDigest != null){
                return inputStreamDigest;
            }

            if (servedOutputStream == null){
                throw new IllegalStateException("Cannot compute the input stream digest because the output stream has not been served yet");
            }

            computeInputStreamDigestAndReadBytes();
            return inputStreamDigest;
        }

        public synchronized long getInputStreamReadBytes(){

            if (inputStreamReadBytes != null){
                return inputStreamReadBytes;
            }

            if (servedOutputStream == null){
                throw new IllegalStateException("Cannot compute the input stream digest because the output stream has not been served yet");
            }

            computeInputStreamDigestAndReadBytes();
            return inputStreamReadBytes;
        }

        private void computeInputStreamDigestAndReadBytes(){

            final ChecksumInputStream inputStream = (ChecksumInputStream)getPartInputStream();
            try {
                byte[] buffer = new byte[5000];
                while (-1 != inputStream.read(buffer)){
                    // Do nothing...
                }
                inputStreamDigest = digestAsHexString(inputStream.getDigest());
                inputStreamReadBytes = inputStream.getReadBytes();

            }catch (Exception e){
                throw new IllegalStateException("Unable to compute the hash for of the part input stream", e);
            }finally {
                IOUtils.closeQuietly(inputStream);
            }
        }

        public static String digestAsHexString(byte[] bytes) {
            StringBuilder sb = new StringBuilder(2 * bytes.length);
            for (byte b : bytes) {
                sb.append(hexDigits[(b >> 4) & 0xf]).append(hexDigits[b & 0xf]);
            }
            return sb.toString();
        }


    }

    /**
     * {@link OutputStream} that computes the number of bytes written and the checksum
     */
    public static class ChecksumOutputStream extends OutputStream {

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

    /**
     * {@link InputStream} that computes the number of bytes read and the checksum
     */
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

        public byte[] getDigest(){
            return digest.digest();
        }

        public long getReadBytes(){
            return readBytes;
        }

    }

}

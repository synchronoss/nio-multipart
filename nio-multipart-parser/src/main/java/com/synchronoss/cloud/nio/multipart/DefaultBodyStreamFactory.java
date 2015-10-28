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

package com.synchronoss.cloud.nio.multipart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * <p>
 *     The {@link DefaultBodyStreamFactory}.
 * </p>
 * @author Silvano Riz.
 */
public class DefaultBodyStreamFactory implements BodyStreamFactory {

    private static final Logger log = LoggerFactory.getLogger(DefaultBodyStreamFactory.class);

    public static final int DEFAULT_MAX_THRESHOLD = 10240;//10kb

    final File tempFolder;
    final int maxSizeThreshold;

    /**
     * <p>
     *     Constructor.
     * </p>
     *
     * @param tempFolderPath The path where to store the temporary files
     * @param maxSizeThreshold The maximum amount of bytes that will be kept in memory for each part. If zero or negative no memory will be used.
     */
    public DefaultBodyStreamFactory(final String tempFolderPath, final int maxSizeThreshold) {
        this.maxSizeThreshold = maxSizeThreshold > 0 ? maxSizeThreshold : 0;
        tempFolder = new File(tempFolderPath);
        if (!tempFolder.exists()){
            if (!tempFolder.mkdirs()){
                throw new IllegalStateException("Unable to create the temporary folder where to store the temporary files of the nio multipart parser");
            }
        }
        if(log.isDebugEnabled())log.debug("Temporary folder: " + tempFolder.getAbsolutePath());
    }

    /**
     * <p>
     *     Constructor tha uses a default threshold of 10kb.
     * </p>
     * @param tempFolderPath The path where to store the temporary files
     */
    public DefaultBodyStreamFactory(final String tempFolderPath) {
        this(tempFolderPath, DEFAULT_MAX_THRESHOLD);
    }

    /**
     * <p>
     *     Constructor tha uses a default default folder ${java.io.tmpdir}/nio-file-upload
     * </p>
     * @param maxSizeThreshold The maximum amount of bytes that will be kept in memory for each part.
     */
    public DefaultBodyStreamFactory(int maxSizeThreshold) {
        this(System.getProperty("java.io.tmpdir") + "/nio-file-upload", maxSizeThreshold);
    }

    /**
     * <p>
     *     Constructor that uses a default threshold of 10kb and a default folder ${java.io.tmpdir}/nio-file-upload
     * </p>
     */
    public DefaultBodyStreamFactory() {
        this(System.getProperty("java.io.tmpdir") + "/nio-file-upload", DEFAULT_MAX_THRESHOLD);
    }

    @Override
    public NamedOutputStreamHolder getOutputStream(final Map<String, List<String>> headers, int partIndex) {
        try {

            final long contentLength = MultipartUtils.getContentLength(headers);
            final String name = String.format("nio-body-%d-%s.tmp", partIndex, UUID.randomUUID().toString());
            final File tempFile = new File(tempFolder, name);
            final PartBodyOutputStream partBodyOutputStream;
            if (contentLength != -1 && contentLength > maxSizeThreshold){
                // Go directly to file, we already know the size is going to be greater than the threshold
                if (log.isDebugEnabled()) log.debug("Size " + contentLength + ". Use temp file");
                partBodyOutputStream = new PartBodyOutputStream(tempFile, 0);
            }else {
                // Use a deferred file output stream. File will be created just if data goes over the threshold
                if (log.isDebugEnabled()) log.debug("Size " + contentLength + ". Use deferred file");
                partBodyOutputStream = new PartBodyOutputStream(tempFile, maxSizeThreshold);
            }
            return new NamedOutputStreamHolder(name, partBodyOutputStream);

        }catch (Exception e){
            throw new IllegalStateException("Unable to create the temporary file where to store the body", e);
        }
    }

    @Override
    public InputStream getInputStream(final NamedOutputStreamHolder namedOutputStreamHolder) {
        try {
            return ((PartBodyOutputStream)namedOutputStreamHolder.getOutputStream()).getInputStream();
        }catch (Exception e){
            throw new IllegalStateException("Unable to create input stream for: " + namedOutputStreamHolder.getName(), e);
        }
    }

    static class PartBodyOutputStream extends OutputStream {

        ByteArrayOutputStream byteArrayOutputStream;
        FileOutputStream fileOutputStream;
        boolean isInMemory = true;
        final File file;
        final int threshold;

        public PartBodyOutputStream(final File file, final int threshold) throws FileNotFoundException {
            this.file = file;
            this.threshold = threshold > 0 ? threshold : 0;
            if (threshold <=0){
                isInMemory = false;
                fileOutputStream = new FileOutputStream(file);
            }else {
                this.byteArrayOutputStream = new ByteArrayOutputStream();
            }
        }

        @Override
        public void write(int b) throws IOException {
            if (checkThreshold(1)){
                byteArrayOutputStream.write(b);
            }else{
                fileOutputStream.write(b);
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (checkThreshold(len)){
                byteArrayOutputStream.write(b, off, len);
            }else{
                fileOutputStream.write(b, off, len);
            }
        }

        @Override
        public void write(byte[] b) throws IOException {
            if (checkThreshold(b.length)){
                byteArrayOutputStream.write(b);
            }else{
                fileOutputStream.write(b);
            }
        }

        @Override
        public void flush() throws IOException {
            if (fileOutputStream != null) {
                fileOutputStream.flush();
            }
        }

        @Override
        public void close() throws IOException {
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
        }

        boolean checkThreshold(int lengthToWrite) throws IOException {
            if (byteArrayOutputStream != null && byteArrayOutputStream.size() + lengthToWrite <= threshold){
                return true;
            }
            if (isInMemory){
                switchToFile();
            }
            return false;
        }

        synchronized void switchToFile() throws IOException {

            if (log.isDebugEnabled()) log.debug("Switching to file");

            fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(byteArrayOutputStream.toByteArray());
            fileOutputStream.flush();
            byteArrayOutputStream.reset();
            byteArrayOutputStream = null;
            isInMemory = false;
        }

        InputStream getInputStream() throws FileNotFoundException {
            final InputStream partInputStream;
            if (isInMemory){
                if (log.isDebugEnabled()) log.debug("Data is in memory.");
                partInputStream =  new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
            }else{

                final File fileToPurge = file;
                if (log.isDebugEnabled()) log.debug("Data is stored in a file: " + fileToPurge.getAbsolutePath());

                partInputStream = new FileInputStream(fileToPurge){
                    @Override
                    public void close() throws IOException {
                        super.close();
                        if (fileToPurge != null && fileToPurge.exists()){
                            try {
                                if (log.isDebugEnabled()) log.debug("Purging temporary file: " + fileToPurge.getAbsolutePath());
                                if (!fileToPurge.delete()){
                                    throw new IllegalStateException("File deleted returned false.");
                                }
                            }catch (Exception e){
                                log.warn("Unable to purge the temporary file: " + fileToPurge.getAbsolutePath(), e.getLocalizedMessage());
                            }
                        }
                    }
                };
            }
            return partInputStream;
        }
    }


}

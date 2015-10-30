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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>
 *     The {@link DefaultPartStreamsFactory}. This factory is creating a {@link PartStreams} where:<br>
 * </p>
 * <p>
 *     {@link PartStreams#getPartOutputStream()} is returning an {@link OutputStream} that
 *     <ul>
 *         <li>Keeps the data in memory until a certain threshold is reached.</li>
 *         <li>Switches to a temporary file when the threshold is reached.</li>
 *     </ul>
 *     {@link PartStreams#getPartInputStream()} is returning an {@link InputStream} that
 *     <ul>
 *         <li>Reads the data from memory or temporary file depending on the status of the OutputStream.</li>
 *         <li>Deletes the temporary file on close.</li>
 *     </ul>
 *
 * <p>
 *     The threshold and the temporary file output folder are configurable at construction time.
 * </p>
 *
 * @author Silvano Riz.
 */
public class DefaultPartStreamsFactory implements PartStreamsFactory {

    private static final Logger log = LoggerFactory.getLogger(DefaultPartStreamsFactory.class);

    /**
     * Default max threshold. 10Kb
     */
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
    public DefaultPartStreamsFactory(final String tempFolderPath, final int maxSizeThreshold) {
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
    public DefaultPartStreamsFactory(final String tempFolderPath) {
        this(tempFolderPath, DEFAULT_MAX_THRESHOLD);
    }

    /**
     * <p>
     *     Constructor tha uses a default default folder ${java.io.tmpdir}/nio-file-upload
     * </p>
     * @param maxSizeThreshold The maximum amount of bytes that will be kept in memory for each part.
     */
    public DefaultPartStreamsFactory(int maxSizeThreshold) {
        this(System.getProperty("java.io.tmpdir") + "/nio-file-upload", maxSizeThreshold);
    }

    /**
     * <p>
     *     Constructor that uses a default threshold of 10kb and a default folder ${java.io.tmpdir}/nio-file-upload
     * </p>
     */
    public DefaultPartStreamsFactory() {
        this(System.getProperty("java.io.tmpdir") + "/nio-file-upload", DEFAULT_MAX_THRESHOLD);
    }

    @Override
    public PartStreams newPartStreams(Map<String, List<String>> headers, int partIndex) {
        try {

            final long contentLength = MultipartUtils.getContentLength(headers);
            final String name = String.format("nio-body-%d-%s.tmp", partIndex, UUID.randomUUID().toString());
            final File tempFile = new File(tempFolder, name);
            int threshold = (contentLength > maxSizeThreshold) ? 0 : maxSizeThreshold;
            return new DefaultPartStreams(tempFile, threshold);
        }catch (Exception e){
            throw new IllegalStateException("Unable to create the temporary file where to store the body", e);
        }
    }

    /**
     * A PartStreams providing:
     *
     * - An OutputStream that
     * 1. Keeps in memory the data until a certain threshold is reached.
     * 2. Switches to a temporary file when the threshold is reached.
     *
     * - An InputStream that
     * 1. Reads the data from memory or temporary file depending on the status of the OutputStream.
     * 2. Deletes the temporary file on close.
     */
    static class DefaultPartStreams implements PartStreams {

        ByteArrayOutputStream byteArrayOutputStream;
        FileOutputStream fileOutputStream;
        boolean isInMemory = true;
        final File file;
        final int threshold;
        final AtomicBoolean outputStreamServed = new AtomicBoolean(false);

        public DefaultPartStreams(File file, int threshold) throws FileNotFoundException {
            this.file = file;
            this.threshold = threshold;

            if(threshold <= 0){
                isInMemory = false;
                fileOutputStream = new FileOutputStream(file);
            }else{
                isInMemory = true;
                byteArrayOutputStream = new ByteArrayOutputStream();
            }

        }

        @Override
        public OutputStream getPartOutputStream() {

            if (!outputStreamServed.compareAndSet(false, true)){
                throw new IllegalStateException("The part output stream has already been created.");
            }

            return new OutputStream() {
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
            };
        }

        @Override
        public InputStream getPartInputStream(){
            final InputStream partInputStream;
            if (isInMemory){
                if (log.isDebugEnabled()) log.debug("Data is in memory.");
                partInputStream =  new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
            }else{

                final File fileToPurge = file;
                if (log.isDebugEnabled()) log.debug("Data is stored in a file: " + fileToPurge.getAbsolutePath());

                try {
                    partInputStream = new FileInputStream(fileToPurge) {
                        @Override
                        public void close() throws IOException {
                            try {
                                super.close();
                            } catch (Exception e) {
                                // nothing to do...
                            }

                            // If there si a file, purge it...
                            if (fileToPurge != null && fileToPurge.exists()) {
                                try {
                                    if (log.isDebugEnabled())
                                        log.debug("Purging temporary file: " + fileToPurge.getAbsolutePath());
                                    if (!fileToPurge.delete()) {
                                        throw new IllegalStateException("File deleted returned false.");
                                    }
                                } catch (Exception e) {
                                    log.warn("Unable to purge the temporary file: " + fileToPurge.getAbsolutePath(), e.getLocalizedMessage());
                                }
                            }
                        }
                    };
                }catch (Exception e){
                    throw new IllegalStateException("Unable to get the input stream", e);
                }
            }
            return partInputStream;
        }
    }

}

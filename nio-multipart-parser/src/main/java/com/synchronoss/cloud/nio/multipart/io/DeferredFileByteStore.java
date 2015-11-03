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

package com.synchronoss.cloud.nio.multipart.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * <p>
 *    A {@link ByteStore} that uses a combination of memory and file to store the data.
 *    If the data is smaller than a configurable threshold the data is kept in memory, if the threshold is reached the
 *    bytes are flushed to disk.
 * </p>
 * @author Silvano Riz
 */
public class DeferredFileByteStore extends ByteStore {

    private static final Logger log = LoggerFactory.getLogger(DeferredFileByteStore.class);

    static final int DEFAULT_THRESHOLD = 10240;//10kb

    final File file;
    final int threshold;
    final boolean purgeFileAfterReadComplete;

    volatile boolean isInMemory = true;
    volatile boolean isClosed = false;

    volatile ByteArrayOutputStream byteArrayOutputStream;
    volatile FileOutputStream fileOutputStream;

    // ------------
    // CONSTRUCTORS
    // ------------

    /**
     * <p>
     *     Constructor.
     * </p>
     * @param file The file that will be used to store the data if the threshold is reached.
     * @param threshold The threshold in bytes. Data smaller than the threshold are kept in memory. If the threshold is reached, the data is flushed to disk.
     * @param purgeFileAfterReadComplete boolean flag that if true it will purge the file after the data has been read. The purge happens when the close method is called on the input stream served by the instance.
     */
    public DeferredFileByteStore(final File file, final int threshold, final boolean purgeFileAfterReadComplete) {
        this.file = file;
        this.threshold = threshold;
        this.purgeFileAfterReadComplete = purgeFileAfterReadComplete;
        if(threshold <= 0){
            isInMemory = false;
            fileOutputStream = newFileOutputStream();
        }else{
            isInMemory = true;
            byteArrayOutputStream = new ByteArrayOutputStream();
        }
    }

    /**
     * <p>
     *     Constructor that uses the default threshold of 10kb
     * </p>
     * @param file The file that will be used to store the data if the threshold is reached.
     * @param purgeFileAfterReadComplete boolean flag that if true it will purge the file after the data has been read. The purge happens when the close method is called on the input stream served by the instance.
     */
    public DeferredFileByteStore(final File file, final boolean purgeFileAfterReadComplete){
        this(file, DEFAULT_THRESHOLD, purgeFileAfterReadComplete);
    }

    /**
     * <p>
     *     Constructor that sets the purgeFileAfterReadComplete to true by default.
     * </p>
     * @param file The file that will be used to store the data if the threshold is reached.
     * @param threshold The threshold in bytes. Data smaller than the threshold are kept in memory. If the threshold is reached, the data is flushed to disk.
     */
    public DeferredFileByteStore(final File file, final int threshold){
        this(file, threshold, true);
    }

    /**
     * <p>
     *     Constructor that uses the default threshold of 10kb and sets the purgeFileAfterReadComplete to true.
     * </p>
     * @param file The file that will be used to store the data if the threshold is reached.
     */
    public DeferredFileByteStore(final File file){
        this(file, DEFAULT_THRESHOLD, true);
    }

    @Override
    public void write(int b) throws IOException {
        assertIsOpen();
        if (checkThreshold(1)){
            byteArrayOutputStream.write(b);
        }else{
            fileOutputStream.write(b);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        assertIsOpen();
        if (checkThreshold(len)){
            byteArrayOutputStream.write(b, off, len);
        }else{
            fileOutputStream.write(b, off, len);
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        assertIsOpen();
        if (checkThreshold(b.length)){
            byteArrayOutputStream.write(b);
        }else{
            fileOutputStream.write(b);
        }
    }

    @Override
    public void flush() throws IOException {
        assertIsOpen();
        if (fileOutputStream != null) {
            fileOutputStream.flush();
        }
    }

    @Override
    public void close() throws IOException {
        isClosed = true;
        if (fileOutputStream != null) {
            fileOutputStream.close();
        }
    }

    @Override
    public InputStream getInputStream() {
        assertIsClosed();
        if (isInMemory){
            return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        }else {
            return newFileInputStream();
        }
    }

    public boolean isInMemory() {
        return isInMemory;
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

    void assertIsOpen(){
        if (isClosed){
            throw new IllegalStateException("OutputStream is closed");
        }
    }

    void assertIsClosed(){
        if (!isClosed){
            throw new IllegalStateException("OutputStream is open");
        }
    }

    void switchToFile() throws IOException {

        if (log.isDebugEnabled()) log.debug("Switching to file");

        fileOutputStream = new FileOutputStream(file);
        fileOutputStream.write(byteArrayOutputStream.toByteArray());
        fileOutputStream.flush();
        byteArrayOutputStream.reset();
        byteArrayOutputStream = null;
        isInMemory = false;
    }

    FileOutputStream newFileOutputStream(){
        try{
            return new FileOutputStream(file);
        }catch (Exception e){
            throw new IllegalStateException("Unable to create the outputStream.", e);
        }
    }

    FileInputStream newFileInputStream(){
        try{
            if (purgeFileAfterReadComplete){
                return new PurgeOnCloseFileInputStream(file);
            }else{
                return new FileInputStream(file);
            }
        }catch (Exception e){
            throw new IllegalStateException("Unable to create the inputStream.", e);
        }
    }
}

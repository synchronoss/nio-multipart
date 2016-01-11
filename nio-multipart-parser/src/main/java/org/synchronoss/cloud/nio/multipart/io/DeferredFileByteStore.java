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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * <p> A {@code ByteStore} that uses a combination of memory and file to store the data.
 *    If the data is smaller than a configurable threshold the data is kept in memory, if the threshold is reached the
 *    bytes are flushed to disk.
 * <p> The {@code DeferredFileByteStore} has two distinct states:
 * <ul>
 *     <li><i>write</i>: The {@code ByteStore} is ONLY writable and NOT readable.</li>
 *     <li><i>read</i>: The {@code ByteStore} is ONLY readable and NOT writable.</li>
 * </ul>
 * <p> A new instance will always start in a <i>write</i> state, ready to accept bytes and any call to the {@link #getInputStream()} will fail.
 * Once all the data has been written, the {@link #close()} method needs to be called to close the write channel and switch the
 * {@code DeferredFileByteStore} to the <i>read</i> state. At that point the data can be read via {@link #getInputStream()}.
 *
 * @author Silvano Riz
 */
public class DeferredFileByteStore extends ByteStore {

    private static final Logger log = LoggerFactory.getLogger(DeferredFileByteStore.class);

    enum ReadWriteStatus {
        READ, WRITE, DISMISSED
    }

    enum StorageMode {
        MEMORY, DISK;
    }

    static final int DEFAULT_THRESHOLD = 10240;//10kb

    final File file;
    final int threshold;
    final boolean purgeFileAfterReadComplete;

    volatile ReadWriteStatus readWriteStatus;
    volatile StorageMode storageMode;
    volatile ByteArrayOutputStream byteArrayOutputStream;
    volatile FileOutputStream fileOutputStream;

    // ------------
    // CONSTRUCTORS
    // ------------

    /**
     * <p> Constructor.
     *
     * @param file The file that will be used to store the data if the threshold is reached.
     * @param threshold The threshold in bytes. Data will be kept in memory until no more data is available or the threshold is reached. If the threshold is reached the data is flushed to disk, the memory is freed and the subsequent writes will go straight to disk. A threshold set to 0 or a negative value means that no memory will be used at all and writes go straight to disk.
     * @param purgeFileAfterReadComplete boolean flag that if true it will purge the file after the data has been read. The purge happens when the close method is called on the input stream served by the instance via {@link #getInputStream()}.
     */
    public DeferredFileByteStore(final File file, final int threshold, final boolean purgeFileAfterReadComplete) {
        this.file = file;
        this.threshold = threshold;
        this.purgeFileAfterReadComplete = purgeFileAfterReadComplete;
        readWriteStatus = ReadWriteStatus.WRITE;
        if(threshold <= 0){
            storageMode = StorageMode.DISK;
            fileOutputStream = newFileOutputStream();
        }else{
            storageMode = StorageMode.MEMORY;
            byteArrayOutputStream = new ByteArrayOutputStream();
        }
    }

    /**
     * <p> Constructor that uses the default threshold of 10kb
     *
     * @param file The file that will be used to store the data if the threshold is reached.
     * @param purgeFileAfterReadComplete boolean flag that if true it will purge the file after the data has been read. The purge happens when the close method is called on the input stream served by the instance.
     */
    public DeferredFileByteStore(final File file, final boolean purgeFileAfterReadComplete){
        this(file, DEFAULT_THRESHOLD, purgeFileAfterReadComplete);
    }

    /**
     * <p> Constructor that sets the purgeFileAfterReadComplete to true by default.
     *
     * @param file The file that will be used to store the data if the threshold is reached.
     * @param threshold The threshold in bytes. Data smaller than the threshold are kept in memory. If the threshold is reached, the data is flushed to disk.
     */
    public DeferredFileByteStore(final File file, final int threshold){
        this(file, threshold, true);
    }

    /**
     * <p> Constructor that uses the default threshold of 10kb and sets the purgeFileAfterReadComplete to true.
     *
     * @param file The file that will be used to store the data if the threshold is reached.
     */
    public DeferredFileByteStore(final File file){
        this(file, DEFAULT_THRESHOLD, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(int b) throws IOException {
        assertIsWritable();
        if (checkThreshold(1)){
            byteArrayOutputStream.write(b);
        }else{
            fileOutputStream.write(b);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        assertIsWritable();
        if (checkThreshold(len)){
            byteArrayOutputStream.write(b, off, len);
        }else{
            fileOutputStream.write(b, off, len);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(byte[] b) throws IOException {
        assertIsWritable();
        if (checkThreshold(b.length)){
            byteArrayOutputStream.write(b);
        }else{
            fileOutputStream.write(b);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() throws IOException {
        assertIsWritable();
        if (fileOutputStream != null) {
            fileOutputStream.flush();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        close(ReadWriteStatus.READ);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getInputStream() {
        if (readWriteStatus.equals(ReadWriteStatus.READ)) {
            if (storageMode.equals(StorageMode.MEMORY)) {
                return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
            } else {
                return newFileInputStream();
            }
        }else{
            throw new IllegalStateException("The DeferredFileByteStore is still in write mode. Call the close() method when all the data has been written before asking for the InputStream.");
        }
    }

    /**
     * <p> Returns if the data has been flushed to disk or if it's still in memory.
     *
     * @return true if the data is in memory, false otherwise
     */
    public boolean isInMemory() {
        return storageMode.equals(StorageMode.MEMORY);
    }

    /**
     * <p> Dismisses the {@code DeferredFileByteStore} closing quietly the {@code OutputStream} and deleting the underlying file if it exists.
     *     This method is useful just in case of errors to free the resources and once called the {@code DeferredFileByteStore} is not usable anymore.
     *
     * @return <code>true</code> if and only if the file was created and it has been deleted successfully; <code>false</code> otherwise.
     */
    @Override
    public boolean dismiss() {
        try {
            close(ReadWriteStatus.DISMISSED);
        } catch (Exception e) {
            // Nothing to do
        }
        return !(file != null && file.exists()) || file.delete();
    }

    void close(final ReadWriteStatus newReadWriteStatus) throws IOException {
        readWriteStatus = newReadWriteStatus;
        if (fileOutputStream != null) {
            fileOutputStream.close();
        }
    }

    boolean checkThreshold(final int lengthToWrite) throws IOException {
        if (byteArrayOutputStream != null && byteArrayOutputStream.size() + lengthToWrite <= threshold){
            return true;
        }
        if (isInMemory()){
            switchToFile();
        }
        return false;
    }

    void assertIsWritable(){
        if (!readWriteStatus.equals(ReadWriteStatus.WRITE)){
            throw new IllegalStateException("OutputStream is closed");
        }
    }

    void switchToFile() throws IOException {

        if (log.isDebugEnabled()) log.debug("Switching to file");

        fileOutputStream = new FileOutputStream(file);
        fileOutputStream.write(byteArrayOutputStream.toByteArray());
        fileOutputStream.flush();
        byteArrayOutputStream.reset();
        byteArrayOutputStream = null;
        storageMode = StorageMode.DISK;
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

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

package org.synchronoss.cloud.nio.multipart.io.buffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;

/**
 * <p> A reusable circular buffer
 *
 * @author Silvano Riz.
 */
public class CircularBuffer {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(CircularBuffer.class);

    // Capacity of the buffer.
    final int size;

    // The buffer
    final byte[] buffer;

    // Pointer to the first slot with valid data
    volatile int startValidDataIndex = 0;

    // Pointer to the first available slot for write
    volatile int nextAvailablePosition = 0;

    // Number of slots of valid data
    volatile int availableReadLength = 0;

    /**
     * <p> Constructor.
     *
     * @param size The size of the buffer. Must be greater than or equal to 1
     */
    public CircularBuffer(final int size) {
        if(size < 1){
            throw new IllegalArgumentException("Size cannot be zero or negative. Size: " + size);
        }
        this.size = size;
        this.buffer = new byte[size];
    }

    /**
     * <p> Writes a byte in the first available slot in the buffer. If the buffer is full the oldest data written will be overwritten.
     *
     * @param data The byte to write.
     */
    public void write(final byte data){

        buffer[nextAvailablePosition] = data;
        nextAvailablePosition = forwards(nextAvailablePosition);

        if (availableReadLength > 0 && nextAvailablePosition == startValidDataIndex + 1){
            // buffer is full
            startValidDataIndex = forwards(startValidDataIndex);
        }

        updateAvailableReadLength(true);

    }

    /**
     * <p> Reads all the available valid data into an {@code OutputStream}
     *
     * @param outputStream The {@code OutputStream} target of the read.
     * @throws IOException If the read fails.
     */
    public void readAll(final OutputStream outputStream) throws IOException {

        if (isEmpty()){
            return;
        }
        readChunk(outputStream, availableReadLength);
    }

    /**
     * <p> Reads a chunk of available data into an {@code OutputStream}
     *
     * @param outputStream The {@code OutputStream}  target of the read.
     * @param chunkSize The size of the chunk. Must be less than or equal {@link #getAvailableDataLength()}
     * @throws IOException If the read fails.
     */
    public void readChunk(final OutputStream outputStream, final int chunkSize) throws IOException {

        if (chunkSize > availableReadLength){
            throw new IllegalArgumentException("The chunk size must be smaller or equal to the amount of available data in the buffer." +
                    " Available data: " + availableReadLength + ", Requested chunk size: " + chunkSize);
        }

        if (chunkSize <= 0 ){
            return;
        }

        if (startValidDataIndex + chunkSize > buffer.length){
            int firstChunkLength = buffer.length - startValidDataIndex;
            outputStream.write(buffer, startValidDataIndex, firstChunkLength);
            outputStream.write(buffer, 0, chunkSize - firstChunkLength);
        }else{
            outputStream.write(buffer, startValidDataIndex, chunkSize);
        }
        startValidDataIndex = forwards(startValidDataIndex, chunkSize);
        outputStream.flush();
        updateAvailableReadLength(false);

    }

    /**
     * <p> Returns if the buffer is full
     *
     * @return true if the buffer is full, false otherwise.
     */
    public boolean isFull(){
        return availableReadLength == size;
    }

    /**
     * <p> Returns if the buffer is empty
     *
     * @return true if the buffer is empty, false otherwise.
     */
    public boolean isEmpty(){
        return availableReadLength == 0;
    }

    /**
     * <p> Returns the number of slots with valid data
     *
     * @return the number of slots with valid data
     */
    public int getAvailableDataLength(){
        return availableReadLength;
    }

    /**
     * <p> Returns the buffer capacity
     *
     * @return The buffer capacity
     */
    public int getBufferSize(){
        return size;
    }

    /**
     * <p> Resets the buffer.
     */
    public void reset(){
        startValidDataIndex = 0;
        nextAvailablePosition = 0;
        availableReadLength = 0;
    }

    int forwards(final int currentPosition){
        if (currentPosition == buffer.length-1){
            return 0;
        }else{
            return currentPosition + 1;
        }
    }

    int backwards(final int currentPosition){
        if(currentPosition == 0){
            return buffer.length-1;
        }else{
            return currentPosition -1;
        }
    }

    int forwards(final int currentPosition, final int positions){

        int newPosition = currentPosition + positions;
        if (newPosition > buffer.length -1){
            newPosition = (currentPosition + positions) % size;
        }

        return newPosition;
    }

    int backwards(final int currentPosition, final int positions){

        int newPosition = currentPosition - positions;
        if (newPosition < 0){
            newPosition = size - (Math.abs(currentPosition - positions) % size);
        }

        return newPosition;
    }

    void updateAvailableReadLength(final boolean isWriteOperation) {
        if(nextAvailablePosition == startValidDataIndex) {
            if(isWriteOperation) {
                availableReadLength = size;
            } else {
                availableReadLength = 0;
            }
        } else if(nextAvailablePosition > startValidDataIndex) {
            availableReadLength = nextAvailablePosition - startValidDataIndex;
        } else {
            availableReadLength = size - startValidDataIndex + nextAvailablePosition;
        }
    }

}

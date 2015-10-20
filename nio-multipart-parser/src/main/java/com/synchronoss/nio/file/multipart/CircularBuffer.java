package com.synchronoss.nio.file.multipart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;

/**
 * <p>
 *     A reusable circular buffer
 * </p>
 *
 * Created by sriz0001 on 15/10/2015.
 */
public class CircularBuffer {

    private static final Logger log = LoggerFactory.getLogger(CircularBuffer.class);

    // Capacity of the buffer.
    final int size;

    // The buffer
    final byte[] buffer;

    // Pointer to the first slot with valid data
    int startValidDataIndex = 0;

    // Pointer to the first available slot for write
    int nextAvailablePosition = 0;

    // Number of slots of valid data
    int availableReadLength = 0;

    /**
     * <p>
     *     Constructor.
     * </p>
     *
     * @param size The size of the buffer. Must be greater than 1
     */
    public CircularBuffer(int size) {
        if(size < 1){
            throw new IllegalArgumentException("Size cannot be zero or negative. Size: " + size);
        }
        this.size = size;
        this.buffer = new byte[size];
    }

    /**
     * <p>
     *     Writes a byte in the first available slot in the buffer. If the buffer is full the oldest data written will be overwritten.
     * </p>
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
     * <p>
     *     Reads all the available valid data into an {@link OutputStream}
     * </p>
     * @param outputStream The {@link OutputStream} target of the read.
     * @throws IOException If the read fails.
     */
    public void readAll(final OutputStream outputStream) throws IOException {

        if (isEmpty()){
            return;
        }

        int bytesToRead = availableReadLength;
        while (bytesToRead > 0){
            // XXX - If this becomes a problem there might be a better way to compute the new index. See sun.plugin2.jvm.CircularByteBuffer
            outputStream.write(buffer[startValidDataIndex]);
            startValidDataIndex = forwards(startValidDataIndex);
            bytesToRead --;
        }
        outputStream.flush();
        updateAvailableReadLength(false);

    }

    /**
     * <p>
     *     Reads a chunk of available data into an {@link OutputStream}
     * </p>
     * @param outputStream The {@link OutputStream}  target of the read.
     * @param chunkSize The size of the chunk. Must be less than or equal {@link #getAvailableDataLength()}
     * @throws IOException If the read fails.
     */
    public void readChunk(final OutputStream outputStream, int chunkSize) throws IOException {

        if (chunkSize > availableReadLength){
            throw new IllegalArgumentException("The chunk size must be smaller or equal to the amount of available data in the buffer." +
                    " Available data: " + availableReadLength + ", Requested chunk size: " + chunkSize);
        }

        if (isEmpty()){
            return;
        }

        int bytesToRead = chunkSize;
        while (bytesToRead > 0){
            // XXX - If this becomes a problem there might be a better way to compute the new index. See sun.plugin2.jvm.CircularByteBuffer
            outputStream.write(buffer[startValidDataIndex]);
            startValidDataIndex = forwards(startValidDataIndex);
            bytesToRead --;
        }
        outputStream.flush();
        updateAvailableReadLength(false);

    }

    /**
     * <p>
     *     Returns if the buffer is full
     * </p>
     * @return true if the buffer is full, false otherwise.
     */
    public boolean isFull(){
        return availableReadLength == size;
    }

    /**
     * <p>
     *     Returns if the buffer is empty
     * </p>
     * @return true if the buffer is empty, false otherwise.
     */
    public boolean isEmpty(){
        return availableReadLength == 0;
    }

    /**
     * <p>
     *     Returns the number of slots with valid data
     * </p>
     * @return the number of slots with valid data
     */
    public int getAvailableDataLength(){
        return availableReadLength;
    }

    /**
     * <p>
     *     Returns the buffer capacity
     * </p>
     * @return The buffer capacity
     */
    public int getBufferSize(){
        return size;
    }

    /**
     * <p>
     *     Resets the buffer.
     * </p>
     */
    public void reset(){
        startValidDataIndex = 0;
        nextAvailablePosition = 0;
        availableReadLength = 0;
    }

    int forwards(int currentPosition){
        if (currentPosition == buffer.length-1){
            return 0;
        }else{
            return currentPosition + 1;
        }
    }

    int backwards(int currentPosition){
        if(currentPosition == 0){
            return buffer.length-1;
        }else{
            return currentPosition -1;
        }
    }

    int forwards(int currentPosition, int positions){

        int newPosition = currentPosition + positions;
        if (newPosition > buffer.length -1){
            newPosition = (currentPosition + positions) % size;
        }

        return newPosition;
    }

    int backwards(int currentPosition, int positions){

        int newPosition = currentPosition - positions;
        if (newPosition < 0){
            newPosition = size - (Math.abs(currentPosition - positions) % size);
        }

        return newPosition;
    }

    void updateAvailableReadLength(boolean isWriteOperation) {
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

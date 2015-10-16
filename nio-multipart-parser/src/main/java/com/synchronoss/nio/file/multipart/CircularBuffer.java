package com.synchronoss.nio.file.multipart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * <p>
 *     Circular buffer
 * </p>
 *
 * Created by sriz0001 on 15/10/2015.
 */
public class CircularBuffer {

    private static final Logger log = LoggerFactory.getLogger(CircularBuffer.class);

    final int size;
    final byte[] buffer;
    int startValidDataIndex = 0;
    int nextAvailablePosition = 0;
    boolean recycling = false;

    public CircularBuffer(int size) {
        if(size < 1){
            throw new IllegalArgumentException("Size cannot be zero or negative. Size: " + size);
        }
        this.size = size;
        this.buffer = new byte[size];
    }

    public void write(final byte b){

        if (recycling && nextAvailablePosition == startValidDataIndex){
            // buffer is full
            startValidDataIndex = forwards(startValidDataIndex);
        }

        buffer[nextAvailablePosition] = b;
        nextAvailablePosition = forwards(nextAvailablePosition);
        recycling = nextAvailablePosition == startValidDataIndex;
    }

    // FIXME - This method is not working properly, it is flushing old bytes...
    public void read(final OutputStream outputStream, int endIndex) throws IOException {

        int endValidDataIndex = backwards(nextAvailablePosition);
        if (endIndex == -1){
            endIndex = endValidDataIndex;
        }

        log.info("Read from " + startValidDataIndex + " TO " + endIndex);

        while(true){
            log.info("Flushing " + buffer[startValidDataIndex] + ". startValidDataIndex: " + startValidDataIndex + ", endIndex: " + endIndex);
            outputStream.write(buffer[startValidDataIndex]);
            startValidDataIndex = forwards(startValidDataIndex);


            if (startValidDataIndex == endIndex){
                log.info("Flushing " + buffer[startValidDataIndex] + ". startValidDataIndex: " + startValidDataIndex + ", endIndex: " + endIndex);
                outputStream.write(buffer[startValidDataIndex]);
                break;
            }
        }

        recycling = !(nextAvailablePosition == startValidDataIndex);
        log.info("Recycling " + recycling);

    }

    public void read(final OutputStream outputStream) throws IOException {
        read(outputStream, -1);
    }

    public boolean isFull(){
        return recycling && startValidDataIndex == nextAvailablePosition;
    }

    public boolean isEmpty(){
        return !recycling && startValidDataIndex == nextAvailablePosition;
    }

    public void reset(){
        startValidDataIndex = 0;
        nextAvailablePosition = 0;
        recycling = false;
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

        log.trace("Forward. Current position: " + currentPosition + ", positions: " + positions + ", New position: " + newPosition);

        return newPosition;
    }

    int backwards(int currentPosition, int positions){

        int newPosition = currentPosition - positions;
        if (newPosition < 0){
            newPosition = size - (Math.abs(currentPosition - positions) % size);
        }

        log.trace("Backwards. Current position: " + currentPosition + ", positions: " + positions + ", New position: " + newPosition);

        return newPosition;
    }

    @Override
    public String toString() {
        return "CircularBuffer{" +
                "size=" + size +
                ", buffer=" + Arrays.toString(buffer) +
                ", startValidDataIndex=" + startValidDataIndex +
                ", nextAvailablePosition=" + nextAvailablePosition +
                ", recycling=" + recycling +
                '}';
    }
}

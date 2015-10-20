package com.synchronoss.nio.file.multipart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;

/**
 * <p>
 *     A reusable buffer that is watching for end of line sequences.
 *     Every time the buffer is full or if an end of line is encountered the data (excluded the end of line sequence) will be flushed to an {@link OutputStream}.
 *     After an end of line sequence has been found, the buffer is not writable anymore and {@link #reset(byte[], OutputStream)} must be call to reuse it.
 * </p>
 * Created by sriz0001 on 18/10/2015.
 */
public class EndOfLineBuffer {

    private static final Logger log = LoggerFactory.getLogger(EndOfLineBuffer.class);

    // Underlying circular buffer.
    final CircularBuffer circularBuffer;

    // The output stream where to flush the buffer when full
    OutputStream flushOutputStream;

    // The end of line sequence
    byte[] endOfLineSequence;
    int endOfLineSequenceMatchingLength;
    /**
     * <p>
     *     Constructor
     * </p>
     * @param size The size of the buffer. Must be greater than the bigger end of line sequence
     * @param endOfLineSequence The end of line sequence. The {@link #write(byte)} method will return true when the end of line sequence is encountered
     * @param flushOutputStream The {@link OutputStream} where to flush the data when the buffer is full. If set to null the buffer can be used to skip bytes until a separator.
     */
    public EndOfLineBuffer(final int size, byte[] endOfLineSequence, final OutputStream flushOutputStream) {
        this.circularBuffer = new CircularBuffer(size);
        this.flushOutputStream = flushOutputStream;
        this.endOfLineSequence = endOfLineSequence;
        this.endOfLineSequenceMatchingLength = 0;
    }

    /**
     * <p>
     *     Resets the buffer
     * </p>
     * @param endOfLineSequence The new end of line sequence.
     * @param flushOutputStream The new {@link OutputStream} where to flush the data when the buffer is full.
     */
    public void reset(byte[] endOfLineSequence, final OutputStream flushOutputStream){
        this.circularBuffer.reset();
        this.flushOutputStream = flushOutputStream;
        this.endOfLineSequence = endOfLineSequence;
        this.endOfLineSequenceMatchingLength = 0;
    }

    /**
     * <p>
     *     Writes a byte of data in the buffer. If the buffer already encountered an end of line sequence, and exception will be thrown.
     * </p>
     * @param data The byte of data.
     * @return true if the buffer encountered one of the end of line sequences, false otherwise.
     */
    public boolean write(final byte data){

        if (isEndOfLine()){
            throw new IllegalStateException("Buffer is in an end of line state. You need to reset it before writing.");
        }

        if (circularBuffer.isFull()){
            flush();
        }

        if (circularBuffer.isFull()){
            throw new IllegalStateException("Unexpected error. Buffer is full after a flush.");
        }

        circularBuffer.write(data);
        boolean isEndOfLine = updateEndOfLineMatchingStatus(data);
        if (isEndOfLine){
            flush();
        }

        return isEndOfLine;
    }

    /**
     * <p>
     *     Returns if an end of line has been encountered.
     * </p>
     * @return true if an end of line sequence has been encountered, false otherwise
     */
    public boolean isEndOfLine(){
        return endOfLineSequenceMatchingLength == endOfLineSequence.length;
    }

    boolean updateEndOfLineMatchingStatus(final byte b){
        if (endOfLineSequence[endOfLineSequenceMatchingLength] == b){
            endOfLineSequenceMatchingLength++;
        }else if (endOfLineSequence[0] == b){
            endOfLineSequenceMatchingLength = 1;
        }else{
            endOfLineSequenceMatchingLength = 0;
        }
        return isEndOfLine();
    }

    void flush(){
        if (flushOutputStream == null){
            return;
        }
        try {
            if (circularBuffer.getAvailableDataLength() > 0) {
                if (endOfLineSequenceMatchingLength > 0) {
                    // Need to flush a chunk
                    int chunkSize = circularBuffer.availableReadLength - endOfLineSequenceMatchingLength;
                    circularBuffer.readChunk(flushOutputStream, chunkSize);
                } else {
                    // flush all
                    circularBuffer.readAll(flushOutputStream);
                }
            }
        }catch (Exception e){
            throw new IllegalStateException("Error flushing the buffer data.", e);
        }
    }

}

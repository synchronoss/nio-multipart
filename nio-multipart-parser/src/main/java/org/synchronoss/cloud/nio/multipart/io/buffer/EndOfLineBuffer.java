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

import java.io.OutputStream;

/**
 * <p> A reusable buffer that is watching for end of line sequences.
 *     Every time the buffer is full or if an end of line is encountered the data (excluded the end of line sequence) will be flushed to an {@code OutputStream}.
 *     After an end of line sequence has been found, the buffer is not writable anymore and {@link #recycle(byte[], OutputStream)} must be call to reuse it.
 *
 * @author Silvano Riz.
 */
public class EndOfLineBuffer {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(EndOfLineBuffer.class);

    // Underlying circular buffer.
    final CircularBuffer circularBuffer;

    // The output stream where to flush the buffer when full or when an enf of line sequence has been found
    volatile OutputStream flushOutputStream;

    // The end of line sequence
    volatile byte[] endOfLineSequence;

    // How many bytes are currently matching the end of line sequence
    volatile int endOfLineSequenceMatchingLength;

    /**
     * <p> Constructor
     *
     * @param size The size of the buffer. Must be greater than the bigger end of line sequence
     * @param endOfLineSequence The end of line sequence. The {@link #write(byte)} method will return true when the end of line sequence is encountered
     * @param flushOutputStream The {@code OutputStream} where to flush the data when the buffer is full. If set to null the buffer can be used to skip bytes until an end of line marker.
     */
    public EndOfLineBuffer(final int size, byte[] endOfLineSequence, final OutputStream flushOutputStream) {

        if (endOfLineSequence.length >= size){
            throw new IllegalArgumentException("The end of line sequence cannot be larger than the buffer size. End of line sequence length: " + endOfLineSequence.length + ", buffer size: " + size);
        }

        this.circularBuffer = new CircularBuffer(size);
        this.flushOutputStream = flushOutputStream;
        this.endOfLineSequence = endOfLineSequence;
        this.endOfLineSequenceMatchingLength = 0;
    }

    /**
     * <p> Recycles the buffer
     *
     * @param endOfLineSequence The new end of line sequence.
     * @param flushOutputStream The new {@code OutputStream} where to flush the data when the buffer is full.
     */
    public void recycle(final byte[] endOfLineSequence, final OutputStream flushOutputStream){
        this.circularBuffer.reset();
        this.flushOutputStream = flushOutputStream;
        this.endOfLineSequence = endOfLineSequence;
        this.endOfLineSequenceMatchingLength = 0;
    }

    /**
     * <p> Writes a byte of data in the buffer. If the buffer already encountered an end of line sequence, and exception will be thrown.
     *
     * @param data The byte of data.
     * @return true if the buffer encountered one of the end of line sequences, false otherwise.
     */
    public boolean write(final byte data){

        if (isEndOfLine()){
            throw new IllegalStateException("Buffer is in an end of line state. You need to recycle it before writing.");
        }

        flushIfNeeded();

        circularBuffer.write(data);
        boolean isEndOfLine = updateEndOfLineMatchingStatus(data);
        if (isEndOfLine){
            flushIfNeeded();
        }

        return isEndOfLine;
    }

    /**
     * <p> Returns if an end of line has been encountered.
     *
     * @return true if an end of line sequence has been encountered, false otherwise
     */
    public boolean isEndOfLine() {
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

    void flushIfNeeded(){
        if (flushOutputStream == null){
            return;
        }
        if (circularBuffer.isFull() || isEndOfLine()) {
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
            } catch (Exception e) {
                throw new IllegalStateException("Error flushing the buffer data.", e);
            }
        }

        if (circularBuffer.isFull()){
            // Still full after flushing should never happen...
            throw new IllegalStateException("Unexpected error. Buffer is full after a flush.");
        }
    }
}

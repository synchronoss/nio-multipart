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

package org.synchronoss.cloud.reactive.multipart.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p> A reusable buffer that is watching for end of line sequences.
 *     Every time the buffer is full or if an end of line is encountered the data (excluded the end of line sequence)
 *
 *
 * @author Silvano Riz.
 */
public class EndOfLineBuffer {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(EndOfLineBuffer.class);

    /**
     * <p> Status returned every write that indicates if the buffer is writable or not.
     * <p> If the buffer is not writable it is because the eol sequence have been encountered or it is full.
     * <p> If the buffer is full the bytes need to be stored somewhere else before writing again
     * <p> If the buffer encountered the eol sequence, the remaining bytes (if any) need to be stored somewhere else
     *     and the buffer must be reset via {@link EndOfLineBuffer#recycle(byte[])} before writing again.
     */
    public enum WriteResult {
        WRITABLE(0),
        NON_WRITABLE_EOL(1),
        NON_WRITABLE_FULL(2),
        NON_WRITABLE_EOL_FULL(3);

        private final int status;

        WriteResult(int status) {
            this.status = status;
        }

        static WriteResult fromBufferStatuses(final boolean isEol, final boolean isFull){
            if (isEol && isFull){
                return NON_WRITABLE_EOL_FULL;
            } else if (isEol /* && !isFull */){
                return NON_WRITABLE_EOL;
            } else if (/*!isEol && */ isFull){
                return NON_WRITABLE_FULL;
            } else /*if (!isEol && !isFull) */{
                return WRITABLE;
            }
        }

        public boolean isWritable(){
            return this == WRITABLE;
        }

        public boolean isFull(){
            return this == NON_WRITABLE_FULL || this == NON_WRITABLE_EOL_FULL;
        }

        public boolean isEol(){
            return this == NON_WRITABLE_EOL || this == NON_WRITABLE_EOL_FULL;
        }

    }

    // Underlying circular buffer.
    final CircularBuffer circularBuffer;

    // The end of line sequence
    volatile byte[] endOfLineSequence;

    // How many bytes are currently matching the end of line sequence
    volatile int endOfLineSequenceMatchingLength;

    /**
     * <p> Constructor
     *
     * @param size The size of the buffer. Must be greater than the bigger end of line sequence
     * @param endOfLineSequence The end of line sequence. The {@link #write(byte)} method will return true when the end of line sequence is encountered
     */
    public EndOfLineBuffer(final int size, byte[] endOfLineSequence) {

        if (endOfLineSequence.length >= size){
            throw new IllegalArgumentException("The end of line sequence cannot be larger than the buffer size. End of line sequence length: " + endOfLineSequence.length + ", buffer size: " + size);
        }

        this.circularBuffer = new CircularBuffer(size);
        this.endOfLineSequence = endOfLineSequence;
        this.endOfLineSequenceMatchingLength = 0;
    }

    /**
     * <p> Recycles the buffer
     *
     * @param endOfLineSequence The new end of line sequence.
     */
    public void recycle(final byte[] endOfLineSequence){
        this.circularBuffer.reset();
        this.endOfLineSequence = endOfLineSequence;
        this.endOfLineSequenceMatchingLength = 0;
    }

    /**
     * <p> Writes a byte of data in the buffer. If the buffer already encountered an end of line sequence, and exception will be thrown.
     *
     * @param data The byte of data.
     * @return true if the buffer encountered one of the end of line sequences, false otherwise.
     */
    public WriteResult write(final byte data){

        if (isEndOfLine()){
            throw new IllegalStateException("Buffer is in an end of line state. You need to recycle it before writing.");
        }
        if (isFull()){
            throw new IllegalStateException("Buffer is full. You need to read the data before writing.");
        }

        circularBuffer.write(data);
        boolean isFull = circularBuffer.isFull();
        boolean isEndOfLine = updateEndOfLineMatchingStatus(data);

        return WriteResult.fromBufferStatuses(isEndOfLine, isFull);
    }

    /**
     * <p> Returns if an end of line has been encountered.
     *
     * @return true if an end of line sequence has been encountered, false otherwise
     */
    public boolean isEndOfLine() {
        return endOfLineSequenceMatchingLength == endOfLineSequence.length;
    }

    /**
     * <p> Returns if the buffer is full.
     *
     * @return {@code true} if the buffer is full, {@code true} otherwise.
     */
    public boolean isFull(){
        return circularBuffer.isFull();
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

    /**
     * <p> Reads the stored bytes making space for new data. It will not return the bytes of the eol sequence.
     *
     * @return The stored data.
     */
    public byte[] read(){

        if (endOfLineSequenceMatchingLength > 0) {
            // Need to flush a chunk
            int chunkSize = circularBuffer.getAvailableDataLength() - endOfLineSequenceMatchingLength;
            return circularBuffer.readChunk(chunkSize);
        } else {
            // flush all
            return circularBuffer.readAll();
        }
    }

    /**
     * <p> Trashes the data read until now.
     * <p> If there a sequence of bytes that can result in an end of line sequence, it is not trashed because it is needed
     *     to identify if it is an actual end of line.
     */
    public void trash(){
        if (endOfLineSequenceMatchingLength > 0) {
            int chunkSize = circularBuffer.getAvailableDataLength() - endOfLineSequenceMatchingLength;
            circularBuffer.readChunk(chunkSize);// TODO - implement method in circularBuffer and avoid creating a byte[]
        }else {
            circularBuffer.readAll(); // TODO - implement method in circularBuffer and avoid creating a byte[]
        }
    }

}

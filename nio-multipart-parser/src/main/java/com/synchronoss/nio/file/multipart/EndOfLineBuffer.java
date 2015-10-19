package com.synchronoss.nio.file.multipart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 *     A reusable buffer that is watching for end of line sequences.
 *     Every time the buffer is full, it will be flushed to an {@link OutputStream}.
 * </p>
 * Created by sriz0001 on 18/10/2015.
 */
public class EndOfLineBuffer {

    private static final Logger log = LoggerFactory.getLogger(EndOfLineBuffer.class);

    // Underlying circular buffer.
    final CircularBuffer circularBuffer;

    // The end of line sequences
    EndOfLineSequences endOfLineSequences;

    // The output stream where to flush the buffer when full
    OutputStream flushOutputStream;

    /**
     * <p>
     *     Constructor
     * </p>
     * @param size The size of the buffer. Must be greater than the bigger end of line sequence
     * @param endOfLineSequences The end of line sequences. A map with the named sequences of bytes that the buffer will watch for.
     * @param flushOutputStream The {@link OutputStream} where to flush the data when the buffer is full. If set to null the buffer can be used to skip bytes until a separator.
     */
    public EndOfLineBuffer(final int size, final Map<String, byte[]> endOfLineSequences, final OutputStream flushOutputStream) {

        this.circularBuffer = new CircularBuffer(size);
        this.endOfLineSequences = new EndOfLineSequences();
        this.flushOutputStream = flushOutputStream;

        // Init the end of line sequences
        for (Map.Entry<String, byte[]> entry : endOfLineSequences.entrySet()){
            addEndOfLine(entry.getKey(), entry.getValue());
        }

    }

    /**
     * <p>
     *     Resets the buffer
     * </p>
     * @param endOfLines The new end of line sequences.
     * @param flushOutputStream The new {@link OutputStream} where to flush the data when the buffer is full.
     */
    public void reset(final Map<String, byte[]> endOfLines, final OutputStream flushOutputStream){

        this.circularBuffer.reset();
        this.endOfLineSequences.reset();
        this.flushOutputStream = flushOutputStream;

        for (Map.Entry<String, byte[]> entry : endOfLines.entrySet()){
            addEndOfLine(entry.getKey(), entry.getValue());
        }
    }

    /**
     * <p>
     *     Writes a byte of data in the buffer. If the buffer already encountered an end of line sequence, and exception will be thrown.
     * </p>
     * @param data The byte of data.
     * @return true if the buffer encountered one of the end of line sequences, false otherwise.
     */
    public boolean write(final byte data){

        if (endOfLineSequences.foundEndOfLine){
            throw new IllegalStateException("Buffer is in an end of line state. You need to reset it before writing.");
        }

        if (circularBuffer.isFull()){
            flush();
        }

        if (circularBuffer.isFull()){
            throw new IllegalStateException("Unexpected error. Buffer is full after a flush.");
        }

        circularBuffer.write(data);
        endOfLineSequences.updateStatusAfterWrite(data);
        if (endOfLineSequences.foundEndOfLine){
            flush();
        }

        return endOfLineSequences.foundEndOfLine;
    }

    /**
     * <p>
     *     Returns the name of the end of line sequence encountered or null if none have been encountered.
     * </p>
     * @return The name of the end of line sequence encountered or null if none have been encountered.
     */
    public String getEndOfLineName(){
        return endOfLineSequences.foundEndOfLineName;
    }

    void flush(){
        if (flushOutputStream == null){
            return;
        }
        try {
            if (circularBuffer.getAvailableDataLength() > 0) {
                if (endOfLineSequences.maxMatching > 0) {
                    // Need to flush a chunk
                    int chunkSize = circularBuffer.availableReadLength - endOfLineSequences.maxMatching;
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

    void addEndOfLine(final String name, final byte[] endOfLineSequence){
        if (endOfLineSequence.length > circularBuffer.size){
            throw new IllegalArgumentException("End of line sequence too long. It must be less then the buffer size. " +
                    "End of line sequence length: " + endOfLineSequence.length +
                    ", Buffer size: " + circularBuffer.size);
        }
        endOfLineSequences.addEndOfLineSequence(name, endOfLineSequence);
    }

    // FIXME - Probably not the best structure. Maybe a tree is better...
    static class EndOfLineSequence {

        final byte[] eolSequence;
        int matchingLength;

        public EndOfLineSequence(final byte[] eolSequence) {
            this.eolSequence = eolSequence;
            this.matchingLength = 0;
        }

        void match(byte b){
            if (eolSequence[matchingLength] == b){
                matchingLength++;
            }else{
                matchingLength = 0;
            }
        }

        boolean isEol(){
            return matchingLength == eolSequence.length;
        }
    }

    static class EndOfLineSequences {

        final Map<String, EndOfLineSequence> endOfLines = new HashMap<String, EndOfLineSequence>();
        boolean foundEndOfLine = false;
        String foundEndOfLineName;
        int maxMatching = 0;

        public void addEndOfLineSequence(final String name, final byte[] endOfLineSequence){
            endOfLines.put(name, new EndOfLineSequence(endOfLineSequence));
        }

        public void updateStatusAfterWrite(final byte b){
            EndOfLineSequence endOfLineSequence;
            for (Map.Entry<String, EndOfLineSequence> endOfLineEntry : endOfLines.entrySet()){
                endOfLineSequence = endOfLineEntry.getValue();
                endOfLineSequence.match(b);
                foundEndOfLine = foundEndOfLine || endOfLineEntry.getValue().isEol();
                maxMatching = Math.max(maxMatching, endOfLineEntry.getValue().matchingLength);
                if (foundEndOfLine){
                    foundEndOfLineName = endOfLineEntry.getKey();
                    break;
                }
            }
        }

        public void reset(){
            endOfLines.clear();
            foundEndOfLineName = null;
            foundEndOfLine = false;
            maxMatching = 0;
        }
    }

}

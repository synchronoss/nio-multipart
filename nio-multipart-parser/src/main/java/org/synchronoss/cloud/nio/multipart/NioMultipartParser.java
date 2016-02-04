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

package org.synchronoss.cloud.nio.multipart;

import org.synchronoss.cloud.nio.multipart.io.FixedSizeByteArrayOutputStream;
import org.synchronoss.cloud.nio.multipart.io.buffer.EndOfLineBuffer;
import org.synchronoss.cloud.nio.multipart.util.HeadersParser;
import org.synchronoss.cloud.nio.multipart.util.IOUtils;
import org.synchronoss.cloud.nio.multipart.util.ParameterParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.synchronoss.cloud.nio.stream.storage.Disposable;
import org.synchronoss.cloud.nio.stream.storage.StreamStorage;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p> The main class for parsing a multipart stream in an NIO mode. A new instance can be created and the
 *     data can be written invoking the {@link #write(byte[], int, int)}, {@link #write(byte[])} or {@link #write(int)} methods.
 *     As data is written, the parser is identifying the various parts and notifying the client via the {@link NioMultipartParserListener} listener.
 *
 * <p> For each part the {@link org.synchronoss.cloud.nio.multipart.NioMultipartParser} will ask the
 *     {@link PartBodyStreamStorageFactory} for a {@code StreamStorage} where the bytes will be written.
 *     Once the parser finished to write, it calls the {@link #close()} method.
 *
 * <p> The class extends {@code OutputStream} and it can be seen as a 'splitter' where the main stream (the multipart body) is saved into different streams (one for each part).
 *     Each individual stream can be read back by the client when it's notified about the part completion.
 *     For more information about the events raised by the parser see {@link NioMultipartParserListener}.
 *
 * @author Silvano Riz.
 */
public class NioMultipartParser extends OutputStream implements Disposable {

    private static final Logger log = LoggerFactory.getLogger(NioMultipartParser.class);

    /**
     * The dash (-) character in bytes
     */
    public static final byte DASH = 0x2D;

    /**
     * The (\r) character in bytes
     */
    public static final byte CR = 0x0D;

    /**
     * The (\n) character in bytes
     */
    public static final byte LF = 0x0A;

    /**
     * The default buffer size: 16Kb
     * The buffer size needs to be bigger than the separator. (usually no more than 70 Characters)
     */
    public static final int DEFAULT_BUFFER_SIZE = 16384;

    /**
     * The default limit in bytes of the headers section.
     */
    public static final int DEFAULT_HEADERS_SECTION_SIZE = 16384;

    /**
     * Sequence of bytes that represents the end of a headers section
     */
    public static final byte[] HEADER_DELIMITER = {CR, LF, CR, LF};

    /**
     * Default number of nested multiparts body.
     */
    public static final int DEFAULT_MAX_LEVEL_OF_NESTED_MULTIPART = 1;

    /**
     * The type of a delimiter is identified using its suffix.
     * For example if the boundary is "XVZ", the sequence
     * DASH,DASH,X,W,Z,CR,LF represents an encapsulation boundary, while the
     * sequence DASH,DASH,X,V,Z,DASH,DASH is the close boundary.
     * This utility class allows to write the 2 byte suffix into an array and identify the type of delimiter.
     */
    private static class DelimiterType {

        enum Type {CLOSE, ENCAPSULATION, UNKNOWN}

        final byte[] delimiterSuffix = new byte[2];
        int index = 0;

        void addDelimiterByte(byte delimiterByte) {
            if (index >= delimiterSuffix.length) {
                throw new IllegalStateException("Cannot write the delimiter byte.");
            }
            delimiterSuffix[index] = delimiterByte;
            index++;
        }

        Type getDelimiterType() {
            if (index == 2) {
                if (delimiterSuffix[0] == CR && delimiterSuffix[1] == LF) {
                    return Type.ENCAPSULATION;
                } else if (delimiterSuffix[0] == DASH && delimiterSuffix[1] == DASH) {
                    return Type.CLOSE;
                }
            }
            return Type.UNKNOWN;
        }

        void reset() {
            index = 0;
        }

    }

    /**
     * Helper class used every time a write is called to pass information between FSM statuses.
     * It provides convenience methods to
     * - read the received data
     * - Decide if the FSM should continue.
     */
    private static class WriteContext {

        private int currentIndex;
        private int indexEnd;
        private byte[] data;
        private boolean finished;

        void init(final int currentIndex, final int indexEnd, final byte[] data, final boolean finished) {
            this.currentIndex = currentIndex;
            this.indexEnd = indexEnd;
            this.data = data;
            this.finished = finished;
        }

        int read() {
            if (currentIndex >= indexEnd) {
                return -1;
            } else {
                byte ret = data[currentIndex];
                currentIndex++;
                return ret & 0xff;
            }
        }

        void setNotFinished() {
            finished = false;
        }

        void setFinishedIfNoMoreData() {
            finished = currentIndex >= indexEnd;
        }

        void setFinished() {
            finished = true;
        }
    }

    // FSM States
    private enum State {
        SKIP_PREAMBLE,
        IDENTIFY_PREAMBLE_DELIMITER,
        GET_READY_FOR_HEADERS,
        READ_HEADERS,
        GET_READY_FOR_BODY,
        READ_BODY,
        IDENTIFY_BODY_DELIMITER,
        PART_COMPLETE,
        GET_READY_FOR_NESTED_MULTIPART,
        NESTED_PART_READ,
        ALL_PARTS_READ,
        SKIP_EPILOGUE,
        ERROR
    }

    /*
     * The multipart context. Content-Type, Content-Length and Char Cncoding
     */
    final MultipartContext multipartContext;

    /*
     * Listener to notify
     */
    final NioMultipartParserListener nioMultipartParserListener;

    /*
     * Factory that will be used to get an OutputStream where to store a multipart body and retrieve its related
     * OutputStream
     */
    final PartBodyStreamStorageFactory partBodyStreamStorageFactory;

    /*
     * A reusable buffer to identify when a preamble, part section or headers section is finished.
     */
    final EndOfLineBuffer endOfLineBuffer;

    /**
     * A reusable in memory output stream to process the headers
     */
    final ByteArrayOutputStream headersByteArrayOutputStream;

    /**
     * Controls how many nested multipart request can be processed.
     */
    final int maxLevelOfNestedMultipart;

    /*
    * Allows to identify the delimiter type
    */
    final DelimiterType delimiterType = new DelimiterType();

    /*
    * Stack of delimiters. Using a stack to support nested multipart requests.
    */
    final Stack<byte[]> delimiterPrefixes = new Stack<byte[]>();

    /*
     * If debug mode is enabled it keeps track of the FSM transitions
     */
    final List<String> fsmTransitions = new ArrayList<String>();

    /*
     * A reusable write context passed between the states during the data processing.
     * The context will be re-set at each write
     */
    final WriteContext wCtx = new WriteContext();

    /*
     * Current state of the ASF
     */
    volatile State currentState = State.SKIP_PREAMBLE;

    /*
     * Current output stream where to flush the body data.
     * It will be instantiated for each part via {@link BodyStreamFactory#getOutputStream(Map, int)} )}
     */
    volatile StreamStorage partBodyStreamStorage = null;

    /*
     * The current headers.
     */
    volatile Map<String, List<String>> headers = null;


    /*
     * Keeps track of how many parts we encountered
     */
    volatile int partIndex = 1;

    /**
     * Close/open status of the output stram
     */
    volatile AtomicBoolean closed = new AtomicBoolean(false);

    // ------------
    // Constructors
    // ------------

    /**
     * <p> Constructs a {@code NioMultipartParser}. The default values for the buffer size, headers section size and nested multipart limit will be used.
     *     The {@link PartBodyStreamStorageFactory} used will be the default implementation provided with the library. See {@link DefaultPartBodyStreamStorageFactory}.
     *
     * @param multipartContext The multipart context
     * @param nioMultipartParserListener The listener that will be notified
     */
    public NioMultipartParser(final MultipartContext multipartContext, final NioMultipartParserListener nioMultipartParserListener) {
        this(multipartContext, nioMultipartParserListener, null, DEFAULT_BUFFER_SIZE, DEFAULT_HEADERS_SECTION_SIZE, DEFAULT_MAX_LEVEL_OF_NESTED_MULTIPART);
    }

    /**
     * <p> Constructs a {@code NioMultipartParser} with default values for the buffer size, headers section size and nested multipart limit, but a
     *     custom implementation of {@code PartBodyStreamStorageFactory}.
     *
     * @param multipartContext The multipart context
     * @param nioMultipartParserListener The listener that will be notified
     * @param partBodyStreamStorageFactory The custom {@code PartBodyStreamStorageFactory}.
     */
    public NioMultipartParser(final MultipartContext multipartContext, final NioMultipartParserListener nioMultipartParserListener, final PartBodyStreamStorageFactory partBodyStreamStorageFactory) {
        this(multipartContext, nioMultipartParserListener, partBodyStreamStorageFactory, DEFAULT_BUFFER_SIZE, DEFAULT_HEADERS_SECTION_SIZE, DEFAULT_MAX_LEVEL_OF_NESTED_MULTIPART);
    }

    /**
     * <p> Constructs a {@code NioMultipartParser} with default values for the headers section size and nested multipart limit and {@link PartBodyStreamStorageFactory}.
     *     It wants the size of the buffer to use.
     *
     * @param multipartContext The multipart context
     * @param nioMultipartParserListener The listener that will be notified
     * @param bufferSize The buffer size
     */
    public NioMultipartParser(final MultipartContext multipartContext, final NioMultipartParserListener nioMultipartParserListener, final int bufferSize) {
        this(multipartContext, nioMultipartParserListener, null, bufferSize, DEFAULT_HEADERS_SECTION_SIZE, DEFAULT_MAX_LEVEL_OF_NESTED_MULTIPART);
    }

    /**
     * <p> Constructs a {@code NioMultipartParser}.
     *
     * @param multipartContext The multipart context
     * @param nioMultipartParserListener The listener that will be notified
     * @param partBodyStreamStorageFactory The custom {@code PartBodyStreamStorageFactory} to use.
     * @param bufferSize The buffer size
     * @param maxHeadersSectionSize The max size of the headers section
     * @param maxLevelOfNestedMultipart the max number of nested multipart
     */
    public NioMultipartParser(final MultipartContext multipartContext,
                              final NioMultipartParserListener nioMultipartParserListener,
                              final PartBodyStreamStorageFactory partBodyStreamStorageFactory,
                              final int bufferSize,
                              final int maxHeadersSectionSize,
                              final int maxLevelOfNestedMultipart) {
        this.multipartContext = multipartContext;
        this.nioMultipartParserListener = nioMultipartParserListener;
        this.delimiterPrefixes.push(getDelimiterPrefix(multipartContext.getContentType()));
        this.maxLevelOfNestedMultipart = maxLevelOfNestedMultipart;

        if (maxHeadersSectionSize == -1) {
            this.headersByteArrayOutputStream = new ByteArrayOutputStream();
        } else {
            this.headersByteArrayOutputStream = new FixedSizeByteArrayOutputStream(maxHeadersSectionSize);
        }

        if (partBodyStreamStorageFactory != null) {
            this.partBodyStreamStorageFactory = partBodyStreamStorageFactory;
        } else {
            this.partBodyStreamStorageFactory = new DefaultPartBodyStreamStorageFactory();
        }

        // At the beginning set up the endOfLineBuffer to skip the preamble.
        this.endOfLineBuffer = new EndOfLineBuffer(bufferSize, getPreambleDelimiterPrefix(delimiterPrefixes.peek()), null);
    }

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            if (partBodyStreamStorage != null) {
                partBodyStreamStorage.close();
            }
        }
    }

    @Override
    public boolean dispose() {
        try {
            close();
        } catch(IOException e) {
            // Do nothing
        }
        if (partBodyStreamStorage != null) {
            return partBodyStreamStorage.dispose();
        }
        return true;
    }

    @Override
    public void flush() throws IOException {
        if (partBodyStreamStorage != null) {
            partBodyStreamStorage.flush();
        }
    }

    @Override
    public void write(final int data) throws IOException {
        write(new byte[]{(byte) data}, 0, 1);
    }

    @Override
    public void write(byte[] data) throws IOException {
        write(data, 0, data.length);
    }

    @Override
    public void write(byte[] data, int indexStart, int indexEnd) {

        if (closed.get()){
            throw new IllegalStateException("Cannot write, the parser is closed.");
        }

        if (data == null) {
            goToState(State.ERROR);
            throw new IllegalArgumentException("Data cannot be null");
        }

        if (data.length == 0) {
            return;
        }

        if (indexEnd < indexStart) {
            goToState(State.ERROR);
            throw new IllegalArgumentException("End index cannot be lower that the start index. End index: " + indexEnd + ", Start index: " + indexStart);
        }

        if (indexStart > data.length) {
            goToState(State.ERROR);
            throw new IllegalArgumentException("The start index cannot be greater than the size of the data. Start index: " + indexStart + ", Data length: " + data.length);
        }

        if (indexEnd > data.length) {
            goToState(State.ERROR);
            throw new IllegalArgumentException("The end index cannot be greater than the size of the data. End index: " + indexEnd + ", Data length: " + data.length);
        }

        wCtx.init(indexStart, indexEnd, data, false);
        while (!wCtx.finished) {
            switch (currentState) {

                case SKIP_PREAMBLE:
                    skipPreamble(wCtx);
                    break;

                case IDENTIFY_PREAMBLE_DELIMITER:
                    identifyPreambleDelimiter(wCtx);
                    break;

                case GET_READY_FOR_HEADERS:
                    getReadyForHeaders(wCtx);
                    break;

                case READ_HEADERS:
                    readHeaders(wCtx);
                    break;

                case GET_READY_FOR_BODY:
                    getReadyForBody(wCtx);
                    break;

                case READ_BODY:
                    readBody(wCtx);
                    break;

                case IDENTIFY_BODY_DELIMITER:
                    identifyBodyDelimiter(wCtx);
                    break;

                case PART_COMPLETE:
                    partComplete(wCtx);
                    break;

                case GET_READY_FOR_NESTED_MULTIPART:
                    getReadyForNestedMultipart(wCtx);
                    break;

                case NESTED_PART_READ:
                    nestedPartRead(wCtx);
                    break;

                case ALL_PARTS_READ:
                    allPartsRead(wCtx);
                    break;

                case SKIP_EPILOGUE:
                    skipEpilogue(wCtx);
                    break;

                case ERROR:
                    throw new IllegalStateException("Parser is in an error state.");

                default:
                    // This should never happen...
                    throw new IllegalStateException("Unknown state");

            }
        }
    }

    // Convenience method to switch state. If debug is enabled il will save the transition sequence.
    void goToState(final State nextState) {
        if (log.isDebugEnabled()) {
            fsmTransitions.add(String.format("%-30s --> %s", currentState.name(), nextState.name()));
        }
        currentState = nextState;
    }

    void skipPreamble(final WriteContext wCtx) {
        int byteOfData;
        while ((byteOfData = wCtx.read()) != -1) {
            if (endOfLineBuffer.write((byte)byteOfData)) {
                goToState(State.IDENTIFY_PREAMBLE_DELIMITER);
                wCtx.setFinishedIfNoMoreData();
                return;
            }
        }
        wCtx.setFinishedIfNoMoreData();
    }

    void getReadyForHeaders(final WriteContext wCtx) {
        headersByteArrayOutputStream.reset();
        endOfLineBuffer.recycle(HEADER_DELIMITER, headersByteArrayOutputStream);
        headers = new HashMap<String, List<String>>();
        goToState(State.READ_HEADERS);
        wCtx.setFinishedIfNoMoreData();
    }


    void readHeaders(final WriteContext wCtx) {
        int byteOfData;
        while ((byteOfData = wCtx.read()) != -1) {
            if (endOfLineBuffer.write((byte)byteOfData)) {
                parseHeaders();
                String contentType = MultipartUtils.getHeader(MultipartUtils.CONTENT_TYPE, headers);
                if (MultipartUtils.isMultipart(contentType)) {
                    goToState(State.GET_READY_FOR_NESTED_MULTIPART);
                } else {
                    goToState(State.GET_READY_FOR_BODY);
                }
                wCtx.setFinishedIfNoMoreData();
                return;
            }
        }
        wCtx.setFinishedIfNoMoreData();
    }

    void parseHeaders() {
        try {
            headers = HeadersParser.parseHeaders(new ByteArrayInputStream(headersByteArrayOutputStream.toByteArray()), multipartContext.getCharEncoding());
            headersByteArrayOutputStream.reset();
        } catch (Exception e) {
            goToState(State.ERROR);
            nioMultipartParserListener.onError("Error parsing the part headers", e);
        }
    }

    void getReadyForBody(final WriteContext wCtx) {
        partBodyStreamStorage = partBodyStreamStorageFactory.newStreamStorageForPartBody(headers, partIndex);
        endOfLineBuffer.recycle(delimiterPrefixes.peek(), partBodyStreamStorage);
        delimiterType.reset();
        goToState(State.READ_BODY);
        wCtx.setFinishedIfNoMoreData();
    }

    void getReadyForNestedMultipart(final WriteContext wCtx) {
        if (delimiterPrefixes.size() > maxLevelOfNestedMultipart + 1) {
            goToState(State.ERROR);
            nioMultipartParserListener.onError("Reached maximum number of nested multiparts: " + maxLevelOfNestedMultipart, null);
        } else {
            byte[] delimiter = getDelimiterPrefix(MultipartUtils.getHeader(MultipartUtils.CONTENT_TYPE, headers));
            delimiterType.reset();
            delimiterPrefixes.push(delimiter);
            endOfLineBuffer.recycle(getPreambleDelimiterPrefix(delimiter), null);
            goToState(State.SKIP_PREAMBLE);
            nioMultipartParserListener.onNestedPartStarted(headers);
        }
        wCtx.setFinishedIfNoMoreData();
    }

    void readBody(final WriteContext wCtx) {
        int byteOfData;
        while ((byteOfData = wCtx.read()) != -1) {
            if (endOfLineBuffer.write((byte)byteOfData)) {
                goToState(State.IDENTIFY_BODY_DELIMITER);
                wCtx.setFinishedIfNoMoreData();
                return;
            }
        }
        wCtx.setFinishedIfNoMoreData();
    }

    void identifyPreambleDelimiter(final WriteContext wCtx) {
        if (delimiterPrefixes.size() > 1) {
            identifyDelimiter(wCtx, State.GET_READY_FOR_HEADERS, State.NESTED_PART_READ);
        } else {
            identifyDelimiter(wCtx, State.GET_READY_FOR_HEADERS, State.ALL_PARTS_READ);
        }
    }

    void identifyBodyDelimiter(final WriteContext ctx) {
        identifyDelimiter(ctx, State.PART_COMPLETE, State.PART_COMPLETE);
    }

    void identifyDelimiter(final WriteContext wCtx, final State onDelimiter, final State onCloseDelimiter) {
        int byteOfData;
        while ((byteOfData = wCtx.read()) != -1) {
            delimiterType.addDelimiterByte((byte)byteOfData);
            if (delimiterType.index >= 2) {

                DelimiterType.Type type = delimiterType.getDelimiterType();

                if (DelimiterType.Type.ENCAPSULATION == type) {
                    goToState(onDelimiter);
                    wCtx.setFinishedIfNoMoreData();
                    return;
                } else if (DelimiterType.Type.CLOSE == type) {
                    goToState(onCloseDelimiter);
                    // Need to continue because we encountered a close delimiter and we might not have more data coming
                    // but we want to switch state and notify.
                    wCtx.setNotFinished();
                    return;
                } else {
                    goToState(State.ERROR);
                    nioMultipartParserListener.onError("Unexpected characters follow a boundary", null);
                    wCtx.setFinished();
                    return;
                }
            }
        }
        wCtx.setFinishedIfNoMoreData();

    }

    void allPartsRead(final WriteContext wCtx) {
        goToState(State.SKIP_EPILOGUE);
        nioMultipartParserListener.onAllPartsFinished();
        wCtx.setFinishedIfNoMoreData();
    }

    void partComplete(final WriteContext wCtx){

        // First flush the output stream and close it...
        try{
            partBodyStreamStorage.flush();
            partBodyStreamStorage.close();
        }catch (Exception e){
            goToState(State.ERROR);
            nioMultipartParserListener.onError("Unable to read/write the body data", e);
            return;
        }

        // Switch state
        if (delimiterType.getDelimiterType() == DelimiterType.Type.CLOSE){
            if (delimiterPrefixes.size() > 1){
                goToState(State.NESTED_PART_READ);
            }else {
                goToState(State.ALL_PARTS_READ);
            }
        }else {
            goToState(State.GET_READY_FOR_HEADERS);
        }

        // Notify
        if (MultipartUtils.isFormField(headers)){
            // It's a form field, need to read the input stream into String and notify via onFormFieldPartFinished(...)
            final InputStream partBodyInputStream =  partBodyStreamStorage.getInputStream();
            try {
                final String fieldName = MultipartUtils.getFieldName(headers);
                final String value = IOUtils.inputStreamAsString(partBodyInputStream, MultipartUtils.getCharEncoding(headers));
                nioMultipartParserListener.onFormFieldPartFinished(fieldName, value, headers);
            }catch (Exception e){
                goToState(State.ERROR);
                nioMultipartParserListener.onError("Unable to read the form parameters", e);
                return;
            }finally {
                IOUtils.closeQuietly(partBodyInputStream);
            }

        }else{
            // Not a form field. Provide the raw input stream to the client.
            nioMultipartParserListener.onPartFinished(partBodyStreamStorage, headers);
        }

        partIndex++;
        wCtx.setFinishedIfNoMoreData();

    }

    void nestedPartRead(final WriteContext wCtx){
        delimiterPrefixes.pop();
        delimiterType.reset();
        endOfLineBuffer.recycle(getPreambleDelimiterPrefix(delimiterPrefixes.peek()), null);
        goToState(State.SKIP_PREAMBLE);
        nioMultipartParserListener.onNestedPartFinished();
        wCtx.setFinishedIfNoMoreData();
    }

    void skipEpilogue(final WriteContext wCtx){
        wCtx.setFinished();
    }

    static byte[] getBoundary(final String contentType) {
        ParameterParser parser = new ParameterParser();
        parser.setLowerCaseNames(true);
        // Parameter parser can handle null input
        Map<String, String> params = parser.parse(contentType, new char[] {';', ','});
        String boundaryStr = params.get("boundary");

        if (boundaryStr == null) {
            return null;
        }
        byte[] boundary;
        try {
            boundary = boundaryStr.getBytes("ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            boundary = boundaryStr.getBytes(); // Intentionally falls back to default charset
        }
        return boundary;
    }

    static byte[] getPreambleDelimiterPrefix(final byte[] delimiterPrefix){

        // This allows to parse multipart bodies starting with a delimiter.
        // From the specs, a delimiter is always preceded by a CR,LF but commons file upload supports it.

        // Remove the CR,LF from the delimiterPrefix
        byte[] preambleDelimiterPrefix = new byte[delimiterPrefix.length-2];
        System.arraycopy(delimiterPrefix, 2, preambleDelimiterPrefix, 0, delimiterPrefix.length -2);
        return preambleDelimiterPrefix;
    }

    static byte[] getDelimiterPrefix(final String contentType){

        byte[] boundary = getBoundary(contentType);
        if (boundary == null || boundary.length == 0){
            throw new IllegalStateException("Invalid boundary in the content type" + contentType);
        }
        byte[] delimiterPrefix = new byte[boundary.length + 4];
        delimiterPrefix[0] = CR;
        delimiterPrefix[1] = LF;
        delimiterPrefix[2] = DASH;
        delimiterPrefix[3] = DASH;
        System.arraycopy(boundary, 0, delimiterPrefix, 4, boundary.length);

        return delimiterPrefix;
    }

    public List<String> geFsmTransitions(){
        if (log.isDebugEnabled()) {
            return fsmTransitions;
        }else{
            return null;
        }
    }

}

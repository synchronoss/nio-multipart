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

package org.synchronoss.cloud.reactive.multipart;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.synchronoss.cloud.nio.multipart.MultipartContext;
import org.synchronoss.cloud.nio.multipart.MultipartUtils;
import org.synchronoss.cloud.nio.multipart.util.HeadersParser;
import org.synchronoss.cloud.reactive.multipart.io.EndOfLineBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.synchronoss.cloud.nio.multipart.MultipartUtils.*;

/**
 * <p> A non blocking multipart parser based on reactive streams.
 *
 * @author Silvano Riz.
 */
public class MultipartParser {

    private static final Logger log = LoggerFactory.getLogger(MultipartParser.class);

    /*
     * The multipart context. Content-Type, Content-Length and Char Cncoding
     */
    final MultipartContext multipartContext;

    /*
     * Stack of delimiters. Using a stack to support nested multipart requests.
     */
    final Stack<byte[]> delimiterPrefixes = new Stack<byte[]>();

    /**
     * Controls how many nested multipart request can be processed.
     */
    final int maxLevelOfNestedMultipart;

    /*
     * A reusable buffer to identify when a preamble, part section or headers section is finished.
     */
    final EndOfLineBuffer endOfLineBuffer;

    /*
     * A reusable write context passed between the states during the data processing.
     * The context will be re-set at each write
     */
    final WriteContext wCtx = new WriteContext();


    final HeadersProducer headersConsumer = new HeadersProducer();

    /*
     * If debug mode is enabled it keeps track of the FSM transitions
     */
    final List<String> fsmTransitions = new ArrayList<String>();

    /*
     * Allows to identify the delimiter type
     */
    final DelimiterType delimiterType = new DelimiterType();

    /*
     * The current headers.
     */
    volatile Map<String, List<String>> headers = null;

    /*
     * Current state of the ASF
     */
    volatile State currentState = State.READY;

    /*
     * Keeps track of how many parts we encountered
     */
    volatile int partIndex = 1;

    /**
     * <p> Result of each write into the end of line buffer
     */
    volatile EndOfLineBuffer.WriteResult writeResult;

    /**
     * <p> Parsed data read from the end of line buffer
     */
    volatile byte[] parsedData;

    /**
     * <p> The subscription provided by the publisher that is producing the un-parsed data.
     */
    volatile Subscription subscription;

    /**
     * <p> Emitter that produced Parts
     */
    volatile FluxSink<Part> emitter;

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
        private Subscription subscription;

        void init(final byte[] data, final Subscription subscription) {
            this.currentIndex = 0;
            this.indexEnd = data.length;
            this.data = data;
            this.finished = currentIndex >= indexEnd;
            this.subscription = subscription;
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

        void setFinishedIfNoMoreDataAndAskForMore() {
            setFinishedIfNoMoreData();
            subscription.request(1);
        }

        void setFinished() {
            finished = true;
        }
    }

    private static class HeadersProducer {

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        void write(final byte[] headersData){
            try {
                baos.write(headersData);
            }catch (Exception e){
                throw new IllegalStateException("Error parsing the headers", e);
            }
        }

        Map<String, List<String>> produce(final String charEncoding){
            Map<String, List<String>> headers = HeadersParser.parseHeaders(new ByteArrayInputStream(baos.toByteArray()), charEncoding);
            baos.reset();
            return headers;
        }

    }

    // FSM States
    private enum State {
        READY,
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

    // ------------
    // Constructors
    // ------------

    /**
     * <p> Creates a new multipart parser
     *
     * @param multipartContext The multipart context
     * @param bufferSize The buffer size, a strictly positive integer.
     *                   The actual buffer size used will be {@link MultipartUtils#getBoundary(String)} + 5 + bufferSize.
     * @param maxLevelOfNestedMultipart the max number of nested multipart
     */
    public MultipartParser(final MultipartContext multipartContext,
                           final int bufferSize,
                           final int maxLevelOfNestedMultipart) {

        if (bufferSize <= 0){
            throw new IllegalArgumentException("The buffer size must be grater than 0. Size specified: " + bufferSize);
        }

        this.multipartContext = multipartContext;
        final byte[] delimiterPrefix = getDelimiterPrefix(multipartContext.getContentType());
        final int actualBufferSize = delimiterPrefix.length + bufferSize;
        this.delimiterPrefixes.push(delimiterPrefix);
        this.maxLevelOfNestedMultipart = maxLevelOfNestedMultipart;

        // At the beginning set up the endOfLineBuffer to skip the preamble.
        this.endOfLineBuffer = new EndOfLineBuffer(actualBufferSize, getPreambleDelimiterPrefix(delimiterPrefixes.peek()));
    }

    /**
     * <p> Start parsing. This method can me called just once, otherwise it will throw an {@link IllegalStateException}.
     *
     * @param inputData The {@link Publisher} that produces the multipart data.
     * @return A {@link Flux} that produces the parsed {@link Part}s
     */
    public Flux<Part> parse(final Publisher<byte[]> inputData){

        if (State.READY != currentState){
            throw new IllegalStateException("Parser in an invalid state.");
        }else{
            goToState(State.SKIP_PREAMBLE);
        }
        
        return Flux.create(new Consumer<FluxSink<Part>>() {
            @Override
            public void accept(final FluxSink<Part> emitter) {

                MultipartParser.this.emitter = emitter;

                inputData.subscribe(new Subscriber<byte[]>() {

                    @Override
                    public void onSubscribe(Subscription subscription) {
                        if (log.isTraceEnabled()) log.trace("onSubscribe : " + subscription);
                        MultipartParser.this.subscription = subscription;
                        subscription.request(1);
                    }

                    @Override
                    public void onNext(byte[] bytes) {
                        if (log.isTraceEnabled()) log.trace(String.format("%-30s | onNext | %s", currentState.name(), Arrays.toString(bytes)));
                        write(bytes);
                    }

                    @Override
                    public void onError(Throwable t) {
                        if (log.isTraceEnabled()) log.trace("onError : " + t);
                        emitter.error(t);
                    }

                    @Override
                    public void onComplete() {
                        if (log.isTraceEnabled()) log.trace("onComplete");
                        emitter.complete();
                    }
                });

            }
        });

    }

    void write(final byte[] data) {

        if (data == null) {
            goToState(State.ERROR);
            throw new IllegalArgumentException("Data cannot be null");
        }

        if (data.length == 0) {
            return;
        }

        wCtx.init(data, subscription);
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

    // Convenience method to switch state. If debug is enabled it will save the transition sequence.
    void goToState(final MultipartParser.State nextState) {
        if (log.isTraceEnabled()) log.trace(String.format("%-30s --> %s", currentState.name(), nextState.name()));
        if (log.isDebugEnabled()) {
            fsmTransitions.add(String.format("%-30s --> %s", currentState.name(), nextState.name()));
        }
        currentState = nextState;
    }

    void skipPreamble(final WriteContext wCtx) {
        int byteOfData;
        while ((byteOfData = wCtx.read()) != -1) {
            writeResult = endOfLineBuffer.write((byte)byteOfData);
            if (writeResult.isEol()) {
                goToState(State.IDENTIFY_PREAMBLE_DELIMITER);
                break;
            }else if (writeResult.isFull()){
                endOfLineBuffer.trash();
            }
        }
        wCtx.setFinishedIfNoMoreDataAndAskForMore();
    }

    void getReadyForHeaders(final WriteContext wCtx) {
        endOfLineBuffer.recycle(HEADER_DELIMITER);
        headers = new HashMap<String, List<String>>();
        goToState(State.READ_HEADERS);
        wCtx.setFinishedIfNoMoreData();
    }

    void readHeaders(final WriteContext wCtx) {
        int byteOfData;
        while ((byteOfData = wCtx.read()) != -1) {
            writeResult = endOfLineBuffer.write((byte)byteOfData);
            if (writeResult.isEol()) {
                headersConsumer.write(endOfLineBuffer.read());
                parseHeaders();
                String contentType = MultipartUtils.getHeader(MultipartUtils.CONTENT_TYPE, headers);
                if (MultipartUtils.isMultipart(contentType)) {
                    goToState(State.GET_READY_FOR_NESTED_MULTIPART);
                } else {
                    goToState(State.GET_READY_FOR_BODY);
                }
                wCtx.setFinishedIfNoMoreDataAndAskForMore();
                return;
            }else if (writeResult.isFull()){
                headersConsumer.write(endOfLineBuffer.read());
            }
        }
        wCtx.setFinishedIfNoMoreDataAndAskForMore();
    }

    void parseHeaders() {
        try {
            headers = headersConsumer.produce(multipartContext.getCharEncoding());
            if (log.isTraceEnabled()) log.trace("Headers: \n" + headers.entrySet().stream().map(entry -> entry.getKey() + " - " + entry.getValue()).collect(Collectors.joining("\n")));
        } catch (Exception e) {
            goToState(State.ERROR);
            emitter.error(new IllegalStateException("Error parsing the part headers", e));
        }
    }

    void getReadyForBody(final WriteContext wCtx) {
        // TODO - this logic should change. Prepare a subscriber that will accept the attachment data.
        //partBodyStreamStorage = partBodyStreamStorageFactory.newStreamStorageForPartBody(headers, partIndex);

        Flux.create(new Consumer<FluxSink<? extends Object>>() {
            @Override
            public void accept(FluxSink<? extends Object> fluxSink) {

            }
        });

        endOfLineBuffer.recycle(delimiterPrefixes.peek());
        delimiterType.reset();
        goToState(State.READ_BODY);
        wCtx.setFinishedIfNoMoreData();
    }

    void getReadyForNestedMultipart(final WriteContext wCtx) {
        if (delimiterPrefixes.size() > maxLevelOfNestedMultipart + 1) {
            goToState(State.ERROR);
            emitter.error(new IllegalStateException("Reached maximum number of nested multiparts: " + maxLevelOfNestedMultipart));
        } else {
            byte[] delimiter = getDelimiterPrefix(MultipartUtils.getHeader(MultipartUtils.CONTENT_TYPE, headers));
            delimiterType.reset();
            delimiterPrefixes.push(delimiter);
            endOfLineBuffer.recycle(getPreambleDelimiterPrefix(delimiter));
            goToState(State.SKIP_PREAMBLE);
            // TODO - this logic should change. Prepare a subscriber that will accept the attachment data.
            //nioMultipartParserListener.onNestedPartStarted(headers);
        }
        wCtx.setFinishedIfNoMoreData();
    }

    // TODO - Make it pub-sub
    void readBody(final WriteContext wCtx) {
        int byteOfData;
        while ((byteOfData = wCtx.read()) != -1) {
            writeResult = endOfLineBuffer.write((byte)byteOfData);
            if (writeResult.isEol()) {
                byte[] read = endOfLineBuffer.read();
                log.info("Body data: " + Arrays.toString(read));
                goToState(State.IDENTIFY_BODY_DELIMITER);
                wCtx.setFinishedIfNoMoreDataAndAskForMore();
                return;
            }else if (writeResult.isFull()){
                byte[] read = endOfLineBuffer.read();
                log.info("Body data: " + Arrays.toString(read));
            }
        }
        wCtx.setFinishedIfNoMoreDataAndAskForMore();// TODO - the subscriber should ask for more data here
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

    void identifyDelimiter(final WriteContext wCtx, final MultipartParser.State onDelimiter, final MultipartParser.State onCloseDelimiter) {
        int byteOfData;
        while ((byteOfData = wCtx.read()) != -1) {
            delimiterType.addDelimiterByte((byte)byteOfData);
            if (delimiterType.index >= 2) {

                DelimiterType.Type type = delimiterType.getDelimiterType();

                if (DelimiterType.Type.ENCAPSULATION == type) {
                    goToState(onDelimiter);
                    wCtx.setFinishedIfNoMoreDataAndAskForMore();
                    return;
                } else if (DelimiterType.Type.CLOSE == type) {
                    goToState(onCloseDelimiter);
                    // Need to continue because we encountered a close delimiter and we might not have more data coming
                    // but we want to switch state and notify.
                    wCtx.setNotFinished();
                    return;
                } else {
                    goToState(State.ERROR);
                    emitter.error(new IllegalStateException("Unexpected characters follow a boundary"));
                    wCtx.setFinished();
                    return;
                }
            }
        }
        wCtx.setFinishedIfNoMoreDataAndAskForMore();

    }

    void allPartsRead(final WriteContext wCtx) {
        goToState(State.SKIP_EPILOGUE);
        wCtx.setFinishedIfNoMoreDataAndAskForMore();
        emitter.complete();
    }

    void partComplete(final WriteContext wCtx){

        // First flush the output stream and close it...
//        try{
//            partBodyStreamStorage.flush();
//            partBodyStreamStorage.close();
//        }catch (Exception e){
//            goToState(State.ERROR);
//            emitter.error(new IllegalStateException("Unable to read/write the body data", e));
//            return;
//        }

        emitter.next(new AttachmentPart(headers));// TODO - The subscriber should emit...

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

        // TODO - this logic should change.
        // Notify
//        if (MultipartUtils.isFormField(headers)){
//            // It's a form field, need to read the input stream into String and notify via onFormFieldPartFinished(...)
//            final InputStream partBodyInputStream =  partBodyStreamStorage.getInputStream();
//            try {
//                final String fieldName = MultipartUtils.getFieldName(headers);
//                final String value = IOUtils.inputStreamAsString(partBodyInputStream, MultipartUtils.getCharEncoding(headers));
//                nioMultipartParserListener.onFormFieldPartFinished(fieldName, value, headers);
//            }catch (Exception e){
//                goToState(State.ERROR);
//                nioMultipartParserListener.onError("Unable to read the form parameters", e);
//                return;
//            }finally {
//                IOUtils.closeQuietly(partBodyInputStream);
//            }
//
//        }else{
//            // Not a form field. Provide the raw input stream to the client.
//            nioMultipartParserListener.onPartFinished(partBodyStreamStorage, headers);
//        }

        partIndex++;
        wCtx.setFinishedIfNoMoreData();

    }

    void nestedPartRead(final WriteContext wCtx){
        delimiterPrefixes.pop();
        delimiterType.reset();
        endOfLineBuffer.recycle(getPreambleDelimiterPrefix(delimiterPrefixes.peek()));
        goToState(State.SKIP_PREAMBLE);
        // TODO - this logic should change. 
        //nioMultipartParserListener.onNestedPartFinished();
        wCtx.setFinishedIfNoMoreData();
    }

    void skipEpilogue(final WriteContext wCtx){
        wCtx.setFinished();
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

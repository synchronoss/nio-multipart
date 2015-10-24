package com.synchronoss.cloud.nio.multipart;

import com.synchronoss.cloud.nio.multipart.BodyStreamFactory.PartOutputStream;
import com.synchronoss.cloud.nio.multipart.buffer.EndOfLineBuffer;
import com.synchronoss.cloud.nio.multipart.buffer.FixedSizeByteArrayOutputStream;
import com.synchronoss.cloud.nio.multipart.util.HeadersParser;
import com.synchronoss.cloud.nio.multipart.util.ParameterParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * <p>
 *    {@link OutputStream} implementing a Non blocking IO Multipart Parser.
 *</p>
 * Created by sriz0001 on 15/10/2015.
 */
public class NioMultipartParser extends OutputStream {

    private static final Logger log = LoggerFactory.getLogger(NioMultipartParser.class);

    public static final byte DASH = 0x2D;
    public static final byte CR = 0x0D;
    public static final byte LF = 0x0A;

    public static final String CHARACTER_SET = "US-ASCII";
    public static final int DEFAULT_BUFFER_SIZE = 16000;//16Kb default. It needs to be bigger than the separator. (70 Characters)
    public static final byte[] HEADER_DELIMITER = {CR, LF, CR, LF};
    public static final int DEFAULT_MAX_LEVEL_OF_NESTED_MULTIPART = 1;

    /**
     * The type of a delimiter is identified using its suffix.
     * For example if the boundary is "XVZ", the sequence
     * DASH,DASH,X,W,Z,CR,LF represents an encapsulation boundary, while the
     * sequence DASH,DASH,X,V,Z,DASH,DASH is the close boundary.
     */
    protected enum DelimiterType {

        CLOSE(new byte[]{DASH, DASH}),
        ENCAPSULATION(new byte[]{CR, LF});
        final byte[] delimiterSuffix;

        DelimiterType(byte[] delimiterSuffix) {
            this.delimiterSuffix = delimiterSuffix;
        }

        boolean matches(byte[] delimiterSuffixToMatch){
            if (delimiterSuffix.length != delimiterSuffixToMatch.length){
                return false;
            }
            for (int i = 0; i < delimiterSuffixToMatch.length; i++) {
                if (delimiterSuffix[i] != delimiterSuffixToMatch[i]) {
                    return false;
                }
            }
            return true;
        }
    }

    protected enum State {
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

    final MultipartContext multipartContext;
    final NioMultipartParserListener nioMultipartParserListener;
    final BodyStreamFactory bodyStreamFactory;
    final EndOfLineBuffer endOfLineBuffer;
    final ByteArrayOutputStream formFieldOutputStream = new ByteArrayOutputStream();
    final ByteArrayOutputStream byteArrayOutputStream;
    final int maxLevelOfNestedMultipart;

    // Current state of the ASF
    State currentState = State.SKIP_PREAMBLE;

    // Current output stream where to flush the body data.
    // It will be instantiated for each part via {@link BodyStreamFactory#getOutputStream(Map, int)} )}
    OutputStream outputStream = null;

    // The current headers.
    Map<String, List<String>> headers = null;

    // Keeps track of the delimiter type encountered
    DelimiterType delimiterType = null;

    // Keeps track of how many parts we encountered
    int partIndex = 1;

    // Stack of delimiters. To support nested multiparts...
    final Stack<byte[]> delimiterPrefixes = new Stack<byte[]>();

    // In debug mode is keeping track of the FSM transitions
    final List<String> fsmTransitions = new ArrayList<String>();

    // ------------
    // Constructors
    // ------------
    public NioMultipartParser(final MultipartContext multipartContext, final NioMultipartParserListener nioMultipartParserListener) {
        this(multipartContext, nioMultipartParserListener, null, DEFAULT_BUFFER_SIZE, DEFAULT_BUFFER_SIZE, DEFAULT_MAX_LEVEL_OF_NESTED_MULTIPART);
    }

    public NioMultipartParser(final MultipartContext multipartContext, final NioMultipartParserListener nioMultipartParserListener, final BodyStreamFactory bodyStreamFactory) {
        this(multipartContext, nioMultipartParserListener, bodyStreamFactory, DEFAULT_BUFFER_SIZE, DEFAULT_BUFFER_SIZE, DEFAULT_MAX_LEVEL_OF_NESTED_MULTIPART);
    }

    public NioMultipartParser(final MultipartContext multipartContext, final NioMultipartParserListener nioMultipartParserListener, final int bufferSize) {
        this(multipartContext, nioMultipartParserListener, null, bufferSize, DEFAULT_BUFFER_SIZE, DEFAULT_MAX_LEVEL_OF_NESTED_MULTIPART);
    }

    public NioMultipartParser(final MultipartContext multipartContext,
                              final NioMultipartParserListener nioMultipartParserListener,
                              final BodyStreamFactory bodyStreamFactory,
                              final int bufferSize,
                              final int maxHeadersSectionSize,
                              final int maxLevelOfNestedMultipart) {
        this.multipartContext = multipartContext;
        this.nioMultipartParserListener = nioMultipartParserListener;
        this.delimiterPrefixes.push(getDelimiterPrefix(multipartContext.getContentType()));
        this.maxLevelOfNestedMultipart = maxLevelOfNestedMultipart;

        if (maxHeadersSectionSize == -1) {
            this.byteArrayOutputStream = new ByteArrayOutputStream();
        }else{
            this.byteArrayOutputStream = new FixedSizeByteArrayOutputStream(maxHeadersSectionSize);
        }

        if (bodyStreamFactory != null){
            this.bodyStreamFactory = bodyStreamFactory;
        }else{
            // By default use a temporary file where to save the body data.
            this.bodyStreamFactory = new DefaultBodyStreamFactory();
        }

        // At the beginning set up the endOfLineBuffer to skip the preamble.
        this.endOfLineBuffer = new EndOfLineBuffer(bufferSize, getPreambleDelimiterPrefix(delimiterPrefixes.peek()), null);

        debug = this.bodyStreamFactory.getOutputStream(new HashMap<String, List<String>>(),0);
    }

    @Override
    public void close() throws IOException {
        if (outputStream != null){
            outputStream.flush();
            outputStream.close();
        }
    }

    @Override
    public void flush() throws IOException {
        // nothing to do.
    }

    @Override
    public void write(final int data) throws IOException{
        write(new byte[]{(byte) data}, 0, 1);
    }

    @Override
    public void write(byte[] data) throws IOException {
        write(data, 0, data.length);
    }

    @Override
    public void write(byte[] data, int indexStart, int indexEnd) {

        if (data == null) {
            throw new NullPointerException();
        }

        if (data.length == 0){
            return;
        }

        if (indexEnd < indexStart){
            throw new IllegalArgumentException("End index cannot be lower that the start index. End index: " + indexEnd + ", Start index: " + indexStart);
        }

        if (indexStart > data.length){
            throw new IllegalArgumentException("The start index cannot be greater than the size of the data. Start index: " + indexStart + ", Data length: " + data.length);
        }

        if (indexEnd > data.length){
            throw new IllegalArgumentException("The end index cannot be greater than the size of the data. End index: " + indexEnd + ", Data length: " + data.length);
        }

        writeForDebug(data, indexStart, indexEnd);

        int currentIndex = indexStart;
        while (currentIndex < indexEnd) {// FIXME - This might be a problem because notification depend on data available
            switch (currentState) {

                case SKIP_PREAMBLE:
                    currentIndex = skipPreamble(data, currentIndex, indexEnd);
                    break;

                case IDENTIFY_PREAMBLE_DELIMITER:
                    currentIndex = identifyPreambleDelimiter(data, currentIndex, indexEnd);
                    break;

                case GET_READY_FOR_HEADERS:
                    getReadyForHeaders();
                    break;

                case READ_HEADERS:
                    currentIndex = readHeaders(data, currentIndex, indexEnd);
                    break;

                case GET_READY_FOR_BODY:
                    getReadyForBody();
                    break;

                case READ_BODY:
                    currentIndex = readBody(data, currentIndex, indexEnd);
                    break;

                case IDENTIFY_BODY_DELIMITER:
                    currentIndex = identifyBodyDelimiter(data, currentIndex, indexEnd);
                    break;

                case PART_COMPLETE:
                    partComplete();
                    break;

                case GET_READY_FOR_NESTED_MULTIPART:
                    getReadyForNestedMultipart();
                    break;

                case NESTED_PART_READ:
                    nestedPartRead();
                    break;

                case ALL_PARTS_READ:
                    allPartsRead();
                    break;

                case SKIP_EPILOGUE:
                    currentIndex ++;
                    break;

                case ERROR:
                    throw new IllegalStateException("Parser is in an error state.");

                default:
                    // This should never happen...
                    throw new IllegalStateException("Unknown state");

            }
        }
    }


    void goToState(final State nextState){
        if (log.isDebugEnabled()){
            log.debug(String.format("%-30s --> %s", currentState.name(), nextState.name()));
            fsmTransitions.add(String.format("%-30s --> %s\n", currentState.name(), nextState.name()));
        }
        currentState = nextState;
    }

    int skipPreamble(final byte[] receivedBytes, int currentIndex, final int indexEnd){
        while (currentIndex < indexEnd) {
            if (endOfLineBuffer.write(receivedBytes[currentIndex])) {
                goToState(State.IDENTIFY_PREAMBLE_DELIMITER);
                return ++currentIndex;
            }
            currentIndex++;
        }
        return ++currentIndex;
    }

    void getReadyForHeaders(){
        byteArrayOutputStream.reset();
        endOfLineBuffer.reset(HEADER_DELIMITER, byteArrayOutputStream);
        headers = new HashMap<String, List<String>>();
        goToState(State.READ_HEADERS);
    }


    int readHeaders(final byte[] receivedBytes, int currentIndex, final int indexEnd){
        while (currentIndex < indexEnd) {
            if (endOfLineBuffer.write(receivedBytes[currentIndex])) {
                parseHeaders();

                String contentType = MultipartUtils.getHeader(MultipartUtils.CONTENT_TYPE, headers);
                if (MultipartUtils.isMultipart(contentType)){
                    goToState(State.GET_READY_FOR_NESTED_MULTIPART);
                }else {
                    goToState(State.GET_READY_FOR_BODY);
                }
                return ++currentIndex;
            }
            currentIndex++;
        }
        return ++currentIndex;
    }

    void parseHeaders() {
        try{
            // TODO - which encoding?
            headers = HeadersParser.parseHeaders(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()), CHARACTER_SET);
            byteArrayOutputStream.reset();
        }catch (Exception e){
            goToState(State.ERROR);
            nioMultipartParserListener.onError("Error parsing the part headers", e);
        }
    }

    void getReadyForBody(){
        if (MultipartUtils.isFormField(headers)){
            formFieldOutputStream.reset();
            endOfLineBuffer.reset(delimiterPrefixes.peek(), formFieldOutputStream);
        }else{
            outputStream = bodyStreamFactory.getOutputStream(headers, partIndex);
            endOfLineBuffer.reset(delimiterPrefixes.peek(), outputStream);
        }
        delimiterType = null;
        goToState(State.READ_BODY);

    }

    void getReadyForNestedMultipart(){
        if (delimiterPrefixes.size() > maxLevelOfNestedMultipart + 1){
            goToState(State.ERROR);
            nioMultipartParserListener.onError("Reached maximum number of nested multiparts: " + maxLevelOfNestedMultipart, null);
        }else {
            byte[] delimiter = getDelimiterPrefix(MultipartUtils.getHeader(MultipartUtils.CONTENT_TYPE, headers));
            delimiterPrefixes.push(delimiter);
            endOfLineBuffer.reset(getPreambleDelimiterPrefix(delimiter), null);
            goToState(State.SKIP_PREAMBLE);
            nioMultipartParserListener.onNestedPartStarted(headers);
        }
    }

    int readBody(final byte[] receivedBytes, int currentIndex, final int indexEnd){
        while (currentIndex < indexEnd) {
            if (endOfLineBuffer.write(receivedBytes[currentIndex])) {
                goToState(State.IDENTIFY_BODY_DELIMITER);
                return ++currentIndex;
            }
            currentIndex++;
        }
        return ++currentIndex;
    }

    int identifyPreambleDelimiter(final byte[] receivedBytes, int currentIndex, final int indexEnd){
        if (delimiterPrefixes.size() > 1) {
            return identifyDelimiter(receivedBytes, currentIndex, indexEnd, State.GET_READY_FOR_HEADERS, State.NESTED_PART_READ);
        }else{
            return identifyDelimiter(receivedBytes, currentIndex, indexEnd, State.GET_READY_FOR_HEADERS, State.ALL_PARTS_READ);
        }
    }

    int identifyBodyDelimiter(final byte[] receivedBytes, int currentIndex, final int indexEnd){
        return identifyDelimiter(receivedBytes, currentIndex, indexEnd, State.PART_COMPLETE, State.PART_COMPLETE);
    }

    int identifyDelimiter(final byte[] receivedBytes, int currentIndex, final int indexEnd, final State onDelimiter, final State onCloseDelimiter){
        while (currentIndex < indexEnd) {
            byteArrayOutputStream.write(receivedBytes[currentIndex]);
            if (byteArrayOutputStream.size() >= 2){

                byte[] suffix = byteArrayOutputStream.toByteArray();

                if (DelimiterType.ENCAPSULATION.matches(suffix)){
                    delimiterType = DelimiterType.ENCAPSULATION;
                    byteArrayOutputStream.reset();
                    goToState(onDelimiter);
                    return ++currentIndex;
                }else if (DelimiterType.CLOSE.matches(suffix)) {
                    delimiterType = DelimiterType.CLOSE;
                    byteArrayOutputStream.reset();
                    goToState(onCloseDelimiter);
                    return ++currentIndex;
                }else{
                    byteArrayOutputStream.reset();
                    goToState(State.ERROR);
                    nioMultipartParserListener.onError("Unexpected characters follow a boundary", null);
                    return ++currentIndex;
                }
            }
            currentIndex++;
        }
        return ++currentIndex;
    }

    void allPartsRead(){
        nioMultipartParserListener.onAllPartsRead();
        goToState(State.SKIP_EPILOGUE);
        logDebugFile();
    }

    void partComplete(){
        if (MultipartUtils.isFormField(headers)){

            final String fieldName = MultipartUtils.getFieldName(headers);
            final String value = formFieldOutputStream.toString();
            nioMultipartParserListener.onFormFieldPartComplete(fieldName, value, headers);

        }else{

            try {
                outputStream.flush();
                outputStream.close();
            }catch (Exception e){
                nioMultipartParserListener.onError("Error flushing and closing the body part output stream", e);
            }
            final String name = ((PartOutputStream)outputStream).getName();
            final InputStream partBodyInputStream =  bodyStreamFactory.getInputStream(name);
            nioMultipartParserListener.onPartComplete(partBodyInputStream, headers);

        }

        if (delimiterType == DelimiterType.CLOSE){
            if (delimiterPrefixes.size() > 1){
                goToState(State.NESTED_PART_READ);
            }else {
                goToState(State.ALL_PARTS_READ);
            }
        }else {
            goToState(State.GET_READY_FOR_HEADERS);
        }
        partIndex++;

    }

    void nestedPartRead(){
        delimiterPrefixes.pop();
        endOfLineBuffer.reset(getPreambleDelimiterPrefix(delimiterPrefixes.peek()), null);
        goToState(State.SKIP_PREAMBLE);
        nioMultipartParserListener.onNestedPartRead();
    }

    byte[] getBoundary(final String contentType) {
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

    byte[] getPreambleDelimiterPrefix(final byte[] delimiterPrefix){

        // This allows to parse multipart bodies starting with a delimiter.
        // From the specs, a delimiter is always preceded by a CR,LF but commons file upload supports it.

        // Remove the CR,LF from the delimiterPrefix
        byte[] preambleDelimiterPrefix = new byte[delimiterPrefix.length-2];
        System.arraycopy(delimiterPrefix, 2, preambleDelimiterPrefix, 0, delimiterPrefix.length -2);
        return preambleDelimiterPrefix;
    }

    byte[] getDelimiterPrefix(final String contentType){

        byte[] boundary = getBoundary(contentType);
        byte[] delimiterPrefix = new byte[boundary.length + 4];
        delimiterPrefix[0] = CR;
        delimiterPrefix[1] = LF;
        delimiterPrefix[2] = DASH;
        delimiterPrefix[3] = DASH;
        System.arraycopy(boundary, 0, delimiterPrefix, 4, boundary.length);

        return delimiterPrefix;
    }

    void closeQuietly(){
        try{
            if (outputStream != null) {
                outputStream.close();
            }
        }catch (Exception e){
            // Stay quiet!
        }
    }

    // Just for testing. It enables the possibility to capture the full multipart body into a file.
    private static final boolean DEBUG = false;
    PartOutputStream debug;
    void writeForDebug(final byte[] receivedBytes, final int indexStart, final int indexEnd){
        if (DEBUG){
            try {
                debug.write(receivedBytes, indexStart, indexEnd);
            }catch (Exception e){
                // nothing to do
            }
        }
    }

    void logDebugFile(){
        if (DEBUG){
            log.debug("Multipart request body is stored here: " + debug.getName());
        }
    }

}

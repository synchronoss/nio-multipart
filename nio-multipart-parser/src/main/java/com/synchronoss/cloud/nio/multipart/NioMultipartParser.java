package com.synchronoss.cloud.nio.multipart;

import com.synchronoss.cloud.nio.multipart.BodyStreamFactory.PartOutputStream;
import com.synchronoss.cloud.nio.multipart.buffer.EndOfLineBuffer;
import com.synchronoss.cloud.nio.multipart.util.HeadersParser;
import com.synchronoss.cloud.nio.multipart.util.ParameterParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * <p>
 * NIO Multipart Parser that process data streams conforming to MIME 'multipart' format as defined in
 * <a href="http://www.ietf.org/rfc/rfc1867.txt">RFC 1867</a>.
 * </p>
 *
 * <p> The format of the stream is defined in the following way:<br>
 *
 * <code>
 *   multipart-body := preamble 1*encapsulation close-delimiter epilogue<br>
 *   encapsulation := delimiter body CRLF<br>
 *   delimiter := "--" boundary CRLF<br>
 *   close-delimiter := "--" boundary "--"<br>
 *   preamble := &lt;ignore&gt;<br>
 *   epilogue := &lt;ignore&gt;<br>
 *   body := header-part CRLF body-part<br>
 *   header-part := 1*header CRLF<br>
 *   header := header-name ":" header-value<br>
 *   header-name := &lt;printable ascii characters except ":"&gt;<br>
 *   header-value := &lt;any ascii characters except CR & LF&gt;<br>
 *   body-data := &lt;arbitrary data&gt;<br>
 * </code>
 *
 * Created by sriz0001 on 15/10/2015.
 */
public class NioMultipartParser extends OutputStream {

    private static final Logger log = LoggerFactory.getLogger(NioMultipartParser.class);

    public static final byte DASH = 0x2D;
    public static final byte CR = 0x0D;
    public static final byte LF = 0x0A;

    public static final String CHARACTER_SET = "US-ASCII";
    public static final int DEFAULT_BUFFER_SIZE = 16000;//16kb, Enough for separator and a full header line.
    public static final byte[] HEADER_DELIMITER = {CR, LF, CR, LF};
    protected static final byte[] CLOSE_DELIMITER_SUFFIX = {DASH, DASH};
    protected static final byte[] ENCAPSULATION_DELIMITER_SUFFIX = {CR, LF};

    protected enum DelimiterType {
        CLOSE, ENCAPSULATION
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
        ALL_PARTS_READ,
        SKIP_EPILOGUE,
        ERROR
    }

    final MultipartContext multipartContext;
    final NioMultipartParserListener nioMultipartParserListener;
    final BodyStreamFactory bodyStreamFactory;
    final EndOfLineBuffer buffer;
    final byte[] delimiterPrefix;

    // Current state of the ASF
    State currentState = State.SKIP_PREAMBLE;

    // Current output stream where to flush the body data.
    // It will be instantiated for each part via {@link BodyStreamFactory#getOutputStream(Map, int)} )}
    OutputStream outputStream = null;

    // Reusable output stream where to flush the header data. will be reset every time an header is parsed.
    ByteArrayOutputStream headerOutputStream = new ByteArrayOutputStream();

    // Reusable output stream for bodies that are form parameters.
    ByteArrayOutputStream formFieldBodyOutputStream = new ByteArrayOutputStream();

    // Reusable output stream used to identify the delimiter type from the last two chars: close delimiter or delimiter.
    ByteArrayOutputStream delimiterSuffixIdentifier = new ByteArrayOutputStream(2);

    // The current headers.
    Map<String, List<String>> headers = null;

    // Keeps track of the delimiter type encountered
    DelimiterType delimiterType = null;

    // Keeps track of how many parts we encountered
    int partIndex = 1;

    // ------------
    // Constructors
    // ------------
    public NioMultipartParser(final MultipartContext multipartContext, final NioMultipartParserListener nioMultipartParserListener) {
        this(multipartContext, nioMultipartParserListener, null, DEFAULT_BUFFER_SIZE);
    }

    public NioMultipartParser(final MultipartContext multipartContext, final NioMultipartParserListener nioMultipartParserListener, final BodyStreamFactory bodyStreamFactory) {
        this(multipartContext, nioMultipartParserListener, bodyStreamFactory, DEFAULT_BUFFER_SIZE);
    }

    public NioMultipartParser(final MultipartContext multipartContext, final NioMultipartParserListener nioMultipartParserListener, final int bufferSize) {
        this(multipartContext, nioMultipartParserListener, null, bufferSize);
    }

    public NioMultipartParser(final MultipartContext multipartContext, final NioMultipartParserListener nioMultipartParserListener, final BodyStreamFactory bodyStreamFactory, final int bufferSize) {
        this.multipartContext = multipartContext;
        this.nioMultipartParserListener = nioMultipartParserListener;
        this.delimiterPrefix = getDelimiterPrefix(multipartContext.getContentType());

        if (bodyStreamFactory != null){
            this.bodyStreamFactory = bodyStreamFactory;
        }else{
            // By default use a temporary file where to save the body data.
            this.bodyStreamFactory = new TempFileBodyStreamFactory();
        }

        // At the beginning set up the buffer to skip the preamble.
        this.buffer = new EndOfLineBuffer(bufferSize, getPreambleDelimiterPrefix(delimiterPrefix), null);

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
    public void write(final int data) throws IOException{
        write(new byte[]{(byte) data}, 0, 1);
    }

    @Override
    public void write(byte[] receivedBytes, int indexStart, int indexEnd) throws IOException{

        if (receivedBytes == null) {
            throw new NullPointerException();
        }

        if (receivedBytes.length == 0){
            return;
        }

        if (indexEnd < indexStart){
            throw new IllegalArgumentException("End index cannot be lower that the start index. End index: " + indexEnd + ", Start index: " + indexStart);
        }

        if (indexStart > receivedBytes.length){
            throw new IllegalArgumentException("The start index cannot be greater than the size of the data. Start index: " + indexStart + ", Data length: " + receivedBytes.length);
        }

        if (indexEnd > receivedBytes.length){
            throw new IllegalArgumentException("The end index cannot be greater than the size of the data. End index: " + indexEnd + ", Data length: " + receivedBytes.length);
        }

        writeForDebug(receivedBytes, indexStart, indexEnd);

        int currentIndex = indexStart;
        while (currentIndex < indexEnd) {
            switch (currentState) {

                case SKIP_PREAMBLE:
                    currentIndex = skipPreamble(receivedBytes, currentIndex, indexEnd);
                    break;

                case IDENTIFY_PREAMBLE_DELIMITER:
                    currentIndex = identifyPreambleDelimiter(receivedBytes, currentIndex, indexEnd);
                    break;

                case GET_READY_FOR_HEADERS:
                    getReadyForHeaders();
                    break;

                case READ_HEADERS:
                    currentIndex = readHeaders(receivedBytes, currentIndex, indexEnd);
                    break;

                case GET_READY_FOR_BODY:
                    getReadyForBody();
                    break;

                case READ_BODY:
                    currentIndex = readBody(receivedBytes, currentIndex, indexEnd);
                    break;

                case IDENTIFY_BODY_DELIMITER:
                    currentIndex = identifyBodyDelimiter(receivedBytes, currentIndex, indexEnd);
                    break;

                case PART_COMPLETE:
                    partComplete();
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

    int skipPreamble(final byte[] receivedBytes, int currentIndex, final int indexEnd){
        while (currentIndex < indexEnd) {
            if (buffer.write(receivedBytes[currentIndex])) {
                if (log.isDebugEnabled())log.debug(currentState + " --> " + State.IDENTIFY_PREAMBLE_DELIMITER);
                currentState = State.IDENTIFY_PREAMBLE_DELIMITER;
                return ++currentIndex;
            }
            currentIndex++;
        }
        return ++currentIndex;
    }

    void getReadyForHeaders(){
        if (log.isDebugEnabled())log.debug(currentState + " --> " + State.READ_HEADERS);
        currentState = State.READ_HEADERS;
        headerOutputStream.reset();
        buffer.reset(HEADER_DELIMITER, headerOutputStream);
        headers = new HashMap<String, List<String>>();
    }


    int readHeaders(final byte[] receivedBytes, int currentIndex, final int indexEnd){
        while (currentIndex < indexEnd) {
            if (buffer.write(receivedBytes[currentIndex])) {
                parseHeaders();
                currentState = State.GET_READY_FOR_BODY;
                return ++currentIndex;
            }
            currentIndex++;
        }
        return ++currentIndex;
    }

    void parseHeaders() {
        try{
            // TODO - which encoding?
            headers = HeadersParser.parseHeaders(new ByteArrayInputStream(headerOutputStream.toByteArray()), CHARACTER_SET);
        }catch (Exception e){
            currentState = State.ERROR;
            nioMultipartParserListener.onError("Error parsing the part headers", e);
        }
    }

    void getReadyForBody(){

        if (log.isDebugEnabled())log.debug(currentState + " --> " + State.READ_BODY);

        currentState = State.READ_BODY;
        delimiterType = null;

        if (MultipartUtils.isFormField(headers)){

            if (log.isDebugEnabled())log.debug("Processing form field body");

            formFieldBodyOutputStream.reset();
            buffer.reset(delimiterPrefix, formFieldBodyOutputStream);
        }else{

            if (log.isDebugEnabled())log.debug("Processing data body");

            outputStream = bodyStreamFactory.getOutputStream(headers, partIndex);
            buffer.reset(delimiterPrefix, outputStream);
        }

    }

    int readBody(final byte[] receivedBytes, int currentIndex, final int indexEnd){
        while (currentIndex < indexEnd) {
            if (buffer.write(receivedBytes[currentIndex])) {
                if (log.isDebugEnabled())log.debug(currentState + " --> " + State.IDENTIFY_BODY_DELIMITER);
                currentState = State.IDENTIFY_BODY_DELIMITER;
                return ++currentIndex;
            }
            currentIndex++;
        }
        return ++currentIndex;
    }

    int identifyPreambleDelimiter(final byte[] receivedBytes, int currentIndex, final int indexEnd){
        return identifyDelimiter(receivedBytes, currentIndex, indexEnd, State.GET_READY_FOR_HEADERS, State.ALL_PARTS_READ);
    }

    int identifyBodyDelimiter(final byte[] receivedBytes, int currentIndex, final int indexEnd){
        return identifyDelimiter(receivedBytes, currentIndex, indexEnd, State.PART_COMPLETE, State.PART_COMPLETE);
    }

    int identifyDelimiter(final byte[] receivedBytes, int currentIndex, final int indexEnd, final State onDelimiter, final State onCloseDelimiter){
        while (currentIndex < indexEnd) {
            delimiterSuffixIdentifier.write(receivedBytes[currentIndex]);
            if (delimiterSuffixIdentifier.size() == 2){
                byte[] suffix = delimiterSuffixIdentifier.toByteArray();

                if (isDelimiterSuffix(suffix)){
                    if (log.isDebugEnabled())log.debug(currentState + " --> " + onDelimiter);
                    delimiterType = DelimiterType.ENCAPSULATION;
                    currentState = onDelimiter;
                    delimiterSuffixIdentifier.reset();
                    return ++currentIndex;
                }else if (isCloseDelimiterSuffix(suffix)){
                    if (log.isDebugEnabled())log.debug(currentState + " --> " + onCloseDelimiter);
                    delimiterType = DelimiterType.CLOSE;
                    currentState = onCloseDelimiter;
                    delimiterSuffixIdentifier.reset();
                    return ++currentIndex;
                }else{
                    if (log.isDebugEnabled())log.debug(currentState + " --> " + State.ERROR);
                    currentState = State.ERROR;
                    delimiterSuffixIdentifier.reset();
                    nioMultipartParserListener.onError("Unexpected characters follow a boundary", null);
                    return ++currentIndex;
                }
            }
            currentIndex++;
        }
        return ++currentIndex;
    }

    void allPartsRead(){
        if (log.isDebugEnabled())log.debug(currentState + " --> " + State.SKIP_EPILOGUE);
        currentState = State.SKIP_EPILOGUE;
        nioMultipartParserListener.onAllPartsRead();
        logDebugFile();
    }

    void partComplete(){
        if (MultipartUtils.isFormField(headers)){

            final String fieldName = MultipartUtils.getFieldName(headers);
            final String value = formFieldBodyOutputStream.toString();
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
            currentState = State.ALL_PARTS_READ;
        }else {
            currentState = State.GET_READY_FOR_HEADERS;
        }
        partIndex++;

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

    boolean isCloseDelimiterSuffix(final byte[] suffix){
        return arrayEquals(CLOSE_DELIMITER_SUFFIX, suffix);
    }

    boolean isDelimiterSuffix(final byte[] suffix){
        return arrayEquals(ENCAPSULATION_DELIMITER_SUFFIX, suffix);
    }

    boolean arrayEquals(byte[] a, byte[] b) {
        if (a.length != b.length){
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    byte[] getPreambleDelimiterPrefix(final byte[] delimiterPrefix){

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

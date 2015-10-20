package com.synchronoss.nio.file.multipart;

import com.synchronoss.nio.file.multipart.BodyStreamFactory.PartOutputStream;
import com.synchronoss.nio.file.multipart.util.ParameterParser;
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
public class NioMultipartParserImpl implements NioMultipartParser, Closeable {

    private static final Logger log = LoggerFactory.getLogger(NioMultipartParserImpl.class);

    public static final byte DASH = 0x2D;
    public static final byte CR = 0x0D;
    public static final byte LF = 0x0A;

    public static final int DEFAULT_BUFFER_SIZE = 16000;//16kb, Enough for separator and a full header line.
    public static final byte[] HEADER_DELIMITER = {CR, LF};
    protected static final byte[] CLOSE_DELIMITER_SUFFIX = {DASH, DASH};
    protected static final byte[] DELIMITER_SUFFIX = {CR, LF};

    protected enum State {
        SKIP_PREAMBLE, IDENTIFY_DELIMITER, GET_READY_FOR_HEADERS, HEADERS, GET_READY_FOR_BODY, BODY, IDENTIFY_DELIMITER_AND_NOTIFY, ALL_PARTS_READ, SKIP_EPILOGUE, ERROR
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

    // Reusable output stream used to identify the delimiter type from the last two chars: close delimiter or delimiter.
    ByteArrayOutputStream delimiterSuffixIdentifier = new ByteArrayOutputStream(2);

    // The current headers.
    Map<String, List<String>> headers = null;

    // ------------
    // Constructors
    // ------------
    public NioMultipartParserImpl(final MultipartContext multipartContext, final NioMultipartParserListener nioMultipartParserListener) {
        this(multipartContext, nioMultipartParserListener, null, DEFAULT_BUFFER_SIZE);
    }

    public NioMultipartParserImpl(final MultipartContext multipartContext, final NioMultipartParserListener nioMultipartParserListener, final BodyStreamFactory bodyStreamFactory) {
        this(multipartContext, nioMultipartParserListener, bodyStreamFactory, DEFAULT_BUFFER_SIZE);
    }

    public NioMultipartParserImpl(final MultipartContext multipartContext, final NioMultipartParserListener nioMultipartParserListener, final int bufferSize) {
        this(multipartContext, nioMultipartParserListener, null, bufferSize);
    }

    public NioMultipartParserImpl(final MultipartContext multipartContext, final NioMultipartParserListener nioMultipartParserListener, final BodyStreamFactory bodyStreamFactory, final int bufferSize) {
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

    /*
     * This method implements a state machine. At each written byte the state can change if a end of line is found.
     */
    @Override
    public void handleBytesReceived(byte[] receivedBytes, int indexStart, int indexEnd) {

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

        int partIndex = 0;
        int currentIndex = indexStart;

        while (currentIndex < indexEnd) {
            switch (currentState) {

                case SKIP_PREAMBLE:
                    currentIndex = readPreambleByte(receivedBytes, currentIndex, indexEnd);
                    break;

                case IDENTIFY_DELIMITER:
                    currentIndex = identifyDelimiterAndNotifyIfNeeded(receivedBytes, currentIndex, indexEnd);
                    break;

                case GET_READY_FOR_HEADERS:
                    getReadyForHeaders();
                    break;

                case HEADERS:
                    currentIndex = readHeadersByte(receivedBytes, currentIndex, indexEnd);
                    break;

                case GET_READY_FOR_BODY:
                    getReadyForBody(partIndex++);
                    break;

                case BODY:
                    currentIndex = readBodyByte(receivedBytes, currentIndex, indexEnd);
                    break;

                case IDENTIFY_DELIMITER_AND_NOTIFY:
                    currentIndex = identifyDelimiterAndNotifyIfNeeded(receivedBytes, currentIndex, indexEnd);
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

    int readPreambleByte(final byte[] receivedBytes, int currentIndex, final int indexEnd){
        while (currentIndex < indexEnd) {
            if (buffer.write(receivedBytes[currentIndex])) {
                if (log.isDebugEnabled())log.debug(currentState + " --> " + State.IDENTIFY_DELIMITER);
                currentState = State.IDENTIFY_DELIMITER;
                return ++currentIndex;
            }
            currentIndex++;
        }
        return ++currentIndex;
    }

    void getReadyForHeaders(){
        if (log.isDebugEnabled())log.debug(currentState + " --> " + State.HEADERS);
        currentState = State.HEADERS;
        headerOutputStream.reset();
        buffer.reset(HEADER_DELIMITER, headerOutputStream);
        headers = new HashMap<String, List<String>>();
    }

    int readHeadersByte(final byte[] receivedBytes, int currentIndex, final int indexEnd){
        while (currentIndex < indexEnd) {
            if (buffer.write(receivedBytes[currentIndex])) {
                final String header = headerToString();
                if (header.length() == 0){
                    // Got an empty value, it means the header section is finished.
                    if (log.isDebugEnabled())log.debug(currentState + " --> " + State.GET_READY_FOR_BODY);
                    currentState = State.GET_READY_FOR_BODY;
                }else{
                    // Add the header to the current headers map...
                    addHeader(header);
                }
                return ++currentIndex;
            }
            currentIndex++;
        }
        return ++currentIndex;
    }

    void addHeader(final String header){

        final String[] headerComponents = header.trim().split(":");

        if(headerComponents.length < 1){
            return;
        }

        final String headerName = headerComponents[0].trim().toLowerCase();// Header names are case insensitive
        List<String> headerValues = headers.get(headerName);
        if (headerValues == null){
            headerValues = new ArrayList<String>();
            headers.put(headerName, headerValues);
        }
        if (headerComponents.length > 1){
            for(String headerValue: headerComponents[1].split(",")){
                headerValues.add(headerValue.trim());
            }
        }

        headerOutputStream.reset();
        buffer.reset(HEADER_DELIMITER, headerOutputStream);
    }

    String headerToString(){
        try{
            return headerOutputStream.toString(multipartContext.getCharEncoding()).trim();
        }catch (Exception e){
            return headerOutputStream.toString().trim();
        }
    }

    void getReadyForBody(final int partIndex){
        if (log.isDebugEnabled())log.debug(currentState + " --> " + State.BODY);
        currentState = State.BODY;
        outputStream = bodyStreamFactory.getOutputStream(headers, partIndex);
        buffer.reset(delimiterPrefix, outputStream);
    }

    int readBodyByte(final byte[] receivedBytes, int currentIndex, final int indexEnd){
        while (currentIndex < indexEnd) {
            if (buffer.write(receivedBytes[currentIndex])) {
                if (log.isDebugEnabled())log.debug(currentState + " --> " + State.IDENTIFY_DELIMITER_AND_NOTIFY);
                currentState = State.IDENTIFY_DELIMITER_AND_NOTIFY;
                return ++currentIndex;
            }
            currentIndex++;
        }
        return ++currentIndex;
    }

    int identifyDelimiterAndNotifyIfNeeded(final byte[] receivedBytes, int currentIndex, final int indexEnd){
        while (currentIndex < indexEnd) {
            delimiterSuffixIdentifier.write(receivedBytes[currentIndex]);
            if (delimiterSuffixIdentifier.size() == 2){
                byte[] suffix = delimiterSuffixIdentifier.toByteArray();

                if (isDelimiterSuffix(suffix)){
                    notifyPart();
                    if (log.isDebugEnabled())log.debug(currentState + " --> " + State.GET_READY_FOR_HEADERS);
                    currentState = State.GET_READY_FOR_HEADERS;
                    delimiterSuffixIdentifier.reset();
                    return ++currentIndex;
                }else if (isCloseDelimiterSuffix(suffix)){
                    notifyPart();
                    if (log.isDebugEnabled())log.debug(currentState + " --> " + State.ALL_PARTS_READ);
                    currentState = State.ALL_PARTS_READ;
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

    void notifyPart(){
        if (currentState == State.IDENTIFY_DELIMITER_AND_NOTIFY){
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
        return arrayEquals(DELIMITER_SUFFIX, suffix);
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

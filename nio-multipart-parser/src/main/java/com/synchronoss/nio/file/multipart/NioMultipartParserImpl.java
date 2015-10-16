package com.synchronoss.nio.file.multipart;

import org.apache.commons.fileupload.ParameterParser;
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
 * Have a look at
 * {@link org.glassfish.grizzly.http.multipart.MultipartScanner} and {@link org.glassfish.grizzly.http.multipart.MultipartReadHandler}
 * {@link org.apache.commons.fileupload.MultipartStream}
 *
 * Created by sriz0001 on 15/10/2015.
 */
public class NioMultipartParserImpl implements NioMultipartParser, Closeable{

    private static final Logger log = LoggerFactory.getLogger(NioMultipartParserImpl.class);

    protected static final int DEFAULT_BUFFER_SIZE = 16000;//16kb, Enough for separator and a full header line.

    protected enum State {
        SKIP_PREAMBLE, GET_READY_FOR_HEADERS, HEADERS, GET_READY_FOR_BODY, BODY, END
    }

    final MultipartContext multipartContext;
    final NioMultipartParserListener nioMultipartParserListener;
    final Buffer buffer;

    State currentState = State.SKIP_PREAMBLE;
    File tempFile = null;
    OutputStream bodyOutputStream = null;
    Map<String, List<String>> headers = null;

    public NioMultipartParserImpl(final MultipartContext multipartContext, final NioMultipartParserListener nioMultipartParserListener ) {
        this(multipartContext, nioMultipartParserListener, DEFAULT_BUFFER_SIZE);
    }

    public NioMultipartParserImpl(final MultipartContext multipartContext, final NioMultipartParserListener nioMultipartParserListener, final int bufferSize) {
        this.multipartContext = multipartContext;
        this.nioMultipartParserListener = nioMultipartParserListener;
        this.buffer = new Buffer(bufferSize, getBoundary(multipartContext.getContentType()));
    }

    @Override
    public void close() throws IOException {
        log.debug("Release resources...");
        // TODO - DO I need to release some resources?
    }

    @Override
    public void handleBytesReceived(byte[] receivedBytes, int indexStart, int indexEnd) {

        log.trace("Handle bytes: " + Arrays.toString(receivedBytes) + " indexStart:" + indexStart + ", indexEnd: " + indexEnd);

        int partIndex = 0;
        int currentIndex = indexStart;


        while (currentIndex < indexEnd) {
            switch (currentState) {

                case SKIP_PREAMBLE:
                    log.info("SKIP PREAMBLE");
                    currentIndex = readPreamble(receivedBytes, currentIndex, indexEnd);
                    break;

                case GET_READY_FOR_HEADERS:
                    log.info("GET READY FOR HEADERS");
                    headers = readyForHeaders();
                    break;

                case HEADERS:
                    log.info("HEADERS");
                    currentIndex = readHeaders(receivedBytes, currentIndex, indexEnd, headers);
                    break;

                case GET_READY_FOR_BODY:
                    log.info("GET READY FOR BODY");
                    tempFile = newTempFile(headers, partIndex++);

                    log.debug("Temporary file for body: " + tempFile.getAbsolutePath());

                    bodyOutputStream = readyForBody(tempFile);
                    break;

                case BODY:
                    log.info("BODY");
                    currentIndex = readBody(receivedBytes, currentIndex, indexEnd, bodyOutputStream, tempFile, headers);
                    break;

                case END:
                    log.info("END");
                    nioMultipartParserListener.onAllPartsRead();
                    currentIndex = indexEnd +1;
                    break;

            }
        }
    }

    int readPreamble(final byte[] receivedBytes, int currentIndex, final int indexEnd){
        for (; currentIndex < indexEnd; currentIndex++) {
            if (buffer.writeForPreamble(receivedBytes[currentIndex])) {
                // end of line
                if (buffer.isPartDelimiter()){
                    buffer.reset();
                    currentState = State.GET_READY_FOR_HEADERS;
                    return currentIndex;

                } else if(buffer.isPartsDelimiter()) {
                    buffer.reset();
                    currentState = State.END;
                    return currentIndex;

                } else {
                    throw new IllegalStateException("Expected a boundary");
                }
            }
        }
        return currentIndex;
    }

    Map<String, List<String>> readyForHeaders(){
        currentState = State.HEADERS;
        return new HashMap<String, List<String>>();
    }

    int readHeaders(final byte[] receivedBytes, int currentIndex, final int indexEnd, final Map<String, List<String>> headers){
        for (; currentIndex < indexEnd; currentIndex++) {
            if (buffer.writeForHeader(receivedBytes[currentIndex])) {
                // end of line
                String header = buffer.readHeaderString().trim();
                if (header.length() == 0){
                    currentState = State.GET_READY_FOR_BODY;
                }else{
                    String[] headerComponents = header.split(":");
                    log.debug("Header: " + Arrays.asList(headerComponents));
                    headers.put(headerComponents[0], headerComponents.length > 1 ? Collections.singletonList(headerComponents[1]) : Collections.EMPTY_LIST);
                }
                buffer.reset();
                return currentIndex;
            }
        }
        return currentIndex;
    }

    OutputStream readyForBody(final File tempFile){
        currentState = State.BODY;
        try {
            return new FileOutputStream(tempFile);
        }catch (Exception e){
            throw new IllegalStateException("Unable to create temporary file", e);
        }
    }

    int readBody(final byte[] receivedBytes, int currentIndex, final int indexEnd, final OutputStream outputStream, final File file, final Map<String, List<String>> headers){
        for (; currentIndex < indexEnd; currentIndex++) {
            if (buffer.writeForBody(receivedBytes[currentIndex], outputStream)) {
                if (buffer.isPartDelimiter()){
                    nioMultipartParserListener.onPartComplete(partBodyInputStream(file), headers);
                    currentState = State.GET_READY_FOR_HEADERS;
                    buffer.reset();
                    return currentIndex;
                }else{
                    nioMultipartParserListener.onPartComplete(partBodyInputStream(file), headers);
                    currentState = State.END;
                    buffer.reset();
                    return currentIndex;
                }
            }
        }
        return currentIndex;
    }

    protected byte[] getBoundary(String contentType) {
        ParameterParser parser = new ParameterParser();// TODO - get rid of the commons fileupload dependency
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

        log.debug("boundary: " + Arrays.toString(boundary));

        return boundary;
    }

    File newTempFile(final Map<String, List<String>> headers, int partIndex){
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        String tempFileName = UUID.randomUUID().toString();
        return new File(tempDir, tempFileName);
    }

    InputStream partBodyInputStream(final File file){
        try{
            return new FileInputStream(file);
        }catch (Exception e){
            throw new IllegalStateException("Unable to create the inputstream for the part body", e);
        }
    }

    /**
     * <p>
     *     A reusable buffer tailored for the Non Blocking IO parsing.
     * </p>
     */
    static class Buffer{

        static final byte DASH = 0x2D;
        static final byte[] CR_LF = new byte[]{0x0D, 0x0A};

        int delimiterStartIndex = 0;
        int crLfStartIndex = 0;
        int closeDelimiterStartIndex = 0;
        int matchingCrLf = 0;
        int matchingDelimiter = 0;
        int matchingCloseDelimiter = 0;

        final byte[] delimiter;
        final byte[] closeDelimiter;
        final CircularBuffer buffer;

        public Buffer(final int bufferSize, final byte[] boundary) {

            if (boundary.length > bufferSize + 4){
                throw new IllegalStateException("The parts terminator cannot fit in the buffer. Buffer size: " + bufferSize + " parts terminator size (including trailing and leading '--'): " + (boundary.length + 4));
            }

            this.buffer = new CircularBuffer(bufferSize);

            this.closeDelimiter = new byte[boundary.length + 4];
            this.closeDelimiter[0] = DASH;
            this.closeDelimiter[1] = DASH;
            this.closeDelimiter[closeDelimiter.length - 1] = DASH;
            this.closeDelimiter[closeDelimiter.length - 2] = DASH;
            System.arraycopy(boundary, 0, closeDelimiter, 2, boundary.length);

            this.delimiter = new byte[boundary.length + 4];
            this.delimiter[0] = DASH;
            this.delimiter[1] = DASH;
            this.delimiter[delimiter.length - 1] = CR_LF[1];
            this.delimiter[delimiter.length - 2] = CR_LF[0];
            System.arraycopy(boundary, 0, delimiter, 2, boundary.length);

            log.debug("delimiter: " + Arrays.toString(delimiter));
            log.debug("closeDelimiter: " + Arrays.toString(closeDelimiter));

        }

        public boolean writeForPreamble(final byte b){
            buffer.write(b);
            lookForDelimiter(b);

            log.trace("Written " + String.format("0x%02X", b) + ". delimiterStartIndex:" + delimiterStartIndex + ", matchingDelimiter: " + matchingDelimiter + ", closeDelimiterStartIndex: " + closeDelimiterStartIndex + ", matchingCloseDelimiter: " + matchingCloseDelimiter + ", delimiter size: " + delimiter.length + ", close delimiter size: " + delimiter.length);

            return isPartsDelimiter() || isPartDelimiter();//TODO - Do i need to check the isPartsDelimiter at this stage?
        }

        public boolean writeForHeader(final byte b){
            if (buffer.isFull()){
                throw new IllegalStateException("The buffer is full. Make sure the header is not too long or the buffer too small. Buffer size: " + buffer.size);
            }
            buffer.write(b);
            lookForCrLf(b);

            log.trace("Written " + String.format("0x%02X", b) + ". crLfStartIndex:" + crLfStartIndex + ", matchingCrLf: " + matchingCrLf);

            return isEndOfLine();
        }

        public String readHeaderString(){
            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            try{
                buffer.read(bao);
                return new String(bao.toByteArray());
            }catch (Exception e){
                throw new IllegalStateException("Unable to read the header", e);
            }
        }

        public boolean writeForBody(final byte b, final OutputStream outputStream){

            if (buffer.isFull()){
                flush(outputStream);
            }

            if (buffer.isFull()){
                throw new IllegalStateException("The buffer is full. Make sure the parts terminator is not too long or the buffer too small. Buffer size: " + buffer.size);
            }

            buffer.write(b);
            lookForDelimiter(b);

            if (isPartsDelimiter() || isPartDelimiter()){
                flush(outputStream);
                return true;
            }

            return false;

        }

        void lookForDelimiter(final byte b){
            if (b == delimiter[matchingDelimiter]){
                if (matchingDelimiter == 0) {
                    delimiterStartIndex = buffer.nextAvailablePosition - 1;
                }
                matchingDelimiter++;
                log.debug("Byte " + b + " is part of the delimiter. matchingDelimiter: " + matchingDelimiter + ", delimiterStartIndex: " + delimiterStartIndex);
            }else {
                matchingDelimiter = 0;
            }
            if (b == closeDelimiter[matchingCloseDelimiter]){
                if (matchingCloseDelimiter == 0) {
                    closeDelimiterStartIndex = buffer.nextAvailablePosition - 1;
                }
                matchingCloseDelimiter++;
                log.debug("Byte " + b + " is part of the close delimiter. matchingDelimiter: " + matchingCloseDelimiter + ", delimiterStartIndex: " + closeDelimiterStartIndex);
            }else {
                matchingCloseDelimiter = 0;
            }
        }

        void lookForCrLf(final byte b){
            if (b == CR_LF[matchingCrLf]){
                crLfStartIndex = buffer.startValidDataIndex;
                matchingCrLf++;
            }else{
                matchingCrLf = 0;
            }
        }

        void flush(final OutputStream outputStream){
            try {
                
                if (mightBeDelimiter() || mightBeCloseDelimiter()) {
                    if (matchingDelimiter < closeDelimiterStartIndex){
                        log.debug("Flushing buffer. Might be a delimiter so flush until " + buffer.backwards(delimiterStartIndex));
                        log.debug("Buffer: " + Arrays.toString(buffer.buffer));
                        buffer.read(outputStream, buffer.backwards(delimiterStartIndex));
                    }else{
                        log.debug("Flushing buffer. Might be a close delimiter so flush until " + buffer.backwards(closeDelimiterStartIndex));
                        log.debug("Buffer: " + Arrays.toString(buffer.buffer));
                        buffer.read(outputStream, buffer.backwards(closeDelimiterStartIndex));
                    }
                }else{
                    log.debug("Flushing whole buffer...");
                    buffer.read(outputStream);
                }
            }catch (Exception e){
                throw new IllegalStateException("Unable to flush the buffer", e);
            }
        }

        boolean mightBeDelimiter(){
            return matchingDelimiter > 0;
        }

        boolean mightBeCloseDelimiter(){
            return matchingCloseDelimiter > 0;
        }

        boolean isPartDelimiter(){
            return matchingDelimiter == delimiter.length;
        }

        boolean isPartsDelimiter(){
            return matchingCloseDelimiter == closeDelimiter.length;
        }

        boolean mightBeEndOfLine(){
            return matchingCrLf > 0;
        }

        boolean isEndOfLine(){
            return matchingCrLf == 2;
        }

        void reset(){
            buffer.reset();
            delimiterStartIndex = 0;
            crLfStartIndex = 0;
            closeDelimiterStartIndex = 0;
            matchingCrLf = 0;
            matchingDelimiter = 0;
            matchingCloseDelimiter = 0;
        }

    }
}

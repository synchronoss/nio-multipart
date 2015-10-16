package com.synchronoss.nio.file.multipart;

import org.apache.commons.fileupload.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * <p>
 *     A naive implementation of {@link NioMultipartParser} just for testing.
 *     It will be removed when the proper implementation is ready.
 * </p>
 * <p>
 *     Usage Example:
 *     <pre>
 *         {@code
 *
 *         final AsyncContext asyncContext = request.getAsyncContext();
 *         final ServletInputStream inputStream = request.getInputStream();
 *
 *         final NioMultipartParserListener listener = new NioMultipartParserListener(){
 *                  public void onPartComplete(final InputStream partBodyInputStream, final Map<String, List<String>> headersFromPart){
 *                      // Application logic
 *                  }
 *
 *                  public void onAllPartsRead(){
 *                      // Application logic
 *                  }
 *
 *                  public void onError(final String message, final Throwable t){
 *                      // Application logic
 *                  }
 *         }
 *
 *         final MultipartContext multipartContext = new MultipartContext(request.getContentType(), request.getContentLength(), request.getCharacterEncoding());
 *
 *         NioMultipartParser parser = new NioNaiveMultipartParser(listener, multipartContext);
 *
 *         inputStream.setReadListener(new ReadListener() {
 *              @Override
 *              public void onDataAvailable() throws IOException {
 *
 *                  // ...
 *
 *                  int bytesRead;
 *                  byte bytes[] = new byte[1024];
 *                  while (inputStream.isReady()  && (bytesRead = inputStream.read(bytes)) != -1) {
 *                      totalBytesRead += bytesRead;
 *                      parser.handleBytesReceived(bytes, 0, bytesRead);
 *                  }
 *
 *                  // ...
 *
 *              }
 *
 *              @Override
 *              public void onAllDataRead() throws IOException {
 *
 *                  // ...
 *
 *                  parser.close();
 *
 *                  // ...
 *
 *              }
 *
 *              @Override
 *              public void onError(Throwable throwable) {
 *
 *                  // ...
 *
 *                  parser.close();
 *
 *                  // ...
 *
 *              }
 *
 *         });
 *      }
 *     </pre>
 * </p>
 * Created by sriz0001 on 12/10/2015.
 */
public class NioNaiveMultipartParser implements NioMultipartParser, Closeable {

    private static final Logger log = LoggerFactory.getLogger(NioNaiveMultipartParser.class);

    final File tempFile;
    final OutputStream outputStream;
    final NioMultipartParserListener nioMultipartParserListener;
    final MultipartContext multipartContext;

    public NioNaiveMultipartParser(final NioMultipartParserListener nioMultipartParserListener,
                                   final MultipartContext multipartContext) {

        this.nioMultipartParserListener = nioMultipartParserListener;
        this.multipartContext = multipartContext;

        // Create the tmp file where I will save the entire multipart request body.
        try {
            tempFile = getTempFile();
            outputStream = new FileOutputStream(tempFile);
        }catch (Exception e){
            cleanUpTempFile();
            throw new IllegalStateException("Unable to initialize the parser. Temporary file creation failed.", e);
        }
    }

    @Override
    public void handleBytesReceived(final byte[] receivedBytes, final int indexStart, final int indexEnd) {
        try {
            // Just write the bytes to the temp file...
            outputStream.write(receivedBytes, indexStart, indexEnd);
        }catch (Exception e){
            cleanUpTempFile();
            nioMultipartParserListener.onError("Unable to write to the temp file",e);
        }
    }

    @Override
    public void close() throws IOException {
        flushAndCloseOutputStream();

        // Call it here because we are sure we received all the data...
        parseAndNotify();
    }

    public void parseAndNotify(){

        // Use the commons file upload utility to parse the multipart body saved in the temp file and notify the NioMultipartParserListener
        final FileUpload fileUpload = new FileUpload();
        try {

            final TempFileRequestContext tempFileRequestContext = new TempFileRequestContext(tempFile,
                    multipartContext.getCharEncoding(),
                    multipartContext.getContentLength(),
                    multipartContext.getContentType());

            final FileItemIterator fileItemIterator = fileUpload.getItemIterator(tempFileRequestContext);

            while (fileItemIterator.hasNext()){
                FileItemStream fileItemStream = fileItemIterator.next();
                nioMultipartParserListener.onPartComplete(fileItemStream.openStream(), getHeadersFromFileItemStream(fileItemStream));
            }

            nioMultipartParserListener.onAllPartsRead();

        }catch (Exception e){
            cleanUpTempFile();
            nioMultipartParserListener.onError("Error parsing the multipart body", e);
        }
    }

    void flushAndCloseOutputStream(){
        if (outputStream != null){
            try {
                outputStream.flush();
                outputStream.close();
            }catch (Exception e){
                // Ignore
            }
        }
    }

    public void cleanUpTempFile(){
        flushAndCloseOutputStream();
        if (tempFile.exists()) {
            boolean deleted = tempFile.delete();
            if (!deleted){
                log.warn("Could not delete " + tempFile.getAbsolutePath());
            }
        }
    }

    Map<String, List<String>> getHeadersFromFileItemStream(final FileItemStream fileItemStream){

        final FileItemHeaders fileItemHeaders = fileItemStream.getHeaders();
        if (fileItemHeaders == null){
            return new HashMap<String, List<String>>();
        }

        final Map<String, List<String>> headers = new HashMap<String, List<String>>();
        final Iterator<String> headerNames = fileItemHeaders.getHeaderNames();
        while (headerNames.hasNext()){
            String headerName = headerNames.next();
            List<String> values = new ArrayList<String>();
            Iterator<String> valuesIterator = fileItemHeaders.getHeaders(headerName);
            while (valuesIterator.hasNext()){
                values.add(valuesIterator.next());
            }

            headers.put(headerName, values);
        }

        return headers;

    }

    protected File getTempFile() {
        File tempFile = new File(new File(System.getProperty("java.io.tmpdir")), String.format("upload_%s.tmp", UUID.randomUUID().toString()));
        tempFile.deleteOnExit();
        return tempFile;
    }

    static class TempFileRequestContext implements RequestContext{

        final File tempFile;
        final InputStream inputStream;
        final String charEncoding;
        final int contentLength;
        final String contentType;

        public TempFileRequestContext(final File tempFile,
                                      final String charEncoding,
                                      final int contentLength,
                                      final String contentType

        ) {
            this.charEncoding = charEncoding != null ? charEncoding : "UTF-8";
            this.contentLength = contentLength;
            this.contentType = contentType;

            this.tempFile = tempFile;
            try {
                inputStream = new FileInputStream(tempFile);
            }catch (Exception e){
                throw new IllegalStateException("Unable to parse the request", e);
            }
        }

        @Override
        public String getCharacterEncoding() {
            return charEncoding;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public int getContentLength() {
            return contentLength;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return inputStream;
        }
    }

}

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

package org.synchronoss.cloud.nio.multipart.example.web;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.gson.Gson;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.synchronoss.cloud.nio.multipart.*;
import org.synchronoss.cloud.nio.multipart.BlockingIOAdapter.PartItem;
import org.synchronoss.cloud.nio.multipart.example.config.RootApplicationConfig;
import org.synchronoss.cloud.nio.multipart.example.io.ChecksumStreamStorage;
import org.synchronoss.cloud.nio.multipart.example.io.ChecksumStreamUtils;
import org.synchronoss.cloud.nio.multipart.example.io.ChecksumStreamUtils.ChecksumAndReadBytes;
import org.synchronoss.cloud.nio.multipart.example.model.FileMetadata;
import org.synchronoss.cloud.nio.multipart.example.model.Metadata;
import org.synchronoss.cloud.nio.multipart.example.model.VerificationItem;
import org.synchronoss.cloud.nio.multipart.example.model.VerificationItems;
import org.synchronoss.cloud.nio.multipart.example.spring.CloseableReadListenerDeferredResult;
import org.synchronoss.cloud.nio.multipart.util.collect.CloseableIterator;
import org.synchronoss.cloud.nio.stream.storage.StreamStorage;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.synchronoss.cloud.nio.multipart.BlockingIOAdapter.PartItem.Type.PART;
import static org.synchronoss.cloud.nio.multipart.Multipart.multipart;

/**
 * <p> A show case of multipart parsing and a useful test tool.
 *     The Controller defines 3 APIs:
 *     <ul>
 *         <li>POST: /nio/dr/multipart - Nio Multipart processing integrated with spring DeferredResult.</li>
 *         <li>POST: /nio/multipart - Plain Nio Multipart Processing using directly the Servlet 3.1 features.</li>
 *         <li>POST: /blockingio/multipart - Multipart processing using apache commons fileupload. Blocking IO.</li>
 *     </ul>
 *
 * <p> In all three cases the request is composed by:
 *     <ul>
 *         <li>Metadata: The first part whose field name MUST be "metatata". It contains a json object containing file path, checksum and size of all the following file attachments.</li>
 *         <li>File attachments: Actual file parts. The field name must be the file name.</li>
 *     </ul>
 *     See {@link Metadata} and {@link FileMetadata}.
 *
 * <p> The response (see {@link VerificationItem} and {@link VerificationItems}) is list of verification items containing for each attached file:
 *     <ul>
 *        <li>Original Checksum specified in the metadata</li>
 *        <li>Original Size specified in the metadata</li>
 *        <li>Checksum of the part body processed</li>
 *        <li>Size part body processed</li>
 *     </ul>
 *     The response will also contain if all the item matched.
 *
 * <p> When the NIO Multipart parser is used, the checksum and size are computed for both the streams returned by the {@link org.synchronoss.cloud.nio.stream.storage.StreamStorage}.
 *     This is not the case when the commons fileupload is used and the
 *     {@link VerificationItem#getPartOutputStreamChecksum()} and {@link VerificationItem#getPartOutputStreamWrittenBytes()}
 *     will not be populated and checked.
 *
 * <p> This controller, it is a showcase of how the NIO multipart parser can be used and it is also a good integration test
 *     that verifies the correct behaviour of the nio multipart parser.
 *
 * @author Silvano Riz.
 */
@RestController
@RequestMapping
@Import(RootApplicationConfig.class)
public class MultipartController {

    private static final Logger log = LoggerFactory.getLogger(MultipartController.class);
    public static final String METADATA_FIELD_NAME = "metadata";

    @Autowired
    private PartBodyStreamStorageFactory partBodyStreamStorageFactory;

    private static Gson GSON = new Gson();

    /**
     * <p> This is an example how the NIO Parser can be used in combination with the Spring DeferredResult.
     *
     * @param request The {@code HttpServletRequest}
     * @return The {@code ReadListenerDeferredResult}
     * @throws IOException if an IO exception happens
     */
    @RequestMapping(value = "/nio/dr/multipart", method = RequestMethod.POST)
    public @ResponseBody
    CloseableReadListenerDeferredResult<VerificationItems> nioDeferredResultMultipart(final HttpServletRequest request) throws IOException {

        assertRequestIsMultipart(request);

        // A ReadListenerDeferredResultProcessingInterceptor is registered in the WebConfig.
        // The interceptor checks the type of the deferred result and if it's a ReadListenerDeferredResult it
        // attaches it to the servlet input stream.
        final CloseableReadListenerDeferredResult<VerificationItems> readListenerDeferredResult = new CloseableReadListenerDeferredResult<VerificationItems>() {

            final AtomicInteger synchronizer = new AtomicInteger(0);

            final ServletInputStream servletInputStream = request.getInputStream();
            final MultipartContext ctx = getMultipartContext(request);


            final VerificationItems verificationItems = new VerificationItems();
            Metadata metadata;
            final NioMultipartParserListener listener = new NioMultipartParserListener() {

                @Override
                public void onPartFinished(StreamStorage partBodyStreamStorage, Map<String, List<String>> headersFromPart) {
                    if(log.isInfoEnabled()) log.info("PARSER LISTENER - onPartFinished");
                    final ChecksumStreamStorage checksumStreamStorage = getChecksumStreamStorageOrThrow(partBodyStreamStorage);
                    final String fieldName = MultipartUtils.getFieldName(headersFromPart);
                    if (METADATA_FIELD_NAME.equals(fieldName)){
                        metadata = unmarshalMetadataOrThrow(checksumStreamStorage);
                    }else{
                        VerificationItem verificationItem = buildVerificationItem(checksumStreamStorage, fieldName);
                        verificationItems.getVerificationItems().add(verificationItem);
                    }
                }

                @Override
                public void onAllPartsFinished() {
                    if(log.isInfoEnabled()) log.info("PARSER LISTENER - onAllPartsFinished");
                    processVerificationItems(verificationItems, metadata, true);
                    sendResponseOrSkip();
                }

                @Override
                public void onNestedPartStarted(Map<String, List<String>> headersFromParentPart) {
                    if(log.isInfoEnabled()) log.info("PARSER LISTENER - onNestedPartStarted");
                }

                @Override
                public void onNestedPartFinished() {
                    if(log.isInfoEnabled()) log.info("PARSER LISTENER - onNestedPartFinished");
                }

                @Override
                public void onError(String message, Throwable cause) {
                    if(log.isInfoEnabled()) log.info("PARSER LISTENER - onError");
                    sendErrorOrSkip(message, cause);
                }


            };

            final NioMultipartParser parser = Multipart.multipart(ctx).usePartBodyStreamStorageFactory(partBodyStreamStorageFactory).forNIO(listener);

            @Override
            public void onDataAvailable() throws IOException {
                if(log.isInfoEnabled())log.info("NIO READ LISTENER - onDataAvailable");
                int bytesRead;
                byte bytes[] = new byte[1024];
                while (servletInputStream.isReady() && (bytesRead = servletInputStream.read(bytes)) != -1) {
                    parser.write(bytes, 0, bytesRead);
                }
                if(log.isInfoEnabled())log.info("Epilogue bytes..." ) ;
            }

            @Override
            public void onAllDataRead() throws IOException {
                if(log.isInfoEnabled())log.info("NIO READ LISTENER - onAllDataRead");
                // do nothing, wait for the parser to come back with onAllPartsFinished()
            }

            @Override
            public void onError(Throwable t) {

                if(log.isInfoEnabled())log.info("NIO READ LISTENER - onAllDataRead");
                sendErrorOrSkip(null, t);
            }

            @Override
            public void close() throws IOException {
                parser.close();
            }

            void sendResponseOrSkip(){
                if (synchronizer.getAndAdd(1) == 0) {
                    this.setResult(verificationItems);
                }
            }

            void sendErrorOrSkip(String message, Throwable cause){
                String messageOrUnknown = message != null ? message : "Unknown Error";
                log.error("Error " + messageOrUnknown, cause);
                if (synchronizer.getAndAdd(1) == 0) {
                    this.setErrorResult(messageOrUnknown);
                }
            }

            synchronized Metadata unmarshalMetadataOrThrow(final String json){
                if (metadata != null){
                    throw new IllegalStateException("Found two metadata fields");
                }
                return unmarshalMetadata(json);
            }

            synchronized Metadata unmarshalMetadataOrThrow(final ChecksumStreamStorage checksumPartStreams){
                if (metadata != null){
                    throw new IllegalStateException("Found two metadata fields");
                }
                return unmarshalMetadata(checksumPartStreams.getInputStream());
            }

        };

        return readListenerDeferredResult;
    }

    /**
     * <p> This is an example how the NIO Parser can be used in a plain Servlet 3.1 fashion.
     *
     * @param request The {@code HttpServletRequest}
     * @throws IOException if an IO exception happens
     */
    @RequestMapping(value = "/nio/multipart", method = RequestMethod.POST)
    public @ResponseBody void nioMultipart(final HttpServletRequest request) throws IOException {

        assertRequestIsMultipart(request);

        final VerificationItems verificationItems = new VerificationItems();
        final AsyncContext asyncContext = switchRequestToAsyncIfNeeded(request);
        final ServletInputStream inputStream = request.getInputStream();
        final AtomicInteger synchronizer = new AtomicInteger(0);

        final NioMultipartParserListener listener = new NioMultipartParserListener() {

            Metadata metadata;

            @Override
            public void onPartFinished(final StreamStorage partBodyStreamStorage, final Map<String, List<String>> headersFromPart) {
                if(log.isInfoEnabled())log.info("PARSER LISTENER - onPartFinished") ;
                final String fieldName = MultipartUtils.getFieldName(headersFromPart);
                final ChecksumStreamStorage checksumPartStreams = getChecksumStreamStorageOrThrow(partBodyStreamStorage);
                if (METADATA_FIELD_NAME.equals(fieldName)){
                    metadata = unmarshalMetadataOrThrow(checksumPartStreams);
                }else{
                    VerificationItem verificationItem = buildVerificationItem(checksumPartStreams, fieldName);
                    verificationItems.getVerificationItems().add(verificationItem);
                }
            }

            @Override
            public void onNestedPartStarted(final Map<String, List<String>> headersFromParentPart) {
                if(log.isInfoEnabled())log.info("PARSER LISTENER - onNestedPartStarted");
            }

            @Override
            public void onNestedPartFinished() {
                if(log.isInfoEnabled())log.info("PARSER LISTENER - onNestedPartFinished");
            }

            @Override
            public void onAllPartsFinished() {
                if(log.isInfoEnabled())log.info("PARSER LISTENER - onAllPartsFinished");
                processVerificationItems(verificationItems, metadata, true);
                sendResponseOrSkip(synchronizer, asyncContext, verificationItems);
            }

            @Override
            public void onError(String message, Throwable cause) {
                // Probably invalid data...
                throw new IllegalStateException("Encountered an error during the parsing: " + message, cause);
            }

            synchronized Metadata unmarshalMetadataOrThrow(final String json){
                if (metadata != null){
                    throw new IllegalStateException("Found two metadata fields");
                }
                return unmarshalMetadata(json);
            }

            synchronized Metadata unmarshalMetadataOrThrow(final ChecksumStreamStorage checksumPartStreams){
                if (metadata != null){
                    throw new IllegalStateException("Found more than one metadata fields");
                }
                return unmarshalMetadata(checksumPartStreams.getInputStream());
            }

        };

        final MultipartContext ctx = getMultipartContext(request);
        final NioMultipartParser parser = multipart(ctx).usePartBodyStreamStorageFactory(partBodyStreamStorageFactory).forNIO(listener);

        // Add a listener to ensure the parser is closed.
        asyncContext.addListener(new AsyncListener() {
            @Override
            public void onComplete(AsyncEvent event) throws IOException {
                parser.close();
            }

            @Override
            public void onTimeout(AsyncEvent event) throws IOException {
                parser.close();
            }

            @Override
            public void onError(AsyncEvent event) throws IOException {
                parser.close();
            }

            @Override
            public void onStartAsync(AsyncEvent event) throws IOException {
                // Nothing to do.
            }
        });

        inputStream.setReadListener(new ReadListener() {

            @Override
            public void onDataAvailable() throws IOException {
                if(log.isInfoEnabled())log.info("NIO READ LISTENER - onDataAvailable");
                int bytesRead;
                byte bytes[] = new byte[2048];
                while (inputStream.isReady() && (bytesRead = inputStream.read(bytes)) != -1) {
                    parser.write(bytes, 0, bytesRead);
                }
                if(log.isInfoEnabled())log.info("Epilogue bytes..." ) ;
            }

            @Override
            public void onAllDataRead() throws IOException {
                if(log.isInfoEnabled())log.info("NIO READ LISTENER - onAllDataRead");
                sendResponseOrSkip(synchronizer, asyncContext, verificationItems);
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("onError", throwable);
                IOUtils.closeQuietly(parser);
                sendErrorOrSkip(synchronizer, asyncContext, "Unknown error");
            }
        });


    }

    void sendResponseOrSkip(final AtomicInteger synchronizer, final AsyncContext asyncContext, final VerificationItems verificationItems){
        if (synchronizer.getAndIncrement() == 1) {
            try {
                final Writer responseWriter = asyncContext.getResponse().getWriter();
                responseWriter.write(GSON.toJson(verificationItems));
                asyncContext.complete();
            } catch (Exception e) {
                log.error("Failed to send back the response", e);
            }
        }
    }

    void sendErrorOrSkip(final AtomicInteger synchronizer, final AsyncContext asyncContext, final String message){
        if (synchronizer.getAndIncrement() <= 1) {
            try {
                final ServletResponse servletResponse = asyncContext.getResponse();
                if (servletResponse instanceof HttpServletResponse){
                    ((HttpServletResponse)servletResponse).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message);
                }else {
                    asyncContext.getResponse().getWriter().write(message);
                }
                asyncContext.complete();
            } catch (Exception e) {
                log.error("Failed to send back the error response", e);
            }
        }
    }

    /**
     * <p> Example of parsing the multipart request with in a blocking IO mode using the {@link BlockingIOAdapter}.
     *
     * @param request The {@code HttpServletRequest}
     * @return The {@code VerificationItems}
     * @throws Exception if an exception happens during the parsing
     */
    @RequestMapping(value = "/blockingio/adapter/multipart", method = RequestMethod.POST)
    public @ResponseBody VerificationItems blockingIoAdapterMultipart(final HttpServletRequest request) throws Exception {

        assertRequestIsMultipart(request);

        final VerificationItems verificationItems = new VerificationItems();
        Metadata metadata = null;

        try (final CloseableIterator<PartItem> parts = Multipart.multipart(getMultipartContext(request)).forBlockingIO(request.getInputStream())){
            while (parts.hasNext()) {
                PartItem partItem = parts.next();
                if (PART.equals(partItem.getType())) {
                    BlockingIOAdapter.Part part = (BlockingIOAdapter.Part) partItem;
                    final String fieldName = MultipartUtils.getFieldName(part.getHeaders());
                    if (METADATA_FIELD_NAME.equals(fieldName)) {
                        metadata = unmarshalMetadata(part.getPartBody());
                    } else {

                        VerificationItem verificationItem = buildVerificationItem(part.getPartBody(), fieldName);
                        verificationItems.getVerificationItems().add(verificationItem);
                    }
                } else {
                    throw new IllegalStateException("Invalid part type " + partItem.getClass().getName());
                }
            }
        }catch (Exception e){
            throw new IllegalStateException("Parsing error", e);
        }
        processVerificationItems(verificationItems, metadata, false);
        return verificationItems;
    }

    /**
     * <p> Example of parsing the multipart request using commons file upload. In this case the parsing happens in blocking io.
     *
     * @param request The {@code HttpServletRequest}
     * @return The {@code VerificationItems}
     * @throws Exception if an exception happens during the parsing
     */
    @RequestMapping(value = "/blockingio/fileupload/multipart", method = RequestMethod.POST)
    public @ResponseBody VerificationItems blockingIoMultipart(final HttpServletRequest request) throws Exception {

        assertRequestIsMultipart(request);

        final ServletFileUpload servletFileUpload = new ServletFileUpload();
        final FileItemIterator fileItemIterator = servletFileUpload.getItemIterator(request);

        final VerificationItems verificationItems = new VerificationItems();
        Metadata metadata = null;
        while (fileItemIterator.hasNext()){
            FileItemStream fileItemStream = fileItemIterator.next();
            if (METADATA_FIELD_NAME.equals(fileItemStream.getFieldName())){
                if (metadata != null){
                    throw new IllegalStateException("Found more than one metadata field");
                }
                metadata = unmarshalMetadata(fileItemStream.openStream());
            }else {
                VerificationItem verificationItem = buildVerificationItem(fileItemStream.openStream(), fileItemStream.getFieldName());
                verificationItems.getVerificationItems().add(verificationItem);
            }
        }
        processVerificationItems(verificationItems, metadata, false);
        return verificationItems;
    }

    // -- ----------------------------------------------------- --
    // Static utility methods
    // -- ----------------------------------------------------- --

    static MultipartContext getMultipartContext(final HttpServletRequest request){
        String contentType = request.getContentType();
        int contentLength = request.getContentLength();
        String charEncoding = request.getCharacterEncoding();
        return new MultipartContext(contentType, contentLength, charEncoding);
    }

    static AsyncContext switchRequestToAsyncIfNeeded(final HttpServletRequest request){
        if (request.isAsyncStarted()){
            if (log.isDebugEnabled()) log.debug("Async context already started. Return it");
            return request.getAsyncContext();
        }else{
            if (log.isDebugEnabled()) log.info("Start async context and return it.");
            return request.startAsync();
        }
    }

    static ChecksumStreamStorage getChecksumStreamStorageOrThrow(final StreamStorage streamStorage){
        if (streamStorage instanceof ChecksumStreamStorage){
            return (ChecksumStreamStorage) streamStorage;
        }else{
            throw new IllegalStateException("Expected ChecksumPartStreams but got " + streamStorage.getClass().getName());
        }
    }

    static Metadata unmarshalMetadata(final InputStream inputStream){
        try {
            return GSON.fromJson(new BufferedReader(new InputStreamReader(inputStream)), Metadata.class);
        }finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    static Metadata unmarshalMetadata(final String json){
        return GSON.fromJson(json, Metadata.class);
    }

    static VerificationItem buildVerificationItem(final ChecksumStreamStorage checksumStreamStorage, final String fieldName){

        final String outputStreamChecksum = ChecksumStreamUtils.digestAsHexString(checksumStreamStorage.getChecksum());
        final long outputStreamWrittenBytes = checksumStreamStorage.getWrittenBytes();

        final ChecksumAndReadBytes checksumAndReadBytes =  ChecksumStreamUtils.computeChecksumAndReadBytes(checksumStreamStorage.getInputStream());
        final String inputStreamChecksum = checksumAndReadBytes.getChecksum();
        final long inputStreamReadBytes = checksumAndReadBytes.getReadBytes();

        VerificationItem verificationItem = new VerificationItem();
        verificationItem.setFile(fieldName);
        verificationItem.setPartInputStreamReadBytes(inputStreamReadBytes);
        verificationItem.setPartInputStreamStreamChecksum(inputStreamChecksum);
        verificationItem.setPartOutputStreamChecksum(outputStreamChecksum);
        verificationItem.setPartOutputStreamWrittenBytes(outputStreamWrittenBytes);
        return verificationItem;

    }

    static VerificationItem buildVerificationItem(final InputStream inputStream, final String fieldName){

        ChecksumAndReadBytes checksumAndReadBytes = ChecksumStreamUtils.computeChecksumAndReadBytes(inputStream, "SHA-256");
        VerificationItem verificationItem = new VerificationItem();
        verificationItem.setFile(fieldName);
        verificationItem.setPartInputStreamReadBytes(checksumAndReadBytes.getReadBytes());
        verificationItem.setPartInputStreamStreamChecksum(checksumAndReadBytes.getChecksum());

        return verificationItem;
    }

    static Map<String, FileMetadata> metadataToFileMetadataMap(final Metadata metadata){
        return Maps.uniqueIndex(metadata.getFilesMetadata(), new Function<FileMetadata, String>() {
            @Override
            public String apply(FileMetadata fileMetadata) {
                return Files.getNameWithoutExtension(fileMetadata.getFile()) + "." + Files.getFileExtension(fileMetadata.getFile());
            }
        });
    }

    static void processVerificationItems(final VerificationItems verificationItems, final Metadata metadata, final boolean checkOutputStream){

        if (metadata == null){
            throw new IllegalStateException("No metadata found");
        }

        Map<String, FileMetadata> metadataMap = metadataToFileMetadataMap(metadata);
        List<VerificationItem> verificationItemList = verificationItems.getVerificationItems();

        if (metadataMap.size() != verificationItemList.size()){
            throw new IllegalStateException("The number of attachments don't match the number of items in the metadata");
        }


        for (VerificationItem verificationItem : verificationItemList){
            FileMetadata fileMetadata = metadataMap.get(verificationItem.getFile());
            if (fileMetadata == null){
                throw new IllegalStateException("Metadata not found for file: " + verificationItem.getFile());
            }

            verificationItem.setReceivedChecksum(fileMetadata.getChecksum());
            verificationItem.setReceivedSize(fileMetadata.getSize());

            // Verify if all hashes and sizes are matching and set the status...
            if (checkOutputStream) {
                if (verificationItem.getPartInputStreamReadBytes() == verificationItem.getPartOutputStreamWrittenBytes() &&
                        verificationItem.getPartInputStreamReadBytes() == fileMetadata.getSize() &&
                        verificationItem.getPartInputStreamStreamChecksum().equals(verificationItem.getPartOutputStreamChecksum()) &&
                        verificationItem.getPartInputStreamStreamChecksum().equals(fileMetadata.getChecksum())) {
                    verificationItem.setStatus("MATCHING");
                } else {
                    verificationItem.setStatus("NOT MATCHING");
                }
            }else{
                if (verificationItem.getPartInputStreamReadBytes() ==  fileMetadata.getSize() &&
                        verificationItem.getPartInputStreamStreamChecksum().equals(fileMetadata.getChecksum())) {
                    verificationItem.setStatus("MATCHING");
                } else {
                    verificationItem.setStatus("NOT MATCHING");
                }
            }
        }
    }

    static void assertRequestIsMultipart(final HttpServletRequest request){
        if (!MultipartUtils.isMultipart(request.getContentType())){
            throw new IllegalStateException("Expected multipart request");
        }
    }

}

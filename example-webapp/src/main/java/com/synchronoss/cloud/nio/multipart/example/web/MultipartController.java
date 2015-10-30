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

package com.synchronoss.cloud.nio.multipart.example.web;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.synchronoss.cloud.nio.multipart.ChecksumPartStreamsFactory.ChecksumPartStreams;
import com.synchronoss.cloud.nio.multipart.*;
import com.synchronoss.cloud.nio.multipart.PartStreamsFactory.PartStreams;
import com.synchronoss.cloud.nio.multipart.example.config.RootApplicationConfig;
import com.synchronoss.cloud.nio.multipart.model.FileMetadata;
import com.synchronoss.cloud.nio.multipart.model.Metadata;
import com.synchronoss.cloud.nio.multipart.model.VerificationItem;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.synchronoss.cloud.nio.multipart.ParserFactory.newParser;

/**
 * <p>
 *     Controller that parses multipart requests in a NIO mode.
 * </p>
 *
 * @author Silvano Riz.
 */
@RestController
@RequestMapping
@Import(RootApplicationConfig.class)
public class MultipartController {

    private static final Logger log = LoggerFactory.getLogger(MultipartController.class);

    @Autowired
    private PartStreamsFactory partStreamsFactory;

    // For simplicity all marshalling and unmarshalling are executed outside spring...
    private Gson gson = new Gson();

    /**
     * NIO processing of a multipart request
     *
     * @param request The {@link HttpServletRequest}
     * @throws IOException
     */
    @RequestMapping(value = "/nio/multipart", method = RequestMethod.POST)
    public @ResponseBody void nioMultipart(final HttpServletRequest request) throws IOException {

        if (log.isDebugEnabled())log.debug("Process multipart request");

        final Map<String, VerificationItem> verificationItems = new HashMap<String, VerificationItem>();
        final AsyncContext asyncContext = switchRequestToAsyncIfNeeded(request);
        final ServletInputStream inputStream = request.getInputStream();
        final AtomicBoolean sendResponse = new AtomicBoolean(false);

        // Set up the listener. This is where the business logic happens...
        final NioMultipartParserListener listener = new NioMultipartParserListener() {

            final AtomicInteger partCounter = new AtomicInteger(0);
            Metadata metadata;

            @Override
            public void onPartReady(final PartStreams partStreams, final Map<String, List<String>> headersFromPart) {
                if(log.isInfoEnabled())log.info("PARSER LISTENER - onPartReady") ;

                final String fieldName = MultipartUtils.getFieldName(headersFromPart);
                if(log.isInfoEnabled())log.info("Processing field: " + fieldName);

                final ChecksumPartStreams checksumPartStreams = getChecksumPartStreamsOrThrow(partStreams);
                if ("metadata".equals(fieldName)){

                    // Metadata part, just read the metadata...
                    InputStream mainPartInputStream = partStreams.getPartInputStream();
                    metadata = gson.fromJson(new BufferedReader(new InputStreamReader(mainPartInputStream)), Metadata.class);

                }else{

                    // Attachments, create the VerificationItem...
                    final String outputStreamDigest = checksumPartStreams.getOutputStreamDigest();
                    final String inputStreamDigest = checksumPartStreams.getInputStreamDigest();

                    final long outputStreamWrittenBytes = checksumPartStreams.getOutputStreamWrittenBytes();
                    final long inputStreamReadBytes = checksumPartStreams.getInputStreamReadBytes();

                    VerificationItem verificationItem = new VerificationItem();
                    verificationItem.setFile(fieldName);
                    verificationItem.setPartInputStreamReadBytes(inputStreamReadBytes);
                    verificationItem.setPartInputStreamStreamChecksum(inputStreamDigest);
                    verificationItem.setPartOutputStreamChecksum(outputStreamDigest);
                    verificationItem.setPartOutputStreamWrittenBytes(outputStreamWrittenBytes);

                    // The fieldName is the file name in this case! Useful to match with the metadata after...
                    verificationItems.put(fieldName, verificationItem);
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
            public void onFormFieldPartReady(String fieldName, String fieldValue, Map<String, List<String>> headersFromPart) {
                if(log.isInfoEnabled()) {
                    log.info("PARSER LISTENER - onFormFieldPartReady");
                    log.info("Processing form field: " + fieldName + " - " + fieldValue);
                }

                // Metadata might be sent as a form field...
                if ("metadata".equals(fieldName)){
                    metadata = gson.fromJson(fieldValue, Metadata.class);
                }

            }

            @Override
            public void onAllPartsFinished() {
                if(log.isInfoEnabled())log.info("PARSER LISTENER - onAllPartsFinished");

                // All parts finished, build the response...
                for(FileMetadata fileMetadata : metadata.getFilesMetadata()){

                    final String fileName = Files.getNameWithoutExtension(fileMetadata.getFile()) + "." + Files.getFileExtension(fileMetadata.getFile());
                    VerificationItem verificationItem = verificationItems.get(fileName);

                    if (verificationItem == null) {
                        throw new IllegalStateException("No attach,ent found for the file anmed " + fileName);
                    }

                    verificationItem.setReceivedChecksum(fileMetadata.getChecksum());
                    verificationItem.setReceivedSize(fileMetadata.getSize());

                    // Verify if all hashes and sizes are matching and set the status...
                    if (verificationItem.getPartInputStreamReadBytes() == verificationItem.getPartOutputStreamWrittenBytes() &&
                            verificationItem.getPartInputStreamReadBytes() == fileMetadata.getSize() &&
                            verificationItem.getPartInputStreamStreamChecksum().equals(verificationItem.getPartOutputStreamChecksum()) &&
                            verificationItem.getPartInputStreamStreamChecksum().equals(fileMetadata.getChecksum())){

                        verificationItem.setStatus("MATCHING");

                    }else{
                        verificationItem.setStatus("NOT MATCHING");
                    }
                }

                sendResponse(sendResponse, asyncContext, verificationItems.values());
            }

            @Override
            public void onError(String message, Throwable cause) {
                // Probably invalid data...
                throw new IllegalStateException("Encountered an error during the parsing: " + message, cause);
            }

        };

        final MultipartContext ctx = getMultipartContext(request);

        // Use a try with resource to make sure the parser is closed...
        try(final NioMultipartParser parser = newParser(ctx, listener).withCustomPartStreamsFactory(partStreamsFactory).forNio()){

            // Set up the Servlet 3.1 read listener and connect it to the parser
            inputStream.setReadListener(new ReadListener() {

                @Override
                public void onDataAvailable() throws IOException {
                    if(log.isInfoEnabled())log.info("NIO READ LISTENER - onDataAvailable");
                    int bytesRead;
                    byte bytes[] = new byte[1024];
                    while (inputStream.isReady() && (bytesRead = inputStream.read(bytes)) != -1) {
                        parser.write(bytes, 0, bytesRead);
                    }
                    if(log.isInfoEnabled())log.info("Epilogue bytes..." ) ;
                }

                @Override
                public void onAllDataRead() throws IOException {
                    if(log.isInfoEnabled())log.info("NIO READ LISTENER - onAllDataRead");
                    sendResponse(sendResponse, asyncContext, verificationItems.values());
                }

                @Override
                public void onError(Throwable throwable) {
                    IOUtils.closeQuietly(parser);
                    log.error("onError", throwable);
                    sendError(sendResponse, asyncContext, "Unknown error");
                }

            });

        }catch (Exception e){
            // Probably bug in the client/nio parser code...
            log.error("Parsing error", e);
            sendError(sendResponse, asyncContext, "Unknown error");
        }
    }

    void sendResponse(final AtomicBoolean sendResponse, final AsyncContext asyncContext, final Collection<VerificationItem> verificationItems){
        if (sendResponse.getAndSet(true)) {
            try {
                final Writer responseWriter = asyncContext.getResponse().getWriter();
                responseWriter.write(gson.toJson(verificationItems));
                asyncContext.complete();
            } catch (Exception e) {
                log.error("Failed to send back the response", e);
            }
        }
    }

    void sendError(final AtomicBoolean sendResponse, final AsyncContext asyncContext, final String message){
        if (sendResponse.getAndSet(true)) {
            try {
                final ServletResponse servletResponse = asyncContext.getResponse();
                if (servletResponse instanceof HttpServletResponse){
                    ((HttpServletResponse)sendResponse).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message);
                }else {
                    asyncContext.getResponse().getWriter().write(message);
                }
                asyncContext.complete();
            } catch (Exception e) {
                log.error("Failed to send back the error response", e);
            }
        }
    }

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

    static ChecksumPartStreams getChecksumPartStreamsOrThrow(final PartStreams partStreams){
        if (partStreams instanceof ChecksumPartStreams){
            return (ChecksumPartStreams)partStreams;
        }else{
            throw new IllegalStateException("Expected ChecksumPartStreams but got " + partStreams.getClass().getName());
        }

    }

}

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

import com.google.common.base.Joiner;
import com.synchronoss.cloud.nio.multipart.MultipartContext;
import com.synchronoss.cloud.nio.multipart.NioMultipartParser;
import com.synchronoss.cloud.nio.multipart.NioMultipartParserListener;
import com.synchronoss.cloud.nio.multipart.example.config.RootApplicationConfig;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>
 *     File Upload Controller
 * </p>
 *
 * @author Silvano Riz.
 */
@RestController
@RequestMapping
@Import(RootApplicationConfig.class)
public class FileUploadController {

    private static final Logger log = LoggerFactory.getLogger(FileUploadController.class);

    @Value("${upload.directory}")
    private String fileUploadDirectory;

    /**
     * NIO processing of a multipart request
     *
     * @param request The {@link HttpServletRequest}
     * @throws IOException
     */
    @RequestMapping(value = "/nio/multipart", method = RequestMethod.POST)
    public @ResponseBody void nioMultipart(final HttpServletRequest request) throws IOException {

        if (log.isDebugEnabled())log.debug("Process multipart request");

        final AsyncContext asyncContext = switchRequestToAsyncIfNeeded(request);
        final ServletInputStream inputStream = request.getInputStream();
        final ResponseBuilder responseBuilder = new ResponseBuilder(asyncContext);

        // Set up the listener. This is where the business logic happens...
        final NioMultipartParserListener listener = new NioMultipartParserListener() {

            @Override
            public void onPartReady(final InputStream partBodyInputStream, final Map<String, List<String>> headersFromPart) {
                responseBuilder.appendToResponse("ON PART READY", headersFromPart, partBodyInputStream);
            }

            @Override
            public void onNestedPartStarted(final Map<String, List<String>> headersFromParentPart) {
                responseBuilder.appendToResponse("ON NESTED PART STARTED", headersFromParentPart);
            }

            @Override
            public void onNestedPartFinished() {
                responseBuilder.appendToResponse("ON NESTED PART FINISHED");
            }

            @Override
            public void onFormFieldPartReady(String fieldName, String fieldValue, Map<String, List<String>> headersFromPart) {
                responseBuilder.appendToResponse("ON FORM FIELD PART READY", headersFromPart, fieldName, fieldValue);

            }

            @Override
            public void onAllPartsFinished() {
                responseBuilder.appendToResponse("ON ALL PART FINISHED");
                responseBuilder.send();
            }

            @Override
            public void onError(String message, Throwable cause) {
                // Invalid data...
                responseBuilder.appendToResponse("ON ERROR", cause);
                responseBuilder.sendError(cause);
            }

        };

        try(NioMultipartParser parser = new NioMultipartParser(getMultipartContext(request), listener)){

            // Set up the Servlet 3.1 read listener and connect it to the parser
            inputStream.setReadListener(new ReadListener() {

                @Override
                public void onDataAvailable() throws IOException {
                    int bytesRead;
                    byte bytes[] = new byte[1024];
                    while (inputStream.isReady()  && (bytesRead = inputStream.read(bytes)) != -1) {
                        parser.write(bytes, 0, bytesRead);
                        responseBuilder.captureRequest(bytes, 0, bytesRead);
                    }
                }

                @Override
                public void onAllDataRead() throws IOException {
                    responseBuilder.send();
                }

                @Override
                public void onError(Throwable throwable) {
                    responseBuilder.sendError(throwable);
                }

            });

        }catch (Exception e){
            // Bug in your code...
        }

    }

    MultipartContext getMultipartContext(final HttpServletRequest request){
        String contentType = request.getContentType();
        int contentLength = request.getContentLength();
        String charEncoding = request.getCharacterEncoding();
        return new MultipartContext(contentType, contentLength, charEncoding);
    }

    AsyncContext switchRequestToAsyncIfNeeded(final HttpServletRequest request){
        if (request.isAsyncStarted()){
            if (log.isDebugEnabled()) log.debug("Async context already started. Return it");
            return request.getAsyncContext();
        }else{
            if (log.isDebugEnabled()) log.info("Start async context and return it.");
            return request.startAsync();
        }
    }


    static class ResponseBuilder{

        private final AsyncContext asyncContext;
        private final AtomicBoolean sendResponse = new AtomicBoolean(false);

        private final StringBuilder response = new StringBuilder();
        private final ByteArrayOutputStream requestCapture =new ByteArrayOutputStream();

        public ResponseBuilder(AsyncContext asyncContext) {
            this.asyncContext = asyncContext;
        }

        public synchronized void send() {
            if (sendResponse.getAndSet(true)) {
                try {
                    String responseText = response.append("\n====== ORIGINAL REQUEST ======\n").append(requestCapture.toString()).toString();
                    final Writer responseWriter = asyncContext.getResponse().getWriter();
                    responseWriter.write(responseText);
                    asyncContext.complete();
                }catch (Exception e){
                    log.error("Could not send response", e);
                }

            }
        }

        public synchronized void sendError(final Throwable t) {
            if (sendResponse.getAndSet(true)) {
                try {
                    String responseText = "Error: " + t.getLocalizedMessage();
                    final Writer responseWriter = asyncContext.getResponse().getWriter();
                    responseWriter.write(responseText);
                    asyncContext.complete();
                }catch (Exception e){
                    log.error("Could not send error response", e);
                }
            }
        }

        public synchronized void appendToResponse(final String event, final Map<String, List<String>> headers, final String formField, final String formFieldValue){
            appendToResponse(event, headers);
            response.append(formField).append(" = ").append(formFieldValue).append('\n');
        }

        public synchronized void appendToResponse(final String event, final Map<String, List<String>> headers){
            response.append("\n[[[ ").append(event).append(" ]]]\n");
            headers(headers);
        }

        public synchronized void appendToResponse(final String event, final Map<String, List<String>> headers, final InputStream bodyInputStream){
            appendToResponse(event, headers);
            body(bodyInputStream);
        }

        public synchronized void appendToResponse(final String event){
            response.append("\n[[[ ").append(event).append(" ]]]\n");
        }

        public synchronized void appendToResponse(final String event, Throwable error){
            response.append("\n[[[ ").append(event).append(" ]]]\n");
            response.append(error.getLocalizedMessage()).append('\n');
        }

        public void captureRequest(final byte[] data, final int indexStart, final int indexEnd){
            requestCapture.write(data, indexStart, indexEnd);
        }

        private void headers(final Map<String, List<String>> headers){
            if (headers == null || headers.size() == 0){
                response.append("No headers");
            }else{
                response.append("Headers:\n");
                for (Map.Entry<String, List<String>> headerEntry : headers.entrySet()) {
                    response.append("Header -> ").append(headerEntry.getKey()).append(" : ").append(Joiner.on(',').join(headerEntry.getValue())).append('\n');
                }
            }
        }

        private void body(final InputStream bodyInputStream){
            try{
                response.append("Body:\n").append(IOUtils.toString(bodyInputStream));
            }catch (Exception e){
                response.append("Unable to convert body content to String");
            }
            response.append('\n');
        }


    }

}

/*
 * The MIT License
 *
 * Copyright 2013 jdmr.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.synchronoss.cloud.nio.multipart.example.web;

import com.google.common.base.Joiner;
import com.synchronoss.cloud.nio.multipart.NioMultipartParser;
import com.synchronoss.cloud.nio.multipart.NioMultipartParserListener;
import com.synchronoss.cloud.nio.multipart.MultipartContext;
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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;


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

        final ByteArrayOutputStream requestDumper = new ByteArrayOutputStream();
        final StringBuilder response = new StringBuilder();

        // Set up the listener. This is where the business logic happens...
        final NioMultipartParserListener listener = new NioMultipartParserListener() {

            @Override
            public void onPartComplete(final InputStream partBodyInputStream, final Map<String, List<String>> headersFromPart) {

                append("On Part Complete", response);
                appendHeaders(headersFromPart, response);
                appendBody(partBodyInputStream, response);

            }

            @Override
            public void onNestedPartStarted(final Map<String, List<String>> headersFromParentPart) {
                append("Nested part started", response);
                appendHeaders(headersFromParentPart, response);
            }

            @Override
            public void onNestedPartRead() {
                append("Nested part read", response);
            }

            @Override
            public void onFormFieldPartComplete(String fieldName, String fieldValue, Map<String, List<String>> headersFromPart) {
                append("On Form Field Complete", response);
                appendHeaders(headersFromPart, response);
                append("Field " + fieldName + ": " + fieldValue, response);

            }

            @Override
            public void onAllPartsRead() {
                append("On All Parts Read", response);
            }

            @Override
            public void onError(String message, Throwable cause) {
                log.error("Parser Error", cause);
                append("On Error: " + message, response);
            }

        };

        // Set up the parser...
        final NioMultipartParser parser = new NioMultipartParser(getMultipartContext(request), listener);

        // Set up the Servlet 3.1 read listener and connect it to the parser
        inputStream.setReadListener(new ReadListener() {

            private AtomicBoolean responseSent = new AtomicBoolean(false);

            @Override
            public void onDataAvailable() throws IOException {
                int bytesRead;
                byte bytes[] = new byte[1024];
                while (inputStream.isReady()  && (bytesRead = inputStream.read(bytes)) != -1) {
                    parser.write(bytes, 0, bytesRead);
                    requestDumper.write(bytes, 0, bytesRead);
                }
            }

            @Override
            public void onAllDataRead() throws IOException {
                sendResponse();
            }

            @Override
            public void onError(Throwable throwable) {
                try {
                    log.error("onError", throwable);
                    asyncContext.getResponse().getWriter().write("Error: " + throwable.getLocalizedMessage());
                    asyncContext.complete();
                    parser.close();
                }catch(IOException e){
                    log.warn("Error closing the asyncMultipartParser", e);
                }
            }

            void sendResponse() {
                if (responseSent.compareAndSet(false, true)){
                    try {
                        final Writer responseWriter = asyncContext.getResponse().getWriter();
                        responseWriter.write("<<<< ORIGINAL REQUEST BODY >>>>\n");
                        responseWriter.write(requestDumper.toString());
                        responseWriter.write("\n<<<< PROCESS LOG >>>>\n");
                        responseWriter.write(response.toString());
                        List<String> fsmTransitions = parser.geFsmTransitions();
                        if (fsmTransitions != null) {
                            responseWriter.write("\n<<<< FSM TRANSITIONS LOG >>>>\n");
                            responseWriter.write(Joiner.on('\n').join(fsmTransitions));
                        }
                        asyncContext.complete();
                    }catch (Exception e){
                        onError(e);
                    }finally {
                        IOUtils.closeQuietly(parser);
                    }
                }
            }

        });

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

    void append(final String message, final StringBuilder stringBuilder) {
        stringBuilder.append(new Date()).append(" - ").append(message).append('\n');
    }

    void appendHeaders(final Map<String, List<String>> headers, final StringBuilder stringBuilder){
        if (headers == null || headers.size() == 0){
            append("No headers", stringBuilder);
        }else{
            for (Map.Entry<String, List<String>> headerEntry : headers.entrySet()) {
                append("Header -> " + headerEntry.getKey() + " : " + Joiner.on(',').join(headerEntry.getValue()), stringBuilder);
            }
        }
    }

    void appendBody(final InputStream inputStream, final StringBuilder stringBuilder){
        try{
            append("Body:\n" + IOUtils.toString(inputStream), stringBuilder);
        }catch (Exception e){
            append("Unable to convert body content toString", stringBuilder);
        }
    }

}

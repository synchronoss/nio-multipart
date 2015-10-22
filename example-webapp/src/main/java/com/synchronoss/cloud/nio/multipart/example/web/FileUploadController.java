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
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;


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

        log.debug("nioUpload");

        final AsyncContext asyncContext = switchRequestToAsyncIfNeeded(request);
        final ServletInputStream inputStream = request.getInputStream();

        NioMultipartParserListener listener = new NioMultipartParserListener() {
            @Override
            public void onPartComplete(InputStream partBodyInputStream, Map<String, List<String>> headersFromPart) {

                log.info("PART COMPLETE");

                for (Map.Entry<String, List<String>> headerEntry : headersFromPart.entrySet()){
                    log.info("Header -> " + headerEntry.getKey() + " : " + Joiner.on(',').join(headerEntry.getValue()));
                }
                log.info("--");
                try {
                    log.info("Body\n" + IOUtils.toString(partBodyInputStream));
                }catch (Exception e){
                    log.error("Cannot read the body", e);
                }

            }

            @Override
            public void onFormFieldPartComplete(String fieldName, String fieldValue, Map<String, List<String>> headersFromPart) {
                log.info("FORM FIELD COMPLETE");

                for (Map.Entry<String, List<String>> headerEntry : headersFromPart.entrySet()){
                    log.info("Header -> " + headerEntry.getKey() + " : " + Joiner.on(',').join(headerEntry.getValue()));
                }
                log.info("Field " + fieldName + ": " + fieldValue);
                log.info("--");

            }

            @Override
            public void onAllPartsRead() {
                log.info("ALL PARTS READ");
            }

            @Override
            public void onError(String message, Throwable cause) {
                log.error("ERROR", cause);
            }
        };

        final MultipartContext multipartContext = getMultipartContext(request);

        log.debug("Multipart context: " + multipartContext);

        final NioMultipartParser parser = new NioMultipartParser(multipartContext, listener);

        inputStream.setReadListener(new ReadListener() {
            private long totalBytesRead = 0;

            @Override
            public void onDataAvailable() throws IOException {
                int bytesRead;
                byte bytes[] = new byte[1024];
                while (inputStream.isReady()  && (bytesRead = inputStream.read(bytes)) != -1) {
                    totalBytesRead += bytesRead;
                    parser.write(bytes, 0, bytesRead);
                }
            }

            @Override
            public void onAllDataRead() throws IOException {
                asyncContext.getResponse().getWriter().write("OK - read " + totalBytesRead + " bytes");
                asyncContext.complete();
                parser.close();
            }

            @Override
            public void onError(Throwable throwable) {
                try {
                    log.error("onError", throwable);
                    asyncContext.getResponse().getWriter().write("KO - err " + throwable.getLocalizedMessage());
                    asyncContext.complete();
                    parser.close();
                }catch(IOException e){
                    log.warn("Error closing the asyncMultipartParser", e);
                }
            }
        });

    }

    final MultipartContext getMultipartContext(final HttpServletRequest request){
        String contentType = request.getContentType();
        int contentLength = request.getContentLength();
        String charEncoding = request.getCharacterEncoding();
        return new MultipartContext(contentType, contentLength, charEncoding);
    }

    AsyncContext switchRequestToAsyncIfNeeded(final HttpServletRequest request){
        if (request.isAsyncStarted()){
            log.info("Async context already started. Return it");
            return request.getAsyncContext();
        }else{
            log.info("Start async context and return it.");
            return request.startAsync();
        }
    }

}

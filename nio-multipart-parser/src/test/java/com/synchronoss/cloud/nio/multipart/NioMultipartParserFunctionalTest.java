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

package com.synchronoss.cloud.nio.multipart;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.synchronoss.cloud.nio.multipart.testutil.ChunksFileReader;
import com.synchronoss.cloud.nio.multipart.testutil.MultipartTestCases;
import com.synchronoss.cloud.nio.multipart.testutil.MultipartTestCases.MultipartTestCase;
import org.apache.commons.fileupload.FileItemHeaders;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 *     Functional test that verifies the library is compliant with the apache commons fileupload.
 * </p>
 * @author Silvano Riz.
 */
@RunWith(Parameterized.class)
public class NioMultipartParserFunctionalTest {

    private static final Logger log = LoggerFactory.getLogger(NioMultipartParserFunctionalTest.class);

    final MultipartTestCase testCase;
    public NioMultipartParserFunctionalTest(final MultipartTestCase testCase){
        this.testCase = testCase;
    }

    @Parameterized.Parameters
    public static Collection data() {
        return MultipartTestCases.ALL_TEST_CASES;
        //return Collections.singletonList(MultipartTestCases. FILE_0001);
    }

    @Test
    public void parse() throws Exception {

        log.info("============= Test Case: " + testCase.getDescription());

        if (log.isDebugEnabled()){
            log.debug("Request body\n" + IOUtils.toString(testCase.getBodyInputStream()));
        }

        final AtomicBoolean finished = new AtomicBoolean(false);
        final FileUpload fileUpload = new FileUpload();
        final FileItemIterator fileItemIterator = fileUpload.getItemIterator(testCase.getRequestContext());
        final NioMultipartParserListener nioMultipartParserListener = nioMultipartParserListenerVerifier(fileItemIterator, finished);


        // Comment out the NioMultipartParserListener above and uncomment the next two lines to
        // skip validation and just print the parts as extracted by the 2 frameworks.
        //dumpFileIterator(fileItemIterator);
        //final NioMultipartParserListener nioMultipartParserListener = nioMultipartParserListenerDumper();

        final MultipartContext multipartContext = testCase.getMultipartContext();
        final ChunksFileReader chunksFileReader = new ChunksFileReader(testCase.getBodyInputStream(), 5, 10);
        final DefaultPartStreamsFactory defaultBodyStreamFactory = new DefaultPartStreamsFactory(3000);// 3kb
        final NioMultipartParser parser = new NioMultipartParser(multipartContext, nioMultipartParserListener);


        byte[] chunk;
        while (true) {

            chunk = chunksFileReader.readChunk();
            if (chunk.length <= 0) {
                break;
            }
            parser.write(chunk, 0, chunk.length);
        }

        int attempts = 0;
        while(!finished.get()){
            Thread.sleep(100);
            if (++attempts > 3){
                Assert.fail("Parser didn't come back in a reasonable time");
            }
        }
        log.info("TRANSITIONS: \n" + Joiner.on('\n').join(parser.geFsmTransitions()));

    }

    NioMultipartParserListener nioMultipartParserListenerVerifier(final FileItemIterator fileItemIterator, final AtomicBoolean finished){

        return new NioMultipartParserListener() {

            AtomicInteger partIndex = new AtomicInteger(0);

            @Override
            public void onPartReady(PartStreamsFactory.PartStreams partStreams, Map<String, List<String>> headersFromPart) {
                log.info("<<<<< On part complete [" + (partIndex.addAndGet(1)) + "] >>>>>>");
                assertFileItemIteratorHasNext(true);
                final FileItemStream fileItemStream = fileItemIteratorNext();
                assertHeadersAreEqual(fileItemStream.getHeaders(), headersFromPart);
                assertInputStreamsAreEqual(fileItemStreamInputStream(fileItemStream), partStreams.getPartInputStream());
            }

            @Override
            public void onFormFieldPartReady(String fieldName, String fieldValue, Map<String, List<String>> headersFromPart) {
                log.info("<<<<< On form field complete [" + (partIndex.addAndGet(1)) + "] >>>>>>");
                assertFileItemIteratorHasNext(true);
                final FileItemStream fileItemStream = fileItemIteratorNext();
                Assert.assertTrue(fileItemStream.isFormField());
                Assert.assertEquals(fieldName, fileItemStream.getFieldName());
                try {
                    Assert.assertEquals(fieldValue, IOUtils.toString(fileItemStream.openStream()));
                }catch (Exception e){
                    throw new IllegalStateException("Unable to assert field value", e);
                }
            }

            @Override
            public void onNestedPartStarted(Map<String, List<String>> headersFromParentPart) {
                log.info("<<<<< On form field complete [" + (partIndex) + "] >>>>>>");
            }


            @Override
            public void onAllPartsFinished() {
                log.info("<<<<< On all parts read: Number of parts ["+ partIndex.get() + "] >>>>>>");
                assertFileItemIteratorHasNext(false);
                finished.set(true);
            }

            @Override
            public void onNestedPartFinished() {
                log.info("<<<<< On form field complete [" + (partIndex) + "] >>>>>>");
            }

            @Override
            public void onError(String message, Throwable cause) {
                log.info("<<<<< On error. Part " + partIndex.get() + "] >>>>>>");
                finished.set(true);
                log.error(message, cause);
                Assert.fail("Got an error from the parser");
            }

            InputStream fileItemStreamInputStream(final FileItemStream fileItemStream){
                try{
                    return fileItemStream.openStream();
                }catch (Exception e){
                    throw new IllegalStateException("Unable to open the file item inputstream", e);
                }
            }

            void assertFileItemIteratorHasNext(boolean hasNext){
                try{
                    Assert.assertTrue("File iterator has next is not " + hasNext, hasNext == fileItemIterator.hasNext());
                }catch (Exception e){
                    throw new IllegalStateException("Unable to verify if the FileItemIterator has a next", e);
                }
            }

            FileItemStream fileItemIteratorNext(){
                try{
                    return fileItemIterator.next();
                }catch (Exception e){
                    throw new IllegalStateException("Unable to retrieve the next FileItemStream", e);
                }
            }

            void assertHeadersAreEqual(final FileItemHeaders fileItemHeaders, final Map<String, List<String>> headersFromPart){
                int i = 0;
                final Iterator<String> headerNamesIterator = fileItemHeaders.getHeaderNames();
                while (headerNamesIterator.hasNext()){
                    i++;

                    String headerName = headerNamesIterator.next();
                    List<String> headerValues = Lists.newArrayList(fileItemHeaders.getHeaders(headerName));
                    List<String> headerValues1 = headersFromPart.get(headerName);

                    if (log.isDebugEnabled()){
                        log.debug("Commons io header values for    '" + headerName + "': " + (headerValues != null ? Joiner.on(",").join(headerValues) : "null"));
                        log.debug("Nio multipart header values for '" + headerName + "': " + (headerValues1 != null ? Joiner.on(",").join(headerValues1) : "null"));
                    }

                    Assert.assertEquals(headerValues, headerValues1);
                }
                Assert.assertEquals(i, headersFromPart.size());
            }

            void assertInputStreamsAreEqual(InputStream fileItemInputStream, InputStream partBodyInputStream){
                try {
                    while (true) {
                        int bOne = fileItemInputStream.read();
                        int bTwo = partBodyInputStream.read();
                        Assert.assertEquals("Byte from commons file upload: " + bTwo + ", Byte from nio: " + bOne ,bOne, bTwo);

                        if (bOne == -1){
                            break;
                        }
                    }
                }catch (Exception e){
                    throw new IllegalStateException("Unable to verify the input streams", e);
                }finally {
                    IOUtils.closeQuietly(fileItemInputStream);
                    IOUtils.closeQuietly(partBodyInputStream);
                }
            }

        };
    }

    void dumpFileIterator(final FileItemIterator fileItemIterator){

        int partIndex = 0;

        try {
            log.info("-- COMMONS FILE UPLOAD --");
            while (fileItemIterator.hasNext()) {
                log.info("-- Part " + partIndex++);
                FileItemStream fileItemStream = fileItemIterator.next();

                FileItemHeaders fileItemHeaders = fileItemStream.getHeaders();
                Iterator<String> headerNames = fileItemHeaders.getHeaderNames();
                while(headerNames.hasNext()){
                    String headerName = headerNames.next();
                    log.info("Header: " + headerName+ ": " + Joiner.on(',').join(fileItemHeaders.getHeaders(headerName)));
                }
                log.info("Body:\n" + IOUtils.toString(fileItemStream.openStream()));
            }
            log.info("-- ------------------- --");
        }catch (Exception e){
            log.error("Error dumping the FileItemIterator", e);
        }

    }

    NioMultipartParserListener nioMultipartParserListenerDumper(){

        return new NioMultipartParserListener() {

            AtomicInteger partIndex = new AtomicInteger(0);

            @Override
            public void onPartReady(PartStreamsFactory.PartStreams partStreams, Map<String, List<String>> headersFromPart) {
                log.info("-- NIO MULTIPART PARSER : On part complete " + (partIndex.addAndGet(1)));
                log.info("-- Part " + partIndex.get());
                for (Map.Entry<String, List<String>> headersEntry : headersFromPart.entrySet()){
                    log.info("Header: " + headersEntry.getKey() + ": " + Joiner.on(',').join(headersEntry.getValue()));
                }
                InputStream partInputStream = partStreams.getPartInputStream();
                try {
                    log.info("Body:\n" + IOUtils.toString(partInputStream));
                }catch (Exception e){
                    log.error("Cannot read the body into a string", e);
                }finally {
                    IOUtils.closeQuietly(partInputStream);
                }

            }

            @Override
            public void onNestedPartStarted(Map<String, List<String>> headersFromParentPart) {
                log.info("-- NIO MULTIPART PARSER : On nested part started " + (partIndex));
                log.info("-- Part " + partIndex.get());
                for (Map.Entry<String, List<String>> headersEntry : headersFromParentPart.entrySet()){
                    log.info("Header: " + headersEntry.getKey() + ": " + Joiner.on(',').join(headersEntry.getValue()));
                }
            }

            @Override
            public void onFormFieldPartReady(String fieldName, String fieldValue, Map<String, List<String>> headersFromPart) {
                log.info("-- NIO MULTIPART PARSER : On form field complete " + partIndex.addAndGet(1));
                log.info("-- Part " + (partIndex.get()));
                for (Map.Entry<String, List<String>> headersEntry : headersFromPart.entrySet()){
                    log.info("Header: " + headersEntry.getKey() + ": " + Joiner.on(',').join(headersEntry.getValue()));
                }
                log.info("Field " + fieldName + ": " + fieldValue);
            }

            @Override
            public void onNestedPartFinished() {
                log.info("-- NIO MULTIPART PARSER : On nested part read");
            }

            @Override
            public void onAllPartsFinished() {
                log.info("-- NIO MULTIPART PARSER : On all parts read");
                log.info("-- Number of parts: " + partIndex.get() );
            }

            @Override
            public void onError(String message, Throwable cause) {
                log.info("-- NIO MULTIPART PARSER : On error");
                log.error("Error: " + message, cause);
            }
        };

    }

}
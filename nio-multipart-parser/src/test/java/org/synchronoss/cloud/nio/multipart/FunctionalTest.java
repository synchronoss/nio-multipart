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
package org.synchronoss.cloud.nio.multipart;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.synchronoss.cloud.nio.multipart.BlockingIOAdapter.PartItem;
import org.synchronoss.cloud.nio.multipart.testutil.ChunksFileReader;
import org.synchronoss.cloud.nio.multipart.testutil.MultipartTestCases;
import org.synchronoss.cloud.nio.multipart.testutil.MultipartTestCases.MultipartTestCase;
import org.synchronoss.cloud.nio.multipart.util.collect.CloseableIterator;
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
import org.synchronoss.cloud.nio.stream.storage.StreamStorage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * <p> Functional test that verifies the library is compliant with the apache commons fileupload.
 *
 * @author Silvano Riz.
 */
@RunWith(Parameterized.class)
public class FunctionalTest {

    private static final Logger log = LoggerFactory.getLogger(FunctionalTest.class);

    final MultipartTestCase testCase;
    public FunctionalTest(final MultipartTestCase testCase){
        this.testCase = testCase;
    }

    @Parameterized.Parameters
    public static Collection data() {
        return MultipartTestCases.ALL_TEST_CASES;
    }

    @Test
    public void nioParserFunctionalTest() throws Exception {

        log.info("NIO PARSER FUNCTIONAL TEST [ " + testCase.getDescription() + " ]");

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
                fail("Parser didn't come back in a reasonable time");
            }
        }
        if (log.isInfoEnabled()){
            List<String> fsmTransitions = parser.geFsmTransitions();
            if (fsmTransitions != null) {
                log.info("TRANSITIONS: \n" + Joiner.on('\n').join(fsmTransitions));
            }else{
                log.info("To see the FSM transitions enable debug on " + NioMultipartParser.class.getName());
            }
        }

    }

    @Test
    public void blockingIOAdapterFunctionalTest() throws Exception {

        log.info("BLOCKING IO ADAPTER FUNCTIONAL TEST [ " + testCase.getDescription() + " ]");

        if (log.isDebugEnabled()){
            log.debug("Request body\n" + IOUtils.toString(testCase.getBodyInputStream()));
        }

        final FileUpload fileUpload = new FileUpload();
        final FileItemIterator fileItemIterator = fileUpload.getItemIterator(testCase.getRequestContext());

        try(final CloseableIterator<PartItem> parts = Multipart.multipart(testCase.getMultipartContext()).forBlockingIO(testCase.getBodyInputStream())) {

            while (parts.hasNext()) {

                BlockingIOAdapter.PartItem partItem = parts.next();
                BlockingIOAdapter.PartItem.Type partItemType = partItem.getType();
                if (BlockingIOAdapter.PartItem.Type.NESTED_END.equals(partItemType) || BlockingIOAdapter.PartItem.Type.NESTED_START.equals(partItemType)) {
                    // Commons file upload is not returning an item representing the start/end of a nested multipart.
                    continue;
                }
                assertTrue(fileItemIterator.hasNext());
                FileItemStream fileItemStream = fileItemIterator.next();
                assertEquals(partItem, fileItemStream);
            }
        }

    }

    static void assertEquals(final BlockingIOAdapter.PartItem partItem, final FileItemStream fileItemStream) throws IOException {

        if (partItem instanceof BlockingIOAdapter.Part){
            BlockingIOAdapter.Part part = (BlockingIOAdapter.Part) partItem;
            assertHeadersAreEqual(fileItemStream.getHeaders(), part.getHeaders());
            assertInputStreamsAreEqual(part.getPartBody(), fileItemStream.openStream());
        }else{
            fail("Invalid part item type " + partItem.getClass());
        }

    }

    static void assertHeadersAreEqual(final FileItemHeaders fileItemHeaders, final Map<String, List<String>> headersFromPart){
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

    static void assertInputStreamsAreEqual(InputStream fileItemInputStream, InputStream partBodyInputStream){
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

    NioMultipartParserListener nioMultipartParserListenerVerifier(final FileItemIterator fileItemIterator, final AtomicBoolean finished){

        return new NioMultipartParserListener() {

            AtomicInteger partIndex = new AtomicInteger(0);

            @Override
            public void onPartFinished(StreamStorage partBodyStreamStorage, Map<String, List<String>> headersFromPart) {
                log.info("<<<<< On part complete [" + (partIndex.addAndGet(1)) + "] >>>>>>");
                assertFileItemIteratorHasNext(true);
                final FileItemStream fileItemStream = fileItemIteratorNext();
                assertHeadersAreEqual(fileItemStream.getHeaders(), headersFromPart);
                assertInputStreamsAreEqual(fileItemStreamInputStream(fileItemStream), partBodyStreamStorage.getInputStream());
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
                fail("Got an error from the parser");
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
                    assertTrue("File iterator has next is not " + hasNext, hasNext == fileItemIterator.hasNext());
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
            public void onPartFinished(StreamStorage partBodyStreamStorage, Map<String, List<String>> headersFromPart) {
                log.info("-- NIO MULTIPART PARSER : On part complete " + (partIndex.addAndGet(1)));
                log.info("-- Part " + partIndex.get());
                for (Map.Entry<String, List<String>> headersEntry : headersFromPart.entrySet()){
                    log.info("Header: " + headersEntry.getKey() + ": " + Joiner.on(',').join(headersEntry.getValue()));
                }
                InputStream partInputStream = partBodyStreamStorage.getInputStream();
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
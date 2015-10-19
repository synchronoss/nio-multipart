package com.synchronoss.nio.file.multipart;

import com.google.common.collect.Lists;
import com.synchronoss.nio.file.multipart.testutil.ChunksFileReader;
import com.synchronoss.nio.file.multipart.testutil.CommonsFileUploadParser;
import com.synchronoss.nio.file.multipart.testutil.TestFiles;
import org.apache.commons.fileupload.FileItemHeaders;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.synchronoss.nio.file.multipart.testutil.TestFiles.TestFile;

/**
 * Created by sriz0001 on 19/10/2015.
 */
@RunWith(Parameterized.class)
public class NioMultipartParserFunctionalTest {

    private static final Logger log = LoggerFactory.getLogger(NioMultipartParserFunctionalTest.class);

    final TestFile testFile;
    public NioMultipartParserFunctionalTest(final TestFile testFile){
        this.testFile = testFile;
    }

    @Parameterized.Parameters
    public static Collection data() {
        return TestFiles.TEST_FILES;
    }

    @Test
    public void parse(){

        log.info("File: " + testFile.getPath());

        final FileItemIterator fileItemIterator = CommonsFileUploadParser.parse(testFile);
        final MultipartContext multipartContext = multipartContextForTestFile(testFile);
        final ChunksFileReader chunksFileReader = new ChunksFileReader(testFile, 5, 10);
        final NioMultipartParserListener nioMultipartParserListener = nioMultipartParserListenerVerifier(fileItemIterator);

        NioMultipartParserImpl parser = new NioMultipartParserImpl(multipartContext, nioMultipartParserListener);

        byte[] chunk;
        while(true){

            chunk = chunksFileReader.readChunk();
            if (chunk.length <= 0){
                break;
            }
            parser.handleBytesReceived(chunk, 0, chunk.length);
        }

    }

    MultipartContext multipartContextForTestFile(final TestFile testFile){
        return new MultipartContext(testFile.getContentType(), testFile.getContentLength(), testFile.getCharEncoding());
    }

    NioMultipartParserListener nioMultipartParserListenerVerifier(final FileItemIterator fileItemIterator){

        return new NioMultipartParserListener() {

            int partIndex = 0;

            @Override
            public void onPartComplete(InputStream partBodyInputStream, Map<String, List<String>> headersFromPart) {
                log.info("-- On part complete " + (partIndex));
                assertFileItemIteratorHasNext(true);
                final FileItemStream fileItemStream = fileItemIteratorNext();
                assertHeadersAreEqual(fileItemStream.getHeaders(), headersFromPart);
                assertInputStreamsAreEqual(fileItemStreamInputStream(fileItemStream), partBodyInputStream);
                partIndex++;
            }

            @Override
            public void onAllPartsRead() {
                log.info("-- On all parts read: Number of parts "+ partIndex);
                assertFileItemIteratorHasNext(false);
            }

            @Override
            public void onError(String message, Throwable cause) {
                log.info("-- On error. Part " + partIndex);
                throw new IllegalStateException("Error: " + message, cause );
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

                    Assert.assertEquals(headerValues, headerValues1);
                }
                Assert.assertEquals(i, headersFromPart.size());
            }

            void assertInputStreamsAreEqual(InputStream fileItemInputStream, InputStream partBodyInputStream){
                try {
                    while (true) {
                        int bOne = fileItemInputStream.read();
                        int bTwo = partBodyInputStream.read();
                        Assert.assertEquals(bOne, bTwo);

                        if (bOne == -1){
                            break;
                        }
                    }
                }catch (Exception e){
                    throw new IllegalStateException("Unable to verify the input streams", e);
                }
            }

        };
    }

}
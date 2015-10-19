package com.synchronoss.nio.file.multipart;

import com.google.common.base.Joiner;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Created by sriz0001 on 19/10/2015.
 */
public class NioMultipartParserImplTest {

    private static final Logger log = LoggerFactory.getLogger(NioMultipartParserImplTest.class);

    @Test
    @Ignore
    public void parse(){

        NioMultipartParserListener listener = new NioMultipartParserListener() {
            @Override
            public void onPartComplete(InputStream partBodyInputStream, Map<String, List<String>> headersFromPart) {
                log.info("-- On part complete");
                for(Map.Entry<String, List<String>> headersEntry : headersFromPart.entrySet()){
                    log.info(headersEntry.getKey() + ": " + Joiner.on(",").join(headersEntry.getValue()));
                }

            }

            @Override
            public void onAllPartsRead() {
                log.info("-- On all parts read");
            }

            @Override
            public void onError(String message, Throwable cause) {
                log.info("-- On error");
                log.info("Message: " + message);
                log.error("Error", cause);
            }
        };

        //String fileName = "/samples/multipart-body-with-boundary-segments.txt";
        String fileName = "/samples/simple.txt";
        TestUtils.ChunksFileReader chunksFileReader = new TestUtils.ChunksFileReader(TestUtils.getTestFile(fileName), 5, 10);
        String contentType = TestUtils.getContentTypeForTestFile(fileName);
        MultipartContext multipartContext = new MultipartContext(contentType,-1,"UTF-8");
        NioMultipartParserImpl parser = new NioMultipartParserImpl(multipartContext, listener);

        byte[] chunk;
        while(true){
            chunk = chunksFileReader.readChunk();
            if (chunk.length <= 0){
                break;
            }
            parser.handleBytesReceived(chunk, 0, chunk.length);
        }

    }

}
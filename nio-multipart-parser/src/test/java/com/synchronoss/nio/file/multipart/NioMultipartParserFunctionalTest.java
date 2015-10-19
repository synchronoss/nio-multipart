package com.synchronoss.nio.file.multipart;

import com.google.common.base.Joiner;
import com.synchronoss.nio.file.multipart.testutil.ChunksFileReader;
import com.synchronoss.nio.file.multipart.testutil.TestFiles;
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
public class NioMultipartParserFunctionalTest {

    private static final Logger log = LoggerFactory.getLogger(NioMultipartParserFunctionalTest.class);

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

        ChunksFileReader chunksFileReader = new ChunksFileReader(TestFiles.TEXT_SIMPLE, 5, 10);
        MultipartContext multipartContext = new MultipartContext(TestFiles.TEXT_SIMPLE.getContentType(), TestFiles.TEXT_SIMPLE.getContentLength(), TestFiles.TEXT_SIMPLE.getCharEncoding());
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
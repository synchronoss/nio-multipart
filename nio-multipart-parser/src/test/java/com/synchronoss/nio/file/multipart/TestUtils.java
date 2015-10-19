package com.synchronoss.nio.file.multipart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mele on 17/10/2015.
 */
public class TestUtils {

    private static final Logger log = LoggerFactory.getLogger(TestUtils.class);

    static final Map<String, String> TEST_FILE_BOUNDARIES = new HashMap<String, String>(){{
        put("/samples/multipart-body-with-boundary-segments.txt", "aaaaaa");
        put("/samples/multipart-jpeg.txt", "MUEYT2qJT0_ZzYUvVQLy_DlrLeADyxzmsA");
        put("/samples/multipart-with-preamble.txt", "simple boundary");
        put("/samples/simple.txt", "BBBBB");
    }};


    public static String printBytesHexEncoded(final byte[] byteArray){

        StringBuilder sb = new StringBuilder(byteArray.length * 2);
        sb.append("[");
        int i=0;
        while (i<byteArray.length) {
            sb.append(printByteHexEncoded(byteArray[i]));
            i++;
            if (i<byteArray.length){
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();

    }

    public static String printByteHexEncoded(byte b){
        return String.format("0x%02x", b & 0xff);
    }

    public static String printCircularBuffer(final CircularBuffer circularBuffer){
            return "CircularBuffer{" +
                    "size=" + circularBuffer.size +
                    ", buffer=" + printBytesHexEncoded(circularBuffer.buffer) +
                    ", startValidDataIndex=" + circularBuffer.startValidDataIndex +
                    ", nextAvailablePosition=" + circularBuffer.nextAvailablePosition +
                    ", availableReadLength=" + circularBuffer.availableReadLength +
                    '}';
    }

    public static void writeDataToCircularBuffer(final CircularBuffer circularBuffer, final byte[] data){
        for (byte aData : data) {
            circularBuffer.write(aData);
        }
    }

    public static void writeDataToEndOfLineBuffer(final EndOfLineBuffer endOfLineBuffer, final byte[] data){
        for (byte aData : data) {
            endOfLineBuffer.write(aData);
        }
    }


    public static String getContentTypeForBoundary(final String boundary){
        return String.format("multipart/mixed; boundary=%s", boundary);
    }

    public static String getContentTypeForTestFile(final String fileName){
        final String boundary = TEST_FILE_BOUNDARIES.get(fileName);
        if (boundary == null){
            throw new IllegalArgumentException("Cannot find the boundary for the file " + fileName);
        }
        return getContentTypeForBoundary(boundary);
    }

    public static String getTestFile(final String fileName){
        try {
            URL resourceUrl = TestUtils.class.getResource(fileName);
            Path resourcePath = Paths.get(resourceUrl.toURI());
            return resourcePath.toFile().getAbsolutePath();
        }catch (Exception e){
            throw new IllegalStateException("Cannot find the test file", e);
        }
    }

    static class ChunksFileReader implements Closeable {

        final FileInputStream input;
        final ByteArrayOutputStream baos;
        final int minChunkSize;
        final int maxChunkSize;

        public ChunksFileReader(final String filePath, final int minChunkSize, final int maxChunkSize) {

            this.maxChunkSize = maxChunkSize;
            this.minChunkSize = minChunkSize;
            this.baos = new ByteArrayOutputStream();
            try {
                File file = new File(filePath);
                this.input = new FileInputStream(file);
            }catch (Exception e){
                throw new IllegalStateException("Unable to read the file: " + filePath, e);
            }
        }

        byte[] readChunk(){

            baos.reset();
            try {

                int chunkSize = newChunkSize();
                boolean readMore = true;
                while(readMore){

                    int b = input.read();
                    if (b != -1){
                        baos.write(b);
                    }

                    readMore = b!=-1 && --chunkSize > 0;
                }

                return baos.toByteArray();

            }catch (Exception e){
                throw new IllegalStateException("Unable to read chunk", e);
            }
        }

        int newChunkSize(){
            return minChunkSize + (int)(Math.random() * maxChunkSize);
        }

        @Override
        public void close() throws IOException {
            input.close();
        }
    }

}

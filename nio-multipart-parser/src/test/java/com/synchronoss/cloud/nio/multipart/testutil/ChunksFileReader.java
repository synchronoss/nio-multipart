package com.synchronoss.cloud.nio.multipart.testutil;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * <p>
 *     An utility class to read an input stream in chunks. Allows to simulate the servlet 3.1 onDataAvailable(...)
 * </p>
 * Created by sriz0001 on 19/10/2015.
 */
public class ChunksFileReader implements Closeable{

    final InputStream inputStream;
    final ByteArrayOutputStream baos;
    final int minChunkSize;
    final int maxChunkSize;

    public ChunksFileReader(final InputStream inputStream, final int minChunkSize, final int maxChunkSize) {

        this.maxChunkSize = maxChunkSize;
        this.minChunkSize = minChunkSize;
        this.baos = new ByteArrayOutputStream();
        this.inputStream = inputStream;
    }

    public byte[] readChunk(){

        baos.reset();
        try {

            int chunkSize = newChunkSize();
            boolean readMore = true;
            while(readMore){

                int b = inputStream.read();
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
        inputStream.close();
    }

}

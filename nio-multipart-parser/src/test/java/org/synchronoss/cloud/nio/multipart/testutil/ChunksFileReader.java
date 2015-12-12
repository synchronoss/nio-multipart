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

package org.synchronoss.cloud.nio.multipart.testutil;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * <p> An utility class to read an input stream in chunks. Allows to simulate the servlet 3.1 onDataAvailable(...)
 *
 * @author Silvano Riz.
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

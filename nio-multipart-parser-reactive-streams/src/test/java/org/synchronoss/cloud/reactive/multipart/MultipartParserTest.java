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

package org.synchronoss.cloud.reactive.multipart;

import org.junit.Test;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.synchronoss.cloud.nio.multipart.MultipartContext;
import reactor.core.publisher.Flux;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * <p> Unit tests for {@link MultipartParser}
 *
 * @author Silvano Riz.
 */
public class MultipartParserTest {

    private static final Logger log = LoggerFactory.getLogger(MultipartParserTest.class);

    private static final String testRequestContentType = "multipart/form-data;boundary=---1234";
    private static final String testRequest =
            "-----1234\r\n" +
            "Content-Disposition: form-data; name=\"file\"; filename=\"foo.tab\"\r\n" +
            "Content-Type: text/whatever\r\n" +
            "\r\n" +
            "This is the content of the file\n" +
            "\r\n" +
            "-----1234\r\n" +
            "Content-Disposition: form-data; name=\"field\"\r\n" +
            "\r\n" +
            "fieldValue\r\n" +
            "-----1234\r\n" +
            "Content-Disposition: form-data; name=\"multi\"\r\n" +
            "\r\n" +
            "value1\r\n" +
            "-----1234\r\n" +
            "Content-Disposition: form-data; name=\"multi\"\r\n" +
            "\r\n" +
            "value2\r\n" +
            "-----1234--\r\n";

    @Test
    public void testParse() throws Exception{

        final byte[] body = testRequest.getBytes(Charset.forName("UTF-8"));
        final MultipartContext ctx = new MultipartContext(testRequestContentType, body.length, "UTF-8");
        final MultipartParser multipartParser = new MultipartParser(ctx, 1, 1);
        final Publisher<byte[]> publisher = Flux.fromIterable(chunks(body));
        final Flux<Part> parts = multipartParser.parse(publisher);
        final Iterable<Part> partsIterable = parts.toIterable();

        for(Part part : partsIterable){
            log.info("Part: " + part);
        }
    }

    private Iterable<byte[]> chunks(final byte[] body){

        log.info("Dividing into chunks the array (length: " + body.length + ") -> " + Arrays.toString(body));

        final List<byte[]> chunks = new ArrayList<>();
        final Random generator = new Random();
        final int maxChunkSize = 5;
        int processed = 0;
        while (processed < body.length){
            int size = generator.nextInt(maxChunkSize) + 1;
            if (processed + size > body.length){
                size = body.length - processed;
            }
            //log.info("Chunk size: " + size);

            byte[] chunk = new byte[size];
            System.arraycopy(body, processed, chunk, 0, size);
            chunks.add(chunk);
            processed+=size;
        }

        //chunks.forEach(bytes -> log.info(Arrays.toString(bytes)));

        return chunks;
    }

}
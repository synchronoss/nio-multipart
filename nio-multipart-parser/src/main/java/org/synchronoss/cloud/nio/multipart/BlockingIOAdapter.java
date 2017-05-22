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

import org.synchronoss.cloud.nio.multipart.util.collect.AbstractIterator;
import org.synchronoss.cloud.nio.multipart.util.collect.CloseableIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.synchronoss.cloud.nio.stream.storage.StreamStorage;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.synchronoss.cloud.nio.multipart.NioMultipartParser.DEFAULT_BUFFER_SIZE;
import static org.synchronoss.cloud.nio.multipart.NioMultipartParser.DEFAULT_HEADERS_SECTION_SIZE;
import static org.synchronoss.cloud.nio.multipart.NioMultipartParser.DEFAULT_MAX_LEVEL_OF_NESTED_MULTIPART;

/**
 * <p> Adapts the {@link NioMultipartParser} to work with blocking IO.
 *     The adapter exposes a set of static methods that return a {@code CloseableIterator} over the parts.
 * <p> An alternative way to obtain the {@code CloseableIterator} is using the fluent API offered by {@link Multipart}.
 *
 * @author Silvano Riz
 */
public class BlockingIOAdapter {

    private static final Logger log = LoggerFactory.getLogger(BlockingIOAdapter.class);

    /**
     * <p>
     *     Parses the multipart stream and it returns the parts in form of {@code CloseableIterator}.
     * </p>
     * @param inputStream The multipart stream
     * @param multipartContext The multipart context
     * @return the parts in the form of a closeable iterator
     */
    public static CloseableIterator<ParserToken> parse(final InputStream inputStream, final MultipartContext multipartContext){
        return parse(inputStream, multipartContext, null, DEFAULT_BUFFER_SIZE, DEFAULT_HEADERS_SECTION_SIZE, DEFAULT_MAX_LEVEL_OF_NESTED_MULTIPART);
    }

    /**
     * <p>
     *     Parses the multipart stream and it return the parts in form of {@code CloseableIterator}.
     * </p>
     *
     * @param inputStream The multipart stream
     * @param multipartContext The multipart context
     * @param partBodyStreamStorageFactory The {@code PartBodyStreamStorageFactory} to use
     * @return the parts in the form of a closeable iterator
     */
    public static CloseableIterator<ParserToken> parse(final InputStream inputStream, final MultipartContext multipartContext, final PartBodyStreamStorageFactory partBodyStreamStorageFactory) {
        return parse(inputStream, multipartContext, partBodyStreamStorageFactory, DEFAULT_BUFFER_SIZE, DEFAULT_HEADERS_SECTION_SIZE, DEFAULT_MAX_LEVEL_OF_NESTED_MULTIPART);
    }

    /**
     * <p>
     *     Parses the multipart stream and it return the parts in form of {@code CloseableIterator}.
     * </p>
     *
     * @param inputStream The multipart stream
     * @param multipartContext The multipart context
     * @param bufferSize The buffer size in bytes
     * @return the parts in the form of a closeable iterator
     */
    public static CloseableIterator<ParserToken> parse(final InputStream inputStream, final MultipartContext multipartContext, final int bufferSize) {
        return parse(inputStream, multipartContext, null, bufferSize, DEFAULT_HEADERS_SECTION_SIZE, DEFAULT_MAX_LEVEL_OF_NESTED_MULTIPART);
    }

    /**
     * <p>
     *     Parses the multipart stream and it return the parts in form of {@link Iterable}.
     * </p>
     *
     * @param inputStream The multipart stream
     * @param multipartContext The multipart context
     * @param partBodyStreamStorageFactory The {@code PartBodyStreamStorageFactory} to use
     * @param bufferSize The buffer size in bytes
     * @param maxHeadersSectionSize The max size of the headers section in bytes
     * @param maxLevelOfNestedMultipart the max number of nested multipart
     * @return the parts in the form of a closeable iterator
     */
    @SuppressWarnings("unchecked")
    public static CloseableIterator<ParserToken> parse(final InputStream inputStream,
                                                       final MultipartContext multipartContext,
                                                       final PartBodyStreamStorageFactory partBodyStreamStorageFactory,
                                                       final int bufferSize,
                                                       final int maxHeadersSectionSize,
                                                       final int maxLevelOfNestedMultipart) {


        return new PartItemsIterator(inputStream, multipartContext, partBodyStreamStorageFactory, bufferSize, maxHeadersSectionSize, maxLevelOfNestedMultipart);
    }

    static class PartItemsIterator extends AbstractIterator<ParserToken> implements CloseableIterator<ParserToken> {

        private static final ParserToken END_OF_DATA = new ParserToken() {
            @Override
            public Type getType() {
                return null;
            }
        };
        private Queue<ParserToken> parserTokens = new ConcurrentLinkedQueue<>();
        private final NioMultipartParser parser;
        private final InputStream inputStream;

        public PartItemsIterator(final InputStream inputStream,
                                 final MultipartContext multipartContext,
                                 final PartBodyStreamStorageFactory partBodyStreamStorageFactory,
                                 final int bufferSize,
                                 final int maxHeadersSectionSize,
                                 final int maxLevelOfNestedMultipart) {

            this.inputStream = inputStream;

            final NioMultipartParserListener listener = new NioMultipartParserListener() {
                @Override
                public void onPartFinished(StreamStorage partBodyStreamStorage, Map<String, List<String>> headersFromPart) {
                    parserTokens.add(new Part(headersFromPart, partBodyStreamStorage));
                }

                @Override
                public void onAllPartsFinished() {
                    parserTokens.add(END_OF_DATA);
                }

                @Override
                public void onNestedPartStarted(Map<String, List<String>> headersFromParentPart) {
                    parserTokens.add(new NestedStart(headersFromParentPart));
                }

                @Override
                public void onNestedPartFinished() {
                    parserTokens.add(new NestedEnd());
                }

                @Override
                public void onError(String message, Throwable cause) {
                    throw new IllegalStateException("Error parsing the multipart stream: " + message, cause);
                }
            };

            this.parser = new NioMultipartParser(multipartContext, listener, partBodyStreamStorageFactory, bufferSize, maxHeadersSectionSize, maxLevelOfNestedMultipart);
        }

        @Override
        protected ParserToken computeNext() {
            byte[] buffer = new byte[1024];
            int read;
            try {

                ParserToken next;
                next = parserTokens.poll();
                if (next != null && next.getType() == null){
                    return endOfData();
                }
                if (next != null){
                    return next;
                }

                while (null == (next = parserTokens.poll()) && -1 != (read = inputStream.read(buffer))) {
                    parser.write(buffer, 0, read);
                }

                if (next != null && next.getType() == null){
                    return endOfData();
                }
                if (next != null){
                    return next;
                }

                throw new IllegalStateException("Error parsing the multipart stream. Stream ended unexpectedly");

            }catch (Exception e){
                throw new IllegalStateException("Error parsing the multipart stream", e);
            }
        }

        @Override
        public void close() throws IOException {
                parser.close();
        }
    }

    /**
     * <p>Interface representing a parser token.
     * <p>The parser tokens can be:
     * <ul>
     *     <li>Part: it contains the parsed part including headers and body</li>
     *     <li>Nested Start: it is a marker token that indicates the start of a nested part. It give access to the headers of the parent part.</li>
     *     <li>Nested End: it is a marker token that indicates the end of a nested part.</li>
     * </ul>
     * <p>The {@code BlockingIOAdapter} is returning a {@code CloseableIterator} of {@code ParserToken}s.
     */
    public interface ParserToken {

        /**
         * Type of a token: part, nested (start) and nested (end)
         */
        enum Type{
            PART,
            NESTED_START,
            NESTED_END
        }

        /**
         * <p>Returns the type of the part
         *
         * @return The type of the part
         */
        Type getType();
    }

    /**
     * <p> Marker {@code PartItem} signalling the end of a nested multipart. The client can keep track of nested multipart
     * if used in combination with {@code NestedStart}. Otherwise the item can just be skipped during the processing.
     */
    public static class NestedEnd implements ParserToken {

        private NestedEnd(){}

        /**
         * {@inheritDoc}
         */
        @Override
        public Type getType() {
            return Type.NESTED_END;
        }
    }

    /**
     * <p> Marker {@code PartItem} signalling the start of a nested multipart. In addition to signal the start of a multipart
     * it provides access to the headers of the parent part.
     */
    public static class NestedStart implements ParserToken {

        final Map<String, List<String>> headers;

        private NestedStart(final Map<String, List<String>> headers) {
            this.headers = headers;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Type getType() {
            return Type.NESTED_START;
        }

        /**
         * <p> Returns the parent part headers.
         *
         * @return The part headers.
         */
        public Map<String, List<String>> getHeaders() {
            return headers;
        }
    }

    /**
     * <p> A {@code PartItem} representing an attachment part.
     * It gives access to the part headers and the body {@code InputStream}
     */
    public static class Part implements ParserToken {

        final Map<String, List<String>> headers;
        final StreamStorage partBodyStreamStorage;

        private Part(final Map<String, List<String>> headers, final StreamStorage partBodyStreamStorage) {
            this.headers = headers;
            this.partBodyStreamStorage = partBodyStreamStorage;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Type getType() {
            return Type.PART;
        }

        /**
         * <p> Returns the part headers.
         *
         * @return The part headers.
         */
        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        /**
         * <p> Returns the {@code InputStream} from where the part body can be read.
         *
         * @return the {@code InputStream} from where the part body can be read.
         */
        public InputStream getPartBody(){
            return partBodyStreamStorage.getInputStream();
        }
    }
}

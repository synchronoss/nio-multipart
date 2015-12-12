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

import org.synchronoss.cloud.nio.multipart.io.ByteStore;
import org.synchronoss.cloud.nio.multipart.util.collect.AbstractIterator;
import org.synchronoss.cloud.nio.multipart.util.collect.CloseableIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * @return The parts.
     */
    public static CloseableIterator<PartItem> parse(final InputStream inputStream, final MultipartContext multipartContext){
        return parse(inputStream, multipartContext, null, DEFAULT_BUFFER_SIZE, DEFAULT_HEADERS_SECTION_SIZE, DEFAULT_MAX_LEVEL_OF_NESTED_MULTIPART);
    }

    /**
     * <p>
     *     Parses the multipart stream and it return the parts in form of {@code CloseableIterator}.
     * </p>
     *
     * @param inputStream The multipart stream
     * @param multipartContext The multipart context
     * @param partBodyByteStoreFactory The {@code PartBodyByteStoreFactory} to use
     */
    public static CloseableIterator<PartItem> parse(final InputStream inputStream, final MultipartContext multipartContext, final PartBodyByteStoreFactory partBodyByteStoreFactory) {
        return parse(inputStream, multipartContext, partBodyByteStoreFactory, DEFAULT_BUFFER_SIZE, DEFAULT_HEADERS_SECTION_SIZE, DEFAULT_MAX_LEVEL_OF_NESTED_MULTIPART);
    }

    /**
     * <p>
     *     Parses the multipart stream and it return the parts in form of {@code CloseableIterator}.
     * </p>
     *
     * @param inputStream The multipart stream
     * @param multipartContext The multipart context
     * @param bufferSize The buffer size in bytes
     */
    public static CloseableIterator<PartItem> parse(final InputStream inputStream, final MultipartContext multipartContext, final int bufferSize) {
        return parse(inputStream, multipartContext, null, bufferSize, DEFAULT_HEADERS_SECTION_SIZE, DEFAULT_MAX_LEVEL_OF_NESTED_MULTIPART);
    }

    /**
     * <p>
     *     Parses the multipart stream and it return the parts in form of {@link Iterable}.
     * </p>
     *
     * @param inputStream The multipart stream
     * @param multipartContext The multipart context
     * @param partBodyByteStoreFactory The {@code PartBodyByteStoreFactory} to use
     * @param bufferSize The buffer size in bytes
     * @param maxHeadersSectionSize The max size of the headers section in bytes
     * @param maxLevelOfNestedMultipart the max number of nested multipart
     */
    @SuppressWarnings("unchecked")
    public static CloseableIterator<PartItem> parse(final InputStream inputStream,
                                           final MultipartContext multipartContext,
                                           final PartBodyByteStoreFactory partBodyByteStoreFactory,
                                           final int bufferSize,
                                           final int maxHeadersSectionSize,
                                           final int maxLevelOfNestedMultipart) {


        return new PartItemsIterator(inputStream, multipartContext, partBodyByteStoreFactory, bufferSize, maxHeadersSectionSize, maxLevelOfNestedMultipart);
    }

    static class PartItemsIterator extends AbstractIterator<PartItem> implements CloseableIterator<PartItem> {

        private static final PartItem END_OF_DATA = new PartItem() {
            @Override
            public Type getType() {
                return null;
            }
        };
        private Queue<PartItem> partItems = new ConcurrentLinkedQueue<PartItem>();
        private final NioMultipartParser parser;
        private final InputStream inputStream;

        public PartItemsIterator(final InputStream inputStream,
                                 final MultipartContext multipartContext,
                                 final PartBodyByteStoreFactory partBodyByteStoreFactory,
                                 final int bufferSize,
                                 final int maxHeadersSectionSize,
                                 final int maxLevelOfNestedMultipart) {

            this.inputStream = inputStream;

            final NioMultipartParserListener listener = new NioMultipartParserListener() {
                @Override
                public void onPartReady(ByteStore partBodyByteStore, Map<String, List<String>> headersFromPart) {
                    partItems.add(new Attachment(headersFromPart, partBodyByteStore));
                }

                @Override
                public void onFormFieldPartReady(String fieldName, String fieldValue, Map<String, List<String>> headersFromPart) {
                    partItems.add(new FormParameter(headersFromPart, fieldName, fieldValue));
                }

                @Override
                public void onAllPartsFinished() {
                    partItems.add(END_OF_DATA);
                }

                @Override
                public void onNestedPartStarted(Map<String, List<String>> headersFromParentPart) {
                    partItems.add(new NestedStart(headersFromParentPart));
                }

                @Override
                public void onNestedPartFinished() {
                    partItems.add(new NestedEnd());
                }

                @Override
                public void onError(String message, Throwable cause) {
                    throw new IllegalStateException("Error parsing the multipart stream: " + message, cause);
                }
            };

            this.parser = new NioMultipartParser(multipartContext, listener, partBodyByteStoreFactory, bufferSize, maxHeadersSectionSize, maxLevelOfNestedMultipart);
        }

        @Override
        protected PartItem computeNext() {
            byte[] buffer = new byte[1024];
            int read;
            try {

                PartItem next;
                next = partItems.poll();
                if (next != null && next.getType() == null){
                    return endOfData();
                }
                if (next != null){
                    return next;
                }

                while (null == (next = partItems.poll()) && -1 != (read = inputStream.read(buffer))) {
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
     * <p>Interface representing a part.
     * The {@code BlockingIOAdapter} is returning a {@code CloseableIterator} over the {@code PartItem}.
     */
    public interface PartItem {

        /**
         * Type of a part: form, attachment, nested (start) and nested (end)
         */
        enum Type{
            FORM,
            ATTACHMENT,
            NESTED_START,
            NESTED_END;
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
    public static class NestedEnd implements PartItem {

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
    public static class NestedStart implements PartItem {

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
     * <p> A {@code PartItem} representing a multipart form parameter.
     * It gives access to the part headers, form field name and form field value.
     */
    public static class FormParameter implements PartItem {

        final Map<String, List<String>> headers;
        final String fieldName;
        final String fieldValue;

        private FormParameter(final Map<String, List<String>> headers, final String fieldName, final String fieldValue) {
            this.headers = headers;
            this.fieldName = fieldName;
            this.fieldValue = fieldValue;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Type getType() {
            return Type.FORM;
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
         * <p>Returns the form field name
         *
         * @return the form field name
         */
        public String getFieldName() {
            return fieldName;
        }

        /**
         * <p> Returns the form field value
         *
         * @return the form field value
         */
        public String getFieldValue() {
            return fieldValue;
        }
    }

    /**
     * <p> A {@code PartItem} representing an attachment part.
     * It gives access to the part headers and the body {@code InputStream}
     */
    public static class Attachment implements PartItem {

        final Map<String, List<String>> headers;
        final ByteStore partBodyByteStore;

        private Attachment(final Map<String, List<String>> headers, final ByteStore partBodyByteStore) {
            this.headers = headers;
            this.partBodyByteStore = partBodyByteStore;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Type getType() {
            return Type.ATTACHMENT;
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
            return partBodyByteStore.getInputStream();
        }
    }
}

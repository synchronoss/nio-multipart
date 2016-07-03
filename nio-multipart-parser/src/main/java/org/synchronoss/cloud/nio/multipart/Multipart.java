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

import org.synchronoss.cloud.nio.multipart.BlockingIOAdapter.PartItem;
import org.synchronoss.cloud.nio.multipart.util.collect.CloseableIterator;
import org.synchronoss.cloud.nio.stream.storage.DeferredFileStreamStorageFactory;

import java.io.InputStream;

/**
 * <p> Easy to use fluent api to build an {@code NioMultipartParser} (for Nio parsing) or to obtain a {@code CloseableIterator} (for Blocking IO parsing)
 *
 * @author Silvano Riz.
 */
public class Multipart {

    private Multipart(){}

    /**
     * <p> Builder that created an {@code NioMultipartParser} or a {@code CloseableIterator} based on the configuration selected via the fluent API.
     */
    public static class Builder {

        private int bufferSize = NioMultipartParser.DEFAULT_BUFFER_SIZE;
        private int headersSizeLimit = NioMultipartParser.DEFAULT_HEADERS_SECTION_SIZE;
        private int nestedMultipartsAllowed = NioMultipartParser.DEFAULT_MAX_LEVEL_OF_NESTED_MULTIPART;
        private String tempFolder = DeferredFileStreamStorageFactory.DEFAULT_TEMP_FOLDER;
        private int bodySizeThreshold = DeferredFileStreamStorageFactory.DEFAULT_MAX_THRESHOLD;
        private PartBodyStreamStorageFactory partBodyStreamStorageFactory;
        private MultipartContext context;

        private Builder(final MultipartContext context) {
            if (context == null){
                throw new IllegalArgumentException("Context cannot be null");
            }
            this.context = context;
        }

        /**
         * <p> Configures a specific buffer size to use during the parsing.
         *
         * @param bufferSize The buffer size in bytes.
         * @return the {@code Builder} itself.
         */
        public Builder withBufferSize(final int bufferSize){
            if (bufferSize < 0){
                throw new IllegalArgumentException("Buffer size cannot be lower than zero");
            }
            this.bufferSize = bufferSize;
            return this;
        }

        /**
         * <p> Configures a specific headers section limit.
         *
         * @param headersSizeLimit headers section limit in bytes.
         * @return the {@code Builder} itself.
         */
        public Builder withHeadersSizeLimit(final int headersSizeLimit){
            if (bufferSize < 0){
                throw new IllegalArgumentException("Headers size limit cannot be lower than zero");
            }
            this.headersSizeLimit = headersSizeLimit;
            return this;
        }

        /**
         * <p> Configures the folder where temporary files are stored during the processing.
         *     This configuration is only valid if the default {@code PartBodyStreamStorageFactory} is used.
         *     If a different {@code PartBodyStreamStorageFactory} is selected using {@link #usePartBodyStreamStorageFactory(PartBodyStreamStorageFactory)}
         *     the configuration has no effect.
         *
         * @param tempFolder The location where to store the temporary files.
         * @return the {@code Builder} itself.
         */
        public Builder saveTemporaryFilesTo(final String tempFolder){
            this.tempFolder = tempFolder;
            return this;
        }

        /**
         * <p> Configures the threshold defining how many bytes of a part's body will be kept in memory before flushing them to disk.
         *     This configuration is only valid if the default {@code PartBodyStreamStorageFactory} is used.
         *     If a different {@code PartBodyStreamStorageFactory} is selected using {@link #usePartBodyStreamStorageFactory(PartBodyStreamStorageFactory)}
         *     the configuration has no effect.
         *
         * @param bodySizeThreshold how many bytes of a part's body will be kept in memory before flushing them to disk.
         * @return the {@code Builder} itself.
         */
        public Builder withMaxMemoryUsagePerBodyPart(final int bodySizeThreshold){
            this.bodySizeThreshold = bodySizeThreshold;
            return this;
        }

        /**
         * <p> Configures a specific {@code PartBodyStreamStorageFactory} to use.
         *
         * @param partBodyStreamStorageFactory The {@code PartBodyStreamStorageFactory} to use
         * @return the {@code Builder} itself.
         */
        public Builder usePartBodyStreamStorageFactory(final PartBodyStreamStorageFactory partBodyStreamStorageFactory){
            this.partBodyStreamStorageFactory = partBodyStreamStorageFactory;
            return this;
        }

        /**
         * <p> Specifies how many nested parts are allowed.
         *
         * @param nestedMultipartsAllowed Number of nested parts allowed
         * @return the {@code Builder} itself.
         */
        public Builder limitNestingPartsTo(final int nestedMultipartsAllowed){
            if (nestedMultipartsAllowed < 1 ){
                throw new IllegalArgumentException("Nested multiparts limit must be grater than 0");
            }
            this.nestedMultipartsAllowed = nestedMultipartsAllowed;
            return this;
        }

        private PartBodyStreamStorageFactory partStreamsFactory(){
            if (partBodyStreamStorageFactory == null){
                return new DefaultPartBodyStreamStorageFactory(tempFolder, bodySizeThreshold);
            }else{
                return partBodyStreamStorageFactory;
            }
        }

        /**
         * <p> Builds a {@code NioMultipartParser}. Use this to process the multipart stream in a non blocking fashion.
         *
         * @param listener The {@code NioMultipartParserListener} listener
         * @return The {@code NioMultipartParser}
         */
        public NioMultipartParser forNIO(final NioMultipartParserListener listener){
            return new NioMultipartParser(context, listener, partStreamsFactory(), bufferSize, headersSizeLimit, nestedMultipartsAllowed);
        }

        /**
         * <p> Creates the {@code CloseableIterator}. Use this to process in a blocking IO manner.
         *
         * @param inputStream The {@code InputStream} with the multipart content.
         * @return The {@code CloseableIterator}
         */
        public CloseableIterator<PartItem> forBlockingIO(final InputStream inputStream){
            return BlockingIOAdapter.parse(inputStream, context, partStreamsFactory(), bufferSize, headersSizeLimit, nestedMultipartsAllowed);
        }
    }

    /**
     * <p> Starting point for parsing a multipart stream in NIO mode or Blocking IO Mode
     *
     * @param context The multipart context
     * @return A builder object that can be used to set additional parameters and build a {@code NioMultipartParser} (for NIO) or a {@code CloseableIterator} (for Blocking IO).
     */
    public static Builder multipart(final MultipartContext context){
        return new Builder(context);
    }



}

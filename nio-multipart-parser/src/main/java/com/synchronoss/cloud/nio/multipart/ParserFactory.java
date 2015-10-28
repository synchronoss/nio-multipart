/*
 * Copyright 2015 Synchronoss Technologies
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

package com.synchronoss.cloud.nio.multipart;

/**
 * <p>
 *     Easy to use fluent api to build a multipart parser
 * </p>
 *
 * @author Silvano Riz.
 */
public class ParserFactory {

    /**
     * Configuration for a {@link NioMultipartParser}
     */
    public static class Config{

        private MultipartContext context;
        private NioMultipartParserListener listener;
        private int bufferSize = 16000;//16Kb
        private int headersSizeLimit = 16000;//16Kb
        private int nestedMultipartsAllowed = 1;
        private PartStreamsFactory partStreamsFactory;
        private String tempFolder = System.getProperty("java.io.tmpdir");
        private int bodySizeThreshold = 10240;// 10Kb

        public Config(final MultipartContext context, final NioMultipartParserListener listener) {
            if (context == null){
                throw new IllegalArgumentException("Context cannot be null");
            }
            if (listener == null){
                throw new IllegalArgumentException("Listener cannot be null");
            }
            this.context = context;
            this.listener = listener;
        }

        public Config withBufferSize(final int bufferSize){
            if (bufferSize < 0){
                throw new IllegalArgumentException("Buffer size cannot be lower than zero");
            }
            this.bufferSize = bufferSize;
            return this;
        }

        public Config withHeadersSizeLimit(final int headersSizeLimit){
            if (bufferSize < 0){
                throw new IllegalArgumentException("Headers size limit cannot be lower than zero");
            }
            this.headersSizeLimit = headersSizeLimit;
            return this;
        }

        public Config saveTemporaryFilesTo(final String tempFolder){
            this.tempFolder = tempFolder;
            return this;
        }

        public Config withMaxMemoryUsagePerBodyPart(final int bodySizeThreshold){
            this.bodySizeThreshold = bodySizeThreshold;
            return this;
        }

        public Config withCustomPartStreamsFactory(final PartStreamsFactory partStreamsFactory){
            this.partStreamsFactory = partStreamsFactory;
            return this;
        }

        private PartStreamsFactory partStreamsFactory(){
            if (partStreamsFactory == null){
                return new DefaultPartStreamsFactory(tempFolder, bodySizeThreshold);
            }else{
                return partStreamsFactory;
            }
        }

        public NioMultipartParser forNio(){
            return new NioMultipartParser(context, listener, partStreamsFactory(), bufferSize, headersSizeLimit, nestedMultipartsAllowed);
        }

    }

    /**
     * <p>
     *     Starting point to create a {@link NioMultipartParser}
     * </p>
     *
     * @param context The {@link MultipartContext}
     * @param listener The {@link NioMultipartParserListener}
     * @return A configuration object that can be used to set additional parameters and build the {@link NioMultipartParser} calling {@link Config#forNio()}
     */
    public static Config newParser(final MultipartContext context, NioMultipartParserListener listener){
        return new Config(context, listener);
    }

}

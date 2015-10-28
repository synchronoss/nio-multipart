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

package com.synchronoss.cloud.nio.multipart;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *     {@link PartStreams} factory. This interface provides an extension point for the {@link NioMultipartParser}.
 *     Custom implementations of {@link PartStreamsFactory} can be provided to implement specific application logic like
 *     streaming the part directly to database or to a rest service.<br>
 *     The default implementation {@link DefaultPartStreamsFactory} uses a mix of memory and temporary files to store the
 *     part body.
 * </p>
 * @author Silvano Riz.
 */
public interface PartStreamsFactory {

    /**
     * <p>
     *     The part body streams to sore and read the body of a particular part.
     *     For a default implementation see {@link com.synchronoss.cloud.nio.multipart.DefaultPartStreamsFactory.DefaultPartStreams}
     * </p>
     */
    interface PartStreams {

        /**
         * <p>
         *     Returns the {@link OutputStream} where the part body will be stored.
         * </p>
         * @return the {@link OutputStream} where the part body will be stored.
         */
        OutputStream getPartOutputStream();

        /**
         * <p>
         *     Returns the {@link InputStream} from where the stored part body can be read.
         * </p>
         * @return the {@link InputStream} from where the stored part body can be read.
         */
        InputStream getPartInputStream();

    }

    /**
     * <p>
     *     Creates the {@link PartStreams} for a specific part.
     * </p>
     * @param headers The headers of the part
     * @param partIndex The index of the part
     * @return the {@link PartStreams} for a specific part.
     */
    PartStreams newPartStreams(final Map<String, List<String>> headers, final int partIndex);

}

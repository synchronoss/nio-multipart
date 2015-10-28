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
 *     Factory for the part body streams.
 * </p>
 * @author Silvano Riz.
 */
public interface BodyStreamFactory {

    /**
     * <p>
     *     Holds an {@link OutputStream} and it's name
     *     The name will be used to retrieve the correspondent {@link InputStream} by calling {@link BodyStreamFactory#getInputStream(NamedOutputStreamHolder)}
     * </p>
     */
    class NamedOutputStreamHolder {

        final String name;
        final OutputStream outputStream;
        public NamedOutputStreamHolder(final String name, final OutputStream outputStream) {
            this.name = name;
            this.outputStream = outputStream;
        }

        /**
         * <p>
         *     Returns the name of the {@link OutputStream}
         * </p>
         * @return The name of the stream.
         */
        public String getName() {
            return name;
        }

        /**
         * <p>
         *     Returns the {@link OutputStream}
         * </p>
         * @return the {@link OutputStream}
         */
        public OutputStream getOutputStream() {
            return outputStream;
        }
    }

    /**
     * <p>
     *     Gets the {@link NamedOutputStreamHolder} where to store the part body.
     * </p>
     * @param headers The headers of the processed part
     * @param partIndex The index of the processed part.
     * @return The {@link NamedOutputStreamHolder} holding the {@link OutputStream} where to store the part body.
     */
    NamedOutputStreamHolder getOutputStream(final Map<String, List<String>> headers, final int partIndex);

    /**
     * <p>
     *     Returns the {@link InputStream} to read the part body.
     * </p>
     * @param namedOutputStreamHolder The {@link NamedOutputStreamHolder} created by the {@link #getOutputStream(Map, int)}
     * @return The {@link InputStream} from where to read the part body.
     */
    InputStream getInputStream(final NamedOutputStreamHolder namedOutputStreamHolder);

}

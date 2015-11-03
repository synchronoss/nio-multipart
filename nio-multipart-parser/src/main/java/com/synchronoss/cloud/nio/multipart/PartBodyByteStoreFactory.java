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

import com.synchronoss.cloud.nio.multipart.io.ByteStore;

import java.util.List;
import java.util.Map;

/**
 * <p>
 *     {@link ByteStore} factory. This interface provides an extension point for the {@link NioMultipartParser}.
 *     Custom implementations of {@link PartBodyByteStoreFactory} can be provided to implement specific application logic like
 *     streaming the part directly to database or to a rest service.<br>
 *     The default implementation {@link DefaultPartBodyByteStoreFactory} uses a mix of memory and temporary files to store the
 *     part body.
 * </p>
 * @author Silvano Riz.
 */
public interface PartBodyByteStoreFactory {

    /**
     * <p>
     *     Creates the {@link ByteStore} for a specific part.
     * </p>
     * @param partHeaders The headers of the part
     * @param partIndex The index of the part
     * @return the {@link ByteStore} for a specific part.
     */
    ByteStore newByteStoreForPartBody(final Map<String, List<String>> partHeaders, final int partIndex);

}

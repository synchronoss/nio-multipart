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
 * <p> Factory for a {@code ByteStore}. This is a powerful extension point and custom implementations can be provided to
 *     implement specific application logic like streaming the part directly to database or to a rest service.
 *
 * <p> The default implementation is {@link DefaultPartBodyByteStoreFactory}
 *
 * @author Silvano Riz.
 */
public interface PartBodyByteStoreFactory {

    /**
     * <p> Creates the {@code ByteStore} for a specific part.
     *
     * @param partHeaders The headers of the part
     * @param partIndex The index of the part
     * @return the {@code ByteStore} for a specific part.
     */
    ByteStore newByteStoreForPartBody(final Map<String, List<String>> partHeaders, final int partIndex);

}

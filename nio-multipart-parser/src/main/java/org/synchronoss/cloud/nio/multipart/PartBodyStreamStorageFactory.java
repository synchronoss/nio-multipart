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

import org.synchronoss.cloud.nio.stream.storage.StreamStorage;

import java.util.List;
import java.util.Map;

/**
 * <p> Factory for a {@code StreamStorage}. This is a powerful extension point and custom implementations can be provided to
 *     implement specific application logic like streaming the part directly to database or to a rest service.
 *
 * <p> The default implementation is {@link DefaultPartBodyStreamStorageFactory}
 *
 * @author Silvano Riz.
 */
public interface PartBodyStreamStorageFactory {

    /**
     * <p> Creates the {@code StreamStorage} for a specific part.
     *
     * @param partHeaders The headers of the part
     * @return the {@code StreamStorage} for a specific part.
     */
    StreamStorage newStreamStorageForPartBody(final Map<String, List<String>> partHeaders, final int partIndex);

}

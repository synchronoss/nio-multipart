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
 * <p> {@code NioMultipartParserListener} providing empty implementation of all the callbacks.
 *
 * @author Silvano Riz
 */
public class AbstractNioMultipartListener implements NioMultipartParserListener{

    @Override
    public void onPartFinished(StreamStorage partBodyStreamStorage, Map<String, List<String>> headersFromPart) {
        // Empty implementation
    }

    @Override
    public void onFormFieldPartFinished(String fieldName, String fieldValue, Map<String, List<String>> headersFromPart) {
        // Empty implementation
    }

    @Override
    public void onAllPartsFinished() {
        // Empty implementation
    }

    @Override
    public void onNestedPartStarted(Map<String, List<String>> headersFromParentPart) {
        // Empty implementation
    }

    @Override
    public void onNestedPartFinished() {
        // Empty implementation
    }

    @Override
    public void onError(String message, Throwable cause) {
        // Empty implementation
    }
}

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
 * <p> Listener that will be notified with the progress of the multipart parsing.
 *
 * @author Silvano Riz.
 */
public interface NioMultipartParserListener {

    /**
     * <p> Called when a part has been parsed.
     *
     * @param partBodyStreamStorage The {@code StreamStorage} from where the part body can be read.
     * @param headersFromPart The part headers.
     */
    void onPartFinished(final StreamStorage partBodyStreamStorage, final Map<String, List<String>> headersFromPart);

    /**
     * <p> Called when a part that is a form field has been parsed
     *
     * @param fieldName The field name
     * @param fieldValue The field value
     * @param headersFromPart The part headers.
     */
    void onFormFieldPartFinished(final String fieldName, final String fieldValue, final Map<String, List<String>> headersFromPart);

    /**
     * <p> Called when all the parts have been read.
     */
    void onAllPartsFinished();

    /**
     * <p> Called when the parser is about to start a nested multipart.
     *
     * @param headersFromParentPart The headers from the parent part.
     */
    void onNestedPartStarted(final Map<String, List<String>> headersFromParentPart);

    /**
     * <p> Called when a nested part has completed.
     */
    void onNestedPartFinished();

    /**
     * <p> Called if an error occurs during the multipart parsing.
     *
     * @param message The error message
     * @param cause The error cause or null if there is no cause.
     */
    void onError(final String message, final Throwable cause);

}

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

import com.synchronoss.cloud.nio.multipart.PartStreamsFactory.PartStreams;

import java.util.List;
import java.util.Map;

/**
 * <p>
 *     Listener that will be notified with the progress of the multipart parsing.
 * </p>
 * @author Silvano Riz.
 */
public interface NioMultipartParserListener {

    /**
     * <p>
     *     Called when a part has been parsed.
     * </p>
     * @param partStreams The {@link PartStreams} from where the part body can be read using the stream returned by {@link PartStreams#getPartInputStream()}
     * @param headersFromPart The part headers.
     */
    void onPartReady(final PartStreams partStreams, final Map<String, List<String>> headersFromPart);

    /**
     * <p>
     *     Called when a part that is a form field has been parsed
     * </p>
     * @param fieldName The field name
     * @param fieldValue The field value
     * @param headersFromPart The part headers.
     */
    void onFormFieldPartReady(final String fieldName, final String fieldValue, final Map<String, List<String>> headersFromPart);

    /**
     * <p>
     *     Called when all the parts have been read.
     * </p>
     */
    void onAllPartsFinished();

    /**
     * <p>
     *     Called when the parser is about to start a nested multipart.
     * </p>
     * @param headersFromParentPart The headers from the parent part.
     */
    void onNestedPartStarted(final Map<String, List<String>> headersFromParentPart);

    /**
     * <p>
     *     Called when a nested part has completed.
     * </p>
     */
    void onNestedPartFinished();

    /**
     * <p>
     *     Called if an error occurs during the multipart parsing.
     * </p>
     * @param message The error message
     * @param cause The error cause or null if there is no cause.
     */
    void onError(final String message, final Throwable cause);

}

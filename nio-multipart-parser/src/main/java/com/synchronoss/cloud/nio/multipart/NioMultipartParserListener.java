package com.synchronoss.cloud.nio.multipart;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *     Listener that will be notified with the progress of the parsing.
 * </p>
 * Created by sriz0001 on 15/10/2015.
 */
public interface NioMultipartParserListener {

    /**
     * <p>
     *     Called when a part has been parsed.
     * </p>
     * @param partBodyInputStream The {@link InputStream} from where the part body can be read.
     * @param headersFromPart The part headers.
     */
    void onPartComplete(final InputStream partBodyInputStream, final Map<String, List<String>> headersFromPart);

    /**
     * <p>
     *     Called when a part that is a form field has been parsed
     * </p>
     * @param fieldName The field name
     * @param fieldValue The field value
     * @param headersFromPart The part headers.
     */
    void onFormFieldPartComplete(final String fieldName, final String fieldValue, final Map<String, List<String>> headersFromPart);

    /**
     * <p>
     *     Called when all the parts have been read.
     * </p>
     */
    void onAllPartsRead();

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
    void onNestedPartRead();

    /**
     * <p>
     *     Called if an error occurs during the multipart parsing.
     * </p>
     * @param message The error message
     * @param cause The error cause
     */
    void onError(final String message, final Throwable cause);

}

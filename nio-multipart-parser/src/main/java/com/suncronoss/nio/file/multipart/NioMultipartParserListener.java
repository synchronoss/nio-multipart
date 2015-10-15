package com.suncronoss.nio.file.multipart;

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

    void onPartComplete(final InputStream partBodyInputStream, final Map<String, List<String>> headersFromPart);
    void onAllPartsRead();
    void onError(final String message, final Throwable t);

}

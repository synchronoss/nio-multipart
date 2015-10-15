package com.suncronoss.nio.file.multipart;

/**
 * <p>
 *    Non Blocking IO multipart parser
 * </p>
 *
 * Created by sriz0001 on 15/10/2015.
 */
public interface NioMultipartParser {

    /**
     * <p>
     *     Handles multipart request body bytes
     * </p>
     * @param receivedBytes The received bytes.
     * @param indexStart Start read index.
     * @param indexEnd End read index.
     */
    void handleBytesReceived(final byte[] receivedBytes, final int indexStart, final int indexEnd);

}

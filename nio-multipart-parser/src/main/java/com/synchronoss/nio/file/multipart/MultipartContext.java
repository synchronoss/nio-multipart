package com.synchronoss.nio.file.multipart;

/**
 * <p>
 *     Multipart context containing:
 *     <ul>
 *         <li>Content Type</li>
 *         <li>Content Length</li>
 *         <li>Character Encoding</li>
 *     </ul>
 * <p/>
 *
 * Created by sriz0001 on 12/10/2015.
 */
public class MultipartContext {

    private final String contentType;
    private final int contentLength;
    private final String charEncoding;

    /**
     * <p>
     *     Constructor
     * </p>
     * @param contentType The content type of the request
     * @param contentLength The content length of the request
     * @param charEncoding The request char encoding.
     */
    public MultipartContext(final String contentType, final int contentLength, final String charEncoding) {

        if (!MultipartUtils.isMultipart(contentType)){
            throw new IllegalStateException("Invalid content type '" + contentType + "'. Expected a multipart request");
        }

        this.contentType = contentType;
        this.contentLength = contentLength;
        this.charEncoding = charEncoding;
    }

    public String getContentType() {
        return contentType;
    }

    public int getContentLength() {
        return contentLength;
    }

    public String getCharEncoding() {
        return charEncoding;
    }

    @Override
    public String toString() {
        return "MultipartContext{" +
                "contentType='" + contentType + '\'' +
                ", contentLength=" + contentLength +
                ", charEncoding='" + charEncoding + '\'' +
                '}';
    }
}
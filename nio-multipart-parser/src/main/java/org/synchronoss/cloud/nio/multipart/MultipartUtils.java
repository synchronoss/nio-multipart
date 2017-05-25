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

import org.synchronoss.cloud.nio.multipart.util.IOUtils;
import org.synchronoss.cloud.nio.multipart.util.ParameterParser;
import org.synchronoss.cloud.nio.stream.storage.StreamStorage;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * <p> Utilities to parse a multipart stream.
 *
 * @author Silvano Riz.
 */
public class MultipartUtils {

    /**
     * The dash (-) character in bytes
     */
    public static final byte DASH = 0x2D;

    /**
     * The (\r) character in bytes
     */
    public static final byte CR = 0x0D;

    /**
     * The (\n) character in bytes
     */
    public static final byte LF = 0x0A;

    /**
     * Sequence of bytes that represents the end of a headers section
     */
    public static final byte[] HEADER_DELIMITER = {CR, LF, CR, LF};

    /**
     * Multipart content type prefix
     */
    public static final String MULTIPART = "multipart/";

    /**
     * Content disposition header name
     */
    public static final String CONTENT_DISPOSITION = "Content-disposition";

    /**
     * Content transfer encoding header name
     */
    public static final String CONTENT_TRANSFER_ENCODING = "Content-transfer-encoding";

    /**
     * Content length header name
     */
    public static final String CONTENT_LENGTH = "Content-length";

    /**
     * Content type header name
     */
    public static final String CONTENT_TYPE = "Content-type";

    /**
     * Content disposition form-data parameter
     */
    public static final String FORM_DATA = "form-data";

    /**
     * Content disposition attachment parameter
     */
    public static final String ATTACHMENT = "attachment";

    /**
     * Specific multipart/form-data content type
     */
    public static final String MULTIPART_FORM_DATA = "multipart/form-data";

    /**
     * Specific multipart/mixed content type
     */
    public static final String MULTIPART_MIXED = "multipart/mixed";

    /**
     * text/plain mime-type
     */
    public static final String TEXT_PLAIN = "text/plain";

    private MultipartUtils(){}// empty private constructor

    /**
     * <p> Checks if the Content-Type header defines a multipart request.
     *
     * @param contentTypeHeaderValue The value of the Content-Type header.
     * @return true if the request is a multipart request, false otherwise.
     */
    public static boolean isMultipart(final String contentTypeHeaderValue){
        return contentTypeHeaderValue!=null && contentTypeHeaderValue.toLowerCase(Locale.ENGLISH).startsWith(MULTIPART);
    }

    /**
     * <p> Checks if the headers contains a Content-Type header that defines a multipart request.
     *
     * @param headers The headers map
     * @return true if the request is a multipart request, false otherwise.
     */
    public static boolean hasMultipartContentType(final Map<String, List<String>> headers){
        return isMultipart(getContentType(headers));
    }

    /**
     * <p> Returns the value of the content type header if present.
     *
     * @param headers The headers map
     * @return the value of the content type header if present.
     */
    public static String getContentType(final Map<String, List<String>> headers){
        return getHeader(CONTENT_TYPE, headers);
    }

    /**
     * <p> Returns the value of the content length header if present. -1 if the header is not present or if the value cannot be converted to a long
     *
     * @param headers The headers map
     * @return the value of the content length header if present. -1 if the header is not present or if the value cannot be converted to a long
     */
    public static long getContentLength(final Map<String, List<String>> headers) {
        long contentLength = -1;
        String contentLengthHeaderValue = getHeader(CONTENT_LENGTH, headers);
        if (contentLengthHeaderValue != null && contentLengthHeaderValue.length() > 0){
            try {
                contentLength = Long.parseLong(contentLengthHeaderValue);
            } catch (Exception e) {
                contentLength = -1;
            }
        }
        return contentLength;
    }

    /**
     * <p> Extracts the charset parameter value from the content type header.
     *
     * @param headers The headers map
     * @return the charset parameter value from the content type header or null if the header is not present of the charset parameter not defined
     */
    public static String getCharEncoding(final Map<String, List<String>> headers) {
        String contentType = getHeader(CONTENT_TYPE, headers);
        if (contentType != null) {
            ParameterParser parser = new ParameterParser();
            parser.setLowerCaseNames(true);
            Map<String, String> params = parser.parse(contentType, ';');
            return params.get("charset");
        }
        return null;
    }

    /**
     * <p> Returns the list of values fo a particular header.
     *
     * @param headerName The header name
     * @param headers The list of headers
     * @return The list of header values or null.
     */
    public static List<String> getHeaders(final String headerName, final Map<String, List<String>> headers){
        return headers.get(headerName.toLowerCase());
    }

    /**
     * <p> Returns the first value of a particular header
     *
     * @param headerName The header name
     * @param headers The headers
     * @return The first value of the header or null
     */
    public static String getHeader(final String headerName, final Map<String, List<String>> headers){
        List<String> headerValues = getHeaders(headerName, headers);
        if (headerValues == null || headerValues.size() == 0){
            return null;
        }
        return headerValues.get(0);
    }

    /**
     * <p> Checks if the part is a form field. The rules are:
     * <ul>
     *     <li>There must not be a file name specified in the 'Content-disposition' header</li>
     *     <li>There must be a field name specified in the 'Content-disposition' header</li>
     *     <li>The main request 'Content-Type' must be 'multipart/form-data'</li>
     *     <li>The part 'Content-Type' must be 'text/plain' (or not defined which defaults to 'text/plain')</li>
     * </ul>
     *
     * @param headers The part headers
     * @param context The multipart context
     * @return true if the part is a form field, false otherwise.
     */
    public static boolean isFormField(final Map<String, List<String>> headers, final MultipartContext context){
        final boolean hasFileName = getFileName(headers) != null;
        if (hasFileName){
            return false;
        }
        final boolean hasFieldName = getFieldName(headers) != null;
        if (!hasFieldName){
            return false;
        }
        final String contentType = getContentType(headers);
        final boolean isMultipartFormData = context.getContentType().startsWith(MULTIPART_FORM_DATA);
        if (!isMultipartFormData){
            return false;
        }
        // By default if the Content-Type header is not specified, the content type is text/plain
        final boolean isTextPlain = contentType == null || contentType.startsWith(TEXT_PLAIN);
        if (!isTextPlain){
            return false;
        }
        return true;
    }

    /**
     * <p> Reads the form fields into a string. The method takes care of handling the char encoding.
     * <p> If an error occurs while reading the {@link StreamStorage}, an {@link IllegalStateException} will be thrown.
     *
     * @param streamStorage The stream storage.
     * @param headers The part headers.
     * @return The form parameter value as string.
     */
    public static String readFormParameterValue(final StreamStorage streamStorage, final Map<String, List<String>> headers){
        try {
            return IOUtils.inputStreamAsString(streamStorage.getInputStream(), MultipartUtils.getCharEncoding(headers));
        }catch (Exception e){
            throw new IllegalStateException("Unable to read the form parameter value", e);
        }
    }

    /**
     * <p> Returns the 'filename' parameter of the Content-disposition header.
     *
     * @param headers The list of headers
     * @return The 'filename' parameter of the Content-disposition header or null
     */
    public static String getFileName(final Map<String, List<String>> headers) {

        final String contentDisposition = getHeader(CONTENT_DISPOSITION, headers);
        String fileName = null;

        if (contentDisposition != null) {
            String contentDispositionLc = contentDisposition.toLowerCase(Locale.ENGLISH);
            if (contentDispositionLc.startsWith(FORM_DATA) || contentDispositionLc.startsWith(ATTACHMENT)) {
                ParameterParser parser = new ParameterParser();
                parser.setLowerCaseNames(true);// Parameter parser can handle null input
                Map<String, String> params = parser.parse(contentDisposition, ';');
                if (params.containsKey("filename")) {
                    fileName = params.get("filename");
                    if (fileName != null) {
                        fileName = fileName.trim();
                    } else {
                        // Even if there is no value, the parameter is present, so we return an empty file name rather than no file name.
                        fileName = "";
                    }
                }
            }
        }

        return fileName;
    }

    /**
     * <p> Returns the 'name' parameter of the Content-disposition header.
     *
     * @param headers The list of headers
     * @return The 'name' parameter of the Content-disposition header or null
     */
    public static String getFieldName(final Map<String, List<String>> headers) {

        final String contentDisposition = getHeader(CONTENT_DISPOSITION, headers);
        String fieldName = null;

        if (contentDisposition != null && contentDisposition.toLowerCase(Locale.ENGLISH).startsWith(FORM_DATA)) {
            ParameterParser parser = new ParameterParser();
            parser.setLowerCaseNames(true); // Parameter parser can handle null input
            Map<String, String> params = parser.parse(contentDisposition, ';');
            fieldName = params.get("name");
            if (fieldName != null) {
                fieldName = fieldName.trim();
            }
        }

        return fieldName;
    }

    /**
     * <p> Returns true if the headers contain the Content-transfer-encoding with value 'base64'.
     *
     * @param partHeaders The list of headers
     * @return true if the headers contain the Content-transfer-encoding with value 'base64', false otherwise
     */
    public static boolean isContentTransferEncodingBase64Encoded(final Map<String, List<String>> partHeaders) {
        String contentEncoding = MultipartUtils.getHeader(CONTENT_TRANSFER_ENCODING, partHeaders);
        return contentEncoding != null && "base64".equalsIgnoreCase(contentEncoding);
    }

    /**
     * <p> Extracts the boundary parameter value defined in the Content-Type header of a multipart request.
     * <p> For example if the Content-Type header is
     * <pre>
     *     {@code
     *     Content-Type: multipart/form-data; boundary=---------------------------735323031399963166993862150
     *     }
     * </pre>
     * This method will extract
     * <pre>
     *     {@code
     *     ---------------------------735323031399963166993862150
     *     }
     * </pre>
     * and return the correspondent bytes.
     * <p> The charset used to encode the string into bytes is ISO-8859-1 and if it is not supported it will fall back to the default charset.
     *
     * @param contentType The value of the Content-Type header.
     * @return The boundary parameter value or {@code null} if not defined.
     */
    public static byte[] getBoundary(final String contentType) {
        ParameterParser parser = new ParameterParser();
        parser.setLowerCaseNames(true);
        // Parameter parser can handle null input
        Map<String, String> params = parser.parse(contentType, new char[] {';', ','});
        String boundaryStr = params.get("boundary");

        if (boundaryStr == null) {
            return null;
        }
        byte[] boundary;
        try {
            boundary = boundaryStr.getBytes("ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            boundary = boundaryStr.getBytes(); // Intentionally falls back to default charset
        }
        return boundary;
    }

}

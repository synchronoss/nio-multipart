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

import org.synchronoss.cloud.nio.multipart.util.ParameterParser;

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
        return isMultipart(getHeader(CONTENT_TYPE, headers));
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
     * <p> Checks if the part is a form field.
     *
     * @param headers The part headers
     * @return true if the part is a form field, false otherwise.
     */
    public static boolean isFormField(final Map<String, List<String>> headers){
        final String fileName = getFileName(headers);
        final String fieldName = getFieldName(headers);

        return fieldName != null && fileName == null;
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

}

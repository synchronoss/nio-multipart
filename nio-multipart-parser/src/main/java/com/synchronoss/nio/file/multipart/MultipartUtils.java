package com.synchronoss.nio.file.multipart;

import java.util.Locale;

/**
 * <p>
 *     Utilities to parse a multipart request/response.
 * </p>
 * Created by sriz0001 on 12/10/2015.
 */
public class MultipartUtils {

    public static final String MULTIPART = "multipart/";

    private MultipartUtils(){}// empty private constructor

    /**
     * <p>
     *     Checks if the Content-Type header defines a multipart request.
     * </p>
     * @param contentTypeHeaderValue The value of the Content-Type header.
     * @return true if the request is a multipart request, false otherwise.
     */
    public static boolean isMultipart(final String contentTypeHeaderValue){
        return contentTypeHeaderValue!=null && contentTypeHeaderValue.toLowerCase(Locale.ENGLISH).startsWith(MULTIPART);
    }

}

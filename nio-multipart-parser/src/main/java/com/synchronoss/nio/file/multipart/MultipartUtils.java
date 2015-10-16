package com.synchronoss.nio.file.multipart;

import java.util.Locale;

/**
 * <p>
 *     Utilities to parse a multipart request/response.
 * </p>
 * Created by mele on 12/10/2015.
 */
public class MultipartUtils {

    public static final String MULTIPART = "multipart/";

    public static boolean isMultipart(final String contentTypeHeaderValue){
        return contentTypeHeaderValue.toLowerCase(Locale.ENGLISH).startsWith(MULTIPART);
    }

}

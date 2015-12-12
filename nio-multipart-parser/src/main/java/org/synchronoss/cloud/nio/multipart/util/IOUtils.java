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
package org.synchronoss.cloud.nio.multipart.util;

import java.io.*;
import java.nio.charset.Charset;

/**
 * <p> Bunch of IO utilities
 *
 * @author Silvano Riz
 */
public class IOUtils {

    public static final String SYSTEM_CHAR_SET = Charset.defaultCharset().name();

    /**
     * <p> Reads an {@code InputStream} into a String with the specified char encoding
     *
     * @param inputStream The {@code InputStream} to read from
     * @param charEncoding The charEncoding to use. If null the system default is used.
     * @return The {@code String} read.
     * @throws IOException If the read fails
     */
    public static String inputStreamAsString(final InputStream inputStream, String charEncoding) throws IOException {

        if (charEncoding == null){
            charEncoding = SYSTEM_CHAR_SET;
        }

        StringWriter sw = new StringWriter();
        InputStreamReader in = new InputStreamReader(inputStream, charEncoding);
        char[] buffer = new char[4096];
        int bytesRead;
        while (-1 != (bytesRead = in.read(buffer))) {
            sw.write(buffer, 0, bytesRead);
        }
        return sw.toString();
    }

    /**
     * <p> Closes an {@code InputStream} silently
     *
     * @param inputStream The {@code InputStream} to close.
     */
    public static void closeQuietly(final InputStream inputStream){
        try{
            if (inputStream != null){
                inputStream.close();
            }
        }catch (Exception e){
            // nothing
        }
    }

}

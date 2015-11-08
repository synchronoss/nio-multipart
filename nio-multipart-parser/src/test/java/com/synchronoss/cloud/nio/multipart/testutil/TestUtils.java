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

package com.synchronoss.cloud.nio.multipart.testutil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p> Test utilities
 *
 * @author Silvano Riz.
 */
public class TestUtils {

    private static final Logger log = LoggerFactory.getLogger(TestUtils.class);

    public static String printBytesHexEncoded(final byte[] byteArray){

        StringBuilder sb = new StringBuilder(byteArray.length * 2);
        sb.append("[");
        int i=0;
        while (i<byteArray.length) {
            sb.append(printByteHexEncoded(byteArray[i]));
            i++;
            if (i<byteArray.length){
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();

    }

    public static String printByteHexEncoded(byte b){
        return String.format("0x%02x", b & 0xff);
    }

}

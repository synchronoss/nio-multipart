package com.synchronoss.cloud.nio.multipart.testutil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * <p>
 *     Test utilities
 * </p>
 *
 * Created by sriz0001 on 17/10/2015.
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

    static String getTestFileFullPath(final String fileName){
        try {
            URL resourceUrl = TestUtils.class.getResource(fileName);
            Path resourcePath = Paths.get(resourceUrl.toURI());
            return resourcePath.toFile().getAbsolutePath();
        }catch (Exception e){
            throw new IllegalStateException("Cannot find the test file", e);
        }
    }

}

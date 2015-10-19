package com.synchronoss.nio.file.multipart.testutil;

import com.synchronoss.nio.file.multipart.CircularBuffer;
import com.synchronoss.nio.file.multipart.EndOfLineBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by mele on 17/10/2015.
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

    public static void writeDataToCircularBuffer(final CircularBuffer circularBuffer, final byte[] data){
        for (byte aData : data) {
            circularBuffer.write(aData);
        }
    }

    public static void writeDataToEndOfLineBuffer(final EndOfLineBuffer endOfLineBuffer, final byte[] data){
        for (byte aData : data) {
            endOfLineBuffer.write(aData);
        }
    }


    public static String getContentTypeForBoundary(final String boundary){
        return String.format("multipart/mixed; boundary=%s", boundary);
    }

}

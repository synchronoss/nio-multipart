package com.synchronoss.nio.file.multipart;

/**
 * Created by mele on 17/10/2015.
 */
public class TestUtils {

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

    public static String printCircularBuffer(final CircularBuffer circularBuffer){
            return "CircularBuffer{" +
                    "size=" + circularBuffer.size +
                    ", buffer=" + printBytesHexEncoded(circularBuffer.buffer) +
                    ", startValidDataIndex=" + circularBuffer.startValidDataIndex +
                    ", nextAvailablePosition=" + circularBuffer.nextAvailablePosition +
                    ", availableReadLength=" + circularBuffer.availableReadLength +
                    '}';
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

}

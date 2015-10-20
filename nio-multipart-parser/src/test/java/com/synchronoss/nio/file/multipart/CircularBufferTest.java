package com.synchronoss.nio.file.multipart;

import com.synchronoss.nio.file.multipart.testutil.TestUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;

/**
 * <p>
 *     Unit tests for {@link CircularBuffer}
 * </p>
 * Created by sriz0001 on 15/10/2015.
 */
public class CircularBufferTest {

    private static final Logger log = LoggerFactory.getLogger(CircularBuffer.class);

    @Test
    public void testCreate_invalidSize() throws Exception {

        Exception expected = null;
        try{
            CircularBuffer buffer = new CircularBuffer(0);
        }catch (Exception e){
            expected = e;
        }
        Assert.assertNotNull(expected);
        Assert.assertTrue(expected instanceof IllegalArgumentException);

        expected = null;
        try{
            CircularBuffer buffer = new CircularBuffer(-1);
        }catch (Exception e){
            expected = e;
        }
        Assert.assertNotNull(expected);
        Assert.assertTrue(expected instanceof IllegalArgumentException);

    }

    @Test
    public void testWrite() throws Exception {

        final CircularBuffer buffer = new CircularBuffer(10);
        log.info("Buffer initial status:\n" + circularBufferToString(buffer)  + "\n");
        Assert.assertEquals(0, buffer.startValidDataIndex);
        Assert.assertEquals(0, buffer.nextAvailablePosition);
        Assert.assertEquals(0, buffer.availableReadLength);

        byte[] chunk1 = {0x01};
        writeDataToCircularBuffer(buffer, chunk1);
        log.info("Buffer after writing 1 byte:\n" + circularBufferToString(buffer) + "\n");
        Assert.assertEquals(0, buffer.startValidDataIndex);
        Assert.assertEquals(1, buffer.nextAvailablePosition);
        Assert.assertEquals(1, buffer.availableReadLength);

        byte[] chunk2 = {0x02, 0x03, 0x04, 0x05};
        writeDataToCircularBuffer(buffer, chunk2);
        log.info("Buffer after writing 5 bytes:\n" + circularBufferToString(buffer) + "\n");
        Assert.assertEquals(0, buffer.startValidDataIndex);
        Assert.assertEquals(5, buffer.nextAvailablePosition);
        Assert.assertEquals(5, buffer.availableReadLength);

        byte[] chunk3 = {0x06, 0x07, 0x08, 0x09};
        writeDataToCircularBuffer(buffer, chunk3);
        log.info("Buffer after writing 9 bytes:\n" + circularBufferToString(buffer) + "\n");
        Assert.assertEquals(0, buffer.startValidDataIndex);
        Assert.assertEquals(9, buffer.nextAvailablePosition);
        Assert.assertEquals(9, buffer.availableReadLength);

        byte[] chunk4 = {0x10};
        writeDataToCircularBuffer(buffer, chunk4);
        log.info("Buffer after writing 10 bytes:\n" + circularBufferToString(buffer) + "\n");
        Assert.assertEquals(0, buffer.startValidDataIndex);
        Assert.assertEquals(0, buffer.nextAvailablePosition);
        Assert.assertEquals(10, buffer.availableReadLength);

        byte[] chunk5 = {0x11};
        writeDataToCircularBuffer(buffer, chunk5);
        log.info("Buffer after writing 11 bytes:\n" + circularBufferToString(buffer) + "\n");
        Assert.assertEquals(1, buffer.startValidDataIndex);
        Assert.assertEquals(1, buffer.nextAvailablePosition);
        Assert.assertEquals(10, buffer.availableReadLength);

        byte[] chunk6 = {0x12};
        writeDataToCircularBuffer(buffer, chunk6);
        log.info("Buffer after writing 12 bytes:\n" + circularBufferToString(buffer) + "\n");
        Assert.assertEquals(2, buffer.startValidDataIndex);
        Assert.assertEquals(2, buffer.nextAvailablePosition);
        Assert.assertEquals(10, buffer.availableReadLength);

    }

    @Test
    public void testReadAll() throws Exception {

        ByteArrayOutputStream readBaos = new ByteArrayOutputStream();
        byte[] readBytes;

        final CircularBuffer buffer = new CircularBuffer(10);
        log.info("Buffer initial status:\n" + circularBufferToString(buffer)  + "\n");

        // Read all when buffer is empty
        buffer.readAll(readBaos);
        readBytes = readBaos.toByteArray();
        log.info("Buffer after reading all:\n" + circularBufferToString(buffer));
        log.info("Read bytes: " + TestUtils.printBytesHexEncoded(readBytes) + "\n");
        Assert.assertArrayEquals(new byte[]{}, readBaos.toByteArray());
        Assert.assertEquals(0, buffer.startValidDataIndex);
        Assert.assertEquals(0, buffer.nextAvailablePosition);
        Assert.assertEquals(0, buffer.availableReadLength);
        buffer.reset();
        readBaos.reset();
        log.info("Buffer after reset:\n" + circularBufferToString(buffer) + "\n");

        byte[] chunk1 = {0x01};
        writeDataToCircularBuffer(buffer, chunk1);
        log.info("Buffer after writing 1 byte:\n" + circularBufferToString(buffer) + "\n");
        buffer.readAll(readBaos);
        readBytes = readBaos.toByteArray();
        log.info("Buffer after reading all:\n" + circularBufferToString(buffer));
        log.info("Read bytes: " + TestUtils.printBytesHexEncoded(readBytes) + "\n");
        Assert.assertArrayEquals(chunk1, readBaos.toByteArray());
        Assert.assertEquals(1, buffer.startValidDataIndex);
        Assert.assertEquals(1, buffer.nextAvailablePosition);
        Assert.assertEquals(0, buffer.availableReadLength);
        buffer.reset();
        readBaos.reset();
        log.info("Buffer after reset:\n" + circularBufferToString(buffer) + "\n");

        byte[] chunk2 = {0x01, 0x02, 0x03, 0x04, 0x05};
        writeDataToCircularBuffer(buffer, chunk2);
        log.info("Buffer after writing 5 byte:\n" + circularBufferToString(buffer) + "\n");
        buffer.readAll(readBaos);
        readBytes = readBaos.toByteArray();
        log.info("Buffer after reading all:\n" + circularBufferToString(buffer));
        log.info("Read bytes: " + TestUtils.printBytesHexEncoded(readBytes) + "\n");
        Assert.assertArrayEquals(chunk2, readBytes);
        Assert.assertEquals(5, buffer.startValidDataIndex);
        Assert.assertEquals(5, buffer.nextAvailablePosition);
        Assert.assertEquals(0, buffer.availableReadLength);
        readBaos.reset();
        byte[] chunk3 = {0x06, 0x07, 0x08, 0x09, 0x10, 0x11, 0x12};
        writeDataToCircularBuffer(buffer, chunk3);
        log.info("Buffer after writing 7 byte more:\n" + circularBufferToString(buffer) + "\n");

        buffer.readAll(readBaos);
        readBytes = readBaos.toByteArray();
        log.info("Buffer after reading all:\n" + circularBufferToString(buffer));
        log.info("Read bytes: " + TestUtils.printBytesHexEncoded(readBytes) + "\n");
        Assert.assertArrayEquals(chunk3, readBytes);
        Assert.assertEquals(2, buffer.startValidDataIndex);
        Assert.assertEquals(2, buffer.nextAvailablePosition);
        Assert.assertEquals(0, buffer.availableReadLength);
    }

    @Test
    public void testReadChunk() throws Exception {

        ByteArrayOutputStream readBaos = new ByteArrayOutputStream();
        byte[] readBytes;

        final CircularBuffer buffer = new CircularBuffer(10);
        log.info("Buffer initial status:\n" + circularBufferToString(buffer)  + "\n");

        buffer.readChunk(readBaos, 0);
        readBytes = readBaos.toByteArray();
        log.info("Buffer after reading a chunk of 0 bytes:\n" + circularBufferToString(buffer));
        log.info("Read bytes: " + TestUtils.printBytesHexEncoded(readBytes) + "\n");
        Assert.assertArrayEquals(new byte[]{}, readBytes);
        Assert.assertEquals(0, buffer.startValidDataIndex);
        Assert.assertEquals(0, buffer.nextAvailablePosition);
        Assert.assertEquals(0, buffer.availableReadLength);
        readBaos.reset();

        writeDataToCircularBuffer(buffer, new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07});
        log.info("Buffer after writing 7 byte:\n" + circularBufferToString(buffer) + "\n");

        buffer.readChunk(readBaos, 0);
        readBytes = readBaos.toByteArray();
        log.info("Buffer after reading a chunk of 0 bytes:\n" + circularBufferToString(buffer));
        log.info("Read bytes: " + TestUtils.printBytesHexEncoded(readBytes) + "\n");
        Assert.assertArrayEquals(new byte[]{}, readBytes);
        Assert.assertEquals(0, buffer.startValidDataIndex);
        Assert.assertEquals(7, buffer.nextAvailablePosition);
        Assert.assertEquals(7, buffer.availableReadLength);
        readBaos.reset();

        buffer.readChunk(readBaos, 3);
        readBytes = readBaos.toByteArray();
        log.info("Buffer after reading a chunk of 3 bytes:\n" + circularBufferToString(buffer));
        log.info("Read bytes: " + TestUtils.printBytesHexEncoded(readBytes) + "\n");
        Assert.assertArrayEquals(new byte[]{0x01, 0x02, 0x03}, readBytes);
        Assert.assertEquals(3, buffer.startValidDataIndex);
        Assert.assertEquals(7, buffer.nextAvailablePosition);
        Assert.assertEquals(4, buffer.availableReadLength);

        readBaos.reset();
        buffer.readChunk(readBaos, 4);
        readBytes = readBaos.toByteArray();
        log.info("Buffer after reading a chunk of 4 bytes:\n" + circularBufferToString(buffer));
        log.info("Read bytes: " + TestUtils.printBytesHexEncoded(readBytes) + "\n");
        Assert.assertArrayEquals(new byte[]{0x04, 0x05, 0x06, 0x07}, readBytes);
        Assert.assertEquals(7, buffer.startValidDataIndex);
        Assert.assertEquals(7, buffer.nextAvailablePosition);
        Assert.assertEquals(0, buffer.availableReadLength);

    }

    @Test
    public void testReadChunk_chunkTooBig() throws Exception {

        ByteArrayOutputStream readBaos = new ByteArrayOutputStream();
        final CircularBuffer buffer = new CircularBuffer(10);
        writeDataToCircularBuffer(buffer, new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07});

        Exception expected = null;
        try{
            buffer.readChunk(readBaos, 8);
        }catch (Exception e){
            expected = e;
        }
        Assert.assertNotNull(expected);
        Assert.assertTrue(expected instanceof IllegalArgumentException);

    }

    @Test
    public void testForwards() throws Exception {

        final CircularBuffer buffer = new CircularBuffer(10);
        Assert.assertEquals(1, buffer.forwards(0));
        Assert.assertEquals(5, buffer.forwards(4));
        Assert.assertEquals(0, buffer.forwards(9));

    }

    @Test
    public void testBackwards() throws Exception {

        final CircularBuffer buffer = new CircularBuffer(10);
        Assert.assertEquals(9, buffer.backwards(0));
        Assert.assertEquals(3, buffer.backwards(4));
        Assert.assertEquals(8, buffer.backwards(9));
    }

    @Test
    public void testForwards_multiplePositions() throws Exception {

        CircularBuffer buffer = new CircularBuffer(10);

        Assert.assertEquals(1, buffer.forwards(0, 1));
        Assert.assertEquals(2, buffer.forwards(0, 2));
        Assert.assertEquals(9, buffer.forwards(0, 9));

        Assert.assertEquals(0, buffer.forwards(0, 10));
        Assert.assertEquals(1, buffer.forwards(0, 11));
        Assert.assertEquals(2, buffer.forwards(0, 12));

        Assert.assertEquals(0, buffer.forwards(0, 20));
        Assert.assertEquals(1, buffer.forwards(0, 21));
        Assert.assertEquals(2, buffer.forwards(0, 22));

    }

    @Test
    public void testBackwards_multiplePositions() throws Exception {

        CircularBuffer buffer = new CircularBuffer(10);

        Assert.assertEquals(4, buffer.backwards(5, 1));
        Assert.assertEquals(3, buffer.backwards(5, 2));
        Assert.assertEquals(0, buffer.backwards(5, 5));

        Assert.assertEquals(9, buffer.backwards(5, 6));
        Assert.assertEquals(8, buffer.backwards(5, 7));

        Assert.assertEquals(5, buffer.backwards(5, 10));
        Assert.assertEquals(4, buffer.backwards(5, 11));

        Assert.assertEquals(5, buffer.backwards(5, 20));
        Assert.assertEquals(4, buffer.backwards(5, 21));

    }

    @Test
    public void testIsFull() throws Exception {
        final CircularBuffer buffer = new CircularBuffer(10);
        //log.info("Buffer initial status:\n" + TestUtils.printCircularBuffer(buffer)  + "\n");

        byte[] chunk1 = {0x01, 0x02, 0x03, 0x04, 0x05};
        writeDataToCircularBuffer(buffer, chunk1);
        //log.info("Buffer after writing 5 bytes:\n" + TestUtils.printCircularBuffer(buffer) + "\n");
        Assert.assertFalse(buffer.isFull());

        byte[] chunk2 = {0x06, 0x07, 0x08, 0x09, 0x10};
        writeDataToCircularBuffer(buffer, chunk2);
        //log.info("Buffer after writing 5 more bytes:\n" + TestUtils.printCircularBuffer(buffer) + "\n");
        Assert.assertTrue(buffer.isFull());
    }

    @Test
    public void testIsEmpty() throws Exception {

        final CircularBuffer buffer = new CircularBuffer(10);
        Assert.assertTrue(buffer.isEmpty());

        writeDataToCircularBuffer(buffer, new byte[]{0x01, 0x02, 0x03});
        Assert.assertFalse(buffer.isEmpty());

        writeDataToCircularBuffer(buffer, new byte[]{0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10});
        Assert.assertFalse(buffer.isEmpty());

        writeDataToCircularBuffer(buffer, new byte[]{0x11});
        Assert.assertFalse(buffer.isEmpty());


    }

    @Test
    public void testGetAvailableLength() throws Exception {

        final CircularBuffer buffer = new CircularBuffer(10);
        Assert.assertEquals(0, buffer.getAvailableDataLength());

        writeDataToCircularBuffer(buffer, new byte[]{0x01, 0x02, 0x03});
        Assert.assertEquals(3, buffer.getAvailableDataLength());

        writeDataToCircularBuffer(buffer, new byte[]{0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10});
        Assert.assertEquals(10, buffer.getAvailableDataLength());

        writeDataToCircularBuffer(buffer, new byte[]{0x11});
        Assert.assertEquals(10, buffer.getAvailableDataLength());


    }

    @Test
    public void testReset() throws Exception {

        final CircularBuffer buffer = new CircularBuffer(10);
        Assert.assertTrue(buffer.isEmpty());

        writeDataToCircularBuffer(buffer, new byte[]{0x01, 0x02, 0x03});
        Assert.assertFalse(buffer.isEmpty());

        buffer.reset();
        Assert.assertTrue(buffer.isEmpty());

    }

    @Test
    public void testGetBufferSize() throws Exception {

        final CircularBuffer buffer = new CircularBuffer(10);
        Assert.assertEquals(10, buffer.getBufferSize());

    }

    static void writeDataToCircularBuffer(final CircularBuffer circularBuffer, final byte[] data){
        for (byte aData : data) {
            circularBuffer.write(aData);
        }
    }

    static String circularBufferToString(final CircularBuffer circularBuffer){
        return "CircularBuffer{" +
                "size=" + circularBuffer.size +
                ", buffer=" + TestUtils.printBytesHexEncoded(circularBuffer.buffer) +
                ", startValidDataIndex=" + circularBuffer.startValidDataIndex +
                ", nextAvailablePosition=" + circularBuffer.nextAvailablePosition +
                ", availableReadLength=" + circularBuffer.availableReadLength +
                '}';
    }
}
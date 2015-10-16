package com.synchronoss.nio.file.multipart;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/**
 * Created by sriz0001 on 15/10/2015.
 */
public class CircularBufferTest {

    private static final Logger log = LoggerFactory.getLogger(CircularBuffer.class);

    @Test
    public void testWrite() throws Exception {

        CircularBuffer buffer = new CircularBuffer(10);

        for(int i=1; i<=3; i++){
            for(int j=0; j<10; j++) {
                buffer.write((byte) i);
                log.info("Buffer: " + buffer);
            }
            log.info("--");
        }

    }

    @Test
    public void testRead() throws Exception {

        CircularBuffer buffer = new CircularBuffer(10);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        for(int i=1; i<=7; i++){
            buffer.write((byte)i);
        }
        log.info("Buffer before reading: " + buffer);
        buffer.read(byteArrayOutputStream, 2);
        log.info("Read bytes: " + Arrays.toString(byteArrayOutputStream.toByteArray()));
        log.info("Buffer after reading 3 positions: " + buffer);
        Assert.assertArrayEquals(new byte[]{1,2,3}, byteArrayOutputStream.toByteArray());

        byteArrayOutputStream.reset();
        buffer.read(byteArrayOutputStream, -1);
        log.info("Read bytes: " + Arrays.toString(byteArrayOutputStream.toByteArray()));
        log.info("Buffer after reading to the end: " + buffer);
        Assert.assertArrayEquals(new byte[]{4,5,6,7}, byteArrayOutputStream.toByteArray());
    }

    @Test
    public void testForwards() throws Exception {

    }

    @Test
    public void testBackwards() throws Exception {

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

    }

    @Test
    public void testIsEmpty() throws Exception {

    }

    @Test
    public void testReset() throws Exception {

    }
}
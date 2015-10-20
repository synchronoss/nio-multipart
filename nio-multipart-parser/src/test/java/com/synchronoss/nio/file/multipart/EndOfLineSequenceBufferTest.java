package com.synchronoss.nio.file.multipart;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;

/**
 * Created by mele on 18/10/2015.
 */
public class EndOfLineSequenceBufferTest {


    @Test
    public void testReset() throws Exception {

    }

    @Test
    public void testWrite() throws Exception {

        ByteArrayOutputStream flush = new ByteArrayOutputStream();
        EndOfLineBuffer endOfLineBuffer = new EndOfLineBuffer(10, new byte[]{0x0D, 0x0A}, flush);

        byte[] chunk = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x0D, 0x0A, 0x08, 0x09, 0x10};
        boolean eol;
        int i = 0;
        do{
            eol = endOfLineBuffer.write(chunk[i]);
            i++;
        }while (!eol && i<chunk.length);
        Assert.assertEquals(9, i);
        Assert.assertArrayEquals(new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07}, flush.toByteArray());


        flush.reset();
        endOfLineBuffer.reset(new byte[]{0x0D, 0x0A}, flush);
        chunk = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x0D, 0x0D, 0x0A, 0x09, 0x10};
        eol = false;
        i = 0;
        do{
            eol = endOfLineBuffer.write(chunk[i]);
            i++;
        }while (!eol && i<chunk.length);
        Assert.assertEquals(10, i);
        Assert.assertArrayEquals(new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x0D}, flush.toByteArray());

    }

}
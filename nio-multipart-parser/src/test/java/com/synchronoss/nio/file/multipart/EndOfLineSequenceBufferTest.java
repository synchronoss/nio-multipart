package com.synchronoss.nio.file.multipart;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mele on 18/10/2015.
 */
public class EndOfLineSequenceBufferTest {

    Map<String, byte[]> crLfSeparator = new HashMap<String, byte[]>(){{
        put("CRLF", new byte[]{0x0D, 0x0A});
    }};

    Map<String, byte[]> multiSeparator = new HashMap<String, byte[]>(){{
        put("EOL1", new byte[]{0x2D, 0x2D, 0x10, 0x2D, 0x2D});
        put("EOL2", new byte[]{0x2D, 0x2D, 0x0A, 0x0D, 0x0A});
    }};

    @Test
    public void testAddEndOfLine() throws Exception {

    }

    @Test
    public void testReset() throws Exception {

    }

    @Test
    public void testSetFlushOutputStream() throws Exception {

    }

    @Test
    public void testWrite() throws Exception {

        ByteArrayOutputStream flush = new ByteArrayOutputStream();
        EndOfLineBuffer endOfLineBuffer = new EndOfLineBuffer(10, crLfSeparator, flush);
        endOfLineBuffer.addEndOfLine("CRLF", new byte[]{0x0D, 0x0A});

        byte[] chunk = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x0D, 0x0A, 0x08, 0x09, 0x10};
        boolean eol;
        int i = 0;
        do{
            eol = endOfLineBuffer.write(chunk[i]);
            i++;
        }while (!eol && i<chunk.length);
        Assert.assertEquals(9, i);
        Assert.assertArrayEquals(new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07}, flush.toByteArray());

    }

    @Test
    public void testWrite_multipleEndOfLines() throws Exception {

        ByteArrayOutputStream flush = new ByteArrayOutputStream();
        EndOfLineBuffer endOfLineBuffer = new EndOfLineBuffer(15, multiSeparator, flush);
        endOfLineBuffer.addEndOfLine("EOL1", new byte[]{0x2D, 0x2D, 0x10, 0x2D, 0x2D});
        endOfLineBuffer.addEndOfLine("EOL2", new byte[]{0x2D, 0x2D, 0x0A, 0x0D, 0x0A});

        byte[] chunk = {0x01, 0x01, 0x02, 0x02, 0x03, 0x03, 0x04, 0x04, 0x2D, 0x2D, 0x10, 0x2D, 0x2D, 0x05, 0x05, 0X06, 0X06};
        boolean eol;
        int i = 0;
        do{
            eol = endOfLineBuffer.write(chunk[i]);
            i++;
        }while (!eol && i<chunk.length);
        Assert.assertEquals(13, i);
        Assert.assertArrayEquals(new byte[]{0x01, 0x01, 0x02, 0x02, 0x03, 0x03, 0x04, 0x04}, flush.toByteArray());

    }

    @Test
    public void testFlush() throws Exception {

    }
}
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

package com.synchronoss.cloud.nio.multipart.buffer;

import com.synchronoss.cloud.nio.multipart.testutil.TestUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * <p>
 *     Unit tests for {@link EndOfLineBuffer}
 * </p>
 * @author Silvano Riz.
 */
public class EndOfLineBufferTest {

    private static final Logger log = LoggerFactory.getLogger(EndOfLineBuffer.class);

    @Test
    public void testConstruct_wrongSize() throws Exception {

        Exception expected = null;
        try{
            new EndOfLineBuffer(5, new byte[]{0x01, 0x02, 0x3, 0x4, 0x5}, null);
        }catch (Exception e){
            expected = e;
        }
        assertNotNull(expected);
        assertTrue(expected instanceof IllegalArgumentException);

        expected = null;
        try{
            new EndOfLineBuffer(5, new byte[]{0x01, 0x02, 0x3, 0x4, 0x5, 0x6}, null);
        }catch (Exception e){
            expected = e;
        }
        assertNotNull(expected);
        assertTrue(expected instanceof IllegalArgumentException);

    }

    @Test
    public void testWrite() throws Exception {

        ByteArrayOutputStream flush = new ByteArrayOutputStream();
        EndOfLineBuffer endOfLineBuffer = new EndOfLineBuffer(10, new byte[]{0x0D, 0x0A}, flush);

        log.info("Buffer initial status:\n" + endOfLineBufferToString(endOfLineBuffer)  + "\n");

        int writtenBytes = writeDataToEndOfLineBuffer(endOfLineBuffer, new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x0D, 0x0A, 0x08, 0x09, 0x10});
        assertEquals(9, writtenBytes);
        assertArrayEquals(new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07}, flush.toByteArray());

        flush.reset();
        endOfLineBuffer.reset(new byte[]{0x0D, 0x0A}, flush);

        writtenBytes = writeDataToEndOfLineBuffer(endOfLineBuffer, new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x0D, 0x0D, 0x0A, 0x09, 0x10});
        assertEquals(10, writtenBytes);
        assertArrayEquals(new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x0D}, flush.toByteArray());

    }

    @Test
    public void testWrite_eolReached() throws Exception {

        ByteArrayOutputStream flush = new ByteArrayOutputStream();
        EndOfLineBuffer endOfLineBuffer = new EndOfLineBuffer(10, new byte[]{0x0D, 0x0A}, flush);
        log.info("Buffer initial status:\n" + endOfLineBufferToString(endOfLineBuffer)  + "\n");

        writeDataToEndOfLineBuffer(endOfLineBuffer, new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x0D, 0x0A, 0x08, 0x09, 0x10});
        assertTrue(endOfLineBuffer.isEndOfLine());

        Exception expected = null;
        try{
            endOfLineBuffer.write((byte)11);
        }catch (Exception e){
            expected = e;
        }
        assertNotNull(expected);
        assertTrue(expected instanceof IllegalStateException);

    }

    @Test
    public void testWrite_flush() throws Exception {

        ByteArrayOutputStream flush = new ByteArrayOutputStream();
        EndOfLineBuffer endOfLineBuffer = new EndOfLineBuffer(10, new byte[]{0x13, 0x14}, flush);
        log.info("Buffer initial status:\n" + endOfLineBufferToString(endOfLineBuffer)  + "\n");

        writeDataToEndOfLineBuffer(endOfLineBuffer, new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09});
        assertEquals(0, flush.size());
        writeDataToEndOfLineBuffer(endOfLineBuffer, new byte[]{0x10, 0x11});
        assertEquals(10, flush.size());
        writeDataToEndOfLineBuffer(endOfLineBuffer, new byte[]{0x12, 0x13, 0x14});
        assertEquals(12, flush.size());

        assertArrayEquals(new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10, 0x11, 0x12}, flush.toByteArray());
    }

    @Test
    public void testWrite_flush_error() throws Exception {

        OutputStream flush = mock(OutputStream.class);
        byte failingWrite = 10;
        doThrow(new IOException("mock throwing the exception")).when(flush).write(any(byte[].class), anyInt(), anyInt());

        EndOfLineBuffer endOfLineBuffer = new EndOfLineBuffer(10, new byte[]{0x0D, 0x0A}, flush);
        log.info("Buffer initial status:\n" + endOfLineBufferToString(endOfLineBuffer)  + "\n");

        writeDataToEndOfLineBuffer(endOfLineBuffer, new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, failingWrite});

        Exception expected = null;
        try{
            endOfLineBuffer.write((byte)1);
        }catch (Exception e){
            expected = e;
        }
        assertNotNull(expected);
        assertTrue(expected instanceof IllegalStateException);

    }

    @Test
    public void testWrite_skip() throws Exception {

        EndOfLineBuffer endOfLineBuffer = new EndOfLineBuffer(10, new byte[]{0x22, 0x23}, null);// Null output stream will not trigger the flush()
        log.info("Buffer initial status:\n" + endOfLineBufferToString(endOfLineBuffer)  + "\n");

        writeDataToEndOfLineBuffer(endOfLineBuffer, new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10});
        writeDataToEndOfLineBuffer(endOfLineBuffer, new byte[]{0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x20});
        writeDataToEndOfLineBuffer(endOfLineBuffer, new byte[]{0x21, 0x22, 0x23});

    }



    @Test
    public void testReset() throws Exception {

    }

    static int writeDataToEndOfLineBuffer(final EndOfLineBuffer circularBuffer, final byte[] data){
        boolean eol;
        int i = 0;
        do{
            eol = circularBuffer.write(data[i]);
            i++;
        }while (!eol && i<data.length);

        return i;// Return how many bytes we've actually written
    }

    static String endOfLineBufferToString(final EndOfLineBuffer buffer){
        return "EndOfLineBuffer{" +
                "circularBuffer=" + CircularBufferTest.circularBufferToString(buffer.circularBuffer) +
                ", flushOutputStream=" + buffer.flushOutputStream +
                ", endOfLineSequence=" + TestUtils.printBytesHexEncoded(buffer.endOfLineSequence) +
                ", endOfLineSequenceMatchingLength=" + buffer.endOfLineSequenceMatchingLength +
                '}';

    }

}
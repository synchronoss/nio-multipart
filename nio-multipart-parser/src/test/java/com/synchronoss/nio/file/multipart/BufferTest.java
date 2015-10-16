package com.synchronoss.nio.file.multipart;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/**
 * Created by sriz0001 on 16/10/2015.
 */
public class BufferTest {

    private static final Logger log = LoggerFactory.getLogger(BufferTest.class);

    static final byte[] BOUNDARY = "AAA".getBytes();
    static final byte[] DELIMITER = "--AAA".getBytes();
    static final byte[] CLOSE_DELIMITER = "--AAA--".getBytes();
    static final byte[] CR_LF = {0x0D, 0x0A};

    @Test
    public void testWriteForPreamble() throws Exception {

        ByteArrayOutputStream preambleBos = new ByteArrayOutputStream();
        for (int i=0; i<100; i++){
            preambleBos.write(1);
        }
        preambleBos.write(DELIMITER, 0, DELIMITER.length);
        preambleBos.write(CR_LF, 0, CR_LF.length);
        byte[] preamble = preambleBos.toByteArray();

        log.info(Arrays.toString(preamble));

        NioMultipartParserImpl.Buffer buffer = new NioMultipartParserImpl.Buffer(10, BOUNDARY);

        for (int i=0; i<preamble.length - 1; i++){
            Assert.assertFalse(buffer.writeForPreamble(preamble[i]));
        }
        Assert.assertTrue(buffer.writeForPreamble(preamble[preamble.length -1]));

    }

    @Test
    public void testWriteForHeader() throws Exception {

        byte[] header = "Content-disposition: attachment; filename=\"file2.gif\"".getBytes();

        NioMultipartParserImpl.Buffer buffer = new NioMultipartParserImpl.Buffer(100, BOUNDARY);
        for (int i=0; i<header.length; i++){
            Assert.assertFalse(buffer.writeForHeader(header[i]));
        }
        Assert.assertFalse(buffer.writeForHeader(CR_LF[0]));
        Assert.assertTrue(buffer.writeForHeader(CR_LF[1]));

    }

    @Test
    public void testReadHeaderString() throws Exception {

    }

    @Test
    public void testWriteForBody() throws Exception {

        ByteArrayOutputStream bodyBos = new ByteArrayOutputStream();
        for(int i=0; i<30; i++){
            bodyBos.write(1);
        }
        bodyBos.write(CLOSE_DELIMITER);
        byte[] body = bodyBos.toByteArray();

        NioMultipartParserImpl.Buffer buffer = new NioMultipartParserImpl.Buffer(10, BOUNDARY);

        ByteArrayOutputStream destination = new ByteArrayOutputStream();

        int i = 0;
        boolean eol;
        while(true){
            eol = buffer.writeForBody(body[i], destination);
            if (eol || i>=body.length){
                break;
            }
            i++;
        }
        log.info("Wrote " + i + " bytes. Eol: " + eol + " Destination Size: " + destination.size() + ", Data: "+ Arrays.toString(destination.toByteArray()));


    }

    @Test
    public void testMightBeDelimiter() throws Exception {

    }

    @Test
    public void testIsPartDelimiter() throws Exception {

    }

    @Test
    public void testIsPartsDelimiter() throws Exception {

    }

    @Test
    public void testMightBeEndOfLine() throws Exception {

    }

    @Test
    public void testIsEndOfLine() throws Exception {

    }

    @Test
    public void testReset() throws Exception {

    }
}
package com.synchronoss.cloud.nio.multipart;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

/**
 * <p>
 *     Unit tests for {@link MultipartContext}
 * </p>
 * Created by sriz0001 on 20/10/2015.
 */
public class MultipartContextTest {

    private static final Logger log = LoggerFactory.getLogger(MultipartContextTest.class);

    @Test
    public void testGetContentType() throws Exception {
        MultipartContext multipartContext = new MultipartContext("multipart/mixed;boundary=gc0p4Jq0M2Yt08jU534c0p", 560, "UTF-8");
        assertEquals("multipart/mixed;boundary=gc0p4Jq0M2Yt08jU534c0p", multipartContext.getContentType());

        log.info("Created multipart context " + multipartContext);

        Exception expected = null;
        try{
            new MultipartContext("application/json", 560, "UTF-8");
        }catch (Exception e){
            expected = e;
        }
        assertNotNull(expected);
        assertTrue(expected instanceof IllegalStateException);

        expected = null;
        try{
            new MultipartContext(null, 560, "UTF-8");
        }catch (Exception e){
            expected = e;
        }
        assertNotNull(expected);
        assertTrue(expected instanceof IllegalStateException);

    }

    @Test
    public void testGetContentLength() throws Exception {
        MultipartContext multipartContext = new MultipartContext("multipart/mixed;boundary=gc0p4Jq0M2Yt08jU534c0p", 560, "UTF-8");
        log.info("Created multipart context " + multipartContext);
        assertEquals(560, multipartContext.getContentLength());
    }

    @Test
    public void testGetCharEncoding() throws Exception {
        MultipartContext multipartContext = new MultipartContext("multipart/mixed;boundary=gc0p4Jq0M2Yt08jU534c0p", 560, "UTF-8");
        log.info("Created multipart context " + multipartContext);
        assertEquals("UTF-8", multipartContext.getCharEncoding());

        multipartContext = new MultipartContext("multipart/mixed;boundary=gc0p4Jq0M2Yt08jU534c0p", 560, null);
        log.info("Created multipart context " + multipartContext);
        assertNull(multipartContext.getCharEncoding());
    }
}
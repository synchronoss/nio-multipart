package com.synchronoss.cloud.nio.multipart;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by mele on 20/10/2015.
 */
public class MultipartUtilsTest {

    @Test
    public void testIsMultipart() throws Exception {
        assertFalse(MultipartUtils.isMultipart((String)null));
        assertTrue(MultipartUtils.isMultipart("multipart/mixed"));
        assertTrue(MultipartUtils.isMultipart("multipart/digest"));
        assertTrue(MultipartUtils.isMultipart("multipart/alternative"));
        assertTrue(MultipartUtils.isMultipart("multipart/form-data"));
        assertFalse(MultipartUtils.isMultipart("application/json"));
        assertFalse(MultipartUtils.isMultipart("text/plain"));
    }
}
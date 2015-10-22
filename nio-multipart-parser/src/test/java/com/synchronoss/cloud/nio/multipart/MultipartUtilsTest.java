package com.synchronoss.cloud.nio.multipart;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by mele on 20/10/2015.
 */
public class MultipartUtilsTest {

    @Test
    public void testIsMultipart() throws Exception {
        Assert.assertFalse(MultipartUtils.isMultipart(null));
        Assert.assertTrue(MultipartUtils.isMultipart("multipart/mixed"));
        Assert.assertTrue(MultipartUtils.isMultipart("multipart/digest"));
        Assert.assertTrue(MultipartUtils.isMultipart("multipart/alternative"));
        Assert.assertTrue(MultipartUtils.isMultipart("multipart/form-data"));
        Assert.assertFalse(MultipartUtils.isMultipart("application/json"));
        Assert.assertFalse(MultipartUtils.isMultipart("text/plain"));
    }
}
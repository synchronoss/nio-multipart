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

package org.synchronoss.cloud.nio.multipart.io;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * <p> Unit tests for {@link FixedSizeByteArrayOutputStream}
 *
 * @author Silvano Riz.
 */
public class FixedSizeByteArrayOutputStreamTest {

    @Test
    public void testWrite() throws Exception {

        FixedSizeByteArrayOutputStream fixedSizeByteArrayOutputStream = new FixedSizeByteArrayOutputStream(10);
        Assert.assertEquals(0, fixedSizeByteArrayOutputStream.size());

        fixedSizeByteArrayOutputStream.write(0);
        Assert.assertEquals(1, fixedSizeByteArrayOutputStream.size());

        fixedSizeByteArrayOutputStream.write(new byte[]{0x01, 0x02, 0x03, 0x04});
        Assert.assertEquals(5, fixedSizeByteArrayOutputStream.size());

        fixedSizeByteArrayOutputStream.write(new byte[]{0x05, 0x06, 0x07, 0x08, 0x09, 0x10, 0x11, 0x12}, 2, 5);
        Assert.assertEquals(10, fixedSizeByteArrayOutputStream.size());

        Assert.assertArrayEquals(new byte[]{0x00, 0x01, 0x02, 0x03, 0x04, 0x07, 0x08, 0x09, 0x10, 0x11}, fixedSizeByteArrayOutputStream.toByteArray());
    }

    @Test
    public void testWrite_full() throws Exception {

        FixedSizeByteArrayOutputStream fixedSizeByteArrayOutputStream = new FixedSizeByteArrayOutputStream(10);
        Assert.assertEquals(0, fixedSizeByteArrayOutputStream.size());
        fixedSizeByteArrayOutputStream.write(new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x010});
        Assert.assertEquals(10, fixedSizeByteArrayOutputStream.size());

        Exception expected = null;
        try{
            fixedSizeByteArrayOutputStream.write(11);
        }catch (Exception e){
            expected = e;
        }
        assertNotNull(expected);
        assertTrue(expected instanceof IllegalStateException);

        FixedSizeByteArrayOutputStream fixedSizeByteArrayOutputStream1 = new FixedSizeByteArrayOutputStream(10);
        Assert.assertEquals(0, fixedSizeByteArrayOutputStream1.size());
        fixedSizeByteArrayOutputStream1.write(new byte[]{0x01, 0x02, 0x03, 0x04, 0x05});

        expected = null;
        try{
            fixedSizeByteArrayOutputStream.write(new byte[]{0x06, 0x07, 0x08, 0x09, 0x010, 0x11});
        }catch (Exception e){
            expected = e;
        }
        assertNotNull(expected);
        assertTrue(expected instanceof IllegalStateException);

        expected = null;
        try{
            fixedSizeByteArrayOutputStream.write(new byte[]{0x06, 0x07, 0x08, 0x09, 0x010, 0x11, 0x12, 0x13, 0x14}, 1, 7);
        }catch (Exception e){
            expected = e;
        }
        assertNotNull(expected);
        assertTrue(expected instanceof IllegalStateException);

    }

    @Test
    public void testReset() throws Exception {
        FixedSizeByteArrayOutputStream fixedSizeByteArrayOutputStream = new FixedSizeByteArrayOutputStream(10);
        Assert.assertEquals(0, fixedSizeByteArrayOutputStream.size());
        fixedSizeByteArrayOutputStream.write(new byte[]{0x01, 0x02, 0x03, 0x04});
        Assert.assertEquals(4, fixedSizeByteArrayOutputStream.size());
        fixedSizeByteArrayOutputStream.reset();
        Assert.assertEquals(0, fixedSizeByteArrayOutputStream.size());
        fixedSizeByteArrayOutputStream.write(new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x6, 0x7});
        Assert.assertEquals(7, fixedSizeByteArrayOutputStream.size());

    }
}
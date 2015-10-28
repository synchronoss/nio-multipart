/*
 * Copyright 2015 Synchronoss Technologies
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
package com.synchronoss.cloud.nio.multipart.util;

import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * <p>
 *     Unit tests for {@link IOUtils}
 * </p>
 * @author Silvano Riz
 */
public class IOUtilsTest {

    @Test
    public void testInputStreamAsString() throws Exception {

        ByteArrayInputStream bais = new ByteArrayInputStream("This is a string".getBytes());
        assertEquals("This is a string", IOUtils.inputStreamAsString(bais, null));

        byte[] utf8 = {0x54, 0x68, 0x69, 0x73, 0x20, 0x69, 0x73, 0x20, 0x61, 0x20, 0x73, 0x74, 0x72, 0x69, 0x6e, 0x67, (byte)0xc2, (byte)0xa9};
        bais = new ByteArrayInputStream(utf8);
        assertEquals("This is a string©", IOUtils.inputStreamAsString(bais, "UTF-8"));

        InputStream failingIs = Mockito.mock(InputStream.class);
        when(failingIs.read(any(byte[].class), anyInt(), anyInt())).thenThrow(new IOException("An IO Exception"));
        Exception expected = null;
        try{
            IOUtils.inputStreamAsString(failingIs, "UTF-8");
        }catch (Exception e){
            expected = e;
        }
        assertNotNull(expected);

    }

    @Test
    public void testCloseQuietly() throws Exception {

        InputStream inputStream = mock(InputStream.class);
        Mockito.doThrow(new IOException("An IO Exception")).when(inputStream).close();
        IOUtils.closeQuietly(inputStream);

        verify(inputStream).close();
    }
}
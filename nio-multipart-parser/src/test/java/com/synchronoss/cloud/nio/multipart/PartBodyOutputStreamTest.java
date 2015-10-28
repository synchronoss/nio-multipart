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
package com.synchronoss.cloud.nio.multipart;

import com.synchronoss.cloud.nio.multipart.DefaultBodyStreamFactory.PartBodyOutputStream;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * <p>
 *     Unit test for {@link PartBodyOutputStream}
 * </p>
 * @author Silvano Riz.
 */
public class PartBodyOutputStreamTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testConstructor() throws IOException{

        assertNotNull(new PartBodyOutputStream(new File(tempFolder.getRoot(), "testConstructor1.tmp"), 100));
        assertNotNull(new PartBodyOutputStream(new File(tempFolder.getRoot(), "testConstructor2.tmp"), -1));
        assertNotNull(new PartBodyOutputStream(new File(tempFolder.getRoot(), "testConstructor3.tmp"), 0));
    }

    @Test
    public void testWrite() throws IOException {

        File file = new File(tempFolder.getRoot(), "partBodyOutputStream.tmp");

        PartBodyOutputStream partBodyOutputStream = new PartBodyOutputStream(file, 3);
        assertTrue(partBodyOutputStream.isInMemory);
        assertFalse(file.exists());
        assertEquals(0, partBodyOutputStream.byteArrayOutputStream.size());

        partBodyOutputStream.write(0x01);
        partBodyOutputStream.write(0x02);
        partBodyOutputStream.write(0x03);

        assertTrue(partBodyOutputStream.isInMemory);
        assertFalse(file.exists());
        assertEquals(3, partBodyOutputStream.byteArrayOutputStream.size());

        partBodyOutputStream.write(0x04);
        assertFalse(partBodyOutputStream.isInMemory);
        assertTrue(file.exists());
        assertEquals(4, file.length());
        assertNull(partBodyOutputStream.byteArrayOutputStream);

    }

    @Test
    public void testWrite1() throws IOException {

        File file = new File(tempFolder.getRoot(), "partBodyOutputStream1.tmp");

        PartBodyOutputStream partBodyOutputStream = new PartBodyOutputStream(file, 3);
        assertTrue(partBodyOutputStream.isInMemory);
        assertFalse(file.exists());
        assertEquals(0, partBodyOutputStream.byteArrayOutputStream.size());

        partBodyOutputStream.write(new byte[]{0x01, 0x02, 0x03});

        assertTrue(partBodyOutputStream.isInMemory);
        assertFalse(file.exists());
        assertEquals(3, partBodyOutputStream.byteArrayOutputStream.size());

        partBodyOutputStream.write(new byte[]{0x04, 0x05, 0x06});
        assertFalse(partBodyOutputStream.isInMemory);
        assertTrue(file.exists());
        assertEquals(6, file.length());
        assertNull(partBodyOutputStream.byteArrayOutputStream);

    }

    @Test
    public void testWrite2() throws IOException {

        File file = new File(tempFolder.getRoot(), "partBodyOutputStream2.tmp");

        PartBodyOutputStream partBodyOutputStream = new PartBodyOutputStream(file, 3);
        assertTrue(partBodyOutputStream.isInMemory);
        assertFalse(file.exists());
        assertEquals(0, partBodyOutputStream.byteArrayOutputStream.size());

        partBodyOutputStream.write(new byte[]{0x00, 0x01, 0x02, 0x03, 0x00}, 1, 3);

        assertTrue(partBodyOutputStream.isInMemory);
        assertFalse(file.exists());
        assertEquals(3, partBodyOutputStream.byteArrayOutputStream.size());

        partBodyOutputStream.write(new byte[]{0x00, 0x04, 0x05, 0x06, 0x00}, 1, 3);
        assertFalse(partBodyOutputStream.isInMemory);
        assertTrue(file.exists());
        assertEquals(6, file.length());
        assertNull(partBodyOutputStream.byteArrayOutputStream);

    }

    @Test
    public void testGetInputStream_memory() throws IOException {

        File file = new File(tempFolder.getRoot(), "partBodyOutputStream4.tmp");

        PartBodyOutputStream partBodyOutputStream = new PartBodyOutputStream(file, 3);
        assertTrue(partBodyOutputStream.isInMemory);
        assertFalse(file.exists());
        assertEquals(0, partBodyOutputStream.byteArrayOutputStream.size());

        // Write just 3 bytes. Still in the threshold so data should leave in memory...
        partBodyOutputStream.write(new byte[]{0x01, 0x02, 0x03});

        assertTrue(partBodyOutputStream.isInMemory);
        assertFalse(file.exists());
        assertEquals(3, partBodyOutputStream.byteArrayOutputStream.size());

        InputStream inputStream = partBodyOutputStream.getInputStream();
        assertNotNull(inputStream);

        assertArrayEquals(new byte[]{0x01, 0x02, 0x03}, IOUtils.toByteArray(inputStream));

    }

    @Test
    public void testGetInputStream_file() throws IOException {

        File file = new File(tempFolder.getRoot(), "partBodyOutputStream5.tmp");

        PartBodyOutputStream partBodyOutputStream = new PartBodyOutputStream(file, 3);
        assertTrue(partBodyOutputStream.isInMemory);
        assertFalse(file.exists());
        assertEquals(0, partBodyOutputStream.byteArrayOutputStream.size());

        // Write 5 bytes (2 bytes more than the threshold). It should switch to use a file
        partBodyOutputStream.write(new byte[]{0x01, 0x02, 0x03, 0x4, 0x5});

        assertFalse(partBodyOutputStream.isInMemory);
        assertTrue(file.exists());
        assertEquals(5, file.length());

        InputStream inputStream = partBodyOutputStream.getInputStream();
        assertNotNull(inputStream);

        assertArrayEquals(new byte[]{0x01, 0x02, 0x03, 0x4, 0x5}, IOUtils.toByteArray(inputStream));

        // On close the temp file should be deleted
        assertTrue(file.exists());
        IOUtils.closeQuietly(inputStream);
        assertFalse(file.exists());

    }

}
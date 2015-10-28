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

import com.synchronoss.cloud.nio.multipart.DefaultPartStreamsFactory.DefaultPartStreams;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.*;

/**
 * <p>
 *     Unit test for {@link DefaultPartStreams}
 * </p>
 * @author Silvano Riz.
 */
public class DefaultPartStreamsTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testConstructor() throws IOException{

        assertNotNull(new DefaultPartStreams(new File(tempFolder.getRoot(), "testConstructor1.tmp"), 100));
        assertNotNull(new DefaultPartStreams(new File(tempFolder.getRoot(), "testConstructor2.tmp"), -1));
        assertNotNull(new DefaultPartStreams(new File(tempFolder.getRoot(), "testConstructor3.tmp"), 0));
    }

    @Test
    public void testWrite() throws IOException {

        File file = new File(tempFolder.getRoot(), "partBodyOutputStream.tmp");

        DefaultPartStreams defaultPartIOStreams = new DefaultPartStreams(file, 3);
        assertTrue(defaultPartIOStreams.isInMemory);
        assertFalse(file.exists());
        assertEquals(0, defaultPartIOStreams.byteArrayOutputStream.size());

        OutputStream outputStream = defaultPartIOStreams.getPartOutputStream();
        outputStream.write(0x01);
        outputStream.write(0x02);
        outputStream.write(0x03);

        assertTrue(defaultPartIOStreams.isInMemory);
        assertFalse(file.exists());
        assertEquals(3, defaultPartIOStreams.byteArrayOutputStream.size());

        outputStream.write(0x04);
        assertFalse(defaultPartIOStreams.isInMemory);
        assertTrue(file.exists());
        assertEquals(4, file.length());
        assertNull(defaultPartIOStreams.byteArrayOutputStream);

    }

    @Test
    public void testWrite1() throws IOException {

        File file = new File(tempFolder.getRoot(), "partBodyOutputStream1.tmp");

        DefaultPartStreams defaultPartIOStreams = new DefaultPartStreams(file, 3);
        assertTrue(defaultPartIOStreams.isInMemory);
        assertFalse(file.exists());
        assertEquals(0, defaultPartIOStreams.byteArrayOutputStream.size());

        OutputStream outputStream = defaultPartIOStreams.getPartOutputStream();
        outputStream.write(new byte[]{0x01, 0x02, 0x03});

        assertTrue(defaultPartIOStreams.isInMemory);
        assertFalse(file.exists());
        assertEquals(3, defaultPartIOStreams.byteArrayOutputStream.size());

        outputStream.write(new byte[]{0x04, 0x05, 0x06});
        assertFalse(defaultPartIOStreams.isInMemory);
        assertTrue(file.exists());
        assertEquals(6, file.length());
        assertNull(defaultPartIOStreams.byteArrayOutputStream);

    }

    @Test
    public void testWrite2() throws IOException {

        File file = new File(tempFolder.getRoot(), "partBodyOutputStream2.tmp");

        DefaultPartStreams defaultPartIOStreams = new DefaultPartStreams(file, 3);
        assertTrue(defaultPartIOStreams.isInMemory);
        assertFalse(file.exists());
        assertEquals(0, defaultPartIOStreams.byteArrayOutputStream.size());

        OutputStream outputStream = defaultPartIOStreams.getPartOutputStream();
        outputStream.write(new byte[]{0x00, 0x01, 0x02, 0x03, 0x00}, 1, 3);

        assertTrue(defaultPartIOStreams.isInMemory);
        assertFalse(file.exists());
        assertEquals(3, defaultPartIOStreams.byteArrayOutputStream.size());

        outputStream.write(new byte[]{0x00, 0x04, 0x05, 0x06, 0x00}, 1, 3);
        assertFalse(defaultPartIOStreams.isInMemory);
        assertTrue(file.exists());
        assertEquals(6, file.length());
        assertNull(defaultPartIOStreams.byteArrayOutputStream);

    }

    @Test
    public void testGetInputStream_memory() throws IOException {

        File file = new File(tempFolder.getRoot(), "partBodyOutputStream4.tmp");

        DefaultPartStreams defaultPartIOStreams = new DefaultPartStreams(file, 3);
        assertTrue(defaultPartIOStreams.isInMemory);
        assertFalse(file.exists());
        assertEquals(0, defaultPartIOStreams.byteArrayOutputStream.size());

        OutputStream outputStream = defaultPartIOStreams.getPartOutputStream();
        // Write just 3 bytes. Still in the threshold so data should leave in memory...
        outputStream.write(new byte[]{0x01, 0x02, 0x03});

        assertTrue(defaultPartIOStreams.isInMemory);
        assertFalse(file.exists());
        assertEquals(3, defaultPartIOStreams.byteArrayOutputStream.size());

        InputStream inputStream = defaultPartIOStreams.getPartInputStream();
        assertNotNull(inputStream);

        assertArrayEquals(new byte[]{0x01, 0x02, 0x03}, IOUtils.toByteArray(inputStream));

    }

    @Test
    public void testGetInputStream_file() throws IOException {

        File file = new File(tempFolder.getRoot(), "partBodyOutputStream5.tmp");

        DefaultPartStreams defaultPartIOStreams = new DefaultPartStreams(file, 3);
        assertTrue(defaultPartIOStreams.isInMemory);
        assertFalse(file.exists());
        assertEquals(0, defaultPartIOStreams.byteArrayOutputStream.size());

        OutputStream outputStream = defaultPartIOStreams.getPartOutputStream();
        // Write 5 bytes (2 bytes more than the threshold). It should switch to use a file
        outputStream.write(new byte[]{0x01, 0x02, 0x03, 0x4, 0x5});

        assertFalse(defaultPartIOStreams.isInMemory);
        assertTrue(file.exists());
        assertEquals(5, file.length());

        InputStream inputStream = defaultPartIOStreams.getPartInputStream();
        assertNotNull(inputStream);

        assertArrayEquals(new byte[]{0x01, 0x02, 0x03, 0x4, 0x5}, IOUtils.toByteArray(inputStream));

        // On close the temp file should be deleted
        assertTrue(file.exists());
        IOUtils.closeQuietly(inputStream);
        assertFalse(file.exists());

    }

}
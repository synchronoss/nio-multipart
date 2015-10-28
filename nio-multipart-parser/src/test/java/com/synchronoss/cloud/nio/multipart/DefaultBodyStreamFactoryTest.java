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
package com.synchronoss.cloud.nio.multipart;

import com.synchronoss.cloud.nio.multipart.DefaultBodyStreamFactory.PartBodyOutputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * <p>
 *     Unit tests for {@link DefaultBodyStreamFactory}
 * </p>
 * @author Silvano Riz.
 */
public class DefaultBodyStreamFactoryTest {

    private static final Logger log = LoggerFactory.getLogger(DefaultBodyStreamFactoryTest.class);

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testConstructors() throws Exception{

        assertNotNull(new DefaultBodyStreamFactory());
        assertNotNull(new DefaultBodyStreamFactory(3000));
        assertNotNull(new DefaultBodyStreamFactory(-1));
        assertNotNull(new DefaultBodyStreamFactory(0));
        String folder = tempFolder.newFolder().getAbsolutePath();
        assertNotNull(new DefaultBodyStreamFactory(folder));
        assertNotNull(new DefaultBodyStreamFactory(folder, 3000));

    }

    @Test
    public void testGetOutputStream() throws IOException {

        // Content length unknown. Should decide to go in memory first
        DefaultBodyStreamFactory defaultBodyStreamFactory = new DefaultBodyStreamFactory(tempFolder.newFolder("testGetOutputStream").getAbsolutePath());

        BodyStreamFactory.NamedOutputStreamHolder outputStreamHolder = defaultBodyStreamFactory.getOutputStream(new HashMap<String, List<String>>(), 1);
        assertNotNull(outputStreamHolder);
        assertNotNull(outputStreamHolder.getName());
        assertNotNull(outputStreamHolder.getOutputStream());
        assertTrue(outputStreamHolder.getOutputStream() instanceof PartBodyOutputStream);
        assertTrue(((PartBodyOutputStream)outputStreamHolder.getOutputStream()).isInMemory);


        // Content length smaller than the threshold. Should decide to go in memory first
        defaultBodyStreamFactory = new DefaultBodyStreamFactory(tempFolder.newFolder("testGetOutputStream1").getAbsolutePath(), 100);

        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put("content-length", Collections.singletonList("99"));

        outputStreamHolder = defaultBodyStreamFactory.getOutputStream(headers, 1);
        assertNotNull(outputStreamHolder);
        assertNotNull(outputStreamHolder.getName());
        assertNotNull(outputStreamHolder.getOutputStream());
        assertTrue(outputStreamHolder.getOutputStream() instanceof PartBodyOutputStream);
        assertTrue(((PartBodyOutputStream)outputStreamHolder.getOutputStream()).isInMemory);

        // Content length greater than the threshold. Should decide to go directly to file...
        defaultBodyStreamFactory = new DefaultBodyStreamFactory(tempFolder.newFolder("testGetOutputStream2").getAbsolutePath(), 100);

        headers = new HashMap<String, List<String>>();
        headers.put("content-length", Collections.singletonList("140"));

        outputStreamHolder = defaultBodyStreamFactory.getOutputStream(headers, 1);
        assertNotNull(outputStreamHolder);
        assertNotNull(outputStreamHolder.getName());
        assertNotNull(outputStreamHolder.getOutputStream());
        assertTrue(outputStreamHolder.getOutputStream() instanceof PartBodyOutputStream);
        assertFalse(((PartBodyOutputStream) outputStreamHolder.getOutputStream()).isInMemory);

    }

    @Test
    public void testGetOutputStream_error() throws IOException {

        File folder = tempFolder.newFolder("testGetOutputStream_error");

        // Content length unknown. Should decide to go in memory first
        DefaultBodyStreamFactory defaultBodyStreamFactory = new DefaultBodyStreamFactory(folder.getAbsolutePath(), 0);
        assertTrue(folder.delete());// Delete the folder so that we have an error creating the file
        Exception expected = null;
        try {
            defaultBodyStreamFactory.getOutputStream(new HashMap<String, List<String>>(), 1);
        }catch (Exception e){
            expected = e;
        }
        assertNotNull(expected);

    }

    @Test
    public void testGetInputStream() throws IOException {

        DefaultBodyStreamFactory defaultBodyStreamFactory = new DefaultBodyStreamFactory(tempFolder.newFolder("testGetInputStream").getAbsolutePath());

        PartBodyOutputStream partBodyOutputStream = mock(PartBodyOutputStream.class);
        InputStream inputStream = mock(InputStream.class);
        when(partBodyOutputStream.getInputStream()).thenReturn(inputStream);

        assertNotNull(defaultBodyStreamFactory.getInputStream(new BodyStreamFactory.NamedOutputStreamHolder("bodyStreams", partBodyOutputStream)));
        verify(partBodyOutputStream).getInputStream();

    }

    @Test
    public void testGetInputStream_error() throws IOException {

        DefaultBodyStreamFactory defaultBodyStreamFactory = new DefaultBodyStreamFactory(tempFolder.newFolder("testGetInputStream").getAbsolutePath());

        PartBodyOutputStream partBodyOutputStream = mock(PartBodyOutputStream.class);
        when(partBodyOutputStream.getInputStream()).thenThrow(new FileNotFoundException("A FileNotFoundException"));

        Exception expected = null;
        try {
            defaultBodyStreamFactory.getInputStream(new BodyStreamFactory.NamedOutputStreamHolder("bodyStreams", partBodyOutputStream));
        }catch (Exception e){
            expected = e;
        }
        assertNotNull(expected);


    }

}
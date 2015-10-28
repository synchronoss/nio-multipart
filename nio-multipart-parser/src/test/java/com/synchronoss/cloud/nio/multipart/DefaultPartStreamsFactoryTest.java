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

import com.synchronoss.cloud.nio.multipart.DefaultPartStreamsFactory.DefaultPartStreams;
import com.synchronoss.cloud.nio.multipart.PartStreamsFactory.PartStreams;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * <p>
 *     Unit tests for {@link DefaultPartStreamsFactory}
 * </p>
 * @author Silvano Riz.
 */
public class DefaultPartStreamsFactoryTest {

    private static final Logger log = LoggerFactory.getLogger(DefaultPartStreamsFactoryTest.class);

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testConstructors() throws Exception{

        assertNotNull(new DefaultPartStreamsFactory());
        assertNotNull(new DefaultPartStreamsFactory(3000));
        assertNotNull(new DefaultPartStreamsFactory(-1));
        assertNotNull(new DefaultPartStreamsFactory(0));
        String folder = tempFolder.newFolder().getAbsolutePath();
        assertNotNull(new DefaultPartStreamsFactory(folder));
        assertNotNull(new DefaultPartStreamsFactory(folder, 3000));

    }

    @Test
    public void testConstructor_error() throws Exception{

        Exception expected = null;
        File folder = tempFolder.newFolder();
        try {
            assertTrue(folder.setWritable(false));
            new DefaultPartStreamsFactory(new File(folder, "testConstructor_error").getAbsolutePath());
        }catch (Exception e){
            expected = e;
        }finally {
            assertTrue(folder.setWritable(true));
        }
        assertNotNull(expected);

    }

    @Test
    public void testNewPartIOStreams() throws IOException {

        // Content length unknown. Should decide to go in memory first
        DefaultPartStreamsFactory defaultPartIOStreamsFactory = new DefaultPartStreamsFactory(tempFolder.newFolder("testGetOutputStream").getAbsolutePath());

        PartStreams partStreams = defaultPartIOStreamsFactory.newPartStreams(new HashMap<String, List<String>>(), 1);
        assertNotNull(partStreams);
        assertTrue(partStreams instanceof DefaultPartStreams);
        DefaultPartStreams defaultPartIOStreams = (DefaultPartStreams) partStreams;
        assertTrue(defaultPartIOStreams.isInMemory);


        // Content length smaller than the threshold. Should decide to go in memory first
        defaultPartIOStreamsFactory = new DefaultPartStreamsFactory(tempFolder.newFolder("testGetOutputStream1").getAbsolutePath(), 100);

        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put("content-length", Collections.singletonList("99"));

        partStreams = defaultPartIOStreamsFactory.newPartStreams(headers, 1);
        assertNotNull(partStreams);
        assertTrue(partStreams instanceof DefaultPartStreams);
        defaultPartIOStreams = (DefaultPartStreams) partStreams;
        assertTrue(defaultPartIOStreams.isInMemory);

        // Content length greater than the threshold. Should decide to go directly to file...
        defaultPartIOStreamsFactory = new DefaultPartStreamsFactory(tempFolder.newFolder("testGetOutputStream2").getAbsolutePath(), 100);

        headers = new HashMap<String, List<String>>();
        headers.put("content-length", Collections.singletonList("140"));

        partStreams = defaultPartIOStreamsFactory.newPartStreams(headers, 1);
        partStreams = defaultPartIOStreamsFactory.newPartStreams(headers, 1);
        assertNotNull(partStreams);
        assertTrue(partStreams instanceof DefaultPartStreams);
        defaultPartIOStreams = (DefaultPartStreams) partStreams;
        assertFalse(defaultPartIOStreams.isInMemory);

    }

    @Test
    public void testNewPartIOStreams_error() throws IOException {

        File folder = tempFolder.newFolder("testGetOutputStream_error");

        // Content length unknown. Should decide to go in memory first
        DefaultPartStreamsFactory defaultBodyStreamFactory = new DefaultPartStreamsFactory(folder.getAbsolutePath(), 0);
        assertTrue(folder.delete());// Delete the folder so that we have an error creating the file
        Exception expected = null;
        try {
            defaultBodyStreamFactory.newPartStreams(new HashMap<String, List<String>>(), 1);
        }catch (Exception e){
            expected = e;
        }
        assertNotNull(expected);

    }

}
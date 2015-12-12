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
package org.synchronoss.cloud.nio.multipart;

import org.synchronoss.cloud.nio.multipart.io.ByteStore;
import org.synchronoss.cloud.nio.multipart.io.DeferredFileByteStore;
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
 * <p> Unit tests for {@link DefaultPartBodyByteStoreFactory}
 *
 * @author Silvano Riz.
 */
public class DefaultPartBodyByteStoreFactoryTest {

    private static final Logger log = LoggerFactory.getLogger(DefaultPartBodyByteStoreFactoryTest.class);

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testConstructors() throws Exception{

        assertNotNull(new DefaultPartBodyByteStoreFactory());
        assertNotNull(new DefaultPartBodyByteStoreFactory(3000));
        assertNotNull(new DefaultPartBodyByteStoreFactory(-1));
        assertNotNull(new DefaultPartBodyByteStoreFactory(0));
        String folder = tempFolder.newFolder().getAbsolutePath();
        assertNotNull(new DefaultPartBodyByteStoreFactory(folder));
        assertNotNull(new DefaultPartBodyByteStoreFactory(folder, 3000));

    }

    @Test
    public void testConstructor_error() throws Exception{

        Exception expected = null;
        File folder = tempFolder.newFolder();
        try {
            assertTrue(folder.setWritable(false));
            new DefaultPartBodyByteStoreFactory(new File(folder, "testConstructor_error").getAbsolutePath());
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
        DefaultPartBodyByteStoreFactory defaultPartIOStreamsFactory = new DefaultPartBodyByteStoreFactory(tempFolder.newFolder("testGetOutputStream").getAbsolutePath());

        ByteStore byteStore = defaultPartIOStreamsFactory.newByteStoreForPartBody(new HashMap<String, List<String>>(), 1);
        assertNotNull(byteStore);
        assertTrue(byteStore instanceof DeferredFileByteStore);
        DeferredFileByteStore deferredFileByteStore = (DeferredFileByteStore) byteStore;
        assertTrue(deferredFileByteStore.isInMemory());


        // Content length smaller than the threshold. Should decide to go in memory first
        defaultPartIOStreamsFactory = new DefaultPartBodyByteStoreFactory(tempFolder.newFolder("testGetOutputStream1").getAbsolutePath(), 100);

        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put("content-length", Collections.singletonList("99"));

        byteStore = defaultPartIOStreamsFactory.newByteStoreForPartBody(headers, 1);
        assertNotNull(byteStore);
        assertTrue(byteStore instanceof DeferredFileByteStore);
        deferredFileByteStore = (DeferredFileByteStore) byteStore;
        assertTrue(deferredFileByteStore.isInMemory());

        // Content length greater than the threshold. Should decide to go directly to file...
        defaultPartIOStreamsFactory = new DefaultPartBodyByteStoreFactory(tempFolder.newFolder("testGetOutputStream2").getAbsolutePath(), 100);

        headers = new HashMap<String, List<String>>();
        headers.put("content-length", Collections.singletonList("140"));

        byteStore = defaultPartIOStreamsFactory.newByteStoreForPartBody(headers, 1);
        byteStore = defaultPartIOStreamsFactory.newByteStoreForPartBody(headers, 1);
        assertNotNull(byteStore);
        assertTrue(byteStore instanceof DeferredFileByteStore);
        deferredFileByteStore = (DeferredFileByteStore) byteStore;
        assertFalse(deferredFileByteStore.isInMemory());

    }

    @Test
    public void testNewPartIOStreams_error() throws IOException {

        File folder = tempFolder.newFolder("testGetOutputStream_error");

        // Content length unknown. Should decide to go in memory first
        DefaultPartBodyByteStoreFactory defaultBodyStreamFactory = new DefaultPartBodyByteStoreFactory(folder.getAbsolutePath(), 0);
        assertTrue(folder.delete());// Delete the folder so that we have an error creating the file
        Exception expected = null;
        try {
            defaultBodyStreamFactory.newByteStoreForPartBody(new HashMap<String, List<String>>(), 1);
        }catch (Exception e){
            expected = e;
        }
        assertNotNull(expected);

    }

}
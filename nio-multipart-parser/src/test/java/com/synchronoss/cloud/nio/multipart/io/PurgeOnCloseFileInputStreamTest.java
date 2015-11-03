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

package com.synchronoss.cloud.nio.multipart.io;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;


import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Silvano Riz
 */
public class PurgeOnCloseFileInputStreamTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testClose() throws Exception {
        File file = tempFolder.newFile("testClose");
        assertTrue(file.exists());
        PurgeOnCloseFileInputStream purgeOnCloseFileInputStream = new PurgeOnCloseFileInputStream(file);
        assertTrue(file.exists());
        purgeOnCloseFileInputStream.close();
        assertFalse(file.exists());
    }

    @Test
    public void testClose_noFile() throws Exception {
        File file = tempFolder.newFile("testClose");
        assertTrue(file.exists());
        PurgeOnCloseFileInputStream purgeOnCloseFileInputStream = new PurgeOnCloseFileInputStream(file);
        assertTrue(file.delete());
        purgeOnCloseFileInputStream.close();
        assertFalse(file.exists());
    }

    @Test
    public void testClose_cannotDelete() throws Exception {

        File file = new File(tempFolder.getRoot(), "testClose_cannotDelete"){
            int deleteCalls = 0;

            @Override
            public boolean delete() {
                if (deleteCalls == 0){
                    deleteCalls++;
                    return false;
                }else {
                    return super.delete();
                }
            }
        };
        assertTrue(file.createNewFile());
        assertTrue(file.exists());
        PurgeOnCloseFileInputStream purgeOnCloseFileInputStream = new PurgeOnCloseFileInputStream(file);
        purgeOnCloseFileInputStream.close();
        assertTrue(file.exists());

        assertTrue(file.setReadable(true));
        assertTrue(file.setWritable(true));
        assertTrue(file.delete());

    }

}
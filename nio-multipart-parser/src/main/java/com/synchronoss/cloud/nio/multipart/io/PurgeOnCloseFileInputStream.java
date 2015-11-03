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

import com.synchronoss.cloud.nio.multipart.DefaultPartBodyByteStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * <p>
 *     {@link FileInputStream} that will purge the file when {@link #close()} is called.
 * </p>
 * @author Silvano Riz
 */
public class PurgeOnCloseFileInputStream extends FileInputStream {

    private static final Logger log = LoggerFactory.getLogger(DefaultPartBodyByteStoreFactory.class);

    private final File fileToPurge;

    /**
     * <p>
     *     Creates a new <code>PurgeOnCloseFileInputStream</code>
     * </p>
     * @param file The file.
     * @throws FileNotFoundException if the file does not exist, is a directory or it cannot be opened for reading.
     */
    public PurgeOnCloseFileInputStream(final File file) throws FileNotFoundException {
        super(file);
        fileToPurge = file;
    }

    /**
     * <p>
     *     Closes the stream and deletes the file.
     *     The close will not fail if the file is already deleted or it cannot be deleted.
     * </p>
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void close() throws IOException {
        super.close();
        if (fileToPurge.exists()){
            if (!fileToPurge.delete()) {
                log.warn("Failed to purge file: " + fileToPurge.getAbsolutePath());
            }
        }
    }
}

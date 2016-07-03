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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.synchronoss.cloud.nio.stream.storage.DeferredFileStreamStorage;
import org.synchronoss.cloud.nio.stream.storage.StreamStorage;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * <p> Default implementation of the {@code PartBodyStreamStorageFactory}.
 *
 * @author Silvano Riz.
 */
public class DefaultPartBodyStreamStorageFactory implements PartBodyStreamStorageFactory {

    private static final Logger log = LoggerFactory.getLogger(DefaultPartBodyStreamStorageFactory.class);

    /**
     * Default max threshold. 10Kb
     */
    public static final int DEFAULT_MAX_THRESHOLD = 10240;//10kb

    static final String DEFAULT_TEMP_FOLDER = System.getProperty("java.io.tmpdir") + "/nio-file-upload";
    final File tempFolder;
    final int maxSizeThreshold;

    /**
     * <p> Constructor.
     *
     * @param tempFolderPath   The path where to store the temporary files
     * @param maxSizeThreshold The maximum amount of bytes that will be kept in memory for each part. If zero or negative no memory will be used.
     */
    public DefaultPartBodyStreamStorageFactory(final String tempFolderPath, final int maxSizeThreshold) {
        this.maxSizeThreshold = maxSizeThreshold > 0 ? maxSizeThreshold : 0;
        this.tempFolder = new File(tempFolderPath);
        if (!tempFolder.exists()) {
            if (!tempFolder.mkdirs()) {
                throw new IllegalStateException("Unable to create the temporary folder: " + tempFolderPath);
            }
        }
        if (log.isDebugEnabled()) log.debug("Temporary folder: " + tempFolder.getAbsolutePath());
    }

    /**
     * <p> Constructor tha uses a default threshold of 10kb.
     *
     * @param tempFolderPath The path where to store the temporary files
     */
    public DefaultPartBodyStreamStorageFactory(final String tempFolderPath) {
        this(tempFolderPath, DEFAULT_MAX_THRESHOLD);
    }

    /**
     * <p> Constructor tha uses a default default folder ${java.io.tmpdir}/nio-file-upload
     *
     * @param maxSizeThreshold The maximum amount of bytes that will be kept in memory for each part.
     */
    public DefaultPartBodyStreamStorageFactory(int maxSizeThreshold) {
        this(DEFAULT_TEMP_FOLDER, maxSizeThreshold);
    }

    /**
     * <p> Constructor that uses a default threshold of 10kb and a default folder ${java.io.tmpdir}/nio-file-upload
     */
    public DefaultPartBodyStreamStorageFactory() {
        this(DEFAULT_TEMP_FOLDER, DEFAULT_MAX_THRESHOLD);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamStorage newStreamStorageForPartBody(Map<String, List<String>> partHeaders, int partIndex) {
        return new DeferredFileStreamStorage(getTempFile(partIndex), getThreshold(partHeaders));
    }

    protected int getThreshold(final Map<String, List<String>> partHeaders) {
        final long contentLength = MultipartUtils.getContentLength(partHeaders);
        return (contentLength > maxSizeThreshold) ? 0 : maxSizeThreshold;
    }

    protected File getTempFile(final int partIndex) {
        final String tempFileName = String.format("nio-body-%d-%s.tmp", partIndex, UUID.randomUUID().toString());
        return new File(tempFolder, tempFileName);
    }


}

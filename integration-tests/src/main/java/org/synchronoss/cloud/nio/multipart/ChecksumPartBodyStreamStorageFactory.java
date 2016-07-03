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

import org.synchronoss.cloud.nio.multipart.example.io.ChecksumStreamStorage;
import org.synchronoss.cloud.nio.stream.storage.StreamStorage;

import java.util.List;
import java.util.Map;

/**
 * <p> Extension of {@code DefaultPartBodyStreamStorageFactory} that provides {@code StreamStorage} capable of computing a data hash and counting bytes read and written.
 *     This class is an example of how the NIO Multipart parser can be extended via a custom {@code PartBodyStreamStorageFactory}.
 *     In the specific case it is used for testing purposes inside the example-webapp to verify the integrity of the data processed.
 *
 * @author Silvano Riz.
 */
public class ChecksumPartBodyStreamStorageFactory extends DefaultPartBodyStreamStorageFactory {

    final String checksumAlgorithm;

    public ChecksumPartBodyStreamStorageFactory(String tempFolderPath, int maxSizeThreshold, final String checksumAlgorithm) {
        super(tempFolderPath, maxSizeThreshold);
        this.checksumAlgorithm = checksumAlgorithm;
    }

    public ChecksumPartBodyStreamStorageFactory(String tempFolderPath, final String checksumAlgorithm) {
        super(tempFolderPath);
        this.checksumAlgorithm = checksumAlgorithm;
    }

    public ChecksumPartBodyStreamStorageFactory(int maxSizeThreshold, final String checksumAlgorithm) {
        super(maxSizeThreshold);
        this.checksumAlgorithm = checksumAlgorithm;
    }

    public ChecksumPartBodyStreamStorageFactory(final String checksumAlgorithm) {
        super();
        this.checksumAlgorithm = checksumAlgorithm;
    }

    @Override
    public StreamStorage newStreamStorageForPartBody(Map<String, List<String>> partHeaders, int partIndex) {
        return new ChecksumStreamStorage(getTempFile(partIndex), getThreshold(partHeaders), true, checksumAlgorithm);
    }

}

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

import com.synchronoss.cloud.nio.multipart.example.io.ChecksumByteStore;
import com.synchronoss.cloud.nio.multipart.io.ByteStore;

import java.util.List;
import java.util.Map;

/**
 * <p> Extension of {@code DefaultPartBodyByteStoreFactory} that provides {@code ByteStore} capable of computing a data hash and counting bytes read and written.
 *     This class is an example of how the NIO Multipart parser can be extended via a custom {@code PartBodyByteStoreFactory}.
 *     In the specific case it is used for testing purposes inside the example-webapp to verify the integrity of the data processed.
 *
 * @author Silvano Riz.
 */
public class ChecksumPartBodyByteStoreFactory extends DefaultPartBodyByteStoreFactory {

    final String checksumAlgorithm;

    public ChecksumPartBodyByteStoreFactory(String tempFolderPath, int maxSizeThreshold, final String checksumAlgorithm) {
        super(tempFolderPath, maxSizeThreshold);
        this.checksumAlgorithm = checksumAlgorithm;
    }

    public ChecksumPartBodyByteStoreFactory(String tempFolderPath, final String checksumAlgorithm) {
        super(tempFolderPath);
        this.checksumAlgorithm = checksumAlgorithm;
    }

    public ChecksumPartBodyByteStoreFactory(int maxSizeThreshold, final String checksumAlgorithm) {
        super(maxSizeThreshold);
        this.checksumAlgorithm = checksumAlgorithm;
    }

    public ChecksumPartBodyByteStoreFactory(final String checksumAlgorithm) {
        super();
        this.checksumAlgorithm = checksumAlgorithm;
    }

    @Override
    public ByteStore newByteStoreForPartBody(Map<String, List<String>> partHeaders, int partIndex) {
        return new ChecksumByteStore(getTempFile(partHeaders, partIndex), getThreshold(partHeaders), true, checksumAlgorithm);
    }

}

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

package com.synchronoss.cloud.nio.multipart.model;

import java.util.List;

/**
 * <p>
 *     Metadata, a list of {@link FileMetadata}
 * </p>
 * Created by sriz0001 on 30/10/2015.
 */
public class Metadata {

    private List<FileMetadata> filesMetadata;

    public List<FileMetadata> getFilesMetadata() {
        return filesMetadata;
    }

    public void setFilesMetadata(List<FileMetadata> filesMetadata) {
        this.filesMetadata = filesMetadata;
    }

    @Override
    public String toString() {
        return "Metadata{" +
                "filesMetadata=" + filesMetadata +
                '}';
    }
}

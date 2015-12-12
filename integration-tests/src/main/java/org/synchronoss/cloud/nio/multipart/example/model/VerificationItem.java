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

package org.synchronoss.cloud.nio.multipart.example.model;

/**
 * <p> Contains the hash and size of the part input stream, the part output streams and the metadata sent in the request.
 *     It can be used to verify the parts are not missing any bytes.
 *
 * @author Silvano Riz.
 */
public class VerificationItem {

    private String file;
    private long partInputStreamReadBytes;
    private long partOutputStreamWrittenBytes;
    private String partOutputStreamChecksum;
    private String partInputStreamStreamChecksum;
    private long receivedSize;
    private String receivedChecksum;
    private String status;

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public long getPartInputStreamReadBytes() {
        return partInputStreamReadBytes;
    }

    public void setPartInputStreamReadBytes(long partInputStreamReadBytes) {
        this.partInputStreamReadBytes = partInputStreamReadBytes;
    }

    public long getPartOutputStreamWrittenBytes() {
        return partOutputStreamWrittenBytes;
    }

    public void setPartOutputStreamWrittenBytes(long partOutputStreamWrittenBytes) {
        this.partOutputStreamWrittenBytes = partOutputStreamWrittenBytes;
    }

    public String getPartOutputStreamChecksum() {
        return partOutputStreamChecksum;
    }

    public void setPartOutputStreamChecksum(String partOutputStreamChecksum) {
        this.partOutputStreamChecksum = partOutputStreamChecksum;
    }

    public String getPartInputStreamStreamChecksum() {
        return partInputStreamStreamChecksum;
    }

    public void setPartInputStreamStreamChecksum(String partInputStreamStreamChecksum) {
        this.partInputStreamStreamChecksum = partInputStreamStreamChecksum;
    }

    public long getReceivedSize() {
        return receivedSize;
    }

    public void setReceivedSize(long receivedSize) {
        this.receivedSize = receivedSize;
    }

    public String getReceivedChecksum() {
        return receivedChecksum;
    }

    public void setReceivedChecksum(String receivedChecksum) {
        this.receivedChecksum = receivedChecksum;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "VerificationItem{" +
                "file='" + file + '\'' +
                ", partInputStreamReadBytes=" + partInputStreamReadBytes +
                ", partOutputStreamWrittenBytes=" + partOutputStreamWrittenBytes +
                ", partOutputStreamChecksum='" + partOutputStreamChecksum + '\'' +
                ", partInputStreamStreamChecksum='" + partInputStreamStreamChecksum + '\'' +
                ", receivedSize=" + receivedSize +
                ", receivedChecksum='" + receivedChecksum + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}

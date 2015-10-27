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

package com.synchronoss.cloud.nio.multipart.example;

import org.junit.Ignore;
import org.junit.Test;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * <p>
 *     Test that can be used to send a request to the running example-webapp.
 * </p>
 * @author Silvano Riz.
 */
public class FileUploadClientTest {

    @Test
    @Ignore
    public void testNioUpload() throws Exception {
        FileUploadClient fileUploadClient = new FileUploadClient();
        FileUploadClient.Metadata metadata = new FileUploadClient.Metadata("malibu.jpeg");
        fileUploadClient.uploadFile(getTestFile("/malibu.jpeg"), metadata, "http://localhost:8080/example-webapp/nio/multipart");
    }

    @Test
    @Ignore
    public void testNioUpload1() throws Exception {
        FileUploadClient fileUploadClient = new FileUploadClient();
        FileUploadClient.Metadata metadata = new FileUploadClient.Metadata("simple.txt");
        fileUploadClient.uploadFile(getTestFile("/simple.txt"), metadata, "http://localhost:8080/example-webapp/nio/multipart");
    }

    @Test
    @Ignore
    public void testNioUpload2() throws Exception {
        FileUploadClient fileUploadClient = new FileUploadClient();
        FileUploadClient.Metadata metadata = new FileUploadClient.Metadata("delimiter-fragments.txt");
        fileUploadClient.uploadFile(getTestFile("/delimiter-fragments.txt"), metadata, "http://localhost:8080/example-webapp/nio/multipart", "MUEYT2qJT0_ZzYUvVQLy_DlrLeADyxzmsA");
    }

    static String getTestFile(final String fileName){
        try {
            URL resourceUrl = FileUploadClientTest.class.getResource(fileName);
            Path resourcePath = Paths.get(resourceUrl.toURI());
            return resourcePath.toFile().getAbsolutePath();
        }catch (Exception e){
            throw new IllegalStateException("Cannot find the test file", e);
        }
    }
}
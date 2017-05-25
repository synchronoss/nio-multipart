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

package org.synchronoss.cloud.nio.multipart.example;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.synchronoss.cloud.nio.multipart.example.model.FileMetadata;
import org.synchronoss.cloud.nio.multipart.example.model.Metadata;
import org.synchronoss.cloud.nio.multipart.example.model.VerificationItem;
import org.synchronoss.cloud.nio.multipart.example.model.VerificationItems;
import org.synchronoss.cloud.nio.multipart.example.utils.FileUploadClient;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * <p> Integration Test
 *
 * @author Silvano Riz.
 */
@RunWith(Parameterized.class)
public class FileUploadClientIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(FileUploadClient.class);

    @Parameterized.Parameters
    public static Collection data() {
        return getTestCases();
    }

    static class TestCase{
        private final File testFile;
        private final String url;

        public TestCase(File testFile, String url) {
            this.testFile = testFile;
            this.url = url;
        }

        @Override
        public String toString() {
            return "TestCase{" +
                    "testFile=" + testFile +
                    ", url='" + url + '\'' +
                    '}';
        }
    }

    private final TestCase testCase;
    public FileUploadClientIntegrationTest(final TestCase testCase){
        this.testCase = testCase;
    }


    @Test
    public void testNioUpload() throws Exception {
        if (log.isInfoEnabled()) log.info("TestCase: " + testCase);

        FileUploadClient fileUploadClient = new FileUploadClient();

        Metadata metadata = new Metadata();
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setFile(testCase.testFile.getAbsolutePath());
        fileMetadata.setSize(testCase.testFile.length());
        fileMetadata.setChecksum(Files.hash(testCase.testFile, Hashing.sha256()).toString());
        metadata.setFilesMetadata(Collections.singletonList(fileMetadata));

        VerificationItems verificationItems = fileUploadClient.uploadFile(testCase.testFile, metadata, testCase.url);

        List<VerificationItem> verificationItemList = verificationItems.getVerificationItems();
        for (VerificationItem verificationItem : verificationItemList){
            Assert.assertEquals("Not matching " + verificationItem, "MATCHING", verificationItem.getStatus());
        }

    }

    static Collection<TestCase> getTestCases(){
        try {
            URL resourceUrl = FileUploadClientIntegrationTest.class.getResource("/test-files");
            Path resourcePath = Paths.get(resourceUrl.toURI());
            File[] files = resourcePath.toFile().listFiles();

            if (files == null){
                log.warn("Empty test-files folder");
                return Collections.emptyList();
            }

            final int applicationServerPort = Integer.parseInt(System.getProperty("application.server.port", "8080"));
            final String applicationServerHost = "localhost";
            List<TestCase> testCases = new ArrayList<TestCase>();
            for(File file : files){
                for (FileUploadClient.Endpoint endpoint : FileUploadClient.Endpoint.values()){
                    testCases.add(new TestCase(file, endpoint.getEndpoint(applicationServerHost, applicationServerPort)));
                }
            }
            return testCases;
        }catch (Exception e){
            throw new IllegalStateException("Cannot find the test file", e);
        }
    }

}
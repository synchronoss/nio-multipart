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
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.google.gson.Gson;
import org.synchronoss.cloud.nio.multipart.example.model.FileMetadata;
import org.synchronoss.cloud.nio.multipart.example.model.Metadata;
import org.synchronoss.cloud.nio.multipart.example.model.VerificationItem;
import org.synchronoss.cloud.nio.multipart.example.model.VerificationItems;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * <p> Integration Test
 *
 * @author Silvano Riz.
 */
@RunWith(Parameterized.class)
public class FileUploadClientIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(FileUploadClient.class);

    private static final List<String> URLS = Arrays.asList(
            "http://localhost:%d/integration-tests/nio/multipart",
            "http://localhost:%d/integration-tests/nio/dr/multipart",
            "http://localhost:%d/integration-tests/blockingio/adapter/multipart",
            "http://localhost:%d/integration-tests/blockingio/fileupload/multipart"
    );

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

            int applicationServerPort = Integer.parseInt(System.getProperty("application.server.port", "8080"));
            List<TestCase> testCases = new ArrayList<TestCase>();
            for(File file : files){
                for (String url : URLS){
                    testCases.add(new TestCase(file, String.format(url, applicationServerPort)));
                }
            }
            return testCases;
        }catch (Exception e){
            throw new IllegalStateException("Cannot find the test file", e);
        }
    }

    static class FileUploadClient {



        private final CloseableHttpClient httpClient;
        private final Gson gson;

        public FileUploadClient() {
            this.httpClient = HttpClients.createDefault();
            this.gson = new Gson();
        }

        public VerificationItems uploadFile(final File file, final Metadata metadata, final String endpoint){
            return uploadFile(file, metadata, endpoint, null);
        }

        public  VerificationItems uploadFile(final File file, final Metadata metadata, final String endpoint, final String boundary){

            final HttpPost httpPost = new HttpPost(endpoint);
            final HttpEntity httpEntity;

            String metadataStr =  gson.toJson(metadata);

            if (log.isInfoEnabled()) {
                log.info("Metadata: " + metadataStr);
                log.info("File: " + file.getAbsolutePath());
            }

            if (boundary != null && boundary.length() > 0){
                httpEntity = MultipartEntityBuilder.create()
                        .setBoundary(boundary)
                        .addTextBody("metadata", metadataStr, ContentType.APPLICATION_JSON)
                        .addPart(file.getName(), new FileBody(file))
                        .build();
            }else{
                httpEntity = MultipartEntityBuilder.create()
                        .addTextBody("metadata", metadataStr, ContentType.APPLICATION_JSON)
                        .addPart(file.getName(), new FileBody(file))
                        .build();
            }

            httpPost.setEntity(httpEntity);
            CloseableHttpResponse response = null;
            Reader responseReader = null;
            try {

                response = httpClient.execute(httpPost);

                if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK){
                    throw new IllegalStateException("Expected Http 200, but got " + response.getStatusLine().getStatusCode() + ". Message: " +  response.getStatusLine().getReasonPhrase());
                }

                HttpEntity resEntity = response.getEntity();
                responseReader = new InputStreamReader(new BufferedInputStream(resEntity.getContent()));
                return gson.fromJson(responseReader, VerificationItems.class);

            }catch (Exception e){
                throw new IllegalStateException("Request failed", e);
            }finally {
                try{
                    Closeables.close(response, true);
                }catch (Exception e){
                    // Nothing to do...
                }
                IOUtils.closeQuietly(responseReader);
            }

        }
    }
}
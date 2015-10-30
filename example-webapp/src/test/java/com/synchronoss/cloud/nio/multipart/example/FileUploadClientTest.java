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

import com.google.common.hash.Hashing;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.synchronoss.cloud.nio.multipart.model.FileMetadata;
import com.synchronoss.cloud.nio.multipart.model.Metadata;
import com.synchronoss.cloud.nio.multipart.model.VerificationItem;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 *     Test that can be used to send a request to the running example-webapp.
 * </p>
 * @author Silvano Riz.
 */
@RunWith(Parameterized.class)
public class FileUploadClientTest {

    private static final Logger log = LoggerFactory.getLogger(FileUploadClient.class);

    @Parameterized.Parameters
    public static Collection data() {
        return Arrays.asList(getTestFiles());
        //return Collections.singletonList(getTestFile("/test-files/jeppetto.jpeg"));
    }

    private final File testFile;
    public FileUploadClientTest(File testFile){
        this.testFile = testFile;
    }

    @Test
    //@Ignore
    public void testNioUpload() throws Exception {

        log.info("File " + testFile.getAbsolutePath());

        FileUploadClient fileUploadClient = new FileUploadClient();

        Metadata metadata = new Metadata();
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setFile(testFile.getAbsolutePath());
        fileMetadata.setSize(testFile.length());
        fileMetadata.setChecksum(Files.hash(testFile, Hashing.sha256()).toString());
        metadata.setFilesMetadata(Collections.singletonList(fileMetadata));

        List<VerificationItem> verificationItems = fileUploadClient.uploadFile(testFile, metadata, "http://localhost:8080/example-webapp/nio/multipart");

        for (VerificationItem verificationItem : verificationItems){
            Assert.assertEquals("MATCHING", verificationItem.getStatus());
        }

    }

    static File getTestFile(final String fileName){
        try {
            URL resourceUrl = FileUploadClientTest.class.getResource(fileName);
            Path resourcePath = Paths.get(resourceUrl.toURI());
            return resourcePath.toFile();
        }catch (Exception e){
            throw new IllegalStateException("Cannot find the test file", e);
        }
    }

    static File[] getTestFiles(){
        try {
            URL resourceUrl = FileUploadClientTest.class.getResource("/test-files");
            Path resourcePath = Paths.get(resourceUrl.toURI());
            return resourcePath.toFile().listFiles();
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

        public List<VerificationItem> uploadFile(final File file, final Metadata metadata, final String endpoint){
            return uploadFile(file, metadata, endpoint, null);
        }

        public  List<VerificationItem> uploadFile(final File file, final Metadata metadata, final String endpoint, final String boundary){

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
                return Arrays.asList(gson.fromJson(responseReader, VerificationItem[].class));

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
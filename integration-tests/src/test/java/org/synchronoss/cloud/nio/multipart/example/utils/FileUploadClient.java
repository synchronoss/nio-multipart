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

package org.synchronoss.cloud.nio.multipart.example.utils;

import com.google.common.io.Closeables;
import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.synchronoss.cloud.nio.multipart.example.model.Metadata;
import org.synchronoss.cloud.nio.multipart.example.model.VerificationItems;
import org.synchronoss.cloud.nio.multipart.example.web.MultipartController;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Silvano Riz
 */
public class FileUploadClient {

    private static final Logger log = LoggerFactory.getLogger(FileUploadClient.class);

    private final CloseableHttpClient httpClient;
    private final Gson gson;

    public enum Endpoint{
        NIO_MULTIPART("http://%s:%d/integration-tests/nio/multipart"),
        DEFERRED_RESULT_NIO_MULTIPART("http://%s:%d/integration-tests/nio/dr/multipart"),
        BLOCKING_IO_NIO_MULTIPART_ADAPTER("http://%s:%d/integration-tests/blockingio/adapter/multipart"),
        BLOCKING_IO_APACHE_FILE_UPLOAD("http://%s:%d/integration-tests/blockingio/adapter/multipart");

        private final String endpoint;

        Endpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getEndpoint(final String host, final int port) {
            return String.format(endpoint, host, port);
        }
    }

    public static final List<String> URLS = Arrays.asList(
            "http://%s:%d/integration-tests/nio/multipart",
            "http://%s:%d/integration-tests/nio/dr/multipart",
            "http://%s:%d/integration-tests/blockingio/adapter/multipart",
            "http://%s:%d/integration-tests/blockingio/fileupload/multipart"
    );

    public FileUploadClient() {
        this.httpClient = HttpClients.createDefault();
        this.gson = new Gson();
    }

    public VerificationItems postForm(final Map<String, String> formParamValueMap, final String endpoint, final String boundary){

        final HttpPost httpPost = new HttpPost(endpoint);
        httpPost.setHeader(MultipartController.VERIFICATION_CONTROL_HEADER_NAME, MultipartController.VERIFICATION_CONTROL_FORM);
        try {

            for (Map.Entry<String, String> param : formParamValueMap.entrySet()) {
                HttpEntity httpEntity = MultipartEntityBuilder
                        .create()
                        .setBoundary(boundary)
                        .setContentType(ContentType.MULTIPART_FORM_DATA)
                        //.addPart(FormBodyPartBuilder.create().addField(param.getKey(), param.getValue()).build())
                        .addPart(param.getKey(), new StringBody(param.getValue()))
                        .build();
                httpPost.setEntity(httpEntity);
            }

        }catch (Exception e){
            throw new IllegalStateException("Error preparing the post request", e);
        }
        return post(httpPost);
    }

    public VerificationItems postForm(final Map<String, String> formParamValueMap, final String endpoint){
        return postForm(formParamValueMap, endpoint, null);
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

        httpEntity = MultipartEntityBuilder.create()
                .setBoundary(boundary)
                .addTextBody("metadata", metadataStr, ContentType.APPLICATION_JSON)
                .addPart(file.getName(), new FileBody(file))
                .build();

        httpPost.setEntity(httpEntity);

        return post(httpPost);
    }

    VerificationItems post(final HttpPost httpPost){
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

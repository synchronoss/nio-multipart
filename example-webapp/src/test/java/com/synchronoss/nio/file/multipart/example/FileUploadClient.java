package com.synchronoss.nio.file.multipart.example;

import com.google.common.io.Closeables;
import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Created by sriz0001 on 09/10/2015.
 */
public class FileUploadClient {

    private static final Logger log = LoggerFactory.getLogger(FileUploadClient.class);

    public static class Metadata{
        private final String name;

        public Metadata(String name) {
            this.name = name;
        }
    }

    private final CloseableHttpClient httpClient;
    private final Gson gson;

    public FileUploadClient() {
        this.httpClient = HttpClients.createDefault();
        this.gson = new Gson();
    }

    public void uploadFile(final String filePath, final Metadata metadata, final String endpoint){

        final File fileToUpload =  new File(filePath);
        final HttpPost httpPost = new HttpPost(endpoint);
        final HttpEntity httpEntity = MultipartEntityBuilder.create()
                .addPart("metadata", new StringBody(gson.toJson(metadata), ContentType.APPLICATION_JSON))
                .addPart("bin", new FileBody(fileToUpload))
                .build();
        httpPost.setEntity(httpEntity);

        CloseableHttpResponse response = null;
        try {

            response = httpClient.execute(httpPost);
            HttpEntity resEntity = response.getEntity();
            if (resEntity != null) {
                String responseString = EntityUtils.toString(resEntity, "UTF-8");
                log.info("Response content length: " + resEntity.getContentLength());
                log.info("Response content: " + responseString);
            }
            EntityUtils.consume(resEntity);

        }catch (Exception e){
            throw new IllegalStateException("File upload failed", e);
        }finally {
            try{
                Closeables.close(response, true);
            }catch (Exception e){
                // Nothing to do...
            }
        }

    }
}

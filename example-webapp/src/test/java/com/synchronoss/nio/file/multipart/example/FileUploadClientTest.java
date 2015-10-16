package com.synchronoss.nio.file.multipart.example;

import org.junit.Test;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by sriz0001 on 09/10/2015.
 */
public class FileUploadClientTest {

    @Test
    //@Ignore
    public void testNioUpload() throws Exception {

        FileUploadClient fileUploadClient = new FileUploadClient();
        FileUploadClient.Metadata metadata = new FileUploadClient.Metadata("Hawaii.jpg");
        fileUploadClient.uploadFile(getTestFile(), metadata, "http://localhost:8080/example-webapp/nio/multipart");

    }

    static String getTestFile(){
        try {
            URL resourceUrl = FileUploadClientTest.class.getResource("/malibu.jpeg");
            Path resourcePath = Paths.get(resourceUrl.toURI());
            return resourcePath.toFile().getAbsolutePath();
        }catch (Exception e){
            throw new IllegalStateException("Cannot find the test file", e);
        }
    }
}
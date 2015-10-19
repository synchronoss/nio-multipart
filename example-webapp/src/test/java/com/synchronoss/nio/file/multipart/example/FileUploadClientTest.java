package com.synchronoss.nio.file.multipart.example;

import org.junit.Ignore;
import org.junit.Test;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by sriz0001 on 09/10/2015.
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
        FileUploadClient.Metadata metadata = new FileUploadClient.Metadata("test.txt");
        fileUploadClient.uploadFile(getTestFile("/test.txt"), metadata, "http://localhost:8080/example-webapp/nio/multipart");
    }

    @Test
    @Ignore
    public void testNioUpload2() throws Exception {
        FileUploadClient fileUploadClient = new FileUploadClient();
        FileUploadClient.Metadata metadata = new FileUploadClient.Metadata("delimiter-fragments.txt");
        fileUploadClient.uploadFile(getTestFile("/delimiter-fragments.txt"), metadata, "http://localhost:8080/example-webapp/nio/multipart");
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
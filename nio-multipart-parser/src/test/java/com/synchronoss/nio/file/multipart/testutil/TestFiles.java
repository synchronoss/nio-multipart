package com.synchronoss.nio.file.multipart.testutil;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Created by sriz0001 on 19/10/2015.
 */
public class TestFiles {

    public static class TestFile{

        final String path;
        final String boundary;
        final int contentLength;
        final String charEncoding;
        final String contentType;

        public TestFile(String path, String boundary, int contentLength, String charEncoding, String contentType) {
            this.path = path;
            this.boundary = boundary;
            this.contentLength = contentLength;
            this.charEncoding = charEncoding;
            this.contentType = contentType;
        }

        public File getFile(){
            return new File(path);
        }

        public InputStream getInputStream() {
            try {
                return new FileInputStream(path);
            }catch (Exception e){
                throw new IllegalStateException("Unable to create the input stream for the file " + path, e);
            }
        }

        public String getPath() {
            return path;
        }

        public String getBoundary() {
            return boundary;
        }

        public int getContentLength() {
            return contentLength;
        }

        public String getCharEncoding() {
            return charEncoding;
        }

        public String getContentType() {
            return contentType;
        }
    }

    public static TestFile TEXT_WITH_BOUNDARY_SEGMENTS_IN_BODY = new TestFile(
            "/samples/multipart-body-with-boundary-segments.txt",
            "aaaaaa",
            354,
            "UTF-8",
            "multipart/mixed"
            );

    public static TestFile JSON_AND_IMAGE = new TestFile(
            "/samples/multipart-jpeg.txt",
            "MUEYT2qJT0_ZzYUvVQLy_DlrLeADyxzmsA",
            7547,
            "UTF-8",
            "multipart/mixed"
    );

    public static TestFile TEXT_WITH_PREAMBLE = new TestFile(
            "/samples/multipart-with-preamble.txt",
            "simple boundary",
            452,
            "UTF-8",
            "multipart/mixed"
    );

    public static TestFile TEXT_SIMPLE = new TestFile(
            "/samples/simple.txt",
            "BBBBB",
            336,
            "UTF-8",
            "multipart/mixed"
    );

    public static List<TestFile> TEST_FILES = Arrays.asList(
            TEXT_WITH_BOUNDARY_SEGMENTS_IN_BODY,
            JSON_AND_IMAGE,
            TEXT_WITH_PREAMBLE,
            TEXT_SIMPLE
    );

}

package com.synchronoss.nio.file.multipart.testutil;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 *     Set of test files containing a multipart body
 * </p>
 * Created by sriz0001 on 19/10/2015.
 */
public class TestFiles {

    public static class TestFile{

        final String path;
        final File file;
        final String boundary;
        final String charEncoding;
        final String contentType;

        public TestFile(String path, String boundary, String charEncoding, String contentType) {
            this.path = path;
            this.file = new File(TestUtils.getTestFileFullPath(path));
            this.boundary = boundary;
            this.charEncoding = charEncoding;
            this.contentType = contentType;
        }

        public File getFile(){
            return file;
        }

        public InputStream getInputStream() {
            try {
                return new FileInputStream(getFile());
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
            if (file.length() > Integer.MAX_VALUE){
                throw new IllegalStateException("File too big. Size in bytes: " + file.length());
            }else{
                return (int)file.length();
            }
        }

        public String getCharEncoding() {
            return charEncoding;
        }

        public String getContentType() {
            return contentType;
        }
    }

    public static TestFile TEST_0001 = new TestFile(
            "/samples/test0001.txt",
            "MUEYT2qJT0_ZzYUvVQLy_DlrLeADyxzmsA",
            "UTF-8",
            "multipart/form-data;boundary=MUEYT2qJT0_ZzYUvVQLy_DlrLeADyxzmsA"
    );

    public static TestFile TEST_0002 = new TestFile(
            "/samples/test0002.txt",
            "MUEYT2qJT0_ZzYUvVQLy_DlrLeADyxzmsA",
            "UTF-8",
            "multipart/form-data;boundary=MUEYT2qJT0_ZzYUvVQLy_DlrLeADyxzmsA"
    );

    public static TestFile TEST_0003 = new TestFile(
            "/samples/test0003.txt",
            "MUEYT2qJT0_ZzYUvVQLy_DlrLeADyxzmsA",
            "UTF-8",
            "multipart/mixed;boundary=MUEYT2qJT0_ZzYUvVQLy_DlrLeADyxzmsA"
    );

    public static TestFile TEST_0004 = new TestFile(
            "/samples/test0004.txt",
            "MUEYT2qJT0_ZzYUvVQLy_DlrLeADyxzmsA",
            "UTF-8",
            "multipart/mixed;boundary=MUEYT2qJT0_ZzYUvVQLy_DlrLeADyxzmsA"
            );

    public static List<TestFile> ALL_TEST_FILES = Arrays.asList(
            TEST_0004,
            TEST_0003,
            TEST_0002,
            TEST_0001
    );

}

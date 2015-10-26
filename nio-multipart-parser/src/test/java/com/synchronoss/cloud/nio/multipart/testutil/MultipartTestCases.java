package com.synchronoss.cloud.nio.multipart.testutil;

import com.synchronoss.cloud.nio.multipart.MultipartContext;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static com.synchronoss.cloud.nio.multipart.testutil.MultipartTestCases.MultipartTestCase.testCaseFromFile;
import static com.synchronoss.cloud.nio.multipart.testutil.MultipartTestCases.MultipartTestCase.testCaseFromString;

/**
 * <p>
 *     Test cases of multipart request bodies
 * </p>
 * Created by sriz0001 on 19/10/2015.
 */
public class MultipartTestCases {

    /**
     * <p>
     *     A test case
     * </p>
     */
    public static class MultipartTestCase {

        private final String description;
        private final String charEncoding;
        private final byte[] body;
        private final String contentType;

        private MultipartTestCase(final String description, final String charEncoding, final String bodyAsString, final String bodyFilePath, final String contentType) {
            this.description = description;
            this.charEncoding = charEncoding;
            this.contentType = contentType;

            try {
                if (bodyAsString != null) {
                    body = bodyAsString.getBytes(charEncoding);
                } else {
                    body = readFile(bodyFilePath);
                }
            }catch (Exception e){
                throw new IllegalStateException("Unable to initialize test case", e);
            }

        }

        private byte[] readFile(final String filePath) throws IOException {

            // If we are reading from file, and the line separator is LF, convert it to CR,LF.
            // This is especially needed if git is normalizing the EoL
            // NB: It seems that the .gitattributes is not working properly in certain cases

            InputStream testFileInputStream = null;
            byte lastWrittenByte = 0x00;
            ByteArrayOutputStream normalizedData = new ByteArrayOutputStream();
            try {
                testFileInputStream = MultipartTestCases.class.getResourceAsStream(filePath);
                while (true) {
                    final int b = testFileInputStream.read();
                    if (b == -1) {
                        break;
                    }
                    if (b == 0x0A) {//LF
                        if (lastWrittenByte == 0x0D) {//CR
                            normalizedData.write(b);
                        } else {
                            normalizedData.write(0x0D);//CR
                            normalizedData.write(0x0A);//LF
                        }
                    } else {
                        normalizedData.write(b);
                    }
                    lastWrittenByte = (byte) b;
                }
                return normalizedData.toByteArray();
            }finally {
                IOUtils.closeQuietly(testFileInputStream);
            }

        }

        public static MultipartTestCase testCaseFromFile(final String description, final String charEncoding, final String contentType, final String bodyFilePath){
            return new MultipartTestCase(description, charEncoding, null, bodyFilePath, contentType);
        }

        public static MultipartTestCase testCaseFromString(final String description, final String charEncoding, final String contentType, final String bodyAsString){
            return new MultipartTestCase(description, charEncoding, bodyAsString, null, contentType);
        }

        public InputStream getBodyInputStream(){
            return new ByteArrayInputStream(body);
        }

        public String getDescription() {
            return description;
        }

        public MultipartContext getMultipartContext(){
            return new MultipartContext(contentType, body.length, charEncoding);
        }

        public RequestContext getRequestContext(){
            return new RequestContext() {
                @Override
                public String getCharacterEncoding() {
                    return charEncoding;
                }

                @Override
                public String getContentType() {
                    return contentType;
                }

                @Override
                public int getContentLength() {
                    return body.length;
                }

                @Override
                public InputStream getInputStream() throws IOException {
                    return getBodyInputStream();
                }
            };
        }

    }

    public static MultipartTestCase FILE_UPLOAD = testCaseFromString("org.apache.commons.fileupload.ServletFileUploadTest.testFileUpload",
            "UTF-8",
            "multipart/form-data;boundary=---1234",
            "-----1234\r\n" +
                    "Content-Disposition: form-data; name=\"file\"; filename=\"foo.tab\"\r\n" +
                    "Content-Type: text/whatever\r\n" +
                    "\r\n" +
                    "This is the content of the file\n" +
                    "\r\n" +
                    "-----1234\r\n" +
                    "Content-Disposition: form-data; name=\"field\"\r\n" +
                    "\r\n" +
                    "fieldValue\r\n" +
                    "-----1234\r\n" +
                    "Content-Disposition: form-data; name=\"multi\"\r\n" +
                    "\r\n" +
                    "value1\r\n" +
                    "-----1234\r\n" +
                    "Content-Disposition: form-data; name=\"multi\"\r\n" +
                    "\r\n" +
                    "value2\r\n" +
                    "-----1234--\r\n"
    );

    public static MultipartTestCase FILENAME_CASE_SENSITIVITY = testCaseFromString("org.apache.commons.fileupload.ServletFileUploadTest.testFilenameCaseSensitivity",
            "UTF-8",
            "multipart/form-data;boundary=---1234",
            "-----1234\r\n" +
                    "Content-Disposition: form-data; name=\"FiLe\"; filename=\"FOO.tab\"\r\n" +
                    "Content-Type: text/whatever\r\n" +
                    "\r\n" +
                    "This is the content of the file\n" +
                    "\r\n" +
                    "-----1234--\r\n"
    );

    public static MultipartTestCase EMPTY_FILE = testCaseFromString("org.apache.commons.fileupload.ServletFileUploadTest.testEmptyFile",
            "UTF-8",
            "multipart/form-data;boundary=---1234",
            "-----1234\r\n" +
                    "Content-Disposition: form-data; name=\"file\"; filename=\"\"\r\n" +
                    "\r\n" +
                    "\r\n" +
                    "-----1234--\r\n"
    );

    public static MultipartTestCase IE5_MAC_BUG = testCaseFromString("org.apache.commons.fileupload.ServletFileUploadTest.testIE5MacBug" ,
            "UTF-8",
            "multipart/form-data;boundary=---1234",
            "-----1234\r\n" +
                    "Content-Disposition: form-data; name=\"field1\"\r\n" +
                    "\r\n" +
                    "fieldValue\r\n" +
                    "-----1234\n" + // NOTE \r missing
                    "Content-Disposition: form-data; name=\"submitName.x\"\r\n" +
                    "\r\n" +
                    "42\r\n" +
                    "-----1234\n" + // NOTE \r missing
                    "Content-Disposition: form-data; name=\"submitName.y\"\r\n" +
                    "\r\n" +
                    "21\r\n" +
                    "-----1234\r\n" +
                    "Content-Disposition: form-data; name=\"field2\"\r\n" +
                    "\r\n" +
                    "fieldValue2\r\n" +
                    "-----1234--\r\n"
    );

    public static MultipartTestCase FILEUPLOAD62 = testCaseFromString("org.apache.commons.fileupload.ServletFileUploadTest.testFILEUPLOAD62",
            "US-ASCII",
            "multipart/form-data; boundary=AaB03x",
            "--AaB03x\r\n" +
                    "content-disposition: form-data; name=\"field1\"\r\n" +
                    "\r\n" +
                    "Joe Blow\r\n" +
                    "--AaB03x\r\n" +
                    "content-disposition: form-data; name=\"pics\"\r\n" +
                    "Content-type: multipart/mixed; boundary=BbC04y\r\n" +
                    "\r\n" +
                    "--BbC04y\r\n" +
                    "Content-disposition: attachment; filename=\"file1.txt\"\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "\r\n" +
                    "... contents of file1.txt ...\r\n" +
                    "--BbC04y\r\n" +
                    "Content-disposition: attachment; filename=\"file2.gif\"\r\n" +
                    "Content-type: image/gif\r\n" +
                    "Content-Transfer-Encoding: binary\r\n" +
                    "\r\n" +
                    "...contents of file2.gif...\r\n" +
                    "--BbC04y--\r\n" +
                    "--AaB03x--"
    );

    public static MultipartTestCase FOLDED_HEADERS = testCaseFromString("org.apache.commons.fileupload.ServletFileUploadTest.testFoldedHeaders",
            "UTF-8",
            "multipart/form-data; boundary=---1234",
            "-----1234\r\n" +
                    "Content-Disposition: form-data; name=\"file\"; filename=\"foo.tab\"\r\n" +
                    "Content-Type: text/whatever\r\n" +
                    "\r\n" +
                    "This is the content of the file\n" +
                    "\r\n" +
                    "-----1234\r\n" +
                    "Content-Disposition: form-data; \r\n" +
                    "\tname=\"field\"\r\n" +
                    "\r\n" +
                    "fieldValue\r\n" +
                    "-----1234\r\n" +
                    "Content-Disposition: form-data;\r\n" +
                    "     name=\"multi\"\r\n" +
                    "\r\n" +
                    "value1\r\n" +
                    "-----1234\r\n" +
                    "Content-Disposition: form-data; name=\"multi\"\r\n" +
                    "\r\n" +
                    "value2\r\n" +
                    "-----1234--\r\n"
    );


    public static MultipartTestCase FILE_UPLOAD_130 = testCaseFromString("org.apache.commons.fileupload.ServletFileUploadTest.testFileUpload130",
            "UTF-8",
            "multipart/form-data; boundary=---1234",
            "-----1234\r\n" +
                    "Content-Disposition: form-data; name=\"file\"; filename=\"foo.tab\"\r\n" +
                    "Content-Type: text/whatever\r\n" +
                    "SomeHeader: present\r\n" +
                    "\r\n" +
                    "This is the content of the file\n" +
                    "\r\n" +
                    "-----1234\r\n" +
                    "Content-Disposition: form-data; \r\n" +
                    "\tname=\"field\"\r\n" +
                    "OtherHeader: Is there\r\n" +
                    "\r\n" +
                    "fieldValue\r\n" +
                    "-----1234\r\n" +
                    "Content-Disposition: form-data;\r\n" +
                    "     name=\"multi\"\r\n" +
                    "YetAnotherHeader: Here\r\n" +
                    "\r\n" +
                    "value1\r\n" +
                    "-----1234\r\n" +
                    "Content-Disposition: form-data; name=\"multi\"\r\n" +
                    "WhatAHeader: Is That\r\n" +
                    "\r\n" +
                    "value2\r\n" +
                    "-----1234--\r\n"
    );

    public static MultipartTestCase PARAMETER_MAP = testCaseFromString("org.apache.commons.fileupload.ServletFileUploadTest.parseParameterMap",
            "US-ASCII",
            "multipart/form-data; boundary=---1234",
            "-----1234\r\n" +
                    "Content-Disposition: form-data; name=\"file\"; filename=\"foo.tab\"\r\n" +
                    "Content-Type: text/whatever\r\n" +
                    "\r\n" +
                    "This is the content of the file\n" +
                    "\r\n" +
                    "-----1234\r\n" +
                    "Content-Disposition: form-data; name=\"field\"\r\n" +
                    "\r\n" +
                    "fieldValue\r\n" +
                    "-----1234\r\n" +
                    "Content-Disposition: form-data; name=\"multi\"\r\n" +
                    "\r\n" +
                    "value1\r\n" +
                    "-----1234\r\n" +
                    "Content-Disposition: form-data; name=\"multi\"\r\n" +
                    "\r\n" +
                    "value2\r\n" +
                    "-----1234--\r\n"
    );

    public static MultipartTestCase CONTENT_TYPE_ATTACHMENT = testCaseFromString("org.apache.commons.fileupload.ServletFileUploadTest.testContentTypeAttachment",
            "US-ASCII",
            "multipart/form-data; boundary=---1234",
            "-----1234\r\n" +
                    "content-disposition: form-data; name=\"field1\"\r\n" +
                    "\r\n" +
                    "Joe Blow\r\n" +
                    "-----1234\r\n" +
                    "content-disposition: form-data; name=\"pics\"\r\n" +
                    "Content-type: multipart/mixed, boundary=---9876\r\n" +
                    "\r\n" +
                    "-----9876\r\n" +
                    "Content-disposition: attachment; filename=\"file1.txt\"\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "\r\n" +
                    "... contents of file1.txt ...\r\n" +
                    "-----9876--\r\n" +
                    "-----1234--\r\n"
    );

    public static MultipartTestCase FILE_0001 = testCaseFromFile("test0001.txt", "UTF-8", "multipart/form-data;boundary=MUEYT2qJT0_ZzYUvVQLy_DlrLeADyxzmsA", "/test-multiparts/test0001.txt");
    public static MultipartTestCase FILE_0002 = testCaseFromFile("test0002.txt","UTF-8", "multipart/form-data;boundary=MUEYT2qJT0_ZzYUvVQLy_DlrLeADyxzmsA", "/test-multiparts/test0002.txt");
    public static MultipartTestCase FILE_0003 = testCaseFromFile("test0003.txt","UTF-8", "multipart/mixed;boundary=MUEYT2qJT0_ZzYUvVQLy_DlrLeADyxzmsA", "/test-multiparts/test0003.txt");
    public static MultipartTestCase FILE_0004 = testCaseFromFile("test0004.txt","UTF-8", "multipart/mixed;boundary=MUEYT2qJT0_ZzYUvVQLy_DlrLeADyxzmsA", "/test-multiparts/test0004.txt");
    public static MultipartTestCase FILE_0005 = testCaseFromFile("test0005.txt","UTF-8", "multipart/mixed;boundary=MUEYT2qJT0_ZzYUvVQLy_DlrLeADyxzmsA", "/test-multiparts/test0005.txt");
    public static MultipartTestCase FILE_0006 = testCaseFromFile("test0006.txt","UTF-8", "multipart/mixed;boundary=MUEYT2qJT0_ZzYUvVQLy_DlrLeADyxzmsA", "/test-multiparts/test0006.txt");
    public static MultipartTestCase FILE_0007 = testCaseFromFile("test0007.txt","UTF-8", "multipart/mixed;boundary=MUEYT2qJT0_ZzYUvVQLy_DlrLeADyxzmsA", "/test-multiparts/test0007.txt");
    public static MultipartTestCase FILE_0008 = testCaseFromFile("test0008.txt","UTF-8", "multipart/mixed;boundary=MUEYT2qJT0_ZzYUvVQLy_DlrLeADyxzmsA", "/test-multiparts/test0008.txt");

    public static List<MultipartTestCase> ALL_TEST_CASES = Arrays.asList(
            FILE_UPLOAD
            ,FILENAME_CASE_SENSITIVITY
            ,EMPTY_FILE
            //,IE5_MAC_BUG// Won't fix
            ,FILEUPLOAD62
            ,FOLDED_HEADERS
            ,FILE_UPLOAD_130
            ,PARAMETER_MAP
            ,CONTENT_TYPE_ATTACHMENT

            ,FILE_0001
            ,FILE_0002
            ,FILE_0003
            ,FILE_0004
            //FILE_0005// Not working with commons file upload because there is no headers.
            ,FILE_0006
            //,FILE_0007// Not working with commons file upload because there is no headers.
            //,FILE_0008
    );

}

package com.synchronoss.cloud.nio.multipart.testutil;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.RequestContext;

import java.io.IOException;
import java.io.InputStream;

/**
 * <p>
 *     Parses a multipart body using the commons file upload lib.
 *     Used in the functional test to verify the nio multipart parser is compliant
 * </p>
 * Created by sriz0001 on 19/10/2015.
 */
public class CommonsFileUploadParser {

    public static FileItemIterator parse(final TestFiles.TestFile testFile){

        final FileUpload fileUpload = new FileUpload();
        final TempFileRequestContext tempFileRequestContext = new TempFileRequestContext(
                new MultipartTestFileInputStream(testFile.getInputStream()),
                testFile.getCharEncoding(),
                testFile.getContentLength(),
                testFile.getContentType()
        );

        try {
            return fileUpload.getItemIterator(tempFileRequestContext);
        }catch (Exception e){
            throw new IllegalStateException("Unable to parse the file with the commons fileupload lib", e);
        }

    }


    static class TempFileRequestContext implements RequestContext{

        final InputStream inputStream;
        final String charEncoding;
        final int contentLength;
        final String contentType;

        public TempFileRequestContext(final InputStream inputStream,
                                      final String charEncoding,
                                      final int contentLength,
                                      final String contentType

        ) {
            this.charEncoding = charEncoding != null ? charEncoding : "UTF-8";
            this.contentLength = contentLength;
            this.contentType = contentType;
            this.inputStream = inputStream;
        }

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
            return contentLength;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return inputStream;
        }
    }

}

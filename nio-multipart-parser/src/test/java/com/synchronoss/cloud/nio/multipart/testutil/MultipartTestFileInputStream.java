package com.synchronoss.cloud.nio.multipart.testutil;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * <p>
 *     Converts all LF in CRLF. If a sequence CRLF is found it will be kept intact.
 *     This allows to store the sample multipart requests in text files and avoid git to interfere with the eol normalization
 * </p>
 * Created by sriz0001 on 22/10/2015.
 */
public class MultipartTestFileInputStream extends InputStream{

    private final ByteArrayOutputStream normalizedData;
    private final InputStream normalizedInputStream;

    public static final byte CR = 0x0D;
    public static final byte LF = 0x0A;

    public MultipartTestFileInputStream(final InputStream inputStreamToNormalize) {

        try {
            normalizedData = new ByteArrayOutputStream();

            byte lastWrittenByte = 0x00;
            while (true) {
                final int b = inputStreamToNormalize.read();
                if (b == -1) {
                    break;
                }
                if (b == LF) {
                    if (lastWrittenByte == CR) {
                        normalizedData.write(b);
                    } else {
                        normalizedData.write(CR);
                        normalizedData.write(LF);
                    }
                } else {
                    normalizedData.write(b);
                }
                lastWrittenByte = (byte) b;
            }
            normalizedInputStream = new ByteArrayInputStream(normalizedData.toByteArray());

        }catch (Exception e){
            throw new IllegalStateException("Unable to create the MultipartTestFileInputStream", e);
        }finally {
            IOUtils.closeQuietly(inputStreamToNormalize);
        }

    }

    @Override
    public int read(byte[] b) throws IOException {
        return normalizedInputStream.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return normalizedInputStream.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return normalizedInputStream.skip(n);
    }

    @Override
    public int available() throws IOException {
        return normalizedInputStream.available();
    }

    @Override
    public void close() throws IOException {
        normalizedInputStream.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        normalizedInputStream.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        normalizedInputStream.reset();
    }

    @Override
    public boolean markSupported() {
        return normalizedInputStream.markSupported();
    }

    @Override
    public int read() throws IOException {
        return normalizedInputStream.read();
    }
}

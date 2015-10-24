package com.synchronoss.cloud.nio.multipart.buffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * <p>
 *     A {@link ByteArrayOutputStream} with a limited capacity.
 * </p>
 * Created by sriz0001 on 24/10/2015.
 */
public class FixedSizeByteArrayOutputStream extends ByteArrayOutputStream {

    private final int maxSize;

    public FixedSizeByteArrayOutputStream(final int maxSize) {
        super(maxSize);
        this.maxSize = maxSize;
    }

    @Override
    public void write(int b) {
        if (size() >= maxSize){
            throw new IllegalStateException("Output Stream is full. Size: " + maxSize);
        }
        super.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        if (len - off > maxSize - size()){
            throw new IllegalStateException("Data too long. It cannot be written to the stream. Size: " + maxSize);
        }
        super.write(b, off, len);
    }

    @Override
    public void write(byte[] b) throws IOException {
        if (b.length > maxSize - size()){
            throw new IllegalStateException("Data too long. It cannot be written to the stream. Size: " + maxSize);
        }
        super.write(b);
    }
}

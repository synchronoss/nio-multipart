package com.synchronoss.cloud.nio.multipart;


import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static com.synchronoss.cloud.nio.multipart.BodyStreamFactory.*;

/**
 * <p>
 *     Unit tests for {@link PartOutputStream}
 * </p>
 * Created by sriz0001 on 21/10/2015.
 */
public class PartOutputStreamTest {

    @Test
    public void testGetName() throws Exception {

        PartOutputStream partOutputStream = new PartOutputStream("Foo") {
            @Override
            public void write(int b) throws IOException {
                // do nothing
            }
        };

        Assert.assertEquals("Foo", partOutputStream.getName());

    }
}
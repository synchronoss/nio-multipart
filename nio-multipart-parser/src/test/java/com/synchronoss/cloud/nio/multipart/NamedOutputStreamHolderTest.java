package com.synchronoss.cloud.nio.multipart;


import com.synchronoss.cloud.nio.multipart.BodyStreamFactory.NamedOutputStreamHolder;
import org.junit.Test;

import java.io.OutputStream;


import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * <p>
 *     Unit tests for {@link NamedOutputStreamHolder}
 * </p>
 * Created by sriz0001 on 21/10/2015.
 */
public class NamedOutputStreamHolderTest {

    @Test
    public void testGetName() throws Exception {

        NamedOutputStreamHolder namedOutputStreamHolder = new NamedOutputStreamHolder("Foo", mock(OutputStream.class));
        assertEquals("Foo", namedOutputStreamHolder.getName());

    }
}
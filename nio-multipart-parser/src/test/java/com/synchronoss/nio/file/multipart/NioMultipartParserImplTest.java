package com.synchronoss.nio.file.multipart;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 *     Unit tests for {@link NioMultipartParserImpl}
 * </p>
 * Created by sriz0001 on 21/10/2015.
 */
public class NioMultipartParserImplTest {

    private static final Logger log = LoggerFactory.getLogger(NioMultipartParserImplTest.class);

    @Test
    public void testConstruction(){

        MultipartContext context = Mockito.mock(MultipartContext.class);
        Mockito.when(context.getContentType()).thenReturn("multipart/form-data;boundary=MUEYT2qJT0_ZzYUvVQLy_DlrLeADyxzmsA");

        NioMultipartParserListener listener = Mockito.mock(NioMultipartParserListener.class);
        BodyStreamFactory bodyStreamFactory = Mockito.mock(BodyStreamFactory.class);

        NioMultipartParserImpl parser = new NioMultipartParserImpl(context, listener);
        Assert.assertNotNull(parser);

        NioMultipartParserImpl parser1 = new NioMultipartParserImpl(context, listener, 5000);
        Assert.assertNotNull(parser1);

        NioMultipartParserImpl parser2 = new NioMultipartParserImpl(context, listener, bodyStreamFactory);
        Assert.assertNotNull(parser2);

        NioMultipartParserImpl parser3 = new NioMultipartParserImpl(context, listener, bodyStreamFactory, 5000);
        Assert.assertNotNull(parser3);

    }


}
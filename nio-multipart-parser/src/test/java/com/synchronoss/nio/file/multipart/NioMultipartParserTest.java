package com.synchronoss.nio.file.multipart;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 *     Unit tests for {@link NioMultipartParser}
 * </p>
 * Created by sriz0001 on 21/10/2015.
 */
public class NioMultipartParserTest {

    private static final Logger log = LoggerFactory.getLogger(NioMultipartParserTest.class);

    @Test
    public void testConstruction(){

        MultipartContext context = Mockito.mock(MultipartContext.class);
        Mockito.when(context.getContentType()).thenReturn("multipart/form-data;boundary=MUEYT2qJT0_ZzYUvVQLy_DlrLeADyxzmsA");

        NioMultipartParserListener listener = Mockito.mock(NioMultipartParserListener.class);
        BodyStreamFactory bodyStreamFactory = Mockito.mock(BodyStreamFactory.class);

        NioMultipartParser parser = new NioMultipartParser(context, listener);
        Assert.assertNotNull(parser);

        NioMultipartParser parser1 = new NioMultipartParser(context, listener, 5000);
        Assert.assertNotNull(parser1);

        NioMultipartParser parser2 = new NioMultipartParser(context, listener, bodyStreamFactory);
        Assert.assertNotNull(parser2);

        NioMultipartParser parser3 = new NioMultipartParser(context, listener, bodyStreamFactory, 5000);
        Assert.assertNotNull(parser3);

    }


}
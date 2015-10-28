/*
 * Copyright 2015 Synchronoss Technologies
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.synchronoss.cloud.nio.multipart;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static com.synchronoss.cloud.nio.multipart.ParserFactory.newParser;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <p>
 *     Unit test for {@link ParserFactory}
 * </p>
 * @author Silvano Riz.
 */
public class ParserFactoryTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testNewParser() throws Exception {

        MultipartContext context = mock(MultipartContext.class);
        when(context.getContentType()).thenReturn("multipart/form-data;boundary=MUEYT2qJT0_ZzYUvVQLy_DlrLeADyxzmsA");
        NioMultipartParserListener listener = mock(NioMultipartParserListener.class);
        PartStreamsFactory partStreamsFactory = mock(PartStreamsFactory.class);

        NioMultipartParser parser = newParser(context, listener).forNio();
        assertNotNull(parser);


        NioMultipartParser parser1 = newParser(context, listener)
                .withBufferSize(500)
                .withHeadersSizeLimit(16000)
                .withMaxMemoryUsagePerBodyPart(100)
                .forNio();

        assertNotNull(parser1);

        NioMultipartParser parser2 = newParser(context, listener)
                .withBufferSize(500)
                .withHeadersSizeLimit(16000)
                .withMaxMemoryUsagePerBodyPart(100)
                .saveTemporaryFilesTo(tempFolder.getRoot().getAbsolutePath())
                .limitNestingPartsTo(2)
                .forNio();

        assertNotNull(parser2);

        NioMultipartParser parser3 = newParser(context, listener)
                .withBufferSize(500)
                .withHeadersSizeLimit(16000)
                .withMaxMemoryUsagePerBodyPart(100)
                .withCustomPartStreamsFactory(partStreamsFactory)
                .forNio();

        assertNotNull(parser3);

    }
}
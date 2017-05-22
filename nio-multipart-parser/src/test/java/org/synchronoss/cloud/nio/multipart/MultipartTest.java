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

package org.synchronoss.cloud.nio.multipart;

import org.synchronoss.cloud.nio.multipart.BlockingIOAdapter.ParserToken;
import org.synchronoss.cloud.nio.multipart.util.collect.CloseableIterator;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.InputStream;

import static org.synchronoss.cloud.nio.multipart.Multipart.multipart;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <p> Unit test for {@link Multipart}
 *
 * @author Silvano Riz.
 */
public class MultipartTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testMultipart_forNio() throws Exception {

        MultipartContext context = mock(MultipartContext.class);
        when(context.getContentType()).thenReturn("multipart/form-data;boundary=MUEYT2qJT0_ZzYUvVQLy_DlrLeADyxzmsA");
        NioMultipartParserListener listener = mock(NioMultipartParserListener.class);
        PartBodyStreamStorageFactory partBodyStreamStorageFactory = mock(PartBodyStreamStorageFactory.class);

        NioMultipartParser parser = multipart(context).forNIO(listener);
        assertNotNull(parser);


        NioMultipartParser parser1 = multipart(context)
                .withBufferSize(500)
                .withHeadersSizeLimit(16000)
                .withMaxMemoryUsagePerBodyPart(100)
                .forNIO(listener);

        assertNotNull(parser1);

        NioMultipartParser parser2 = multipart(context)
                .withBufferSize(500)
                .withHeadersSizeLimit(16000)
                .withMaxMemoryUsagePerBodyPart(100)
                .saveTemporaryFilesTo(tempFolder.getRoot().getAbsolutePath())
                .limitNestingPartsTo(2)
                .forNIO(listener);

        assertNotNull(parser2);

        NioMultipartParser parser3 = multipart(context)
                .withBufferSize(500)
                .withHeadersSizeLimit(16000)
                .withMaxMemoryUsagePerBodyPart(100)
                .usePartBodyStreamStorageFactory(partBodyStreamStorageFactory)
                .forNIO(listener);

        assertNotNull(parser3);

    }

    @Test
    public void testMultipart_forBlockingIO() throws Exception {

        MultipartContext context = mock(MultipartContext.class);
        when(context.getContentType()).thenReturn("multipart/form-data;boundary=MUEYT2qJT0_ZzYUvVQLy_DlrLeADyxzmsA");
        PartBodyStreamStorageFactory partBodyStreamStorageFactory = mock(PartBodyStreamStorageFactory.class);
        InputStream inputStream = mock(InputStream.class);

        CloseableIterator<ParserToken> parts = multipart(context).forBlockingIO(inputStream);
        assertNotNull(parts);

        CloseableIterator<ParserToken> parts1 = multipart(context)
                .withBufferSize(500)
                .withHeadersSizeLimit(16000)
                .withMaxMemoryUsagePerBodyPart(100)
                .forBlockingIO(inputStream);

        assertNotNull(parts1);

        CloseableIterator<ParserToken> parts2 = multipart(context)
                .withBufferSize(500)
                .withHeadersSizeLimit(16000)
                .withMaxMemoryUsagePerBodyPart(100)
                .saveTemporaryFilesTo(tempFolder.getRoot().getAbsolutePath())
                .limitNestingPartsTo(2)
                .forBlockingIO(inputStream);

        assertNotNull(parts2);

        CloseableIterator<ParserToken> parts3 = multipart(context)
                .withBufferSize(500)
                .withHeadersSizeLimit(16000)
                .withMaxMemoryUsagePerBodyPart(100)
                .usePartBodyStreamStorageFactory(partBodyStreamStorageFactory)
                .forBlockingIO(inputStream);

        assertNotNull(parts3);

    }
}
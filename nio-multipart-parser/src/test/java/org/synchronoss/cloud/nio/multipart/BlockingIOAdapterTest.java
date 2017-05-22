/*
 * Copyright (C) 2015 Synchronoss Technologies
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
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <p> Unit test for {@link BlockingIOAdapter}
 *
 * @author Silvano Riz
 */
public class BlockingIOAdapterTest {

    private static final Logger log = LoggerFactory.getLogger(BlockingIOAdapterTest.class);

    @Test
    public void testCreation() throws Exception {

        final PartBodyStreamStorageFactory partBodyStreamStorageFactory = mock(PartBodyStreamStorageFactory.class);
        final  InputStream inputStream = mock(InputStream.class);
        final MultipartContext context = mock(MultipartContext.class);
        when(context.getContentType()).thenReturn("multipart/form-data;boundary=MUEYT2qJT0_ZzYUvVQLy_DlrLeADyxzmsA");

        CloseableIterator<ParserToken> parts = BlockingIOAdapter.parse(inputStream, context);
        assertNotNull(parts);

        CloseableIterator<ParserToken> parts1 = BlockingIOAdapter.parse(inputStream, context, 500);
        assertNotNull(parts1);


        CloseableIterator<ParserToken> parts2 = BlockingIOAdapter.parse(inputStream, context, partBodyStreamStorageFactory);
        assertNotNull(parts2);

        CloseableIterator<ParserToken> parts3 = BlockingIOAdapter.parse(inputStream, context, partBodyStreamStorageFactory, 500, 200, 2);
        assertNotNull(parts3);

    }
}
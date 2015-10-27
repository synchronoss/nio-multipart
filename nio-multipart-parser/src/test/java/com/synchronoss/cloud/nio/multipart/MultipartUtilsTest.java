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

package com.synchronoss.cloud.nio.multipart;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * <p>
 *     Unit Tests for {@link MultipartUtils}
 * </p>
 * Created by mele on 20/10/2015.
 */
public class MultipartUtilsTest {

    @Test
    public void testIsMultipart() throws Exception {
        assertFalse(MultipartUtils.isMultipart((String)null));
        assertTrue(MultipartUtils.isMultipart("multipart/mixed"));
        assertTrue(MultipartUtils.isMultipart("multipart/digest"));
        assertTrue(MultipartUtils.isMultipart("multipart/alternative"));
        assertTrue(MultipartUtils.isMultipart("multipart/form-data"));
        assertFalse(MultipartUtils.isMultipart("application/json"));
        assertFalse(MultipartUtils.isMultipart("text/plain"));
    }
}
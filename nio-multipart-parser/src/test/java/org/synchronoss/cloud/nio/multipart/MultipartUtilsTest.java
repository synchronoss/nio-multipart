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

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * <p> Unit Tests for {@link MultipartUtils}
 *
 * @author Silvano Riz.
 */
public class MultipartUtilsTest {

    @Test
    public void testIsMultipart() throws Exception {
        assertFalse(MultipartUtils.isMultipart(null));
        assertTrue(MultipartUtils.isMultipart("multipart/mixed"));
        assertTrue(MultipartUtils.isMultipart("multipart/digest"));
        assertTrue(MultipartUtils.isMultipart("multipart/alternative"));
        assertTrue(MultipartUtils.isMultipart("multipart/form-data"));
        assertFalse(MultipartUtils.isMultipart("application/json"));
        assertFalse(MultipartUtils.isMultipart("text/plain"));
    }

    @Test
    public void testHasMultipartContentType() throws Exception {

        Map<String, List<String>> multipart = new HashMap<String, List<String>>();
        multipart.put(MultipartUtils.CONTENT_TYPE.toLowerCase(), Collections.singletonList("multipart/mixed"));

        Map<String, List<String>> jpeg = new HashMap<String, List<String>>();
        jpeg.put(MultipartUtils.CONTENT_TYPE.toLowerCase(), Collections.singletonList("image/jpeg"));

        assertFalse(MultipartUtils.hasMultipartContentType(jpeg));
        assertTrue(MultipartUtils.hasMultipartContentType(multipart));
        assertFalse(MultipartUtils.hasMultipartContentType(new HashMap<String, List<String>>()));
    }

    @Test
    public void getContentLength(){

        Map<String, List<String>> contentLength100 = new HashMap<String, List<String>>();
        contentLength100.put(MultipartUtils.CONTENT_LENGTH.toLowerCase(), Collections.singletonList("100"));

        Map<String, List<String>> contentLengthWrongValue = new HashMap<String, List<String>>();
        contentLengthWrongValue.put(MultipartUtils.CONTENT_LENGTH.toLowerCase(), Collections.singletonList("ABC"));

        assertEquals(100, MultipartUtils.getContentLength(contentLength100));
        assertEquals(-1, MultipartUtils.getContentLength(contentLengthWrongValue));
        assertEquals(-1, MultipartUtils.getContentLength(new HashMap<String, List<String>>()));

    }

    @Test
    public void getCharEncoding(){

        Map<String, List<String>> charEncodingUTF8 = new HashMap<String, List<String>>();
        charEncodingUTF8.put(MultipartUtils.CONTENT_TYPE.toLowerCase(), Collections.singletonList("text/plain; charset=UTF-8"));

        Map<String, List<String>> noCharEncoding = new HashMap<String, List<String>>();
        noCharEncoding.put(MultipartUtils.CONTENT_TYPE.toLowerCase(), Collections.singletonList("text/plain"));

        assertEquals("UTF-8", MultipartUtils.getCharEncoding(charEncodingUTF8));
        assertNull(MultipartUtils.getCharEncoding(noCharEncoding));
        assertNull(MultipartUtils.getCharEncoding(new HashMap<String, List<String>>()));

    }

    @Test
    public void testGetHeaders(){

        Map<String, List<String>> singleValueHeader = new HashMap<String, List<String>>();
        singleValueHeader.put("my-header", Collections.singletonList("single value"));

        assertEquals(Collections.singletonList("single value"), MultipartUtils.getHeaders("my-header", singleValueHeader));

        Map<String, List<String>> multiValueHeader = new HashMap<String, List<String>>();
        multiValueHeader.put("my-multi-value-header", Arrays.asList("value 1", "value 2"));

        assertEquals(Arrays.asList("value 1", "value 2"), MultipartUtils.getHeaders("my-multi-value-header", multiValueHeader));

        Map<String, List<String>> emptyHeaders = new HashMap<String, List<String>>();
        assertNull(MultipartUtils.getHeaders("no-header", emptyHeaders));

    }

    @Test
    public void testGetHeader(){

        Map<String, List<String>> singleValueHeader = new HashMap<String, List<String>>();
        singleValueHeader.put("my-header", Collections.singletonList("single value"));

        assertEquals("single value", MultipartUtils.getHeader("my-header", singleValueHeader));

        Map<String, List<String>> multiValueHeader = new HashMap<String, List<String>>();
        multiValueHeader.put("my-multi-value-header", Arrays.asList("value 1", "value 2"));

        assertEquals("value 1", MultipartUtils.getHeader("my-multi-value-header", multiValueHeader));

        Map<String, List<String>> emptyHeaders = new HashMap<String, List<String>>();
        assertNull(MultipartUtils.getHeader("no-header", emptyHeaders));

    }

    @Test
    public void testIsFormField(){


        Map<String, List<String>> contentDispositionWithFilenameAndName = new HashMap<String, List<String>>();
        contentDispositionWithFilenameAndName.put(MultipartUtils.CONTENT_TYPE.toLowerCase(), Collections.singletonList("multipart/form-data; boundary=--------1234"));
        contentDispositionWithFilenameAndName.put(MultipartUtils.CONTENT_DISPOSITION.toLowerCase(), Collections.singletonList("form-data; name=\"file\"; filename=\"file.txt\""));
        assertFalse(MultipartUtils.isFormField(contentDispositionWithFilenameAndName));

        Map<String, List<String>> contentDispositionWithFilename = new HashMap<String, List<String>>();
        contentDispositionWithFilename.put(MultipartUtils.CONTENT_TYPE.toLowerCase(), Collections.singletonList("multipart/form-data; boundary=--------1234"));
        contentDispositionWithFilename.put(MultipartUtils.CONTENT_DISPOSITION.toLowerCase(), Collections.singletonList("form-data; filename=\"file.txt\""));
        assertFalse(MultipartUtils.isFormField(contentDispositionWithFilename));

        Map<String, List<String>> contentDispositionWithName = new HashMap<String, List<String>>();
        contentDispositionWithName.put(MultipartUtils.CONTENT_TYPE.toLowerCase(), Collections.singletonList("multipart/form-data; boundary=--------1234"));
        contentDispositionWithName.put(MultipartUtils.CONTENT_DISPOSITION.toLowerCase(), Collections.singletonList("form-data; name=\"file\""));
        assertTrue(MultipartUtils.isFormField(contentDispositionWithName));

        Map<String, List<String>> contentDispositionWithNameNotFormData = new HashMap<String, List<String>>();
        contentDispositionWithName.put(MultipartUtils.CONTENT_TYPE.toLowerCase(), Collections.singletonList("multipart/mixed; boundary=--------1234"));
        contentDispositionWithName.put(MultipartUtils.CONTENT_DISPOSITION.toLowerCase(), Collections.singletonList("form-data; name=\"file\""));
        assertFalse(MultipartUtils.isFormField(contentDispositionWithName));

        Map<String, List<String>> contentDispositionNoNameNoFileName = new HashMap<String, List<String>>();
        contentDispositionNoNameNoFileName.put(MultipartUtils.CONTENT_TYPE.toLowerCase(), Collections.singletonList("multipart/form-data; boundary=--------1234"));
        contentDispositionNoNameNoFileName.put(MultipartUtils.CONTENT_DISPOSITION.toLowerCase(), Collections.singletonList("form-data"));
        assertFalse(MultipartUtils.isFormField(contentDispositionNoNameNoFileName));

    }

    @Test
    public void testGetFileName(){

        Map<String, List<String>> contentDispositionWithFilenameAndName = new HashMap<String, List<String>>();
        contentDispositionWithFilenameAndName.put(MultipartUtils.CONTENT_DISPOSITION.toLowerCase(), Collections.singletonList("form-data; name=\"file\"; filename=\"file.txt\""));
        assertEquals("file.txt", MultipartUtils.getFileName(contentDispositionWithFilenameAndName));

        Map<String, List<String>> contentDispositionWithName = new HashMap<String, List<String>>();
        contentDispositionWithName.put(MultipartUtils.CONTENT_DISPOSITION.toLowerCase(), Collections.singletonList("form-data; name=\"file\""));
        assertNull(MultipartUtils.getFileName(contentDispositionWithName));

        Map<String, List<String>> contentDispositionNoNameNoFileName = new HashMap<String, List<String>>();
        contentDispositionNoNameNoFileName.put(MultipartUtils.CONTENT_DISPOSITION.toLowerCase(), Collections.singletonList("form-data"));
        assertNull(MultipartUtils.getFileName(contentDispositionNoNameNoFileName));

        assertNull(MultipartUtils.getFileName(new HashMap<String, List<String>>()));
    }


    @Test
    public void testGetFieldName(){

        Map<String, List<String>> contentDispositionWithFilenameAndName = new HashMap<String, List<String>>();
        contentDispositionWithFilenameAndName.put(MultipartUtils.CONTENT_DISPOSITION.toLowerCase(), Collections.singletonList("form-data; name=\"file\"; filename=\"file.txt\""));
        assertEquals("file", MultipartUtils.getFieldName(contentDispositionWithFilenameAndName));

        Map<String, List<String>> contentDispositionWithFilename = new HashMap<String, List<String>>();
        contentDispositionWithFilename.put(MultipartUtils.CONTENT_DISPOSITION.toLowerCase(), Collections.singletonList("form-data; filename=\"file.txt\""));
        assertNull(MultipartUtils.getFieldName(contentDispositionWithFilename));

        Map<String, List<String>> contentDispositionNoNameNoFileName = new HashMap<String, List<String>>();
        contentDispositionNoNameNoFileName.put(MultipartUtils.CONTENT_DISPOSITION.toLowerCase(), Collections.singletonList("form-data"));
        assertNull(MultipartUtils.getFieldName(contentDispositionNoNameNoFileName));

        assertNull(MultipartUtils.getFieldName(new HashMap<String, List<String>>()));

    }

    @Test
    public void testIsContentTransferEncodingBase64Encoded(){
        Map<String, List<String>> base64ContentTransferEncodingHeaders = new HashMap<String, List<String>>();
        base64ContentTransferEncodingHeaders.put(MultipartUtils.CONTENT_TRANSFER_ENCODING.toLowerCase(), Collections.singletonList("base64"));
        assertTrue(MultipartUtils.isContentTransferEncodingBase64Encoded(base64ContentTransferEncodingHeaders));

        Map<String, List<String>> base64ContentTransferEncodingHeadersCaseInsensitive = new HashMap<String, List<String>>();
        base64ContentTransferEncodingHeadersCaseInsensitive.put(MultipartUtils.CONTENT_TRANSFER_ENCODING.toLowerCase(), Collections.singletonList("bASe64"));
        assertTrue(MultipartUtils.isContentTransferEncodingBase64Encoded(base64ContentTransferEncodingHeadersCaseInsensitive));

        Map<String, List<String>> base64ContentTransferEncodingHeadersEmpty = new HashMap<String, List<String>>();
        base64ContentTransferEncodingHeadersEmpty.put(MultipartUtils.CONTENT_TRANSFER_ENCODING.toLowerCase(), Collections.singletonList(""));
        assertFalse(MultipartUtils.isContentTransferEncodingBase64Encoded(base64ContentTransferEncodingHeadersEmpty));

        Map<String, List<String>> base64ContentTransferEncodingHeadersNull = new HashMap<String, List<String>>();
        base64ContentTransferEncodingHeadersNull.put(MultipartUtils.CONTENT_TRANSFER_ENCODING.toLowerCase(), null);
        assertFalse(MultipartUtils.isContentTransferEncodingBase64Encoded(base64ContentTransferEncodingHeadersNull));

        Map<String, List<String>> base64ContentTransferEncodingHeadersMissing = new HashMap<String, List<String>>();
        base64ContentTransferEncodingHeadersMissing.put(MultipartUtils.CONTENT_TRANSFER_ENCODING.toLowerCase(), null);
        assertFalse(MultipartUtils.isContentTransferEncodingBase64Encoded(base64ContentTransferEncodingHeadersMissing));

    }


}
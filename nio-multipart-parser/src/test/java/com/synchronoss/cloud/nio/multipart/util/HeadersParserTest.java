package com.synchronoss.cloud.nio.multipart.util;


import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.junit.Assert.*;

/**
 * <p>
 *     Unit tests for {@link HeadersParser}
 * </p>
 */
public class HeadersParserTest {

    private static final String CHARACTER_SET = "US-ASCII";

    @Test
    public void testReadLine() throws Exception {
        InputStream lines = new ByteArrayInputStream("\r\r\nfoo bar\r\n".getBytes(CHARACTER_SET));
        assertEquals("\r", HeadersParser.readLine(lines, CHARACTER_SET));
        assertEquals("foo bar", HeadersParser.readLine(lines, CHARACTER_SET));
        assertEquals(null, HeadersParser.readLine(lines, CHARACTER_SET));

        lines = new ByteArrayInputStream("\n\r\nfoo bar\r\n".getBytes(CHARACTER_SET));
        assertEquals("", HeadersParser.readLine(lines, CHARACTER_SET));
        assertEquals("", HeadersParser.readLine(lines, CHARACTER_SET));
        assertEquals("foo bar", HeadersParser.readLine(lines, CHARACTER_SET));
        assertEquals(null, HeadersParser.readLine(lines, CHARACTER_SET));
    }

    @Test
    public void testReadHeaders_folded() throws Exception {

        InputStream headersSection = new ByteArrayInputStream("headerNameA: headerValueA\r\nheaderNameB: headerValueB\r\n   part of b".getBytes(CHARACTER_SET));
        Map<String, List<String>> headers = HeadersParser.parseHeaders(headersSection, CHARACTER_SET);
        assertNotNull(headers);
        assertNotNull(headers);
        assertEquals(2, headers.size());
        assertEquals(singletonList("headerValueA"), headers.get("headerNameA".toLowerCase()));
        assertEquals(singletonList("headerValueB part of b"), headers.get("headerNameB".toLowerCase()));

        headersSection = new ByteArrayInputStream("headerNameA: headerValueA\r\nheaderNameB: headerValueB\r\n\t\t\tpart of b".getBytes(CHARACTER_SET));
        headers = HeadersParser.parseHeaders(headersSection, CHARACTER_SET);
        assertNotNull(headers);
        assertNotNull(headers);
        assertEquals(2, headers.size());
        assertEquals(singletonList("headerValueA"), headers.get("headerNameA".toLowerCase()));
        assertEquals(singletonList("headerValueB part of b"), headers.get("headerNameB".toLowerCase()));

    }

    @Test
    public void testReadHeaders() throws Exception {
        InputStream headersSection = new ByteArrayInputStream("headerNameA: headerValueA\r\nheaderNameB: headerValueB\r\n\r\nmore stuff here".getBytes(CHARACTER_SET));
        Map<String, List<String>> headers = HeadersParser.parseHeaders(headersSection, CHARACTER_SET);
        assertNotNull(headers);
        assertEquals(2, headers.size());
        assertEquals(singletonList("headerValueA"), headers.get("headerNameA".toLowerCase()));
        assertEquals(singletonList("headerValueB"), headers.get("headerNameB".toLowerCase()));
    }

    @Test
    public void testReadHeaders_malformed() throws Exception {
        InputStream headersSection = new ByteArrayInputStream("headerNameA: headerValueA\r\nheaderNameB headerValueB\r\n\r\nmore stuff here".getBytes(CHARACTER_SET));
        Exception expected = null;
        try {
            Map<String, List<String>> headers = HeadersParser.parseHeaders(headersSection, CHARACTER_SET);
        } catch (Exception e) {
            expected = e;
        }
        assertNotNull(expected);
        assertTrue(expected instanceof IllegalStateException);

    }

    @Test
    public void testReadHeaders_lenientTerminator() throws Exception {

        InputStream headersSection = new ByteArrayInputStream("headerNameA: headerValueA\r\nheaderNameB: headerValueB\r\n\r\r\nmore stuff here".getBytes(CHARACTER_SET));
        Map<String, List<String>> headers = HeadersParser.parseHeaders(headersSection, CHARACTER_SET);
        assertNotNull(headers);
        assertNotNull(headers);
        assertEquals(2, headers.size());
        assertEquals(singletonList("headerValueA"), headers.get("headerNameA".toLowerCase()));
        assertEquals(singletonList("headerValueB"), headers.get("headerNameB".toLowerCase()));

        headersSection = new ByteArrayInputStream("headerNameA: headerValueA\r\nheaderNameB: headerValueB\r\n    \r\nmore stuff here".getBytes(CHARACTER_SET));
        headers = HeadersParser.parseHeaders(headersSection, CHARACTER_SET);
        assertNotNull(headers);
        assertNotNull(headers);
        assertEquals(2, headers.size());
        assertEquals(singletonList("headerValueA"), headers.get("headerNameA".toLowerCase()));
        assertEquals(singletonList("headerValueB"), headers.get("headerNameB".toLowerCase()));

    }
}

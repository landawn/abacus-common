package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.util.Charsets;
import com.landawn.abacus.util.IOUtil;

public class HttpUtilTest extends TestBase {

    // Mock HttpURLConnection for testing
    private static class MockHttpURLConnection extends HttpURLConnection {
        private Map<String, List<String>> headerFields = new HashMap<>();
        private Map<String, String> requestProperties = new HashMap<>();
        private InputStream inputStream;
        private InputStream errorStream;
        private OutputStream outputStream;
        private boolean throwOnGetInputStream = false;

        protected MockHttpURLConnection() throws IOException {
            super(new URL("http://example.com"));
        }

        public void setHeaderFields(Map<String, List<String>> headerFields) {
            this.headerFields = headerFields;
        }

        public void setInputStream(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        public void setErrorStream(InputStream errorStream) {
            this.errorStream = errorStream;
        }

        public void setOutputStream(OutputStream outputStream) {
            this.outputStream = outputStream;
        }

        public void setThrowOnGetInputStream(boolean throwOnGetInputStream) {
            this.throwOnGetInputStream = throwOnGetInputStream;
        }

        @Override
        public Map<String, List<String>> getHeaderFields() {
            return headerFields;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            if (throwOnGetInputStream) {
                throw new IOException("Test exception");
            }
            return inputStream;
        }

        @Override
        public InputStream getErrorStream() {
            return errorStream;
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return outputStream;
        }

        @Override
        public void setRequestProperty(String key, String value) {
            requestProperties.put(key, value);
        }

        @Override
        public String getRequestProperty(String key) {
            return requestProperties.get(key);
        }

        @Override
        public void disconnect() {
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void connect() throws IOException {
        }
    }

    @Test
    public void testIsSuccessfulResponseCode() {
        assertTrue(HttpUtil.isSuccessfulResponseCode(200));
        assertTrue(HttpUtil.isSuccessfulResponseCode(201));
        assertTrue(HttpUtil.isSuccessfulResponseCode(299));
        assertFalse(HttpUtil.isSuccessfulResponseCode(199));
        assertFalse(HttpUtil.isSuccessfulResponseCode(300));
        assertFalse(HttpUtil.isSuccessfulResponseCode(404));
        assertFalse(HttpUtil.isSuccessfulResponseCode(500));
    }

    @Test
    public void testIsValidHttpHeader() {
        assertTrue(HttpUtil.isValidHttpHeader("Content-Type", "application/json"));
        assertTrue(HttpUtil.isValidHttpHeader("Accept", "text/html"));
        assertFalse(HttpUtil.isValidHttpHeader("", "value"));
        assertFalse(HttpUtil.isValidHttpHeader(null, "value"));
        assertFalse(HttpUtil.isValidHttpHeader("key:colon", "value"));
        assertFalse(HttpUtil.isValidHttpHeader("key\nvalue", "value"));
        assertTrue(HttpUtil.isValidHttpHeader("key", null));
        assertTrue(HttpUtil.isValidHttpHeader("key", ""));
    }

    @Test
    public void testIsValidHttpHeaderWithLineFeed() {
        // RFC 7230: only CRLF (\r\n) is a valid obs-fold separator; a bare LF (\n)
        // must always be rejected to prevent header injection, even if followed by
        // a continuation space or tab.
        assertFalse(HttpUtil.isValidHttpHeader("key", "value\n continuation"));
        assertFalse(HttpUtil.isValidHttpHeader("key", "value\n\tcontinuation"));

        // Invalid: line feed not followed by space or tab
        assertFalse(HttpUtil.isValidHttpHeader("key", "value\nnoSpace"));
    }

    @Test
    public void testIsValidHttpHeaderWithCarriageReturn() {
        assertFalse(HttpUtil.isValidHttpHeader("key", "value\r\n continuation"));
        assertFalse(HttpUtil.isValidHttpHeader("key", "value\r\n\tcontinuation"));
        assertFalse(HttpUtil.isValidHttpHeader("key", "value\r"));
        assertFalse(HttpUtil.isValidHttpHeader("key", "value\rnext"));
    }

    // --- Bug fix: bare LF must be rejected to prevent header injection ---

    @Test
    public void testIsValidHttpHeader_bareLfRejected_securityFix() {
        // A bare LF (\n) without a preceding \r is invalid per RFC 7230 and must be
        // rejected even when followed by a continuation SP or HT, to prevent header injection.
        assertFalse(HttpUtil.isValidHttpHeader("X-Inject", "value\n X-Evil: injected"));
        assertFalse(HttpUtil.isValidHttpHeader("X-Inject", "value\n\tinjected"));
        assertFalse(HttpUtil.isValidHttpHeader("X-Inject", "value\nnoSpace"));
        // A lone LF at end of value must also be rejected.
        assertFalse(HttpUtil.isValidHttpHeader("X-Inject", "value\n"));
    }

    @Test
    public void testIsValidHttpHeader_crlfObsFoldRejectedForSending() {
        // Recipients may interpret legacy obs-fold, but RFC 7230 forbids senders from generating it.
        assertFalse(HttpUtil.isValidHttpHeader("key", "long value\r\n continuation"));
        assertFalse(HttpUtil.isValidHttpHeader("key", "long value\r\n\tcontinuation"));
    }

    @Test
    public void testIsValidHttpHeader_crlfWithoutFoldRejected() {
        // CRLF not followed by SP or HT is rejected.
        assertFalse(HttpUtil.isValidHttpHeader("key", "value\r\nnoFold"));
        // A bare CR is also rejected.
        assertFalse(HttpUtil.isValidHttpHeader("key", "value\r"));
        assertFalse(HttpUtil.isValidHttpHeader("key", "value\rX"));
    }

    @Test
    public void testIsValidHttpHeaderRejectsInvalidNameAndValueControls() {
        assertTrue(HttpUtil.isValidHttpHeader("X-Custom_123", "value\twith-tab"));
        assertFalse(HttpUtil.isValidHttpHeader("Bad Header", "value"));
        assertFalse(HttpUtil.isValidHttpHeader("Bad,Header", "value"));
        assertFalse(HttpUtil.isValidHttpHeader("Bad(Header)", "value"));
        assertFalse(HttpUtil.isValidHttpHeader("X-Control", "before\u0000after"));
        assertFalse(HttpUtil.isValidHttpHeader("X-Control", "before\u007fafter"));
    }

    @Test
    public void testReadHttpHeadValue() {
        assertNull(HttpUtil.readHttpHeaderValue(null));
        assertEquals("value", HttpUtil.readHttpHeaderValue("value"));
        assertEquals("123", HttpUtil.readHttpHeaderValue(123));

        // M1: readHttpHeaderValue now delegates to HttpHeaders.valueOf so the join separator
        // is single-sourced as ", " (RFC 7230 §3.2.2), matching the write path.
        List<String> list = Arrays.asList("val1", "val2", "val3");
        assertEquals("val1, val2, val3", HttpUtil.readHttpHeaderValue(list));

        List<String> emptyList = Arrays.asList();
        assertEquals("", HttpUtil.readHttpHeaderValue(emptyList));

        List<String> singleList = Arrays.asList("single");
        assertEquals("single", HttpUtil.readHttpHeaderValue(singleList));
    }

    @Test
    public void testReadHttpHeadValueWithMultipleValues() {
        List<Integer> numbers = Arrays.asList(1, 2, 3);
        assertEquals("1, 2, 3", HttpUtil.readHttpHeaderValue(numbers));

        List<Object> mixed = Arrays.asList("str", 123, true);
        assertEquals("str, 123, true", HttpUtil.readHttpHeaderValue(mixed));
    }

    @Test
    public void testReadHttpHeaderValueConsistentWithHttpHeadersValueOf() {
        // M1: the only difference between the two stringifiers is the null contract.
        assertNull(HttpUtil.readHttpHeaderValue(null));
        assertEquals("", HttpHeaders.valueOf(null));

        // Date/Instant are now HTTP-date formatted on the read path too (single-sourced).
        java.util.Date date = new java.util.Date(0L);
        assertEquals(HttpHeaders.valueOf(date), HttpUtil.readHttpHeaderValue(date));

        java.time.Instant instant = java.time.Instant.ofEpochMilli(0L);
        assertEquals(HttpHeaders.valueOf(instant), HttpUtil.readHttpHeaderValue(instant));

        // Multi-value join is identical.
        List<String> values = Arrays.asList("gzip", "deflate");
        assertEquals(HttpHeaders.valueOf(values), HttpUtil.readHttpHeaderValue(values));
    }

    @Test
    public void testGetContentTypeFromMap() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        assertEquals("application/json", HttpUtil.getContentType(headers));

        headers.clear();
        headers.put("content-type", "text/html");
        assertEquals("text/html", HttpUtil.getContentType(headers));

        headers.clear();
        headers.put("CONTENT-TYPE", "text/plain");
        assertEquals("text/plain", HttpUtil.getContentType(headers));

        assertNull(HttpUtil.getContentType((Map<String, Object>) null));
        assertNull(HttpUtil.getContentType(new HashMap<>()));
    }

    @Test
    public void testGetContentTypeFromHttpHeaders() {
        HttpHeaders headers = HttpHeaders.create();
        headers.set(HttpHeaders.Names.CONTENT_TYPE, "application/json");
        assertEquals("application/json", HttpUtil.getContentType(headers));

        headers.clear();
        headers.set("CONTENT-TYPE", "text/plain");
        assertEquals("text/plain", HttpUtil.getContentType(headers));

        assertNull(HttpUtil.getContentType((HttpHeaders) null));
        assertNull(HttpUtil.getContentType(HttpHeaders.create()));
    }

    @Test
    public void testGetContentTypeFromHttpSettings() {
        HttpSettings settings = HttpSettings.create();
        settings.header(HttpHeaders.Names.CONTENT_TYPE, "application/json");
        assertEquals("application/json", HttpUtil.getContentType(settings));

        assertNull(HttpUtil.getContentType((HttpSettings) null));
        assertNull(HttpUtil.getContentType(HttpSettings.create()));
    }

    @Test
    public void testGetContentType_ContentFormat() {
        assertEquals("", HttpUtil.getContentType((ContentFormat) null));
        assertEquals("", HttpUtil.getContentType(ContentFormat.NONE));

        assertEquals("application/json", HttpUtil.getContentType(ContentFormat.JSON));
        assertEquals("application/json", HttpUtil.getContentType(ContentFormat.JSON_GZIP));
        assertEquals("application/xml", HttpUtil.getContentType(ContentFormat.XML));
        assertEquals("application/xml", HttpUtil.getContentType(ContentFormat.XML_LZ4));
        assertEquals("application/x-www-form-urlencoded", HttpUtil.getContentType(ContentFormat.FORM_URL_ENCODED));
        // KRYO.contentType() is "" by design (per owner); getContentType(ContentFormat) returns the enum's
        // contentType(), so getContentType(KRYO) is "". The "application/kryo" wire type lives only in
        // HttpUtil.contentFormat2Type. See M3 RESOLVED — WON'T-FIX (by design).
        assertEquals("", HttpUtil.getContentType(ContentFormat.KRYO));
    }

    @Test
    public void testGetContentTypeFromConnection() throws IOException {
        MockHttpURLConnection connection = new MockHttpURLConnection();
        assertNull(HttpUtil.getContentType(connection));

        Map<String, List<String>> headerFields = new HashMap<>();
        headerFields.put("Content-Type", Arrays.asList("application/json"));
        connection.setHeaderFields(headerFields);

        assertEquals("application/json", HttpUtil.getContentType(connection));
    }

    @Test
    public void testGetContentEncodingFromMap() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("Content-Encoding", "gzip");
        assertEquals("gzip", HttpUtil.getContentEncoding(headers));

        headers.clear();
        headers.put("content-encoding", "deflate");
        assertEquals("deflate", HttpUtil.getContentEncoding(headers));

        headers.clear();
        headers.put("CONTENT-ENCODING", "br");
        assertEquals("br", HttpUtil.getContentEncoding(headers));

        assertNull(HttpUtil.getContentEncoding((Map<String, Object>) null));
        assertNull(HttpUtil.getContentEncoding(new HashMap<>()));
    }

    @Test
    public void testGetContentEncodingFromHttpHeaders() {
        HttpHeaders headers = HttpHeaders.create();
        headers.set(HttpHeaders.Names.CONTENT_ENCODING, "gzip");
        assertEquals("gzip", HttpUtil.getContentEncoding(headers));

        headers.clear();
        headers.set("CONTENT-ENCODING", "br");
        assertEquals("br", HttpUtil.getContentEncoding(headers));

        assertNull(HttpUtil.getContentEncoding((HttpHeaders) null));
        assertNull(HttpUtil.getContentEncoding(HttpHeaders.create()));
    }

    @Test
    public void testGetContentEncodingFromHttpSettings() {
        assertNull(HttpUtil.getContentEncoding((HttpSettings) null));

        HttpSettings settings = HttpSettings.create();
        assertNull(HttpUtil.getContentEncoding(settings));

        settings.setContentEncoding("snappy");
        assertEquals("snappy", HttpUtil.getContentEncoding(settings));
    }

    @Test
    public void testGetContentEncoding_ContentFormat() {
        assertEquals("", HttpUtil.getContentEncoding((ContentFormat) null));
        assertEquals("", HttpUtil.getContentEncoding(ContentFormat.NONE));

        assertEquals("", HttpUtil.getContentEncoding(ContentFormat.JSON));
        assertEquals("gzip", HttpUtil.getContentEncoding(ContentFormat.JSON_GZIP));
        assertEquals("br", HttpUtil.getContentEncoding(ContentFormat.JSON_BR));
        assertEquals("snappy", HttpUtil.getContentEncoding(ContentFormat.XML_SNAPPY));
        assertEquals("lz4", HttpUtil.getContentEncoding(ContentFormat.XML_LZ4));
        assertEquals("kryo", HttpUtil.getContentEncoding(ContentFormat.KRYO));
    }

    @Test
    public void testGetContentEncodingFromConnection() throws IOException {
        MockHttpURLConnection connection = new MockHttpURLConnection();
        assertNull(HttpUtil.getContentEncoding(connection));

        Map<String, List<String>> headerFields = new HashMap<>();
        headerFields.put("Content-Encoding", Arrays.asList("lz4"));
        connection.setHeaderFields(headerFields);

        assertEquals("lz4", HttpUtil.getContentEncoding(connection));
    }

    @Test
    public void testGetAcceptFromMap() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        assertEquals("application/json", HttpUtil.getAccept(headers));

        assertNull(HttpUtil.getAccept((Map<String, Object>) null));
        assertNull(HttpUtil.getAccept(new HashMap<>()));
    }

    @Test
    public void testGetAcceptFromHttpHeaders() {
        assertNull(HttpUtil.getAccept((HttpHeaders) null));

        HttpHeaders headers = HttpHeaders.create();
        assertNull(HttpUtil.getAccept(headers));

        headers.setAccept("application/xml");
        assertEquals("application/xml", HttpUtil.getAccept(headers));

        headers.clear();
        headers.set("ACCEPT", "application/json");
        assertEquals("application/json", HttpUtil.getAccept(headers));
    }

    @Test
    public void testGetAcceptFromHttpSettings() {
        assertNull(HttpUtil.getAccept((HttpSettings) null));

        HttpSettings settings = HttpSettings.create();
        assertNull(HttpUtil.getAccept(settings));

        settings.header("Accept", "text/html");
        assertEquals("text/html", HttpUtil.getAccept(settings));
    }

    @Test
    public void testGetAcceptFromConnection() throws IOException {
        MockHttpURLConnection connection = new MockHttpURLConnection();
        assertNull(HttpUtil.getAccept(connection));

        Map<String, List<String>> headerFields = new HashMap<>();
        headerFields.put("Accept", Arrays.asList("*/*"));
        connection.setHeaderFields(headerFields);

        assertEquals("*/*", HttpUtil.getAccept(connection));
    }

    @Test
    public void testGetAcceptEncodingFromMap() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("Accept-Encoding", "gzip, deflate");
        assertEquals("gzip, deflate", HttpUtil.getAcceptEncoding(headers));

        assertNull(HttpUtil.getAcceptEncoding((Map<String, Object>) null));
    }

    @Test
    public void testGetAcceptEncodingFromHttpHeaders() {
        assertNull(HttpUtil.getAcceptEncoding((HttpHeaders) null));

        HttpHeaders headers = HttpHeaders.create();
        assertNull(HttpUtil.getAcceptEncoding(headers));

        headers.setAcceptEncoding("gzip");
        assertEquals("gzip", HttpUtil.getAcceptEncoding(headers));

        headers.clear();
        headers.set("ACCEPT-ENCODING", "br");
        assertEquals("br", HttpUtil.getAcceptEncoding(headers));
    }

    @Test
    public void testGetAcceptEncodingFromHttpSettings() {
        assertNull(HttpUtil.getAcceptEncoding((HttpSettings) null));

        HttpSettings settings = HttpSettings.create();
        assertNull(HttpUtil.getAcceptEncoding(settings));

        settings.header("Accept-Encoding", "deflate");
        assertEquals("deflate", HttpUtil.getAcceptEncoding(settings));
    }

    @Test
    public void testGetAcceptEncodingFromConnection() throws IOException {
        MockHttpURLConnection connection = new MockHttpURLConnection();
        assertNull(HttpUtil.getAcceptEncoding(connection));

        Map<String, List<String>> headerFields = new HashMap<>();
        headerFields.put("Accept-Encoding", Arrays.asList("*"));
        connection.setHeaderFields(headerFields);

        assertEquals("*", HttpUtil.getAcceptEncoding(connection));
    }

    @Test
    public void testGetAcceptCharsetFromMap() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("Accept-Charset", "utf-8");
        assertEquals("utf-8", HttpUtil.getAcceptCharset(headers));

        assertNull(HttpUtil.getAcceptCharset((Map<String, Object>) null));
    }

    @Test
    public void testGetAcceptCharsetFromHttpHeaders() {
        assertNull(HttpUtil.getAcceptCharset((HttpHeaders) null));

        HttpHeaders headers = HttpHeaders.create();
        assertNull(HttpUtil.getAcceptCharset(headers));

        headers.setAcceptCharset("utf-16");
        assertEquals("utf-16", HttpUtil.getAcceptCharset(headers));

        headers.clear();
        headers.set("ACCEPT-CHARSET", "utf-8");
        assertEquals("utf-8", HttpUtil.getAcceptCharset(headers));
    }

    @Test
    public void testGetAcceptCharsetFromHttpSettings() {
        assertNull(HttpUtil.getAcceptCharset((HttpSettings) null));

        HttpSettings settings = HttpSettings.create();
        assertNull(HttpUtil.getAcceptCharset(settings));

        settings.header("Accept-Charset", "us-ascii");
        assertEquals("us-ascii", HttpUtil.getAcceptCharset(settings));
    }

    @Test
    public void testGetAcceptCharsetFromConnection() throws IOException {
        MockHttpURLConnection connection = new MockHttpURLConnection();
        assertNull(HttpUtil.getAcceptCharset(connection));

        Map<String, List<String>> headerFields = new HashMap<>();
        headerFields.put("Accept-Charset", Arrays.asList("*"));
        connection.setHeaderFields(headerFields);

        assertEquals("*", HttpUtil.getAcceptCharset(connection));
    }

    @Test
    public void testGetContentFormat() {
        assertEquals(ContentFormat.JSON, HttpUtil.getContentFormat("application/json", null));
        assertEquals(ContentFormat.JSON_GZIP, HttpUtil.getContentFormat("application/json", "gzip"));
        assertEquals(ContentFormat.XML, HttpUtil.getContentFormat("application/xml", null));
        assertEquals(ContentFormat.XML_GZIP, HttpUtil.getContentFormat("application/xml", "gzip"));
        assertEquals(ContentFormat.FORM_URL_ENCODED, HttpUtil.getContentFormat("application/x-www-form-urlencoded", null));
        assertEquals(ContentFormat.GZIP, HttpUtil.getContentFormat(null, "gzip"));
        assertEquals(ContentFormat.NONE, HttpUtil.getContentFormat(null, null));
    }

    @Test
    public void testGetContentFormatWithMediaSubtypeMatching() {
        assertEquals(ContentFormat.JSON, HttpUtil.getContentFormat("application/json; charset=UTF-8", null));
        assertEquals(ContentFormat.XML, HttpUtil.getContentFormat("text/xml; charset=UTF-8", null));
        assertEquals(ContentFormat.JSON_GZIP, HttpUtil.getContentFormat("something/json", "gzip"));
        assertEquals(ContentFormat.XML_SNAPPY, HttpUtil.getContentFormat("something/xml", "snappy"));
        assertEquals(ContentFormat.JSON, HttpUtil.getContentFormat("application/problem+json", null));

        // A format name must occupy the whole subtype or a recognized subtype suffix.
        assertEquals(ContentFormat.NONE, HttpUtil.getContentFormat("application/jsonp", null));
        assertEquals(ContentFormat.NONE, HttpUtil.getContentFormat("application/notjson", null));
        assertEquals(ContentFormat.NONE, HttpUtil.getContentFormat("application/not-json", null));
    }

    @Test
    public void testGetContentFormatWithEdgeCases() {
        // Empty strings
        assertEquals(ContentFormat.NONE, HttpUtil.getContentFormat("", ""));

        // Unknown content type with known encoding
        assertEquals(ContentFormat.GZIP, HttpUtil.getContentFormat("unknown/type", "gzip"));
        assertEquals(ContentFormat.LZ4, HttpUtil.getContentFormat("unknown/type", "lz4"));
        assertEquals(ContentFormat.SNAPPY, HttpUtil.getContentFormat("unknown/type", "snappy"));

        // Content codings are tokens, not substrings. In particular, "zebra" must not
        // accidentally select Brotli merely because it contains the letters "br".
        assertEquals(ContentFormat.NONE, HttpUtil.getContentFormat("unknown/type", "zebra"));
        assertEquals(ContentFormat.JSON, HttpUtil.getContentFormat("application/json", "zebra"));
        assertEquals(ContentFormat.JSON_GZIP, HttpUtil.getContentFormat("application/json", "x-gzip"));
        assertEquals(ContentFormat.JSON_BR, HttpUtil.getContentFormat("application/json", "unknown, BR"));
        assertEquals(ContentFormat.JSON_GZIP, HttpUtil.getContentFormat("application/json", "br, gzip"));
        assertEquals(ContentFormat.JSON, HttpUtil.getContentFormat("application/json", "zebra, unknown"));

        // Case insensitive matching
        assertEquals(ContentFormat.JSON_GZIP, HttpUtil.getContentFormat("application/JSON", "GZIP"));
    }

    @Test
    public void testGetContentFormatWithKryoAndUrlEncoded() {
        assertEquals(ContentFormat.KRYO, HttpUtil.getContentFormat("application/x-kryo", null));
        assertEquals(ContentFormat.NONE, HttpUtil.getContentFormat("application/not-x-kryo", null));
        assertEquals(ContentFormat.FORM_URL_ENCODED, HttpUtil.getContentFormat("application/x-www-form-urlencoded", null));
        assertEquals(ContentFormat.FORM_URL_ENCODED, HttpUtil.getContentFormat("application/x-www-form-urlencoded", ""));

        // Kryo with encoding
        assertEquals(ContentFormat.NONE, HttpUtil.getContentFormat("unknown/type", "unknown"));

        // BR encoding
        assertEquals(ContentFormat.JSON_BR, HttpUtil.getContentFormat("application/json", "br"));
        assertEquals(ContentFormat.XML_BR, HttpUtil.getContentFormat("application/xml", "br"));
    }

    @Test
    public void testGetContentFormatFromConnection() throws IOException {
        MockHttpURLConnection connection = new MockHttpURLConnection();
        assertEquals(ContentFormat.NONE, HttpUtil.getContentFormat(connection));

        Map<String, List<String>> headerFields = new HashMap<>();
        headerFields.put("Content-Type", Arrays.asList("application/json"));
        headerFields.put("Content-Encoding", Arrays.asList("gzip"));
        connection.setHeaderFields(headerFields);

        assertEquals(ContentFormat.JSON_GZIP, HttpUtil.getContentFormat(connection));
    }

    @Test
    public void testGetResponseContentFormat() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        assertEquals(ContentFormat.JSON, HttpUtil.getResponseContentFormat(headers, null));

        headers.put("Content-Encoding", "gzip");
        assertEquals(ContentFormat.JSON_GZIP, HttpUtil.getResponseContentFormat(headers, null));

        // Fallback to request format when no content type in response
        headers.clear();
        assertEquals(ContentFormat.XML, HttpUtil.getResponseContentFormat(headers, ContentFormat.XML));
    }

    @Test
    public void testGetResponseContentFormatWithNullHeaders() {
        assertEquals(ContentFormat.XML, HttpUtil.getResponseContentFormat(null, ContentFormat.XML));
        assertEquals(ContentFormat.NONE, HttpUtil.getResponseContentFormat(null, null));
    }

    @Test
    public void testGetParser() {
        assertNotNull(HttpUtil.getParser(ContentFormat.JSON));
        assertNotNull(HttpUtil.getParser(ContentFormat.XML));
        assertNotNull(HttpUtil.getParser(ContentFormat.KRYO));
        assertNotNull(HttpUtil.getParser(null));
        assertNotNull(HttpUtil.getParser(ContentFormat.NONE));
        assertNotNull(HttpUtil.getParser(ContentFormat.FORM_URL_ENCODED));
        assertNotNull(HttpUtil.getParser(ContentFormat.GZIP));
        assertNotNull(HttpUtil.getParser(ContentFormat.LZ4));
        assertNotNull(HttpUtil.getParser(ContentFormat.SNAPPY));
    }

    @Test
    public void testWrapInputStream() throws IOException {
        byte[] data = new byte[0];
        ByteArrayInputStream bais = new ByteArrayInputStream(data);

        assertNotNull(HttpUtil.wrapInputStream(null, ContentFormat.JSON));
        assertSame(bais, HttpUtil.wrapInputStream(bais, null));
        assertSame(bais, HttpUtil.wrapInputStream(bais, ContentFormat.NONE));
        assertSame(bais, HttpUtil.wrapInputStream(bais, ContentFormat.JSON));
        assertSame(bais, HttpUtil.wrapInputStream(bais, ContentFormat.XML));
        assertSame(bais, HttpUtil.wrapInputStream(bais, ContentFormat.FORM_URL_ENCODED));

    }

    @Test
    public void testWrapInputStreamWithCompression() throws IOException {
        // Test GZIP wrap/unwrap round-trip
        ByteArrayOutputStream gzipBaos = new ByteArrayOutputStream();
        OutputStream gzipOs = HttpUtil.wrapOutputStream(gzipBaos, ContentFormat.GZIP);
        gzipOs.write("hello gzip".getBytes());
        HttpUtil.flush(gzipOs);
        gzipOs.close();
        byte[] compressed = gzipBaos.toByteArray();
        assertTrue(compressed.length > 0);

        InputStream gzipIs = HttpUtil.wrapInputStream(new ByteArrayInputStream(compressed), ContentFormat.GZIP);
        assertNotNull(gzipIs);
        byte[] decompressed = gzipIs.readAllBytes();
        assertEquals("hello gzip", new String(decompressed));
        gzipIs.close();

        // Test LZ4 wrap/unwrap round-trip
        ByteArrayOutputStream lz4Baos = new ByteArrayOutputStream();
        OutputStream lz4Os = HttpUtil.wrapOutputStream(lz4Baos, ContentFormat.LZ4);
        lz4Os.write("hello lz4".getBytes());
        HttpUtil.flush(lz4Os);
        lz4Os.close();
        byte[] lz4Compressed = lz4Baos.toByteArray();
        assertTrue(lz4Compressed.length > 0);

        InputStream lz4Is = HttpUtil.wrapInputStream(new ByteArrayInputStream(lz4Compressed), ContentFormat.LZ4);
        assertNotNull(lz4Is);
        byte[] lz4Decompressed = lz4Is.readAllBytes();
        assertEquals("hello lz4", new String(lz4Decompressed));
        lz4Is.close();

        // Test Snappy wrap/unwrap round-trip
        ByteArrayOutputStream snappyBaos = new ByteArrayOutputStream();
        OutputStream snappyOs = HttpUtil.wrapOutputStream(snappyBaos, ContentFormat.SNAPPY);
        snappyOs.write("hello snappy".getBytes());
        HttpUtil.flush(snappyOs);
        snappyOs.close();
        byte[] snappyCompressed = snappyBaos.toByteArray();
        assertTrue(snappyCompressed.length > 0);

        InputStream snappyIs = HttpUtil.wrapInputStream(new ByteArrayInputStream(snappyCompressed), ContentFormat.SNAPPY);
        assertNotNull(snappyIs);
        byte[] snappyDecompressed = snappyIs.readAllBytes();
        assertEquals("hello snappy", new String(snappyDecompressed));
        snappyIs.close();
    }

    @Test
    public void testWrapOutputStreamWithNullAndNone() {
        assertNull(HttpUtil.wrapOutputStream(null, ContentFormat.JSON));
        assertNull(HttpUtil.wrapOutputStream(null, ContentFormat.NONE));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        assertSame(baos, HttpUtil.wrapOutputStream(baos, null));
        assertSame(baos, HttpUtil.wrapOutputStream(baos, ContentFormat.NONE));
    }

    @Test
    public void testWrapOutputStream() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStream os = HttpUtil.wrapOutputStream(baos, ContentFormat.JSON);
        assertNotNull(os);

        assertNull(HttpUtil.wrapOutputStream(null, ContentFormat.JSON));

        ByteArrayOutputStream gzipBaos = new ByteArrayOutputStream();
        OutputStream gzipOs = HttpUtil.wrapOutputStream(gzipBaos, ContentFormat.JSON_GZIP);
        assertNotNull(gzipOs);

        assertThrows(UnsupportedOperationException.class, () -> HttpUtil.wrapOutputStream(new ByteArrayOutputStream(), ContentFormat.JSON_BR));
    }

    @Test
    public void testWrapOutputStreamWithDifferentEncodings() throws IOException {
        // Test LZ4
        ByteArrayOutputStream lz4Baos = new ByteArrayOutputStream();
        OutputStream lz4Os = HttpUtil.wrapOutputStream(lz4Baos, ContentFormat.JSON_LZ4);
        assertNotNull(lz4Os);
        lz4Os.write("test".getBytes());
        HttpUtil.flush(lz4Os);
        lz4Os.close();
        assertTrue(lz4Baos.size() > 0);

        // Test Snappy
        ByteArrayOutputStream snappyBaos = new ByteArrayOutputStream();
        OutputStream snappyOs = HttpUtil.wrapOutputStream(snappyBaos, ContentFormat.JSON_SNAPPY);
        assertNotNull(snappyOs);
        snappyOs.write("test".getBytes());
        HttpUtil.flush(snappyOs);
        snappyOs.close();
        assertTrue(snappyBaos.size() > 0);
    }

    @Test
    public void testGetOutputStream() throws IOException {
        MockHttpURLConnection connection = new MockHttpURLConnection();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        connection.setOutputStream(baos);

        OutputStream os = HttpUtil.getOutputStream(connection, ContentFormat.JSON, null, null);
        assertNotNull(os);
        assertEquals("application/json", connection.getRequestProperty("Content-Type"));
        assertNull(connection.getRequestProperty("Content-Encoding"));

        connection = new MockHttpURLConnection();
        connection.setOutputStream(new ByteArrayOutputStream());
        os = HttpUtil.getOutputStream(connection, ContentFormat.JSON_GZIP, null, null);
        assertNotNull(os);
        assertEquals("application/json", connection.getRequestProperty("Content-Type"));
        assertEquals("gzip", connection.getRequestProperty("Content-Encoding"));

        connection = new MockHttpURLConnection();
        connection.setOutputStream(new ByteArrayOutputStream());
        os = HttpUtil.getOutputStream(connection, null, "text/plain", "deflate");
        assertNotNull(os);
        assertEquals("text/plain", connection.getRequestProperty("Content-Type"));
        assertEquals("deflate", connection.getRequestProperty("Content-Encoding"));
    }

    @Test
    public void testGetOutputStreamWithNullContentFormat() throws IOException {
        MockHttpURLConnection connection = new MockHttpURLConnection();
        connection.setOutputStream(new ByteArrayOutputStream());
        OutputStream os = HttpUtil.getOutputStream(connection, null, null, null);
        assertNotNull(os);
    }

    @Test
    public void testGetInputStream() throws IOException {
        MockHttpURLConnection connection = new MockHttpURLConnection();
        connection.setInputStream(new ByteArrayInputStream("test".getBytes()));

        InputStream is = HttpUtil.getInputStream(connection, ContentFormat.JSON);
        assertNotNull(is);

        // Test error stream fallback
        connection = new MockHttpURLConnection();
        connection.setErrorStream(new ByteArrayInputStream("error".getBytes()));
        connection.setThrowOnGetInputStream(true);

        is = HttpUtil.getInputStream(connection, ContentFormat.JSON);
        assertNotNull(is);
        assertEquals("error", new String(is.readAllBytes()));
    }

    @Test
    public void testGetInputStreamWithNullStreams() throws IOException {
        MockHttpURLConnection connection = new MockHttpURLConnection();
        connection.setThrowOnGetInputStream(true);
        // Both input and error streams are null: preserve the original I/O failure
        // (do not silently return an empty body that hides the root cause).
        final com.landawn.abacus.exception.UncheckedIOException ex = assertThrows(com.landawn.abacus.exception.UncheckedIOException.class,
                () -> HttpUtil.getInputStream(connection, ContentFormat.JSON));
        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof IOException);
    }

    @Test
    public void testFlush() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        HttpUtil.flush(baos);

        // Test with GZIP stream
        ByteArrayOutputStream gzipBaos = new ByteArrayOutputStream();
        OutputStream gzipOs = IOUtil.newGZIPOutputStream(gzipBaos);
        gzipOs.write("test".getBytes());
        HttpUtil.flush(gzipOs);
        assertTrue(gzipBaos.size() > 0);
    }

    @Test
    public void testFlushWithLZ4Stream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStream lz4Os = HttpUtil.wrapOutputStream(baos, ContentFormat.LZ4);
        lz4Os.write("test data for lz4".getBytes());
        HttpUtil.flush(lz4Os);
        lz4Os.close();
        assertTrue(baos.size() > 0);
    }

    @Test
    public void testGetRequestCharset() {
        HttpHeaders headers = HttpHeaders.create();
        headers.set(HttpHeaders.Names.CONTENT_TYPE, "application/json; charset=ISO-8859-1");
        Charset charset = HttpUtil.getRequestCharset(headers);
        assertEquals(Charset.forName("ISO-8859-1"), charset);

        assertEquals(HttpUtil.DEFAULT_CHARSET, HttpUtil.getRequestCharset(HttpHeaders.create()));
    }

    @Test
    public void testGetResponseCharset() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("Content-Type", "text/html; charset=ISO-8859-1");
        Charset charset = HttpUtil.getResponseCharset(headers, Charsets.UTF_8);
        assertEquals(Charset.forName("ISO-8859-1"), charset);

        assertEquals(Charsets.UTF_8, HttpUtil.getResponseCharset(new HashMap<>(), Charsets.UTF_8));
    }

    @Test
    public void testGetCharset() {
        Charset utf8 = HttpUtil.getCharset("text/html; charset=UTF-8");
        assertEquals(StandardCharsets.UTF_8, utf8);

        Charset iso = HttpUtil.getCharset("text/html; charset=ISO-8859-1");
        assertEquals(Charset.forName("ISO-8859-1"), iso);

        assertEquals(Charsets.UTF_8, HttpUtil.getCharset(null));
        assertEquals(Charsets.UTF_8, HttpUtil.getCharset(""));
        assertEquals(Charsets.UTF_8, HttpUtil.getCharset("text/html"));

        Charset defaultCharset = Charsets.ISO_8859_1;
        assertEquals(defaultCharset, HttpUtil.getCharset(null, defaultCharset));
        assertEquals(defaultCharset, HttpUtil.getCharset("", defaultCharset));
    }

    @Test
    public void testGetCharsetWithComplexContentType() {
        Charset charset = HttpUtil.getCharset("text/html; charset=UTF-8; boundary=something");
        assertEquals(StandardCharsets.UTF_8, charset);

        Charset charset2 = HttpUtil.getCharset("application/json;charset=ISO-8859-1");
        assertEquals(Charset.forName("ISO-8859-1"), charset2);
    }

    @Test
    public void testGetCharsetWithDefault() {
        Charset defaultCharset = StandardCharsets.ISO_8859_1;
        assertEquals(defaultCharset, HttpUtil.getCharset(null, defaultCharset));
        assertEquals(defaultCharset, HttpUtil.getCharset("", defaultCharset));
        assertEquals(defaultCharset, HttpUtil.getCharset("application/json", defaultCharset));

        assertEquals(StandardCharsets.UTF_8, HttpUtil.getCharset("application/json; charset=UTF-8", defaultCharset));
    }

    @Test
    public void testGetCharsetWithQuotedValue() {
        Charset charset = HttpUtil.getCharset("text/html; charset=\"UTF-8\"");
        assertEquals(StandardCharsets.UTF_8, charset);

        Charset charset2 = HttpUtil.getCharset("text/html; charset='UTF-8'");
        assertEquals(StandardCharsets.UTF_8, charset2);

        // charset= with no value after (edge case)
        Charset defaultCharset = HttpUtil.getCharset("text/html; charset");
        assertEquals(Charsets.UTF_8, defaultCharset);
    }

    @Test
    public void testGetCharsetWithInvalidCharset() {
        Charset defaultCharset = Charsets.UTF_8;
        Charset result = HttpUtil.getCharset("text/html; charset=INVALID_CHARSET_NAME_XYZ", defaultCharset);
        assertEquals(defaultCharset, result);
    }

    @Test
    public void testGetCharsetIgnoresCharsetSubstringInOtherParameter() {
        // A non-charset parameter whose name merely contains "charset" must not be
        // mistaken for the real charset parameter that follows it.
        Charset result = HttpUtil.getCharset("text/html; x-charset-hint=foo; charset=ISO-8859-1", Charsets.UTF_8);
        assertEquals(Charset.forName("ISO-8859-1"), result);

        // "charset" only appearing inside another token (no real charset param) -> default.
        assertEquals(Charsets.UTF_8, HttpUtil.getCharset("application/x-charset-test", Charsets.UTF_8));

        // Real charset parameter still resolved when preceded by an unrelated token.
        assertEquals(StandardCharsets.UTF_8, HttpUtil.getCharset("multipart/form-data; mycharsetparam=1; charset=UTF-8"));

        // '+' and the other RFC tchar punctuation are valid inside a parameter name. The old
        // partial token predicate treated the embedded suffix as a real charset parameter.
        assertEquals(Charsets.UTF_8, HttpUtil.getCharset("text/plain; vendor+charset=ISO-8859-1", Charsets.UTF_8));

        // A media-type component is not a parameter merely because '/' precedes "charset".
        assertEquals(Charsets.UTF_8, HttpUtil.getCharset("text/charset=UTF-16", Charsets.UTF_8));
        assertEquals(Charsets.UTF_8, HttpUtil.getCharset("application/charset=UTF-16", Charsets.UTF_8));

        // Text inside a quoted value is data, not another parameter declaration.
        assertEquals(Charset.forName("ISO-8859-1"), HttpUtil.getCharset("text/plain; note=\"charset=UTF-16\"; charset=ISO-8859-1", Charsets.UTF_8));
        assertEquals(Charset.forName("ISO-8859-1"),
                HttpUtil.getCharset("text/plain; note=\"escaped\\\"; charset=UTF-16\"; charset=ISO-8859-1", Charsets.UTF_8));
    }

    @Test
    public void testTurnOffCertificateValidation() {
        assertDoesNotThrow(() -> {
            // This test can only verify that the method doesn't throw an exception
            // Actually testing the certificate validation would require HTTPS connections
            HttpUtil.disableCertificateValidation();
        });
    }

    @Test
    public void testHttpDateParse() {
        String dateStr = "Wed, 21 Oct 2015 07:28:00 GMT";
        Date date = HttpUtil.HttpDate.parse(dateStr);
        assertNotNull(date);

        assertNull(HttpUtil.HttpDate.parse(""));
        assertNull(HttpUtil.HttpDate.parse("invalid date"));
    }

    @Test
    public void testHttpDateFormat() {
        Date date = new Date(1445412480000L); // Wed, 21 Oct 2015 07:28:00 GMT
        String formatted = HttpUtil.HttpDate.format(date);
        assertNotNull(formatted);
        assertTrue(formatted.contains("GMT"));
    }

    @Test
    public void testHttpDateConstants() {
        assertNotNull(HttpUtil.HttpDate.UTC);
        assertEquals("GMT", HttpUtil.HttpDate.UTC.getID());
        assertEquals(253_402_300_799_999L, HttpUtil.HttpDate.MAX_DATE);
    }

    @Test
    public void testConstants() {
        assertEquals(Charsets.UTF_8, HttpUtil.DEFAULT_CHARSET);
        assertEquals(ContentFormat.JSON, HttpUtil.DEFAULT_CONTENT_FORMAT);
    }

    @Test
    public void testHttpDateParseWithVariousFormats() {
        // RFC 1123 format
        assertNotNull(HttpUtil.HttpDate.parse("Sun, 06 Nov 1994 08:49:37 GMT"));

        // RFC 850 format
        assertNotNull(HttpUtil.HttpDate.parse("Sunday, 06-Nov-94 08:49:37 GMT"));

        // ANSI C asctime() format
        assertNotNull(HttpUtil.HttpDate.parse("Sun Nov  6 08:49:37 1994"));
    }

    // ------------------------------------------------------------------------------------------------
    // a07 F-1: DEFAULT_EXECUTOR must run on daemon threads, otherwise one async call pins the JVM.
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testDefaultExecutorRunsTasksOnDaemonNormalPriorityThreads() throws Exception {
        final CompletableFuture<Thread> worker = new CompletableFuture<>();

        HttpUtil.DEFAULT_EXECUTOR.execute(() -> worker.complete(Thread.currentThread()));

        final Thread thread = worker.get(10, TimeUnit.SECONDS);
        assertTrue(thread.isDaemon(), "DEFAULT_EXECUTOR worker must be a daemon thread: " + thread);
        assertEquals(Thread.NORM_PRIORITY, thread.getPriority());
        assertTrue(thread.getName().startsWith("abacus-http-async-"), thread.getName());
    }

    @Test
    public void testDefaultAsyncExecutorRunsTasksOnDaemonNormalPriorityThreads() throws Exception {
        final CompletableFuture<Thread> worker = new CompletableFuture<>();

        HttpUtil.DEFAULT_ASYNC_EXECUTOR.execute(() -> worker.complete(Thread.currentThread())).get(10, TimeUnit.SECONDS);

        final Thread thread = worker.get(10, TimeUnit.SECONDS);
        assertTrue(thread.isDaemon(), "DEFAULT_ASYNC_EXECUTOR worker must be a daemon thread: " + thread);
        assertEquals(Thread.NORM_PRIORITY, thread.getPriority());
        assertTrue(thread.getName().startsWith("abacus-http-async-"), thread.getName());
    }

    @Test
    public void testDefaultExecutorAllowsCoreThreadTimeOut() {
        assertTrue(HttpUtil.DEFAULT_EXECUTOR instanceof ThreadPoolExecutor);
        assertTrue(((ThreadPoolExecutor) HttpUtil.DEFAULT_EXECUTOR).allowsCoreThreadTimeOut(),
                "idle core workers must be allowed to time out so an idle pool releases its threads");
    }

    /**
     * Entry point for the forked-JVM test below: uses the default executor once and returns from
     * {@code main}. The JVM must then exit on its own, without {@code System.exit}.
     */
    public static class DefaultExecutorExitMain {
        public static void main(final String[] args) throws Exception {
            final CountDownLatch ran = new CountDownLatch(1);

            HttpUtil.DEFAULT_EXECUTOR.execute(ran::countDown);

            if (!ran.await(10, TimeUnit.SECONDS)) {
                System.out.println("TASK-NOT-RUN");
                System.exit(3);
            }

            System.out.println("MAIN-END");
            // Return normally: with daemon workers the JVM terminates; with non-daemon ones it hangs.
        }
    }

    @Test
    public void testJvmExitsNaturallyAfterUsingDefaultExecutor() throws Exception {
        final List<String> command = new ArrayList<>();
        command.add(Paths.get(System.getProperty("java.home"), "bin", "java").toString());

        // Carry the module-access flags of this JVM over to the child so class initialisation behaves the same.
        final List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();

        for (int i = 0; i < jvmArgs.size(); i++) {
            final String arg = jvmArgs.get(i);

            if (arg.startsWith("--add-opens") || arg.startsWith("--add-exports")) {
                command.add(arg);

                if ((arg.equals("--add-opens") || arg.equals("--add-exports")) && i + 1 < jvmArgs.size()) {
                    command.add(jvmArgs.get(++i));
                }
            }
        }

        command.add("-Xmx256m");
        command.add(DefaultExecutorExitMain.class.getName());

        final ProcessBuilder processBuilder = new ProcessBuilder(command);
        // The class path is passed through the environment to stay clear of the command-line length limit.
        processBuilder.environment().put("CLASSPATH", System.getProperty("java.class.path"));
        processBuilder.redirectErrorStream(true);

        final File log = File.createTempFile("abacus-http-executor-exit", ".log");
        processBuilder.redirectOutput(log);

        final Process child = processBuilder.start();

        try {
            final boolean exited = child.waitFor(20, TimeUnit.SECONDS);
            final String output = new String(Files.readAllBytes(log.toPath()), StandardCharsets.UTF_8);

            assertTrue(exited, "child JVM did not exit within 20 s after main returned (non-daemon pool workers keep it alive):\n" + output);
            assertEquals(0, child.exitValue(), output);
            assertTrue(output.contains("MAIN-END"), output);
        } finally {
            if (child.isAlive()) {
                child.destroyForcibly();
                child.waitFor(10, TimeUnit.SECONDS);
            }

            log.delete();
        }
    }

    // ------------------------------------------------------------------------------------------------
    // a07 F-2: getInputStream must close the stream it opened when the decoder cannot be constructed.
    // ------------------------------------------------------------------------------------------------

    private static final class TrackingInputStream extends ByteArrayInputStream {
        volatile boolean closed;

        TrackingInputStream(final byte[] bytes) {
            super(bytes);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    @Test
    public void testGetInputStreamClosesRawStreamWhenCompressedBodyIsEmpty() throws IOException {
        final MockHttpURLConnection connection = new MockHttpURLConnection();
        final TrackingInputStream raw = new TrackingInputStream(new byte[0]);
        connection.setInputStream(raw);

        assertThrows(UncheckedIOException.class, () -> HttpUtil.getInputStream(connection, ContentFormat.JSON_GZIP));
        assertTrue(raw.closed, "the successfully opened raw stream must be closed when the GZIP decoder cannot be built");
    }

    @Test
    public void testGetInputStreamClosesErrorStreamWhenCompressedErrorBodyIsMalformed() throws IOException {
        final MockHttpURLConnection connection = new MockHttpURLConnection();
        connection.setThrowOnGetInputStream(true);
        final TrackingInputStream errorStream = new TrackingInputStream(new byte[] { 1, 2, 3 });
        connection.setErrorStream(errorStream);

        final RuntimeException ex = assertThrows(RuntimeException.class, () -> HttpUtil.getInputStream(connection, ContentFormat.JSON_GZIP));

        assertTrue(errorStream.closed, "the error stream must be closed when its decoder cannot be built");
        assertEquals(1, ex.getSuppressed().length, "the original IOException must travel as a suppressed exception");
        assertTrue(ex.getSuppressed()[0] instanceof IOException, String.valueOf(ex.getSuppressed()[0]));
        assertEquals("Test exception", ex.getSuppressed()[0].getMessage());
    }

    @Test
    public void testGetInputStreamWithNoneReturnsRawStreamUnchangedAndOpen() throws IOException {
        final MockHttpURLConnection connection = new MockHttpURLConnection();
        final TrackingInputStream raw = new TrackingInputStream("body".getBytes(StandardCharsets.UTF_8));
        connection.setInputStream(raw);

        assertSame(raw, HttpUtil.getInputStream(connection, ContentFormat.NONE));
        assertFalse(raw.closed);

        // Error-stream branch without a decoder: the error stream itself is handed back, still open.
        final MockHttpURLConnection failing = new MockHttpURLConnection();
        failing.setThrowOnGetInputStream(true);
        final TrackingInputStream errorStream = new TrackingInputStream("error".getBytes(StandardCharsets.UTF_8));
        failing.setErrorStream(errorStream);

        assertSame(errorStream, HttpUtil.getInputStream(failing, ContentFormat.NONE));
        assertFalse(errorStream.closed);

        // A valid GZIP body is still decoded on the success path.
        final ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (OutputStream gzip = HttpUtil.wrapOutputStream(compressed, ContentFormat.JSON_GZIP)) {
            gzip.write("{\"ok\":true}".getBytes(StandardCharsets.UTF_8));
        }
        final MockHttpURLConnection ok = new MockHttpURLConnection();
        ok.setInputStream(new ByteArrayInputStream(compressed.toByteArray()));
        assertEquals("{\"ok\":true}", IOUtil.readAllToString(HttpUtil.getInputStream(ok, ContentFormat.JSON_GZIP)));
    }

    // ------------------------------------------------------------------------------------------------
    // a07 F-3: flush is terminal for GZIP/LZ4; pin the per-codec behaviour of a second flush.
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testFlushTwiceIsToleratedByGzipAndSnappyButRejectedByLz4() throws IOException {
        final byte[] payload = "payload for the double-flush pin".getBytes(StandardCharsets.UTF_8);

        // Plain stream: flush is a no-op that may be repeated.
        final ByteArrayOutputStream plain = new ByteArrayOutputStream();
        HttpUtil.flush(plain);
        assertDoesNotThrow(() -> HttpUtil.flush(plain));
        assertEquals(0, plain.size());

        // GZIP: finish() is idempotent, the second flush is accepted and the payload stays decodable.
        final ByteArrayOutputStream gzipBytes = new ByteArrayOutputStream();
        final OutputStream gzipOs = HttpUtil.wrapOutputStream(gzipBytes, ContentFormat.GZIP);
        gzipOs.write(payload);
        HttpUtil.flush(gzipOs);
        assertDoesNotThrow(() -> HttpUtil.flush(gzipOs));
        gzipOs.close();
        assertArrayEquals(payload, IOUtil.readAllBytes(HttpUtil.wrapInputStream(new ByteArrayInputStream(gzipBytes.toByteArray()), ContentFormat.GZIP)));

        // Snappy: no finish step, the second flush is accepted.
        final ByteArrayOutputStream snappyBytes = new ByteArrayOutputStream();
        final OutputStream snappyOs = HttpUtil.wrapOutputStream(snappyBytes, ContentFormat.SNAPPY);
        snappyOs.write(payload);
        HttpUtil.flush(snappyOs);
        assertDoesNotThrow(() -> HttpUtil.flush(snappyOs));
        snappyOs.close();
        assertArrayEquals(payload, IOUtil.readAllBytes(HttpUtil.wrapInputStream(new ByteArrayInputStream(snappyBytes.toByteArray()), ContentFormat.SNAPPY)));

        // LZ4: the block stream refuses to finish twice - documented on HttpUtil.flush.
        final ByteArrayOutputStream lz4Bytes = new ByteArrayOutputStream();
        final OutputStream lz4Os = HttpUtil.wrapOutputStream(lz4Bytes, ContentFormat.LZ4);
        lz4Os.write(payload);
        HttpUtil.flush(lz4Os);
        assertThrows(IllegalStateException.class, () -> HttpUtil.flush(lz4Os));
        lz4Os.close();
        assertArrayEquals(payload, IOUtil.readAllBytes(HttpUtil.wrapInputStream(new ByteArrayInputStream(lz4Bytes.toByteArray()), ContentFormat.LZ4)));
    }

    // ------------------------------------------------------------------------------------------------
    // a07 F-4: an apostrophe is a token character, not a quoted-string delimiter (RFC 9110).
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testGetCharsetApostropheInAnotherParameterDoesNotHideTheCharsetParameter() {
        // The triggering input: x='y is a legal unquoted token value; the charset that follows must be found.
        assertEquals(StandardCharsets.ISO_8859_1, HttpUtil.getCharset("text/plain; x='y; charset=ISO-8859-1", null));
        assertEquals(StandardCharsets.ISO_8859_1, HttpUtil.getCharset("text/plain; x='y; charset=ISO-8859-1", Charsets.UTF_8));

        // Lenient unquoting of the charset value itself is unchanged.
        assertEquals(StandardCharsets.UTF_8, HttpUtil.getCharset("text/html; charset='utf-8'", StandardCharsets.ISO_8859_1));

        // An apostrophe INSIDE a real (double-quoted) value is data.
        assertEquals(StandardCharsets.UTF_16, HttpUtil.getCharset("text/plain; note=\"it's\"; charset=UTF-16", Charsets.UTF_8));

        // Non-ASCII inside an unrelated quoted value does not disturb the scan either.
        assertEquals(StandardCharsets.ISO_8859_1, HttpUtil.getCharset("text/plain; note=\"häé; charset=UTF-16\"; charset=ISO-8859-1", Charsets.UTF_8));

        // Locked: an unbalanced DQUOTE swallows the remainder, so the charset after it is not found.
        assertEquals(Charsets.UTF_8, HttpUtil.getCharset("text/plain; x=\"y; charset=ISO-8859-1", Charsets.UTF_8));
        assertNull(HttpUtil.getCharset("text/plain; x=\"y; charset=ISO-8859-1", null));
    }

    // ------------------------------------------------------------------------------------------------
    // a07 F-5: codings other than gzip/br/snappy/lz4 are identity on the read side (documented, pinned).
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testDeflateContentEncodingIsTreatedAsIdentity() {
        // Documented limitation on getContentFormat/wrapInputStream: deflate is not decoded.
        assertEquals(ContentFormat.JSON, HttpUtil.getContentFormat("application/json", "deflate"));
        assertEquals(ContentFormat.XML, HttpUtil.getContentFormat("application/xml", "deflate"));
        assertEquals(ContentFormat.JSON, HttpUtil.getContentFormat("application/json", "zstd"));

        final InputStream raw = new ByteArrayInputStream(new byte[] { 120, -100, 1, 2, 3 });
        assertSame(raw, HttpUtil.wrapInputStream(raw, HttpUtil.getContentFormat("application/json", "deflate")));
    }

    // G02-45: the media-subtype fallback must test the real IANA subtype (x-www-form-urlencoded); the token
    // "urlencoded" it used matched no real form media type, so variants fell through to NONE.
    @Test
    public void reviewFixes20260908_formUrlEncodedVariantsAreRecognised() {
        assertEquals(ContentFormat.FORM_URL_ENCODED, HttpUtil.getContentFormat("application/x-www-form-urlencoded", null));
        assertEquals(ContentFormat.FORM_URL_ENCODED, HttpUtil.getContentFormat("application/x-www-form-urlencoded; charset=utf-8", null));
        assertEquals(ContentFormat.FORM_URL_ENCODED, HttpUtil.getContentFormat("text/x-www-form-urlencoded", null));
        assertEquals(ContentFormat.FORM_URL_ENCODED, HttpUtil.getContentFormat("TEXT/X-WWW-FORM-URLENCODED", null));
        assertEquals(ContentFormat.FORM_URL_ENCODED, HttpUtil.getContentFormat("application/www-form-urlencoded", null));
        assertEquals(ContentFormat.FORM_URL_ENCODED, HttpUtil.getContentFormat("application/vnd.foo+www-form-urlencoded", null));

        // the tightening the media-subtype rewrite introduced is kept: a subtype that merely contains the
        // token, or an unrelated form-ish type, is still not a form-encoded type
        assertEquals(ContentFormat.NONE, HttpUtil.getContentFormat("application/urlencoded", null));
        assertEquals(ContentFormat.NONE, HttpUtil.getContentFormat("application/x-urlencoded", null));
        assertEquals(ContentFormat.NONE, HttpUtil.getContentFormat("multipart/form-data", null));
        assertEquals(ContentFormat.NONE, HttpUtil.getContentFormat("application/jsonp", null));
    }

    // G04-100/101: the parameter scan ran over contentType.toLowerCase(Locale.ROOT) but the charset value was
    // then read out of the ORIGINAL string. toLowerCase is not length-preserving - U+0130 lower-cases to two
    // characters - so once the two index spaces are more than "charset=" apart, the value was read from an
    // offset past the parameter and the default was returned for a header that declares a charset.
    @Test
    public void reviewFixes20260908_charsetIsFoundAfterCharactersThatGrowWhenLowerCased() {
        final String dottedI = "İ"; // lower-cases to "i" + U+0307, i.e. one character becomes two

        assertEquals(Charset.forName("ISO-8859-1"), HttpUtil.getCharset("text/plain; x=" + dottedI + "; charset=ISO-8859-1", Charsets.UTF_8));
        assertEquals(Charset.forName("ISO-8859-1"), HttpUtil.getCharset("text/plain; x=" + dottedI.repeat(8) + "; charset=ISO-8859-1", Charsets.UTF_8));
        assertEquals(Charset.forName("ISO-8859-1"), HttpUtil.getCharset("text/plain; x=" + dottedI.repeat(20) + "; charset=ISO-8859-1; y=1", Charsets.UTF_8));

        // ... including inside a quoted value, which the scan skips over
        assertEquals(Charset.forName("ISO-8859-1"),
                HttpUtil.getCharset("application/json; note=\"" + dottedI.repeat(12) + "; charset=UTF-16\"; charset=ISO-8859-1", Charsets.UTF_8));

        // the parameter name is still matched case-insensitively, and a quoted value is still unquoted
        assertEquals(Charset.forName("ISO-8859-1"), HttpUtil.getCharset("text/plain; CHARSET=ISO-8859-1", Charsets.UTF_8));
        assertEquals(Charset.forName("ISO-8859-1"), HttpUtil.getCharset("text/plain; " + dottedI.repeat(9) + "=1; ChArSeT=\"ISO-8859-1\"", Charsets.UTF_8));

        // and a token that merely ends in "charset" is still not the charset parameter
        assertEquals(Charsets.UTF_8, HttpUtil.getCharset("text/plain; x-" + dottedI + "-charset=UTF-16", Charsets.UTF_8));
    }

}

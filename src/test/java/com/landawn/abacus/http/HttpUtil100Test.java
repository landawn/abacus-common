package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Charsets;

@Tag("new-test")
public class HttpUtil100Test extends TestBase {

    @Test
    public void testIsSuccessfulResponseCode() {
        assertTrue(HttpUtil.isSuccessfulResponseCode(200));
        assertTrue(HttpUtil.isSuccessfulResponseCode(201));
        assertTrue(HttpUtil.isSuccessfulResponseCode(204));
        assertTrue(HttpUtil.isSuccessfulResponseCode(299));

        assertFalse(HttpUtil.isSuccessfulResponseCode(199));
        assertFalse(HttpUtil.isSuccessfulResponseCode(300));
        assertFalse(HttpUtil.isSuccessfulResponseCode(400));
        assertFalse(HttpUtil.isSuccessfulResponseCode(404));
        assertFalse(HttpUtil.isSuccessfulResponseCode(500));
    }

    @Test
    public void testIsValidHttpHeader() {
        assertTrue(HttpUtil.isValidHttpHeader("Content-Type", "application/json"));
        assertTrue(HttpUtil.isValidHttpHeader("Accept", "text/plain"));
        assertTrue(HttpUtil.isValidHttpHeader("X-Custom-Header", "value"));

        assertFalse(HttpUtil.isValidHttpHeader("", "value"));
        assertFalse(HttpUtil.isValidHttpHeader(null, "value"));
        assertFalse(HttpUtil.isValidHttpHeader("Header:WithColon", "value"));
        assertFalse(HttpUtil.isValidHttpHeader("Header\nWith\nNewlines", "value"));
        assertFalse(HttpUtil.isValidHttpHeader("Header\rWith\rReturns", "value"));

        // Value with newline followed by space/tab is valid (header folding)
        assertTrue(HttpUtil.isValidHttpHeader("Header", "value\n continues"));
        assertTrue(HttpUtil.isValidHttpHeader("Header", "value\n\tcontinues"));

        // Value with newline not followed by space/tab is invalid
        assertFalse(HttpUtil.isValidHttpHeader("Header", "value\nnot continued"));
    }

    @Test
    public void testReadHttpHeadValue() {
        assertEquals("value", HttpUtil.readHttpHeaderValue("value"));
        assertNull(HttpUtil.readHttpHeaderValue(null));
        assertEquals("123", HttpUtil.readHttpHeaderValue(123));

        // Test with Collection
        List<String> values = Arrays.asList("value1", "value2", "value3");
        assertEquals("value1,value2,value3", HttpUtil.readHttpHeaderValue(values));

        // Test with empty Collection
        assertEquals("", HttpUtil.readHttpHeaderValue(new ArrayList<>()));

        // Test with single-element Collection
        assertEquals("single", HttpUtil.readHttpHeaderValue(Arrays.asList("single")));
    }

    @Test
    public void testGetContentTypeFromMap() {
        Map<String, String> headers = new HashMap<>();
        assertNull(HttpUtil.getContentType((Map<String, ?>) null));
        assertNull(HttpUtil.getContentType(headers));

        headers.put("Content-Type", "application/json");
        assertEquals("application/json", HttpUtil.getContentType(headers));

        headers.clear();
        headers.put("content-type", "text/plain");
        assertEquals("text/plain", HttpUtil.getContentType(headers));
    }

    @Test
    public void testGetContentTypeFromHttpHeaders() {
        assertNull(HttpUtil.getContentType((HttpHeaders) null));

        HttpHeaders headers = HttpHeaders.create();
        assertNull(HttpUtil.getContentType(headers));

        headers.setContentType("application/xml");
        assertEquals("application/xml", HttpUtil.getContentType(headers));
    }

    @Test
    public void testGetContentTypeFromHttpSettings() {
        assertNull(HttpUtil.getContentType((HttpSettings) null));

        HttpSettings settings = HttpSettings.create();
        assertNull(HttpUtil.getContentType(settings));

        settings.setContentType("text/html");
        assertEquals("text/html", HttpUtil.getContentType(settings));
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
        Map<String, String> headers = new HashMap<>();
        assertNull(HttpUtil.getContentEncoding((Map<String, ?>) null));
        assertNull(HttpUtil.getContentEncoding(headers));

        headers.put("Content-Encoding", "gzip");
        assertEquals("gzip", HttpUtil.getContentEncoding(headers));

        headers.clear();
        headers.put("content-encoding", "deflate");
        assertEquals("deflate", HttpUtil.getContentEncoding(headers));
    }

    @Test
    public void testGetContentEncodingFromHttpHeaders() {
        assertNull(HttpUtil.getContentEncoding((HttpHeaders) null));

        HttpHeaders headers = HttpHeaders.create();
        assertNull(HttpUtil.getContentEncoding(headers));

        headers.setContentEncoding("br");
        assertEquals("br", HttpUtil.getContentEncoding(headers));
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
        Map<String, String> headers = new HashMap<>();
        assertNull(HttpUtil.getAccept((Map<String, ?>) null));
        assertNull(HttpUtil.getAccept(headers));

        headers.put("Accept", "application/json");
        assertEquals("application/json", HttpUtil.getAccept(headers));

        headers.clear();
        headers.put("accept", "text/plain");
        assertEquals("text/plain", HttpUtil.getAccept(headers));
    }

    @Test
    public void testGetAcceptFromHttpHeaders() {
        assertNull(HttpUtil.getAccept((HttpHeaders) null));

        HttpHeaders headers = HttpHeaders.create();
        assertNull(HttpUtil.getAccept(headers));

        headers.setAccept("application/xml");
        assertEquals("application/xml", HttpUtil.getAccept(headers));
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
        Map<String, String> headers = new HashMap<>();
        assertNull(HttpUtil.getAcceptEncoding((Map<String, ?>) null));
        assertNull(HttpUtil.getAcceptEncoding(headers));

        headers.put("Accept-Encoding", "gzip, deflate");
        assertEquals("gzip, deflate", HttpUtil.getAcceptEncoding(headers));

        headers.clear();
        headers.put("accept-encoding", "br");
        assertEquals("br", HttpUtil.getAcceptEncoding(headers));
    }

    @Test
    public void testGetAcceptEncodingFromHttpHeaders() {
        assertNull(HttpUtil.getAcceptEncoding((HttpHeaders) null));

        HttpHeaders headers = HttpHeaders.create();
        assertNull(HttpUtil.getAcceptEncoding(headers));

        headers.setAcceptEncoding("gzip");
        assertEquals("gzip", HttpUtil.getAcceptEncoding(headers));
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
        Map<String, String> headers = new HashMap<>();
        assertNull(HttpUtil.getAcceptCharset((Map<String, ?>) null));
        assertNull(HttpUtil.getAcceptCharset(headers));

        headers.put("Accept-Charset", "utf-8");
        assertEquals("utf-8", HttpUtil.getAcceptCharset(headers));

        headers.clear();
        headers.put("accept-charset", "iso-8859-1");
        assertEquals("iso-8859-1", HttpUtil.getAcceptCharset(headers));
    }

    @Test
    public void testGetAcceptCharsetFromHttpHeaders() {
        assertNull(HttpUtil.getAcceptCharset((HttpHeaders) null));

        HttpHeaders headers = HttpHeaders.create();
        assertNull(HttpUtil.getAcceptCharset(headers));

        headers.setAcceptCharset("utf-16");
        assertEquals("utf-16", HttpUtil.getAcceptCharset(headers));
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
    public void testGetContentTypeForContentFormat() {
        assertEquals("", HttpUtil.getContentType((ContentFormat) null));
        assertEquals("", HttpUtil.getContentType(ContentFormat.NONE));

        assertEquals("application/json", HttpUtil.getContentType(ContentFormat.JSON));
        assertEquals("application/json", HttpUtil.getContentType(ContentFormat.JSON_GZIP));
        assertEquals("application/xml", HttpUtil.getContentType(ContentFormat.XML));
        assertEquals("application/xml", HttpUtil.getContentType(ContentFormat.XML_LZ4));
        assertEquals("application/x-www-form-urlencoded", HttpUtil.getContentType(ContentFormat.FormUrlEncoded));
        assertEquals("", HttpUtil.getContentType(ContentFormat.KRYO));
    }

    @Test
    public void testGetContentEncodingForContentFormat() {
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
    public void testGetContentFormat() {
        assertEquals(ContentFormat.NONE, HttpUtil.getContentFormat(null, null));
        assertEquals(ContentFormat.NONE, HttpUtil.getContentFormat("", ""));

        assertEquals(ContentFormat.JSON, HttpUtil.getContentFormat("application/json", ""));
        assertEquals(ContentFormat.JSON_GZIP, HttpUtil.getContentFormat("application/json", "gzip"));
        assertEquals(ContentFormat.JSON_BR, HttpUtil.getContentFormat("application/json", "br"));
        assertEquals(ContentFormat.JSON_SNAPPY, HttpUtil.getContentFormat("application/json", "snappy"));
        assertEquals(ContentFormat.JSON_LZ4, HttpUtil.getContentFormat("application/json", "lz4"));

        assertEquals(ContentFormat.XML, HttpUtil.getContentFormat("application/xml", ""));
        assertEquals(ContentFormat.XML_GZIP, HttpUtil.getContentFormat("application/xml", "gzip"));
        assertEquals(ContentFormat.XML_BR, HttpUtil.getContentFormat("application/xml", "br"));
        assertEquals(ContentFormat.XML_SNAPPY, HttpUtil.getContentFormat("application/xml", "snappy"));
        assertEquals(ContentFormat.XML_LZ4, HttpUtil.getContentFormat("application/xml", "lz4"));

        assertEquals(ContentFormat.FormUrlEncoded, HttpUtil.getContentFormat("application/x-www-form-urlencoded", ""));
        assertEquals(ContentFormat.KRYO, HttpUtil.getContentFormat("application/kryo", ""));

        assertEquals(ContentFormat.GZIP, HttpUtil.getContentFormat("", "gzip"));
        assertEquals(ContentFormat.BR, HttpUtil.getContentFormat("", "br"));
        assertEquals(ContentFormat.SNAPPY, HttpUtil.getContentFormat("", "snappy"));
        assertEquals(ContentFormat.LZ4, HttpUtil.getContentFormat("", "lz4"));
        assertEquals(ContentFormat.KRYO, HttpUtil.getContentFormat("", "kryo"));

        // Test partial matches
        assertEquals(ContentFormat.JSON, HttpUtil.getContentFormat("text/json", ""));
        assertEquals(ContentFormat.JSON, HttpUtil.getContentFormat("something/json", ""));
        assertEquals(ContentFormat.XML, HttpUtil.getContentFormat("text/xml", ""));
        assertEquals(ContentFormat.FormUrlEncoded, HttpUtil.getContentFormat("something/urlencoded", ""));
        assertEquals(ContentFormat.KRYO, HttpUtil.getContentFormat("something/kryo", ""));

        // Test case insensitive
        assertEquals(ContentFormat.JSON_GZIP, HttpUtil.getContentFormat("APPLICATION/JSON", "GZIP"));
        assertEquals(ContentFormat.JSON_GZIP, HttpUtil.getContentFormat("application/json", "GZip"));
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
        Map<String, List<String>> respHeaders = new HashMap<>();
        assertEquals(ContentFormat.NONE, HttpUtil.getResponseContentFormat(respHeaders, null));

        respHeaders.put("Content-Type", Arrays.asList("application/json"));
        assertEquals(ContentFormat.JSON, HttpUtil.getResponseContentFormat(respHeaders, null));

        respHeaders.put("Content-Encoding", Arrays.asList("gzip"));
        assertEquals(ContentFormat.JSON_GZIP, HttpUtil.getResponseContentFormat(respHeaders, null));

        // Test fallback to request format when response doesn't specify content type
        respHeaders.clear();
        assertEquals(ContentFormat.XML, HttpUtil.getResponseContentFormat(respHeaders, ContentFormat.XML));
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
        assertSame(bais, HttpUtil.wrapInputStream(bais, ContentFormat.FormUrlEncoded));

    }

    @Test
    public void testWrapOutputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        assertSame(baos, HttpUtil.wrapOutputStream(baos, null));
        assertSame(baos, HttpUtil.wrapOutputStream(baos, ContentFormat.NONE));
        assertSame(baos, HttpUtil.wrapOutputStream(baos, ContentFormat.JSON));
        assertSame(baos, HttpUtil.wrapOutputStream(baos, ContentFormat.XML));
        assertSame(baos, HttpUtil.wrapOutputStream(baos, ContentFormat.FormUrlEncoded));
        assertNull(HttpUtil.wrapOutputStream(null, ContentFormat.JSON));

        assertNotSame(baos, HttpUtil.wrapOutputStream(baos, ContentFormat.JSON_GZIP));
        assertNotSame(baos, HttpUtil.wrapOutputStream(baos, ContentFormat.JSON_SNAPPY));
        assertNotSame(baos, HttpUtil.wrapOutputStream(baos, ContentFormat.JSON_LZ4));
        assertNotSame(baos, HttpUtil.wrapOutputStream(baos, ContentFormat.GZIP));
        assertNotSame(baos, HttpUtil.wrapOutputStream(baos, ContentFormat.SNAPPY));
        assertNotSame(baos, HttpUtil.wrapOutputStream(baos, ContentFormat.LZ4));

        // Brotli output is not supported
        assertThrows(UnsupportedOperationException.class, () -> HttpUtil.wrapOutputStream(baos, ContentFormat.JSON_BR));
        assertThrows(UnsupportedOperationException.class, () -> HttpUtil.wrapOutputStream(baos, ContentFormat.BR));
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
    public void testFlush() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        HttpUtil.flush(baos);

        // Test with GZIPOutputStream
        GZIPOutputStream gzos = new GZIPOutputStream(baos);
        HttpUtil.flush(gzos);

        // Test with LZ4 would require LZ4BlockOutputStream instance
    }

    @Test
    public void testGetRequestCharset() {
        assertEquals(HttpUtil.DEFAULT_CHARSET, HttpUtil.getRequestCharset(null));

        HttpHeaders headers = HttpHeaders.create();
        assertEquals(HttpUtil.DEFAULT_CHARSET, HttpUtil.getRequestCharset(headers));

        headers.setContentType("application/json; charset=ISO-8859-1");
        assertEquals(Charset.forName("ISO-8859-1"), HttpUtil.getRequestCharset(headers));
    }

    @Test
    public void testGetResponseCharset() {
        Charset requestCharset = StandardCharsets.UTF_16;
        assertEquals(requestCharset, HttpUtil.getResponseCharset(null, requestCharset));

        Map<String, List<String>> headers = new HashMap<>();
        assertEquals(requestCharset, HttpUtil.getResponseCharset(headers, requestCharset));

        headers.put("Content-Type", Arrays.asList("text/plain; charset=US-ASCII"));
        assertEquals(StandardCharsets.US_ASCII, HttpUtil.getResponseCharset(headers, requestCharset));
    }

    @Test
    public void testGetCharset() {
        assertEquals(HttpUtil.DEFAULT_CHARSET, HttpUtil.getCharset(null));
        assertEquals(HttpUtil.DEFAULT_CHARSET, HttpUtil.getCharset(""));
        assertEquals(HttpUtil.DEFAULT_CHARSET, HttpUtil.getCharset("application/json"));

        assertEquals(StandardCharsets.UTF_8, HttpUtil.getCharset("application/json; charset=UTF-8"));
        assertEquals(StandardCharsets.ISO_8859_1, HttpUtil.getCharset("text/html; charset=ISO-8859-1"));
        assertEquals(StandardCharsets.US_ASCII, HttpUtil.getCharset("text/plain; charset=US-ASCII"));

        // Test with different delimiters
        assertEquals(StandardCharsets.UTF_16, HttpUtil.getCharset("text/xml; charset=UTF-16; boundary=something"));
        assertEquals(StandardCharsets.UTF_16, HttpUtil.getCharset("text/xml; charset=UTF-16, something=else"));

        // Test without spaces
        assertEquals(StandardCharsets.UTF_8, HttpUtil.getCharset("application/json;charset=UTF-8"));
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
    public void testTurnOffCertificateValidation() {
        // This test can only verify that the method doesn't throw an exception
        // Actually testing the certificate validation would require HTTPS connections
        HttpUtil.turnOffCertificateValidation();
    }

    @Test
    public void testHttpDateParse() {
        assertNull(HttpUtil.HttpDate.parse(""));

        // Test standard format
        Date date = HttpUtil.HttpDate.parse("Wed, 21 Oct 2015 07:28:00 GMT");
        assertNotNull(date);

        // Test various formats
        assertNotNull(HttpUtil.HttpDate.parse("Wednesday, 21-Oct-15 07:28:00 GMT"));
        assertNotNull(HttpUtil.HttpDate.parse("Wed Oct 21 07:28:00 2015"));

        // Test invalid format
        assertNull(HttpUtil.HttpDate.parse("invalid date format"));
    }

    @Test
    public void testHttpDateFormat() {
        Date date = new Date();
        String formatted = HttpUtil.HttpDate.format(date);
        assertNotNull(formatted);
        assertTrue(formatted.endsWith(" GMT"));

        // Test format pattern
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        assertEquals(sdf.format(date), formatted);
    }

    @Test
    public void testConstants() {
        assertEquals(Charsets.UTF_8, HttpUtil.DEFAULT_CHARSET);
        assertEquals(ContentFormat.JSON, HttpUtil.DEFAULT_CONTENT_FORMAT);
    }

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
}

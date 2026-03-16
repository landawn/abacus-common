package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.Parser;
import com.landawn.abacus.util.Charsets;
import com.landawn.abacus.util.IOUtil;

@Tag("2025")
public class HttpUtilTest extends TestBase {

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
    public void testReadHttpHeadValue() {
        assertNull(HttpUtil.readHttpHeaderValue(null));
        assertEquals("value", HttpUtil.readHttpHeaderValue("value"));
        assertEquals("123", HttpUtil.readHttpHeaderValue(123));

        List<String> list = Arrays.asList("val1", "val2", "val3");
        assertEquals("val1,val2,val3", HttpUtil.readHttpHeaderValue(list));

        List<String> emptyList = Arrays.asList();
        assertEquals("", HttpUtil.readHttpHeaderValue(emptyList));

        List<String> singleList = Arrays.asList("single");
        assertEquals("single", HttpUtil.readHttpHeaderValue(singleList));
    }

    @Test
    public void testGetContentTypeFromMap() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        assertEquals("application/json", HttpUtil.getContentType(headers));

        headers.clear();
        headers.put("content-type", "text/html");
        assertEquals("text/html", HttpUtil.getContentType(headers));

        assertNull(HttpUtil.getContentType((Map<String, Object>) null));
        assertNull(HttpUtil.getContentType(new HashMap<>()));
    }

    @Test
    public void testGetContentTypeFromHttpHeaders() {
        HttpHeaders headers = HttpHeaders.create();
        headers.set(HttpHeaders.Names.CONTENT_TYPE, "application/json");
        assertEquals("application/json", HttpUtil.getContentType(headers));

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
    public void testGetContentEncodingFromMap() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("Content-Encoding", "gzip");
        assertEquals("gzip", HttpUtil.getContentEncoding(headers));

        headers.clear();
        headers.put("content-encoding", "deflate");
        assertEquals("deflate", HttpUtil.getContentEncoding(headers));

        assertNull(HttpUtil.getContentEncoding((Map<String, Object>) null));
        assertNull(HttpUtil.getContentEncoding(new HashMap<>()));
    }

    @Test
    public void testGetContentEncodingFromHttpHeaders() {
        HttpHeaders headers = HttpHeaders.create();
        headers.set(HttpHeaders.Names.CONTENT_ENCODING, "gzip");
        assertEquals("gzip", HttpUtil.getContentEncoding(headers));

        assertNull(HttpUtil.getContentEncoding((HttpHeaders) null));
        assertNull(HttpUtil.getContentEncoding(HttpHeaders.create()));
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
    public void testGetAcceptEncodingFromMap() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("Accept-Encoding", "gzip, deflate");
        assertEquals("gzip, deflate", HttpUtil.getAcceptEncoding(headers));

        assertNull(HttpUtil.getAcceptEncoding((Map<String, Object>) null));
    }

    @Test
    public void testGetAcceptCharsetFromMap() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("Accept-Charset", "utf-8");
        assertEquals("utf-8", HttpUtil.getAcceptCharset(headers));

        assertNull(HttpUtil.getAcceptCharset((Map<String, Object>) null));
    }

    @Test
    public void testGetContentTypeFromContentFormat() {
        assertEquals(HttpHeaders.Values.APPLICATION_JSON, HttpUtil.getContentType(ContentFormat.JSON));
        assertEquals(HttpHeaders.Values.APPLICATION_XML, HttpUtil.getContentType(ContentFormat.XML));
        assertEquals(HttpHeaders.Values.APPLICATION_JSON, HttpUtil.getContentType(ContentFormat.JSON_GZIP));
        assertEquals("", HttpUtil.getContentType(ContentFormat.NONE));
        assertEquals("", HttpUtil.getContentType((ContentFormat) null));
    }

    @Test
    public void testGetContentEncodingFromContentFormat() {
        assertEquals("", HttpUtil.getContentEncoding(ContentFormat.JSON));
        assertEquals("gzip", HttpUtil.getContentEncoding(ContentFormat.JSON_GZIP));
        assertEquals("gzip", HttpUtil.getContentEncoding(ContentFormat.GZIP));
        assertEquals("lz4", HttpUtil.getContentEncoding(ContentFormat.LZ4));
        assertEquals("", HttpUtil.getContentEncoding(ContentFormat.NONE));
        assertEquals("", HttpUtil.getContentEncoding((ContentFormat) null));
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
    public void testGetParser() {
        Parser<?, ?> jsonParser = HttpUtil.getParser(ContentFormat.JSON);
        assertNotNull(jsonParser);

        Parser<?, ?> xmlParser = HttpUtil.getParser(ContentFormat.XML);
        assertNotNull(xmlParser);

        Parser<?, ?> nullParser = HttpUtil.getParser(null);
        assertNotNull(nullParser);

        Parser<?, ?> kryoParser = HttpUtil.getParser(ContentFormat.KRYO);
        assertNotNull(kryoParser);
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
    public void testIsValidHttpHeaderWithLineFeed() {
        // Valid: line feed followed by space or tab
        assertTrue(HttpUtil.isValidHttpHeader("key", "value\n continuation"));
        assertTrue(HttpUtil.isValidHttpHeader("key", "value\n\tcontinuation"));

        // Invalid: line feed not followed by space or tab
        assertFalse(HttpUtil.isValidHttpHeader("key", "value\nnoSpace"));
    }

    @Test
    public void testGetContentFormatWithPartialMatching() {
        // Test partial content type matching
        assertEquals(ContentFormat.JSON, HttpUtil.getContentFormat("application/json; charset=UTF-8", null));
        assertEquals(ContentFormat.XML, HttpUtil.getContentFormat("text/xml; charset=UTF-8", null));
        assertEquals(ContentFormat.JSON_GZIP, HttpUtil.getContentFormat("something/json", "gzip"));
        assertEquals(ContentFormat.XML_SNAPPY, HttpUtil.getContentFormat("something/xml", "snappy"));
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
    public void testReadHttpHeadValueWithMultipleValues() {
        List<Integer> numbers = Arrays.asList(1, 2, 3);
        assertEquals("1,2,3", HttpUtil.readHttpHeaderValue(numbers));

        List<Object> mixed = Arrays.asList("str", 123, true);
        assertEquals("str,123,true", HttpUtil.readHttpHeaderValue(mixed));
    }

    @Test
    public void testGetCharsetWithComplexContentType() {
        Charset charset = HttpUtil.getCharset("text/html; charset=UTF-8; boundary=something");
        assertEquals(StandardCharsets.UTF_8, charset);

        Charset charset2 = HttpUtil.getCharset("application/json;charset=ISO-8859-1");
        assertEquals(Charset.forName("ISO-8859-1"), charset2);
    }

    @Test
    public void testGetContentFormatWithEdgeCases() {
        // Empty strings
        assertEquals(ContentFormat.NONE, HttpUtil.getContentFormat("", ""));

        // Unknown content type with known encoding
        assertEquals(ContentFormat.GZIP, HttpUtil.getContentFormat("unknown/type", "gzip"));
        assertEquals(ContentFormat.LZ4, HttpUtil.getContentFormat("unknown/type", "lz4"));
        assertEquals(ContentFormat.SNAPPY, HttpUtil.getContentFormat("unknown/type", "snappy"));

        // Case insensitive matching
        assertEquals(ContentFormat.JSON_GZIP, HttpUtil.getContentFormat("application/JSON", "GZIP"));
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
        assertEquals("application/x-www-form-urlencoded", HttpUtil.getContentType(ContentFormat.FORM_URL_ENCODED));
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
        HttpUtil.disableCertificateValidation();
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
    public void testGetContentFormatWithKryoAndUrlEncoded() {
        assertEquals(ContentFormat.KRYO, HttpUtil.getContentFormat("application/x-kryo", null));
        assertEquals(ContentFormat.FORM_URL_ENCODED, HttpUtil.getContentFormat("application/x-www-form-urlencoded", null));
        assertEquals(ContentFormat.FORM_URL_ENCODED, HttpUtil.getContentFormat("application/x-www-form-urlencoded", ""));

        // Kryo with encoding
        assertEquals(ContentFormat.NONE, HttpUtil.getContentFormat("unknown/type", "unknown"));

        // BR encoding
        assertEquals(ContentFormat.JSON_BR, HttpUtil.getContentFormat("application/json", "br"));
        assertEquals(ContentFormat.XML_BR, HttpUtil.getContentFormat("application/xml", "br"));
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
    public void testGetResponseContentFormatWithNullHeaders() {
        assertEquals(ContentFormat.XML, HttpUtil.getResponseContentFormat(null, ContentFormat.XML));
        assertEquals(ContentFormat.NONE, HttpUtil.getResponseContentFormat(null, null));
    }

    @Test
    public void testGetParserForAllFormats() {
        assertNotNull(HttpUtil.getParser(ContentFormat.NONE));
        assertNotNull(HttpUtil.getParser(ContentFormat.FORM_URL_ENCODED));
        assertNotNull(HttpUtil.getParser(ContentFormat.GZIP));
        assertNotNull(HttpUtil.getParser(ContentFormat.LZ4));
        assertNotNull(HttpUtil.getParser(ContentFormat.SNAPPY));
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
    public void testGetOutputStreamWithNullContentFormat() throws IOException {
        MockHttpURLConnection connection = new MockHttpURLConnection();
        connection.setOutputStream(new ByteArrayOutputStream());
        OutputStream os = HttpUtil.getOutputStream(connection, null, null, null);
        assertNotNull(os);
    }

    @Test
    public void testGetInputStreamWithNullStreams() throws IOException {
        MockHttpURLConnection connection = new MockHttpURLConnection();
        connection.setThrowOnGetInputStream(true);
        // Both input and error streams are null, should return empty input stream
        InputStream is = HttpUtil.getInputStream(connection, ContentFormat.JSON);
        assertNotNull(is);
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

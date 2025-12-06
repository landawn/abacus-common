package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
public class HttpUtil2025Test extends TestBase {

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
        assertNull(HttpUtil.readHttpHeadValue(null));
        assertEquals("value", HttpUtil.readHttpHeadValue("value"));
        assertEquals("123", HttpUtil.readHttpHeadValue(123));

        List<String> list = Arrays.asList("val1", "val2", "val3");
        assertEquals("val1,val2,val3", HttpUtil.readHttpHeadValue(list));

        List<String> emptyList = Arrays.asList();
        assertEquals("", HttpUtil.readHttpHeadValue(emptyList));

        List<String> singleList = Arrays.asList("single");
        assertEquals("single", HttpUtil.readHttpHeadValue(singleList));
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
        assertEquals(ContentFormat.FormUrlEncoded, HttpUtil.getContentFormat("application/x-www-form-urlencoded", null));
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
        Date date = new Date(1445412480000L);   // Wed, 21 Oct 2015 07:28:00 GMT
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
        assertEquals("1,2,3", HttpUtil.readHttpHeadValue(numbers));

        List<Object> mixed = Arrays.asList("str", 123, true);
        assertEquals("str,123,true", HttpUtil.readHttpHeadValue(mixed));
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
}

package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class HttpSettings100Test extends TestBase {

    @Test
    public void testCreate() {
        HttpSettings settings = HttpSettings.create();
        assertNotNull(settings);
        assertEquals(0, settings.getConnectTimeout());
        assertEquals(0, settings.getReadTimeout());
        assertFalse(settings.getUseCaches());
        assertTrue(settings.doInput());
        assertTrue(settings.doOutput());
        assertFalse(settings.isOneWayRequest());
        assertNull(settings.getContentFormat());
        assertNull(settings.getSSLSocketFactory());
        assertNull(settings.getProxy());
    }

    @Test
    public void testGetconnectTimeout() {
        HttpSettings settings = new HttpSettings();
        assertEquals(0, settings.getConnectTimeout());

        settings.setConnectTimeout(5000L);
        assertEquals(5000L, settings.getConnectTimeout());
    }

    @Test
    public void testSetconnectTimeout() {
        HttpSettings settings = new HttpSettings();
        HttpSettings result = settings.setConnectTimeout(3000L);
        assertSame(settings, result);
        assertEquals(3000L, settings.getConnectTimeout());
    }

    @Test
    public void testGetReadTimeout() {
        HttpSettings settings = new HttpSettings();
        assertEquals(0, settings.getReadTimeout());

        settings.setReadTimeout(8000L);
        assertEquals(8000L, settings.getReadTimeout());
    }

    @Test
    public void testSetReadTimeout() {
        HttpSettings settings = new HttpSettings();
        HttpSettings result = settings.setReadTimeout(10000L);
        assertSame(settings, result);
        assertEquals(10000L, settings.getReadTimeout());
    }

    @Test
    public void testGetSSLSocketFactory() {
        HttpSettings settings = new HttpSettings();
        assertNull(settings.getSSLSocketFactory());
    }

    @Test
    public void testSetSSLSocketFactory() throws Exception {
        HttpSettings settings = new HttpSettings();
        SSLContext sslContext = SSLContext.getDefault();
        SSLSocketFactory factory = sslContext.getSocketFactory();

        HttpSettings result = settings.setSSLSocketFactory(factory);
        assertSame(settings, result);
        assertEquals(factory, settings.getSSLSocketFactory());
    }

    @Test
    public void testGetProxy() {
        HttpSettings settings = new HttpSettings();
        assertNull(settings.getProxy());
    }

    @Test
    public void testSetProxy() {
        HttpSettings settings = new HttpSettings();
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxy.example.com", 8080));

        HttpSettings result = settings.setProxy(proxy);
        assertSame(settings, result);
        assertEquals(proxy, settings.getProxy());
    }

    @Test
    public void testGetUseCaches() {
        HttpSettings settings = new HttpSettings();
        assertFalse(settings.getUseCaches());

        settings.setUseCaches(true);
        assertTrue(settings.getUseCaches());
    }

    @Test
    public void testSetUseCaches() {
        HttpSettings settings = new HttpSettings();
        HttpSettings result = settings.setUseCaches(true);
        assertSame(settings, result);
        assertTrue(settings.getUseCaches());

        settings.setUseCaches(false);
        assertFalse(settings.getUseCaches());
    }

    @Test
    public void testDoInput() {
        HttpSettings settings = new HttpSettings();
        assertTrue(settings.doInput());

        settings.doInput(false);
        assertFalse(settings.doInput());
    }

    @Test
    public void testSetDoInput() {
        HttpSettings settings = new HttpSettings();
        HttpSettings result = settings.doInput(false);
        assertSame(settings, result);
        assertFalse(settings.doInput());
    }

    @Test
    public void testDoOutput() {
        HttpSettings settings = new HttpSettings();
        assertTrue(settings.doOutput());

        settings.doOutput(false);
        assertFalse(settings.doOutput());
    }

    @Test
    public void testSetDoOutput() {
        HttpSettings settings = new HttpSettings();
        HttpSettings result = settings.doOutput(false);
        assertSame(settings, result);
        assertFalse(settings.doOutput());
    }

    @Test
    public void testIsOneWayRequest() {
        HttpSettings settings = new HttpSettings();
        assertFalse(settings.isOneWayRequest());

        settings.isOneWayRequest(true);
        assertTrue(settings.isOneWayRequest());
    }

    @Test
    public void testSetIsOneWayRequest() {
        HttpSettings settings = new HttpSettings();
        HttpSettings result = settings.isOneWayRequest(true);
        assertSame(settings, result);
        assertTrue(settings.isOneWayRequest());
    }

    @Test
    public void testGetContentFormat() {
        HttpSettings settings = new HttpSettings();
        assertNull(settings.getContentFormat());

        settings.setContentFormat(ContentFormat.JSON);
        assertEquals(ContentFormat.JSON, settings.getContentFormat());
    }

    @Test
    public void testGetContentFormatFromHeaders() {
        HttpSettings settings = new HttpSettings();
        settings.header(HttpHeaders.Names.CONTENT_TYPE, "application/json");
        settings.header(HttpHeaders.Names.CONTENT_ENCODING, "gzip");

        ContentFormat format = settings.getContentFormat();
        assertEquals(ContentFormat.JSON_GZIP, format);
    }

    @Test
    public void testSetContentFormat() {
        HttpSettings settings = new HttpSettings();
        HttpSettings result = settings.setContentFormat(ContentFormat.XML);
        assertSame(settings, result);
        assertEquals(ContentFormat.XML, settings.getContentFormat());
    }

    @Test
    public void testGetContentType() {
        HttpSettings settings = new HttpSettings();
        assertNull(settings.getContentType());

        settings.setContentType("application/json");
        assertEquals("application/json", settings.getContentType());
    }

    @Test
    public void testGetContentTypeFromContentFormat() {
        HttpSettings settings = new HttpSettings();
        settings.setContentFormat(ContentFormat.JSON);
        assertEquals("application/json", settings.getContentType());
    }

    @Test
    public void testSetContentType() {
        HttpSettings settings = new HttpSettings();
        HttpSettings result = settings.setContentType("text/plain");
        assertSame(settings, result);
        assertEquals("text/plain", settings.getContentType());
    }

    @Test
    public void testGetContentEncoding() {
        HttpSettings settings = new HttpSettings();
        assertNull(settings.getContentEncoding());

        settings.setContentEncoding("gzip");
        assertEquals("gzip", settings.getContentEncoding());
    }

    @Test
    public void testGetContentEncodingFromContentFormat() {
        HttpSettings settings = new HttpSettings();
        settings.setContentFormat(ContentFormat.JSON_GZIP);
        assertEquals("gzip", settings.getContentEncoding());
    }

    @Test
    public void testSetContentEncoding() {
        HttpSettings settings = new HttpSettings();
        HttpSettings result = settings.setContentEncoding("deflate");
        assertSame(settings, result);
        assertEquals("deflate", settings.getContentEncoding());
    }

    @Test
    public void testBasicAuth() {
        HttpSettings settings = new HttpSettings();
        HttpSettings result = settings.basicAuth("user", "password");
        assertSame(settings, result);

        String authHeader = (String) settings.headers().get(HttpHeaders.Names.AUTHORIZATION);
        assertNotNull(authHeader);
        assertTrue(authHeader.startsWith("Basic "));
    }

    @Test
    public void testHeader() {
        HttpSettings settings = new HttpSettings();
        HttpSettings result = settings.header("X-Custom-Header", "value");
        assertSame(settings, result);
        assertEquals("value", settings.headers().get("X-Custom-Header"));
    }

    @Test
    public void testHeadersWithTwoHeaders() {
        HttpSettings settings = new HttpSettings();
        HttpSettings result = settings.headers("Header1", "value1", "Header2", "value2");
        assertSame(settings, result);
        assertEquals("value1", settings.headers().get("Header1"));
        assertEquals("value2", settings.headers().get("Header2"));
    }

    @Test
    public void testHeadersWithThreeHeaders() {
        HttpSettings settings = new HttpSettings();
        HttpSettings result = settings.headers("Header1", "value1", "Header2", "value2", "Header3", "value3");
        assertSame(settings, result);
        assertEquals("value1", settings.headers().get("Header1"));
        assertEquals("value2", settings.headers().get("Header2"));
        assertEquals("value3", settings.headers().get("Header3"));
    }

    @Test
    public void testHeadersWithMap() {
        HttpSettings settings = new HttpSettings();
        Map<String, String> headers = new HashMap<>();
        headers.put("Header1", "value1");
        headers.put("Header2", "value2");

        HttpSettings result = settings.headers(headers);
        assertSame(settings, result);
        assertEquals("value1", settings.headers().get("Header1"));
        assertEquals("value2", settings.headers().get("Header2"));
    }

    @Test
    public void testHeadersWithHttpHeaders() {
        HttpSettings settings = new HttpSettings();
        HttpHeaders headers = HttpHeaders.create().set("Header1", "value1").set("Header2", "value2");

        HttpSettings result = settings.headers(headers);
        assertSame(settings, result);
        assertEquals("value1", settings.headers().get("Header1"));
        assertEquals("value2", settings.headers().get("Header2"));
    }

    @Test
    public void testHeadersWithHttpHeadersReplacesExisting() {
        HttpSettings settings = new HttpSettings();
        settings.header("Old-Header", "old-value");

        HttpHeaders headers = HttpHeaders.create().set("New-Header", "new-value");

        settings.headers(headers);
        assertNull(settings.headers().get("Old-Header"));
        assertEquals("new-value", settings.headers().get("New-Header"));
    }

    @Test
    public void testHeadersWithNullHttpHeaders() {
        HttpSettings settings = new HttpSettings();
        settings.header("Existing-Header", "value");

        settings.headers((HttpHeaders) null);
        assertNull(settings.headers().get("Existing-Header"));
        assertTrue(settings.headers().isEmpty());
    }

    @Test
    public void testHeaders() {
        HttpSettings settings = new HttpSettings();
        HttpHeaders headers = settings.headers();
        assertNotNull(headers);
        assertTrue(headers.isEmpty());

        assertSame(headers, settings.headers());
    }

    @Test
    public void testCopy() {
        HttpSettings original = new HttpSettings();
        original.setConnectTimeout(5000L)
                .setReadTimeout(10000L)
                .setUseCaches(true)
                .doInput(false)
                .doOutput(false)
                .isOneWayRequest(true)
                .setContentFormat(ContentFormat.JSON)
                .header("Header1", "value1");

        SSLSocketFactory sslFactory = null;
        try {
            sslFactory = SSLContext.getDefault().getSocketFactory();
        } catch (Exception e) {
        }
        if (sslFactory != null) {
            original.setSSLSocketFactory(sslFactory);
        }

        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxy.example.com", 8080));
        original.setProxy(proxy);

        HttpSettings copy = original.copy();

        assertEquals(original.getConnectTimeout(), copy.getConnectTimeout());
        assertEquals(original.getReadTimeout(), copy.getReadTimeout());
        assertEquals(original.getSSLSocketFactory(), copy.getSSLSocketFactory());
        assertEquals(original.getProxy(), copy.getProxy());
        assertEquals(original.getUseCaches(), copy.getUseCaches());
        assertEquals(original.doInput(), copy.doInput());
        assertEquals(original.doOutput(), copy.doOutput());
        assertEquals(original.isOneWayRequest(), copy.isOneWayRequest());
        assertEquals(original.getContentFormat(), copy.getContentFormat());
        assertEquals("value1", copy.headers().get("Header1"));

        original.header("Header2", "value2");
        assertNull(copy.headers().get("Header2"));
    }

    @Test
    public void testCopyWithNullHeaders() {
        HttpSettings original = new HttpSettings();
        original.setConnectTimeout(5000L);

        HttpSettings copy = original.copy();
        assertEquals(5000L, copy.getConnectTimeout());
        assertNotNull(copy.headers());
        assertTrue(copy.headers().isEmpty());
    }

    @Test
    public void testToString() {
        HttpSettings settings = new HttpSettings();
        settings.setConnectTimeout(5000L).setReadTimeout(10000L).setUseCaches(true).setContentFormat(ContentFormat.JSON).header("Header1", "value1");

        String str = settings.toString();
        assertNotNull(str);
        assertTrue(str.contains("connectTimeout=5000"));
        assertTrue(str.contains("readTimeout=10000"));
        assertTrue(str.contains("useCaches=true"));
        assertTrue(str.contains("contentFormat=JSON"));
        assertTrue(str.contains("headers="));
    }
}

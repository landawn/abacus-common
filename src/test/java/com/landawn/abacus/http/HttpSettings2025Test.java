package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

@Tag("2025")
public class HttpSettings2025Test extends TestBase {

    @Test
    public void testConstructor() {
        HttpSettings settings = new HttpSettings();
        assertNotNull(settings);
    }

    @Test
    public void testCreate() {
        HttpSettings settings = HttpSettings.create();
        assertNotNull(settings);
    }

    @Test
    public void testGetConnectionTimeoutDefault() {
        HttpSettings settings = HttpSettings.create();
        assertEquals(0L, settings.getConnectionTimeout());
    }

    @Test
    public void testSetConnectionTimeout() {
        HttpSettings settings = HttpSettings.create();
        HttpSettings result = settings.setConnectionTimeout(5000L);
        assertEquals(5000L, settings.getConnectionTimeout());
        assertEquals(settings, result); // Verify method chaining
    }

    @Test
    public void testGetReadTimeoutDefault() {
        HttpSettings settings = HttpSettings.create();
        assertEquals(0L, settings.getReadTimeout());
    }

    @Test
    public void testSetReadTimeout() {
        HttpSettings settings = HttpSettings.create();
        HttpSettings result = settings.setReadTimeout(10000L);
        assertEquals(10000L, settings.getReadTimeout());
        assertEquals(settings, result);
    }

    @Test
    public void testGetSSLSocketFactoryDefault() {
        HttpSettings settings = HttpSettings.create();
        assertNull(settings.getSSLSocketFactory());
    }

    @Test
    public void testSetSSLSocketFactory() throws Exception {
        HttpSettings settings = HttpSettings.create();
        SSLContext sslContext = SSLContext.getDefault();
        SSLSocketFactory factory = sslContext.getSocketFactory();

        HttpSettings result = settings.setSSLSocketFactory(factory);
        assertEquals(factory, settings.getSSLSocketFactory());
        assertEquals(settings, result);
    }

    @Test
    public void testGetProxyDefault() {
        HttpSettings settings = HttpSettings.create();
        assertNull(settings.getProxy());
    }

    @Test
    public void testSetProxy() {
        HttpSettings settings = HttpSettings.create();
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxy.example.com", 8080));

        HttpSettings result = settings.setProxy(proxy);
        assertEquals(proxy, settings.getProxy());
        assertEquals(settings, result);
    }

    @Test
    public void testGetUseCachesDefault() {
        HttpSettings settings = HttpSettings.create();
        assertFalse(settings.getUseCaches());
    }

    @Test
    public void testSetUseCaches() {
        HttpSettings settings = HttpSettings.create();
        HttpSettings result = settings.setUseCaches(true);
        assertTrue(settings.getUseCaches());
        assertEquals(settings, result);
    }

    @Test
    public void testSetUseCachesFalse() {
        HttpSettings settings = HttpSettings.create();
        settings.setUseCaches(false);
        assertFalse(settings.getUseCaches());
    }

    @Test
    public void testDoInputDefault() {
        HttpSettings settings = HttpSettings.create();
        assertTrue(settings.doInput());
    }

    @Test
    public void testDoInputSet() {
        HttpSettings settings = HttpSettings.create();
        HttpSettings result = settings.doInput(false);
        assertFalse(settings.doInput());
        assertEquals(settings, result);
    }

    @Test
    public void testDoOutputDefault() {
        HttpSettings settings = HttpSettings.create();
        assertTrue(settings.doOutput());
    }

    @Test
    public void testDoOutputSet() {
        HttpSettings settings = HttpSettings.create();
        HttpSettings result = settings.doOutput(false);
        assertFalse(settings.doOutput());
        assertEquals(settings, result);
    }

    @Test
    public void testIsOneWayRequestDefault() {
        HttpSettings settings = HttpSettings.create();
        assertFalse(settings.isOneWayRequest());
    }

    @Test
    public void testIsOneWayRequestSet() {
        HttpSettings settings = HttpSettings.create();
        HttpSettings result = settings.isOneWayRequest(true);
        assertTrue(settings.isOneWayRequest());
        assertEquals(settings, result);
    }

    @Test
    public void testGetContentFormatDefault() {
        HttpSettings settings = HttpSettings.create();
        assertNull(settings.getContentFormat());
    }

    @Test
    public void testSetContentFormat() {
        HttpSettings settings = HttpSettings.create();
        HttpSettings result = settings.setContentFormat(ContentFormat.JSON);
        assertEquals(ContentFormat.JSON, settings.getContentFormat());
        assertEquals(settings, result);
    }

    @Test
    public void testSetContentFormatWithEncoding() {
        HttpSettings settings = HttpSettings.create();
        settings.setContentFormat(ContentFormat.JSON_GZIP);
        assertEquals(ContentFormat.JSON_GZIP, settings.getContentFormat());
    }

    @Test
    public void testGetContentTypeDefault() {
        HttpSettings settings = HttpSettings.create();
        assertNull(settings.getContentType());
    }

    @Test
    public void testSetContentType() {
        HttpSettings settings = HttpSettings.create();
        HttpSettings result = settings.setContentType("application/json");
        assertEquals("application/json", settings.getContentType());
        assertEquals(settings, result);
    }

    @Test
    public void testGetContentTypeFromFormat() {
        HttpSettings settings = HttpSettings.create();
        settings.setContentFormat(ContentFormat.JSON);
        assertEquals("application/json", settings.getContentType());
    }

    @Test
    public void testGetContentEncodingDefault() {
        HttpSettings settings = HttpSettings.create();
        assertNull(settings.getContentEncoding());
    }

    @Test
    public void testSetContentEncoding() {
        HttpSettings settings = HttpSettings.create();
        HttpSettings result = settings.setContentEncoding("gzip");
        assertEquals("gzip", settings.getContentEncoding());
        assertEquals(settings, result);
    }

    @Test
    public void testGetContentEncodingFromFormat() {
        HttpSettings settings = HttpSettings.create();
        settings.setContentFormat(ContentFormat.JSON_GZIP);
        assertEquals("gzip", settings.getContentEncoding());
    }

    @Test
    public void testBasicAuth() {
        HttpSettings settings = HttpSettings.create();
        HttpSettings result = settings.basicAuth("user", "password");
        assertNotNull(settings.headers().get(HttpHeaders.Names.AUTHORIZATION));
        assertEquals(settings, result);
    }

    @Test
    public void testHeaderSingle() {
        HttpSettings settings = HttpSettings.create();
        HttpSettings result = settings.header("X-Custom-Header", "custom-value");
        assertEquals("custom-value", settings.headers().get("X-Custom-Header"));
        assertEquals(settings, result);
    }

    @Test
    public void testHeadersTwo() {
        HttpSettings settings = HttpSettings.create();
        HttpSettings result = settings.headers("Header1", "value1", "Header2", "value2");
        assertEquals("value1", settings.headers().get("Header1"));
        assertEquals("value2", settings.headers().get("Header2"));
        assertEquals(settings, result);
    }

    @Test
    public void testHeadersThree() {
        HttpSettings settings = HttpSettings.create();
        HttpSettings result = settings.headers("Header1", "value1", "Header2", "value2", "Header3", "value3");
        assertEquals("value1", settings.headers().get("Header1"));
        assertEquals("value2", settings.headers().get("Header2"));
        assertEquals("value3", settings.headers().get("Header3"));
        assertEquals(settings, result);
    }

    @Test
    public void testHeadersMap() {
        HttpSettings settings = HttpSettings.create();
        Map<String, String> headers = new HashMap<>();
        headers.put("Header1", "value1");
        headers.put("Header2", "value2");

        HttpSettings result = settings.headers(headers);
        assertEquals("value1", settings.headers().get("Header1"));
        assertEquals("value2", settings.headers().get("Header2"));
        assertEquals(settings, result);
    }

    @Test
    public void testHeadersHttpHeaders() {
        HttpSettings settings = HttpSettings.create();
        HttpHeaders headers = HttpHeaders.create()
                .set("Header1", "value1")
                .set("Header2", "value2");

        HttpSettings result = settings.headers(headers);
        assertEquals("value1", settings.headers().get("Header1"));
        assertEquals("value2", settings.headers().get("Header2"));
        assertEquals(settings, result);
    }

    @Test
    public void testHeadersHttpHeadersNull() {
        HttpSettings settings = HttpSettings.create();
        settings.header("OldHeader", "oldValue");

        settings.headers((HttpHeaders) null);
        assertNull(settings.headers().get("OldHeader"));
    }

    @Test
    public void testHeadersGetter() {
        HttpSettings settings = HttpSettings.create();
        HttpHeaders headers = settings.headers();
        assertNotNull(headers);
    }

    @Test
    public void testCopy() {
        HttpSettings original = HttpSettings.create()
                .setConnectionTimeout(5000L)
                .setReadTimeout(10000L)
                .setUseCaches(true)
                .doInput(false)
                .doOutput(false)
                .isOneWayRequest(true)
                .setContentFormat(ContentFormat.JSON)
                .header("X-Custom", "value");

        HttpSettings copy = original.copy();
        assertNotNull(copy);
        assertEquals(5000L, copy.getConnectionTimeout());
        assertEquals(10000L, copy.getReadTimeout());
        assertTrue(copy.getUseCaches());
        assertFalse(copy.doInput());
        assertFalse(copy.doOutput());
        assertTrue(copy.isOneWayRequest());
        assertEquals(ContentFormat.JSON, copy.getContentFormat());
        assertEquals("value", copy.headers().get("X-Custom"));

        // Verify it's a true copy
        copy.setConnectionTimeout(1000L);
        assertEquals(5000L, original.getConnectionTimeout());
    }

    @Test
    public void testCopyWithoutHeaders() {
        HttpSettings original = HttpSettings.create()
                .setConnectionTimeout(5000L);

        HttpSettings copy = original.copy();
        assertNotNull(copy);
        assertEquals(5000L, copy.getConnectionTimeout());
    }

    @Test
    public void testCopyWithSSLSocketFactory() throws Exception {
        HttpSettings original = HttpSettings.create();
        SSLContext sslContext = SSLContext.getDefault();
        SSLSocketFactory factory = sslContext.getSocketFactory();
        original.setSSLSocketFactory(factory);

        HttpSettings copy = original.copy();
        assertEquals(factory, copy.getSSLSocketFactory());
    }

    @Test
    public void testCopyWithProxy() {
        HttpSettings original = HttpSettings.create();
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxy.example.com", 8080));
        original.setProxy(proxy);

        HttpSettings copy = original.copy();
        assertEquals(proxy, copy.getProxy());
    }

    @Test
    public void testToString() {
        HttpSettings settings = HttpSettings.create()
                .setConnectionTimeout(5000L)
                .setReadTimeout(10000L);

        String str = settings.toString();
        assertNotNull(str);
        assertTrue(str.contains("connectionTimeout"));
        assertTrue(str.contains("5000"));
        assertTrue(str.contains("readTimeout"));
        assertTrue(str.contains("10000"));
    }

    @Test
    public void testMethodChaining() {
        HttpSettings settings = HttpSettings.create()
                .setConnectionTimeout(5000L)
                .setReadTimeout(10000L)
                .setUseCaches(false)
                .doInput(true)
                .doOutput(true)
                .isOneWayRequest(false)
                .setContentFormat(ContentFormat.JSON)
                .setContentType("application/json")
                .setContentEncoding("gzip")
                .header("Accept", "application/json");

        assertEquals(5000L, settings.getConnectionTimeout());
        assertEquals(10000L, settings.getReadTimeout());
        assertFalse(settings.getUseCaches());
        assertTrue(settings.doInput());
        assertTrue(settings.doOutput());
        assertFalse(settings.isOneWayRequest());
        assertEquals(ContentFormat.JSON, settings.getContentFormat());
    }

    @Test
    public void testMultipleHeaderCalls() {
        HttpSettings settings = HttpSettings.create();
        settings.header("Header1", "value1");
        settings.header("Header2", "value2");
        settings.header("Header3", "value3");

        assertEquals("value1", settings.headers().get("Header1"));
        assertEquals("value2", settings.headers().get("Header2"));
        assertEquals("value3", settings.headers().get("Header3"));
    }

    @Test
    public void testOverwriteHeader() {
        HttpSettings settings = HttpSettings.create();
        settings.header("X-Custom", "original");
        settings.header("X-Custom", "updated");

        assertEquals("updated", settings.headers().get("X-Custom"));
    }

    @Test
    public void testSetContentFormatNONE() {
        HttpSettings settings = HttpSettings.create();
        settings.setContentFormat(ContentFormat.NONE);
        assertEquals(ContentFormat.NONE, settings.getContentFormat());
    }

    @Test
    public void testBasicAuthWithObjectPassword() {
        HttpSettings settings = HttpSettings.create();
        settings.basicAuth("user", Integer.valueOf(12345));
        assertNotNull(settings.headers().get(HttpHeaders.Names.AUTHORIZATION));
    }
}

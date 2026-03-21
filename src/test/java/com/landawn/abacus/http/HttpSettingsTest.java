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

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class HttpSettingsTest extends TestBase {

    // --- constructor ---

    @Test
    public void testConstructor() {
        HttpSettings settings = new HttpSettings();
        assertNotNull(settings);
    }

    @Test
    public void testSetDoInput() {
        HttpSettings settings = new HttpSettings();
        HttpSettings result = settings.doInput(false);
        assertSame(settings, result);
        assertFalse(settings.doInput());
    }

    @Test
    public void testSetDoOutput() {
        HttpSettings settings = new HttpSettings();
        HttpSettings result = settings.doOutput(false);
        assertSame(settings, result);
        assertFalse(settings.doOutput());
    }

    @Test
    public void testSetIsOneWayRequest() {
        HttpSettings settings = new HttpSettings();
        HttpSettings result = settings.setOneWayRequest(true);
        assertSame(settings, result);
        assertTrue(settings.isOneWayRequest());
    }

    @Test
    public void testGetUseCachesDefault() {
        HttpSettings settings = HttpSettings.create();
        assertFalse(settings.useCaches());
    }

    @Test
    public void testSetUseCaches() {
        HttpSettings settings = HttpSettings.create();
        HttpSettings result = settings.useCaches(true);
        assertTrue(settings.useCaches());
        assertEquals(settings, result);
    }

    @Test
    public void testSetUseCachesFalse() {
        HttpSettings settings = HttpSettings.create();
        settings.useCaches(false);
        assertFalse(settings.useCaches());
    }

    @Test
    public void testOverwriteHeader() {
        HttpSettings settings = HttpSettings.create();
        settings.header("X-Custom", "original");
        settings.header("X-Custom", "updated");

        assertEquals("updated", settings.headers().get("X-Custom"));
    }

    // --- create ---

    @Test
    public void testCreate() {
        HttpSettings settings = HttpSettings.create();
        assertNotNull(settings);
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
    public void testGetconnectTimeoutDefault() {
        HttpSettings settings = HttpSettings.create();
        assertEquals(0L, settings.getConnectTimeout());
    }

    // --- getConnectTimeout / setConnectTimeout ---

    @Test
    public void testGetConnectTimeout() {
        HttpSettings settings = new HttpSettings();
        assertEquals(0, settings.getConnectTimeout());

        settings.setConnectTimeout(5000L);
        assertEquals(5000L, settings.getConnectTimeout());
    }

    @Test
    public void testSetConnectTimeout() {
        HttpSettings settings = HttpSettings.create();
        HttpSettings result = settings.setConnectTimeout(5000L);
        assertEquals(5000L, settings.getConnectTimeout());
        assertEquals(settings, result); // Verify method chaining
    }

    @Test
    public void testSetConnectTimeout_Zero() {
        HttpSettings settings = HttpSettings.create();
        HttpSettings result = settings.setConnectTimeout(0L);
        assertEquals(0L, settings.getConnectTimeout());
        assertSame(settings, result);
    }

    @Test
    public void testGetReadTimeoutDefault() {
        HttpSettings settings = HttpSettings.create();
        assertEquals(0L, settings.getReadTimeout());
    }

    // --- getReadTimeout / setReadTimeout ---

    @Test
    public void testGetReadTimeout() {
        HttpSettings settings = new HttpSettings();
        assertEquals(0, settings.getReadTimeout());

        settings.setReadTimeout(8000L);
        assertEquals(8000L, settings.getReadTimeout());
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

    // --- getSSLSocketFactory / setSSLSocketFactory ---

    @Test
    public void testGetSSLSocketFactory() {
        HttpSettings settings = new HttpSettings();
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

    // --- getProxy / setProxy ---

    @Test
    public void testGetProxy() {
        HttpSettings settings = new HttpSettings();
        assertNull(settings.getProxy());
    }

    // --- useCaches (getter) / useCaches (setter) ---

    @Test
    public void testUseCaches() {
        HttpSettings settings = new HttpSettings();
        assertFalse(settings.useCaches());

        settings.useCaches(true);
        assertTrue(settings.useCaches());
    }

    // --- doInput (getter) / doInput (setter) ---

    @Test
    public void testDoInput() {
        HttpSettings settings = new HttpSettings();
        assertTrue(settings.doInput());

        settings.doInput(false);
        assertFalse(settings.doInput());
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

    // --- doOutput (getter) / doOutput (setter) ---

    @Test
    public void testDoOutput() {
        HttpSettings settings = new HttpSettings();
        assertTrue(settings.doOutput());

        settings.doOutput(false);
        assertFalse(settings.doOutput());
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
        HttpSettings result = settings.setOneWayRequest(true);
        assertTrue(settings.isOneWayRequest());
        assertEquals(settings, result);
    }

    // --- isOneWayRequest / setOneWayRequest ---

    @Test
    public void testIsOneWayRequest() {
        HttpSettings settings = new HttpSettings();
        assertFalse(settings.isOneWayRequest());

        settings.setOneWayRequest(true);
        assertTrue(settings.isOneWayRequest());
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
    public void testGetContentFormatDefault() {
        HttpSettings settings = HttpSettings.create();
        assertNull(settings.getContentFormat());
    }

    @Test
    public void testGetContentFormat_NoneWithHeaders() {
        HttpSettings settings = HttpSettings.create();
        settings.setContentFormat(ContentFormat.NONE);
        settings.header(HttpHeaders.Names.CONTENT_TYPE, "application/json");

        ContentFormat format = settings.getContentFormat();
        assertNotNull(format);
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

    // --- getContentFormat / setContentFormat ---

    @Test
    public void testGetContentFormat() {
        HttpSettings settings = new HttpSettings();
        assertNull(settings.getContentFormat());

        settings.setContentFormat(ContentFormat.JSON);
        assertEquals(ContentFormat.JSON, settings.getContentFormat());
    }

    @Test
    public void testSetContentFormatNONE() {
        HttpSettings settings = HttpSettings.create();
        settings.setContentFormat(ContentFormat.NONE);
        assertEquals(ContentFormat.NONE, settings.getContentFormat());
    }

    @Test
    public void testGetContentTypeFromFormat() {
        HttpSettings settings = HttpSettings.create();
        settings.setContentFormat(ContentFormat.JSON);
        assertEquals("application/json", settings.getContentType());
    }

    @Test
    public void testGetContentTypeFromContentFormat() {
        HttpSettings settings = new HttpSettings();
        settings.setContentFormat(ContentFormat.JSON);
        assertEquals("application/json", settings.getContentType());
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

    // --- getContentType / setContentType ---

    @Test
    public void testGetContentType() {
        HttpSettings settings = new HttpSettings();
        assertNull(settings.getContentType());

        settings.setContentType("application/json");
        assertEquals("application/json", settings.getContentType());
    }

    @Test
    public void testGetContentEncodingFromFormat() {
        HttpSettings settings = HttpSettings.create();
        settings.setContentFormat(ContentFormat.JSON_GZIP);
        assertEquals("gzip", settings.getContentEncoding());
    }

    @Test
    public void testGetContentEncodingFromContentFormat() {
        HttpSettings settings = new HttpSettings();
        settings.setContentFormat(ContentFormat.JSON_GZIP);
        assertEquals("gzip", settings.getContentEncoding());
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

    // --- getContentEncoding / setContentEncoding ---

    @Test
    public void testGetContentEncoding() {
        HttpSettings settings = new HttpSettings();
        assertNull(settings.getContentEncoding());

        settings.setContentEncoding("gzip");
        assertEquals("gzip", settings.getContentEncoding());
    }

    // --- basicAuth ---

    @Test
    public void testBasicAuth() {
        HttpSettings settings = HttpSettings.create();
        HttpSettings result = settings.basicAuth("user", "password");
        assertNotNull(settings.headers().get(HttpHeaders.Names.AUTHORIZATION));
        assertEquals(settings, result);
    }

    @Test
    public void testBasicAuth_EncodedValue() {
        HttpSettings settings = HttpSettings.create();
        settings.basicAuth("user", "pass");
        String authValue = (String) settings.headers().get(HttpHeaders.Names.AUTHORIZATION);
        assertNotNull(authValue);
        assertTrue(authValue.startsWith("Basic "));
        assertTrue(authValue.length() > "Basic ".length());
    }

    @Test
    public void testBasicAuthWithObjectPassword() {
        HttpSettings settings = HttpSettings.create();
        settings.basicAuth("user", "12345");
        assertNotNull(settings.headers().get(HttpHeaders.Names.AUTHORIZATION));
    }

    // --- header ---

    @Test
    public void testHeader() {
        HttpSettings settings = new HttpSettings();
        HttpSettings result = settings.header("X-Custom-Header", "value");
        assertSame(settings, result);
        assertEquals("value", settings.headers().get("X-Custom-Header"));
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
        HttpHeaders headers = HttpHeaders.create().set("Header1", "value1").set("Header2", "value2");

        HttpSettings result = settings.headers(headers);
        assertEquals("value1", settings.headers().get("Header1"));
        assertEquals("value2", settings.headers().get("Header2"));
        assertEquals(settings, result);
    }

    // --- headers(String, Object, String, Object) ---

    @Test
    public void testHeadersWithTwoHeaders() {
        HttpSettings settings = new HttpSettings();
        HttpSettings result = settings.headers("Header1", "value1", "Header2", "value2");
        assertSame(settings, result);
        assertEquals("value1", settings.headers().get("Header1"));
        assertEquals("value2", settings.headers().get("Header2"));
    }

    // --- headers(String, Object, String, Object, String, Object) ---

    @Test
    public void testHeadersWithThreeHeaders() {
        HttpSettings settings = new HttpSettings();
        HttpSettings result = settings.headers("Header1", "value1", "Header2", "value2", "Header3", "value3");
        assertSame(settings, result);
        assertEquals("value1", settings.headers().get("Header1"));
        assertEquals("value2", settings.headers().get("Header2"));
        assertEquals("value3", settings.headers().get("Header3"));
    }

    // --- headers(Map) ---

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

    // --- headers(HttpHeaders) ---

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
    public void testHeadersHttpHeadersNull() {
        HttpSettings settings = HttpSettings.create();
        settings.header("OldHeader", "oldValue");

        settings.headers((HttpHeaders) null);
        assertNull(settings.headers().get("OldHeader"));
    }

    @Test
    public void testHeadersHttpHeaders_SameReference() {
        HttpSettings settings = HttpSettings.create();
        settings.header("Header1", "value1");
        HttpHeaders currentHeaders = settings.headers();

        HttpSettings result = settings.headers(currentHeaders);
        assertSame(settings, result);
        assertEquals("value1", settings.headers().get("Header1"));
    }

    // --- headers() getter ---

    @Test
    public void testHeaders() {
        HttpSettings settings = new HttpSettings();
        HttpHeaders headers = settings.headers();
        assertNotNull(headers);
        assertTrue(headers.isEmpty());

        assertSame(headers, settings.headers());
    }

    @Test
    public void testHeadersGetter() {
        HttpSettings settings = HttpSettings.create();
        HttpHeaders headers = settings.headers();
        assertNotNull(headers);
    }

    @Test
    public void testCopyWithProxy() {
        HttpSettings original = HttpSettings.create();
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxy.example.com", 8080));
        original.setProxy(proxy);

        HttpSettings copy = original.copy();
        assertEquals(proxy, copy.getProxy());
    }

    // --- copy ---

    @Test
    public void testCopy() {
        HttpSettings original = HttpSettings.create()
                .setConnectTimeout(5000L)
                .setReadTimeout(10000L)
                .useCaches(true)
                .doInput(false)
                .doOutput(false)
                .setOneWayRequest(true)
                .setContentFormat(ContentFormat.JSON)
                .header("X-Custom", "value");

        HttpSettings copy = original.copy();
        assertNotNull(copy);
        assertEquals(5000L, copy.getConnectTimeout());
        assertEquals(10000L, copy.getReadTimeout());
        assertTrue(copy.useCaches());
        assertFalse(copy.doInput());
        assertFalse(copy.doOutput());
        assertTrue(copy.isOneWayRequest());
        assertEquals(ContentFormat.JSON, copy.getContentFormat());
        assertEquals("value", copy.headers().get("X-Custom"));

        // Verify it's a true copy
        copy.setConnectTimeout(1000L);
        assertEquals(5000L, original.getConnectTimeout());
    }

    @Test
    public void testCopyWithoutHeaders() {
        HttpSettings original = HttpSettings.create().setConnectTimeout(5000L);

        HttpSettings copy = original.copy();
        assertNotNull(copy);
        assertEquals(5000L, copy.getConnectTimeout());
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
    public void testCopyWithSSLSocketFactory() throws Exception {
        HttpSettings original = HttpSettings.create();
        SSLContext sslContext = SSLContext.getDefault();
        SSLSocketFactory factory = sslContext.getSocketFactory();
        original.setSSLSocketFactory(factory);

        HttpSettings copy = original.copy();
        assertEquals(factory, copy.getSSLSocketFactory());
    }

    // --- toString ---

    @Test
    public void testToString() {
        HttpSettings settings = HttpSettings.create().setConnectTimeout(5000L).setReadTimeout(10000L);

        String str = settings.toString();
        assertNotNull(str);
        assertTrue(str.contains("connectTimeout"));
        assertTrue(str.contains("5000"));
        assertTrue(str.contains("readTimeout"));
        assertTrue(str.contains("10000"));
    }

    // --- integration / chaining tests ---

    @Test
    public void testMethodChaining() {
        HttpSettings settings = HttpSettings.create()
                .setConnectTimeout(5000L)
                .setReadTimeout(10000L)
                .useCaches(false)
                .doInput(true)
                .doOutput(true)
                .setOneWayRequest(false)
                .setContentFormat(ContentFormat.JSON)
                .setContentType("application/json")
                .setContentEncoding("gzip")
                .header("Accept", "application/json");

        assertEquals(5000L, settings.getConnectTimeout());
        assertEquals(10000L, settings.getReadTimeout());
        assertFalse(settings.useCaches());
        assertTrue(settings.doInput());
        assertTrue(settings.doOutput());
        assertFalse(settings.isOneWayRequest());
        assertEquals(ContentFormat.JSON, settings.getContentFormat());
    }

}

package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class HttpSettingsTest extends TestBase {

    // --- constructor ---

    @Test
    public void testCreate() {
        assertNotNull(new HttpSettings());
        assertNotNull(HttpSettings.create());
    }

    @Test
    public void testOverwriteHeader() {
        HttpSettings settings = HttpSettings.create();
        settings.header("X-Custom", "original");
        settings.header("X-Custom", "updated");

        assertEquals("updated", settings.headers().get("X-Custom"));
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
    public void testGetConnectTimeout() {
        HttpSettings settings = new HttpSettings();
        assertEquals(0, settings.getConnectTimeout());

        HttpSettings result = settings.setConnectTimeout(5000L);
        assertEquals(5000L, settings.getConnectTimeout());
        assertSame(settings, result);

        settings.setConnectTimeout(0L);
        assertEquals(0L, settings.getConnectTimeout());
    }

    @Test
    public void testTimeoutsRejectNegativeValues() {
        final HttpSettings settings = HttpSettings.create();

        assertThrows(IllegalArgumentException.class, () -> settings.setConnectTimeout(-1));
        assertThrows(IllegalArgumentException.class, () -> settings.setReadTimeout(-1));
        assertEquals(0, settings.getConnectTimeout());
        assertEquals(0, settings.getReadTimeout());
    }

    @Test
    public void testGetReadTimeout() {
        HttpSettings settings = new HttpSettings();
        assertEquals(0, settings.getReadTimeout());

        HttpSettings result = settings.setReadTimeout(8000L);
        assertEquals(8000L, settings.getReadTimeout());
        assertSame(settings, result);
    }

    @Test
    public void testSetSSLSocketFactory() throws Exception {
        HttpSettings settings = HttpSettings.create();
        assertNull(settings.getSSLSocketFactory());
        SSLContext sslContext = SSLContext.getDefault();
        SSLSocketFactory factory = sslContext.getSocketFactory();

        HttpSettings result = settings.setSSLSocketFactory(factory);
        assertEquals(factory, settings.getSSLSocketFactory());
        assertEquals(settings, result);
    }

    @Test
    public void testSetProxy() {
        HttpSettings settings = HttpSettings.create();
        assertNull(settings.getProxy());
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 8080));

        HttpSettings result = settings.setProxy(proxy);
        assertEquals(proxy, settings.getProxy());
        assertEquals(settings, result);
    }

    // --- getProxy / setProxy ---

    @Test
    public void testUseCaches() {
        HttpSettings settings = new HttpSettings();
        assertFalse(settings.useCaches());

        HttpSettings result = settings.useCaches(true);
        assertTrue(settings.useCaches());
        assertEquals(settings, result);

        settings.useCaches(false);
        assertFalse(settings.useCaches());
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
    public void testDoOutput() {
        HttpSettings settings = new HttpSettings();
        assertTrue(settings.doOutput());

        settings.doOutput(false);
        assertFalse(settings.doOutput());
    }

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
    public void testGetContentFormat_NoneWithHeaders() {
        HttpSettings settings = HttpSettings.create();
        settings.setContentFormat(ContentFormat.NONE);
        settings.header(HttpHeaders.Names.CONTENT_TYPE, "application/json");

        ContentFormat format = settings.getContentFormat();
        assertNotNull(format);
    }

    @Test
    public void testGetContentFormat() {
        HttpSettings settings = new HttpSettings();
        assertNull(settings.getContentFormat());

        settings.setContentFormat(ContentFormat.JSON);
        assertEquals(ContentFormat.JSON, settings.getContentFormat());
    }

    @Test
    public void testGetContentTypeFromContentFormat() {
        HttpSettings settings = new HttpSettings();
        settings.setContentFormat(ContentFormat.JSON);
        assertEquals("application/json", settings.getContentType());
    }

    @Test
    public void testGetContentTypeDoesNotMutateHeaders() {
        HttpSettings settings = HttpSettings.create().setContentFormat(ContentFormat.JSON);

        // Derives from the content format...
        assertEquals("application/json", settings.getContentType());
        // ...but does not materialize a Content-Type header as a side effect.
        assertNull(settings.headers().get(HttpHeaders.Names.CONTENT_TYPE));
        assertTrue(settings.headers().isEmpty());
    }

    @Test
    public void testGetContentEncodingDoesNotMutateHeaders() {
        HttpSettings settings = HttpSettings.create().setContentFormat(ContentFormat.JSON_GZIP);

        assertEquals("gzip", settings.getContentEncoding());
        assertNull(settings.headers().get(HttpHeaders.Names.CONTENT_ENCODING));
        assertTrue(settings.headers().isEmpty());
    }

    @Test
    public void testGetContentType() {
        HttpSettings settings = new HttpSettings();
        assertNull(settings.getContentType());

        settings.setContentType("application/json");
        assertEquals("application/json", settings.getContentType());
    }

    @Test
    public void testGetContentEncodingFromContentFormat() {
        HttpSettings settings = new HttpSettings();
        settings.setContentFormat(ContentFormat.JSON_GZIP);
        assertEquals("gzip", settings.getContentEncoding());
    }

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
    public void testBasicAuth_Encoded() {
        HttpSettings settings = HttpSettings.create();
        settings.basicAuth("user", "pass");
        String authValue = (String) settings.headers().get(HttpHeaders.Names.AUTHORIZATION);
        assertNotNull(authValue);
        assertTrue(authValue.startsWith("Basic "));
        assertTrue(authValue.length() > "Basic ".length());
    }

    @Test
    public void testHeader() {
        HttpSettings settings = new HttpSettings();
        HttpSettings result = settings.header("X-Custom-Header", "value");
        assertSame(settings, result);
        assertEquals("value", settings.headers().get("X-Custom-Header"));
    }

    @Test
    public void testSetHeadersReplacesAll() {
        HttpSettings settings = HttpSettings.create().header("Old", "value");
        HttpHeaders headers = HttpHeaders.create().set("Accept", "application/json");

        HttpSettings result = settings.setHeaders(headers);
        assertSame(settings, result);
        assertEquals("application/json", settings.headers().get("Accept"));
        assertNull(settings.headers().get("Old")); // replace-all cleared the prior header
    }

    @Test
    public void testSetHeadersNullClearsAll() {
        HttpSettings settings = HttpSettings.create().header("Old", "value");

        settings.setHeaders((HttpHeaders) null);
        assertNull(settings.headers().get("Old"));
        assertTrue(settings.headers().isEmpty());
    }

    @Test
    public void testHeadersMapMergesWhileSetHeadersReplaces() {
        // headers(Map) merges with existing headers...
        HttpSettings merge = HttpSettings.create().header("Existing", "keep");
        merge.headers(Map.of("Added", "v"));
        assertEquals("keep", merge.headers().get("Existing"));
        assertEquals("v", merge.headers().get("Added"));

        // ...setHeaders(HttpHeaders) replaces all of them.
        HttpSettings replace = HttpSettings.create().header("Existing", "keep");
        replace.setHeaders(HttpHeaders.create().set("Added", "v"));
        assertNull(replace.headers().get("Existing"));
        assertEquals("v", replace.headers().get("Added"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testDeprecatedHeadersHttpHeadersDelegatesToSetHeaders() {
        HttpSettings settings = HttpSettings.create().header("Old", "value");
        settings.headers(HttpHeaders.create().set("Accept", "application/json"));
        assertEquals("application/json", settings.headers().get("Accept"));
        assertNull(settings.headers().get("Old"));
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
    public void testHeaders_SameReference() {
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
    public void testCopyWithProxy() {
        HttpSettings original = HttpSettings.create();
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 8080));
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

    // --- Bug fix: headers() lazy-init race condition ---

    @Test
    public void testHeaders_LazyInitReturnsSameInstance() {
        // Single-threaded: calling headers() twice on a fresh settings must return
        // the same HttpHeaders instance (double-checked locking must not create two).
        final HttpSettings settings = new HttpSettings();
        final HttpHeaders first = settings.headers();
        final HttpHeaders second = settings.headers();
        assertSame(first, second, "headers() must return the same instance on every call");
    }

    @Test
    public void testHeaders_HeaderSetBeforeConcurrentReadVisible() throws InterruptedException {
        // A header set on the settings object must be visible to concurrent readers.
        final HttpSettings settings = new HttpSettings();
        settings.header("X-Thread-Safe", "yes");

        final int threadCount = 10;
        final CountDownLatch done = new CountDownLatch(threadCount);
        final AtomicInteger missingCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    final Object value = settings.headers().get("X-Thread-Safe");
                    if (!"yes".equals(value)) {
                        missingCount.incrementAndGet();
                    }
                } finally {
                    done.countDown();
                }
            }).start();
        }

        done.await();
        assertEquals(0, missingCount.get(), "All threads must see the header set before concurrent access");
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

    // --- regression tests for 2026-06-11 deep-review fixes ---

    @Test
    public void testHttpRequestSettingsMergeDoesNotMutateSharedSettings() {
        // regression: HttpRequest.settings() used Beans.mergeInto, which ALIASED the caller's live
        // HttpHeaders into the request (per-request basicAuth credentials leaked back into the
        // shared template) and injected null-valued Content-Type/Content-Encoding header entries
        final HttpSettings shared = HttpSettings.create().header("Accept", "application/json").setConnectTimeout(1234);

        HttpRequest.url("http://localhost:1/never-connected").settings(shared).basicAuth("alice", "secretA");

        assertEquals(1, shared.headers().toMap().size());
        assertEquals("application/json", shared.headers().toMap().get("Accept"));
        assertFalse(shared.headers().toMap().containsKey("Authorization"));
        assertFalse(shared.headers().toMap().containsKey("Content-Type"));
        assertEquals(1234L, shared.getConnectTimeout()); // scalar settings still merged from, untouched
    }

    // ------------------------------------------------------------------------------------------
    // 2026-09-06 a04 F-5: getContentFormat() answers NONE (not null) once any header exists, and
    // HttpRequest.settings must not materialise headers on its read-only source.
    // ------------------------------------------------------------------------------------------

    @Test
    public void testGetContentFormatIsNoneWhenHeadersCarryNoContentType() {
        final HttpSettings settings = HttpSettings.create().header("Accept", "application/json");

        assertEquals(ContentFormat.NONE, settings.getContentFormat());
    }

    @Test
    public void testGetContentFormatIsNullOnlyWhenNeitherFormatNorHeaderWasEverSet() {
        final HttpSettings settings = HttpSettings.create();

        assertNull(settings.getContentFormat());
        assertTrue(settings.toString().contains("headers=null"), settings.toString());
    }

    @Test
    public void testSettingsDoesNotMaterialiseHeadersOnTheTemplate() {
        // Previously settings() called the template's headers(), which created an empty
        // HttpHeaders on it and flipped its getContentFormat() from null to NONE.
        final HttpSettings template = HttpSettings.create().setConnectTimeout(1234);
        assertNull(template.getContentFormat());

        final HttpRequest request = HttpRequest.url("http://localhost:1/never-connected").settings(template);

        assertNull(template.getContentFormat());
        assertTrue(template.toString().contains("headers=null"), template.toString());
        assertEquals(1234L, request.checkSettings().getConnectTimeout());
    }

    @Test
    public void testSettingsStillMergesTemplateHeaders() {
        final HttpSettings template = HttpSettings.create().header("Accept", "application/json").header("X-A", "1");

        final HttpRequest request = HttpRequest.url("http://localhost:1/never-connected").header("X-B", "2").settings(template);

        final Map<String, Object> merged = request.checkSettings().headers().toMap();
        assertEquals("application/json", merged.get("Accept"));
        assertEquals("1", merged.get("X-A"));
        assertEquals("2", merged.get("X-B"));
        assertEquals(2, template.headers().toMap().size()); // the template gained nothing
    }

    @Test
    public void testContentFormatOrNullReturnsTheRawFieldOnly() {
        assertNull(HttpSettings.create().setContentType("application/json").contentFormatOrNull());
        assertEquals(ContentFormat.JSON, HttpSettings.create().setContentType("application/json").getContentFormat());
        assertEquals(ContentFormat.XML, HttpSettings.create().setContentFormat(ContentFormat.XML).contentFormatOrNull());
        assertNull(HttpSettings.create().headersOrNull());
        assertNotNull(HttpSettings.create().header("A", "b").headersOrNull());
    }

}

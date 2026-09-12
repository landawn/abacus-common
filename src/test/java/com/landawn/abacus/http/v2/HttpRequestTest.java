package com.landawn.abacus.http.v2;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.http.HttpMethod;
import com.sun.net.httpserver.HttpServer;

public class HttpRequestTest extends HttpRequestTestSupport {

    @Test
    public void testCreate() {
        HttpClient client = HttpClient.newHttpClient();
        assertNotNull(HttpRequest.create(TEST_URL, client));
        assertNotNull(HttpRequest.create(testUrl, mockHttpClient));
    }

    @Test
    public void testCreate_URI() {
        HttpClient client = HttpClient.newHttpClient();
        assertNotNull(HttpRequest.create(URI.create(TEST_URL), client));
        assertNotNull(HttpRequest.create(testUri, mockHttpClient));
    }

    @Test
    public void testCreate_URIContainingQueryParams() {
        assertNotNull(HttpRequest.url(URI.create("https://httpbin.org/get?key=value")));
    }

    @Test
    public void testCreate_DifferentHttpClients() {
        HttpClient client1 = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        HttpClient client2 = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        assertNotNull(HttpRequest.create(testUrl, client1));
        assertNotNull(HttpRequest.create(testUrl, client2));
    }

    @Test
    public void testCreate_HttpsAndHttpUrls() {
        assertNotNull(HttpRequest.url("https://httpbin.org/get"));
        assertNotNull(HttpRequest.url("http://httpbin.org/get"));
    }

    @Test
    public void testCreate_URL() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        assertNotNull(HttpRequest.create(new URL(TEST_URL), client));
        assertNotNull(HttpRequest.create(new URL(testUrl), mockHttpClient));
    }

    @Test
    public void testCreate_TimeoutsUsesNewClient() {
        HttpRequest request = HttpRequest.url(testUrl, 1000L, 2000L);
        assertNotNull(request);
        assertDoesNotThrow(() -> {
            try {
                request.get();
            } catch (Exception e) {
                // Network errors are expected
            }
        });
    }

    @Test
    public void testConnectTimeout() {
        HttpRequest request = HttpRequest.url(testUrl);
        assertNotNull(request.connectTimeout(Duration.ofSeconds(10)));
        assertSame(request, request.connectTimeout(Duration.ofSeconds(5)));
        assertNotNull(HttpRequest.url(testUrl).connectTimeout(Duration.ZERO));
        assertNotNull(HttpRequest.url(testUrl).connectTimeout(Duration.ofMillis(1)));
        assertNotNull(HttpRequest.url(testUrl).connectTimeout(Duration.ofMinutes(5)));
        assertNotNull(HttpRequest.create(testUrl, HttpClient.newHttpClient()).connectTimeout(Duration.ofSeconds(10)));
    }

    @Test
    public void testConnectTimeout_OwnsReplacementClient() throws Exception {
        final HttpRequest request = HttpRequest.create(testUrl, HttpClient.newHttpClient()).connectTimeout(Duration.ofSeconds(1));

        assertEquals(true, booleanField(request, "requireNewClient"));
        assertEquals(true, booleanField(request, "closeHttpClientAfterExecution"));
    }

    @Test
    public void testConnectTimeout_EmptyDoesNotReplaceSharedClient() throws Exception {
        final HttpRequest request = HttpRequest.url(testUrl).connectTimeout(Duration.ZERO).connectTimeout((Duration) null);

        assertEquals(false, booleanField(request, "requireNewClient"));
        assertEquals(false, booleanField(request, "closeHttpClientAfterExecution"));
        assertEquals(null, field(request, "clientBuilder"));
    }

    @Test
    public void testConnectTimeout_ThenAuthenticator() {
        Authenticator auth = mock(Authenticator.class);
        assertNotNull(HttpRequest.url(testUrl).connectTimeout(Duration.ofSeconds(5)).authenticator(auth));
    }

    @Test
    public void testReadTimeout() {
        HttpRequest request = HttpRequest.url(testUrl);
        assertNotNull(request.readTimeout(Duration.ofSeconds(30)));
        assertSame(request, request.readTimeout(Duration.ofSeconds(30)));
        assertNotNull(HttpRequest.url(testUrl).readTimeout(Duration.ZERO));
        assertNotNull(HttpRequest.url(testUrl).readTimeout(Duration.ofMinutes(10)));
    }

    @Test
    public void testAuthenticator() {
        Authenticator authenticator = mock(Authenticator.class);
        HttpRequest request = HttpRequest.url(testUrl);
        assertNotNull(request.authenticator(authenticator));
        assertSame(request, request.authenticator(authenticator));
        assertNotNull(HttpRequest.create(testUrl, HttpClient.newHttpClient()).authenticator(authenticator));
    }

    @Test
    public void testAuthenticator_OwnsReplacementClient() throws Exception {
        final HttpRequest request = HttpRequest.create(testUrl, HttpClient.newHttpClient()).authenticator(mock(Authenticator.class));

        assertEquals(true, booleanField(request, "requireNewClient"));
        assertEquals(true, booleanField(request, "closeHttpClientAfterExecution"));
    }

    @Test
    public void testAuthenticator_ThenConnectTimeout() {
        Authenticator auth = mock(Authenticator.class);
        assertNotNull(HttpRequest.url(testUrl).authenticator(auth).connectTimeout(Duration.ofSeconds(5)));
    }

    @Test
    public void testBasicAuth() {
        HttpRequest request = HttpRequest.url(testUrl);
        assertNotNull(request.basicAuth("username", "password"));
        assertSame(request, request.basicAuth("user", "pass"));
        assertNotNull(HttpRequest.url(testUrl).basicAuth("", ""));
        assertNotNull(HttpRequest.url(testUrl).basicAuth("user@domain.com", "p@ss:w0rd!"));
        assertNotNull(HttpRequest.url(testUrl).basicAuth("admin", 12345));
    }

    @Test
    public void testBasicAuth_CharArrayPasswordIsEncodedAsString() throws Exception {
        HttpRequest request = HttpRequest.url(testUrl).basicAuth("user", new char[] { 'p', 'a', 's', 's' });

        java.lang.reflect.Field f = HttpRequest.class.getDeclaredField("requestBuilder");
        f.setAccessible(true);
        java.net.http.HttpRequest.Builder builder = (java.net.http.HttpRequest.Builder) f.get(request);
        java.net.http.HttpRequest built = builder.uri(URI.create(testUrl)).GET().build();

        String auth = built.headers().firstValue("Authorization").orElseThrow();
        String expected = "Basic " + java.util.Base64.getEncoder().encodeToString("user:pass".getBytes(StandardCharsets.UTF_8));
        assertEquals(expected, auth);
    }

    @Test
    public void testBasicAuth_StringOverload() throws Exception {
        HttpRequest request = HttpRequest.url(testUrl).basicAuth("user", "pass");

        java.lang.reflect.Field f = HttpRequest.class.getDeclaredField("requestBuilder");
        f.setAccessible(true);
        java.net.http.HttpRequest.Builder builder = (java.net.http.HttpRequest.Builder) f.get(request);
        java.net.http.HttpRequest built = builder.uri(URI.create(testUrl)).GET().build();

        String auth = built.headers().firstValue("Authorization").orElseThrow();
        String expected = "Basic " + java.util.Base64.getEncoder().encodeToString("user:pass".getBytes(StandardCharsets.UTF_8));
        assertEquals(expected, auth);
    }

    @Test
    public void testConnectTimeout_LongMillis() {
        HttpRequest request = HttpRequest.url(testUrl);
        assertSame(request, request.connectTimeout(5000L));
        assertSame(request, request.connectTimeout(0L));
    }

    @Test
    public void testReadTimeout_LongMillis() {
        HttpRequest request = HttpRequest.url(testUrl);
        assertSame(request, request.readTimeout(60_000L));
        assertSame(request, request.readTimeout(0L));
    }

    @Test
    public void testHeader_ReplacesPreviousValueInsteadOfAppending() throws Exception {
        HttpRequest request = HttpRequest.url(testUrl).header("Content-Type", "text/plain").header("Content-Type", "application/json");

        java.lang.reflect.Field f = HttpRequest.class.getDeclaredField("requestBuilder");
        f.setAccessible(true);
        java.net.http.HttpRequest.Builder builder = (java.net.http.HttpRequest.Builder) f.get(request);
        java.net.http.HttpRequest built = builder.uri(URI.create(testUrl)).GET().build();

        java.util.List<String> values = built.headers().allValues("Content-Type");
        assertEquals(1, values.size());
        assertEquals("application/json", values.get(0));
    }

    @Test
    public void testJsonBody_DoesNotDuplicateContentTypeHeader() throws Exception {
        HttpRequest request = HttpRequest.url(POST_URL).header("Content-Type", "text/plain").jsonBody("{\"k\":\"v\"}");

        java.lang.reflect.Field f = HttpRequest.class.getDeclaredField("requestBuilder");
        f.setAccessible(true);
        java.net.http.HttpRequest.Builder builder = (java.net.http.HttpRequest.Builder) f.get(request);
        java.net.http.HttpRequest built = builder.uri(URI.create(POST_URL)).POST(BodyPublishers.ofString("{\"k\":\"v\"}")).build();

        java.util.List<String> values = built.headers().allValues("Content-Type");
        assertEquals(1, values.size());
        assertEquals("application/json", values.get(0));
    }

    @Test
    public void testHeader() {
        HttpRequest request = HttpRequest.url(testUrl);
        assertNotNull(request.header("Accept", "application/json"));
        assertSame(request, request.header("Accept", "application/json"));
        assertNotNull(HttpRequest.url(testUrl).header("X-Request-Id", 12345).header("X-Debug", true).header("X-Empty", "").header("X-Timestamp", 1L));
    }

    @Test
    public void testHeaders_Two() {
        HttpRequest request = HttpRequest.url(testUrl);
        assertNotNull(request.headers("Accept", "application/json", "User-Agent", "TestAgent"));
        assertSame(request, request.headers("Accept", "application/json", "User-Agent", "TestAgent"));
        assertNotNull(HttpRequest.url(testUrl).headers("X-Id", 100, "X-Count", 200));
    }

    @Test
    public void testHeaders_Three() {
        HttpRequest request = HttpRequest.url(testUrl);
        assertNotNull(request.headers("Accept", "application/json", "User-Agent", "TestAgent", "X-Custom", "value"));
        assertSame(request, request.headers("Accept", "application/json", "User-Agent", "TestAgent", "X-Custom", "value"));
        assertNotNull(HttpRequest.url(testUrl).headers("X-String", "abc", "X-Int", 42, "X-Bool", true));
    }

    @Test
    public void testHeaders_Map() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        headers.put("User-Agent", "TestAgent");
        HttpRequest request = HttpRequest.url(testUrl);
        assertNotNull(request.headers(headers));
        assertSame(request, request.headers(headers));
        assertNotNull(HttpRequest.url(testUrl).headers((Map<String, String>) null));
        assertNotNull(HttpRequest.url(testUrl).headers(new HashMap<>()));
    }

    @Test
    public void testQuery() {
        HttpRequest request = HttpRequest.url(testUrl);
        assertNotNull(request.query("param1=value1&param2=value2"));
        assertSame(request, request.query("param=value"));
        assertNotNull(HttpRequest.url(testUrl).query(""));
        assertNotNull(HttpRequest.url(testUrl).query((String) null));
        assertNotNull(HttpRequest.url(testUrl).query("q=hello+world&lang=en"));
    }

    @Test
    public void testQuery_Map() {
        Map<String, Object> params = new HashMap<>();
        params.put("param1", "value1");
        params.put("param2", 123);
        HttpRequest request = HttpRequest.url(testUrl);
        assertNotNull(request.query(params));
        assertSame(request, request.query(params));
        assertNotNull(HttpRequest.url(testUrl).query(new HashMap<>()));
        assertNotNull(HttpRequest.url(testUrl).query((Map<String, Object>) null));
    }

    @Test
    public void testJsonBody() {
        HttpRequest request = HttpRequest.url(testUrl);
        assertNotNull(request.jsonBody("{\"key\":\"value\"}"));
        assertSame(request, request.jsonBody("{\"key\":\"value\"}"));
        assertNotNull(HttpRequest.url(testUrl).jsonBody("{}"));
        assertNotNull(HttpRequest.url(testUrl).jsonBody("[1,2,3]"));
    }

    @Test
    public void testJsonBody_Object() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "test");
        data.put("value", 123);
        HttpRequest request = HttpRequest.url(testUrl);
        assertNotNull(request.jsonBody(data));
        assertSame(request, request.jsonBody(data));
        assertNotNull(HttpRequest.url(testUrl).jsonBody(new HashMap<>()));

        TestBean bean = new TestBean();
        bean.setName("test");
        bean.setValue(42);
        assertNotNull(HttpRequest.url(testUrl).jsonBody(bean));
    }

    @Test
    public void testXmlBody() {
        HttpRequest request = HttpRequest.url(testUrl);
        assertNotNull(request.xmlBody("<root><key>value</key></root>"));
        assertSame(request, request.xmlBody("<root/>"));
        assertNotNull(HttpRequest.url(testUrl).xmlBody("<root/>"));
    }

    @Test
    public void testXmlBody_Object() {
        Map<String, String> obj = new HashMap<>();
        obj.put("key", "value");
        HttpRequest request = HttpRequest.url(testUrl);
        assertNotNull(request.xmlBody(obj));
        assertSame(request, request.xmlBody(obj));
        assertNotNull(HttpRequest.url(testUrl).xmlBody(new HashMap<>()));

        TestBean bean = new TestBean();
        bean.setName("xmlTest");
        bean.setValue(99);
        assertNotNull(HttpRequest.url(testUrl).xmlBody(bean));
    }

    @Test
    public void testFormBody() {
        Map<String, String> formData = new HashMap<>();
        formData.put("username", "testuser");
        formData.put("password", "testpass");
        HttpRequest request = HttpRequest.url(testUrl);
        assertNotNull(request.formBody(formData));
        assertSame(request, request.formBody(formData));
        assertNotNull(HttpRequest.url(testUrl).formBody(new HashMap<>()));
    }

    @Test
    public void testFormBody_Bean() {
        TestBean bean = new TestBean();
        bean.setName("test");
        bean.setValue(123);
        HttpRequest request = HttpRequest.url(testUrl);
        assertNotNull(request.formBody(bean));
        assertSame(request, request.formBody(bean));
        assertNotNull(HttpRequest.url(testUrl).formBody(new TestBean()));
    }

    @Test
    public void testFormBody_ThenJsonBody() {
        Map<String, String> formData = new HashMap<>();
        formData.put("key", "value");
        HttpRequest request = HttpRequest.url(testUrl);
        request.formBody(formData);
        request.jsonBody("{\"key\":\"value\"}");
        assertNotNull(request);
    }

    @Test
    public void testBody() {
        HttpRequest request = HttpRequest.url(testUrl);
        assertNotNull(request.body(BodyPublishers.ofString("test data")));
        assertSame(request, request.body(BodyPublishers.ofString("test")));
        assertNotNull(HttpRequest.url(testUrl).body(BodyPublishers.noBody()));
        assertNotNull(HttpRequest.url(testUrl).body(BodyPublishers.ofByteArray(new byte[] { 1, 2, 3 })));
        assertNotNull(HttpRequest.url(testUrl).body(BodyPublishers.ofByteArray(new byte[0])));
    }

    @Test
    public void testGet() throws Exception {
        final HttpServer server = startLocalServer("get body");

        try {
            HttpResponse<String> response = HttpRequest.url(localUrl(server), 1_000L, 5_000L).get();
            assertNotNull(response);
            assertEquals(200, response.statusCode());
            assertEquals("get body", response.body());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testGet_BodyHandler() throws Exception {
        final HttpServer server = startLocalServer("handler body");

        try {
            HttpResponse<String> response = HttpRequest.url(localUrl(server), 1_000L, 5_000L).get(BodyHandlers.ofString());
            assertNotNull(response);
            assertEquals(200, response.statusCode());
            assertEquals("handler body", response.body());
        } finally {
            server.stop(0);
        }

        HttpResponse<byte[]> bytes = HttpRequest.url(TEST_URL).get(BodyHandlers.ofByteArray());
        assertNotNull(bytes);
        assertEquals(200, bytes.statusCode());

        HttpResponse<Void> discarded = HttpRequest.url(TEST_URL).get(BodyHandlers.discarding());
        assertNotNull(discarded);
        assertEquals(200, discarded.statusCode());
    }

    @Test
    public void testGet_ResultClass() {
        String result = HttpRequest.url(TEST_URL).get(String.class);
        assertNotNull(result);

        byte[] bytes = HttpRequest.url(TEST_URL).get(byte[].class);
        assertNotNull(bytes);
    }

    @Test
    public void testGet_Query() {
        Map<String, Object> params = new HashMap<>();
        params.put("key", "value");
        HttpResponse<String> mapped = HttpRequest.url(TEST_URL).query(params).get();
        assertNotNull(mapped);
        assertEquals(200, mapped.statusCode());

        HttpResponse<String> query = HttpRequest.url(TEST_URL).query("key=value&other=123").get();
        assertNotNull(query);
        assertEquals(200, query.statusCode());
    }

    @Test
    public void testPost() {
        HttpResponse<String> response = HttpRequest.url(POST_URL).jsonBody("{\"test\":\"data\"}").post();
        assertNotNull(response);
        assertEquals(200, response.statusCode());
    }

    @Test
    public void testPost_BodyHandler() {
        HttpResponse<String> stringResponse = HttpRequest.url(POST_URL).jsonBody("{\"test\":\"data\"}").post(BodyHandlers.ofString());
        assertNotNull(stringResponse);
        assertEquals(200, stringResponse.statusCode());

        HttpResponse<byte[]> bytesResponse = HttpRequest.url(POST_URL).jsonBody("{\"test\":\"data\"}").post(BodyHandlers.ofByteArray());
        assertNotNull(bytesResponse);
        assertEquals(200, bytesResponse.statusCode());
    }

    @Test
    public void testPost_ResultClass() {
        String result = HttpRequest.url(POST_URL).jsonBody("{\"test\":\"data\"}").post(String.class);
        assertNotNull(result);
    }

    @Test
    public void testPost_FormAndXmlBody() {
        Map<String, String> formData = new HashMap<>();
        formData.put("username", "testuser");
        formData.put("password", "testpass");
        assertEquals(200, HttpRequest.url(POST_URL).formBody(formData).post().statusCode());
        assertEquals(200, HttpRequest.url(POST_URL).xmlBody("<root><key>value</key></root>").post().statusCode());
    }

    @Test
    public void testPut() {
        HttpResponse<String> response = HttpRequest.url(PUT_URL).jsonBody("{\"test\":\"data\"}").put();
        assertNotNull(response);
        assertEquals(200, response.statusCode());
    }

    @Test
    public void testPut_BodyHandler() {
        HttpResponse<String> stringResponse = HttpRequest.url(PUT_URL).jsonBody("{\"test\":\"data\"}").put(BodyHandlers.ofString());
        assertNotNull(stringResponse);
        assertEquals(200, stringResponse.statusCode());

        HttpResponse<byte[]> bytesResponse = HttpRequest.url(PUT_URL).jsonBody("{\"test\":\"data\"}").put(BodyHandlers.ofByteArray());
        assertNotNull(bytesResponse);
        assertEquals(200, bytesResponse.statusCode());
    }

    @Test
    public void testPut_ResultClass() {
        String result = HttpRequest.url(PUT_URL).jsonBody("{\"test\":\"data\"}").put(String.class);
        assertNotNull(result);
    }

    @Test
    public void testPatch() {
        HttpResponse<String> response = HttpRequest.url(PATCH_URL).jsonBody("{\"test\":\"data\"}").patch();
        assertNotNull(response);
        assertEquals(200, response.statusCode());
    }

    @Test
    public void testPatch_BodyHandler() {
        HttpResponse<String> stringResponse = HttpRequest.url(PATCH_URL).jsonBody("{\"test\":\"data\"}").patch(BodyHandlers.ofString());
        assertNotNull(stringResponse);
        assertEquals(200, stringResponse.statusCode());

        HttpResponse<byte[]> bytesResponse = HttpRequest.url(PATCH_URL).jsonBody("{\"test\":\"data\"}").patch(BodyHandlers.ofByteArray());
        assertNotNull(bytesResponse);
        assertEquals(200, bytesResponse.statusCode());
    }

    @Test
    public void testPatch_ResultClass() {
        String result = HttpRequest.url(PATCH_URL).jsonBody("{\"test\":\"data\"}").patch(String.class);
        assertNotNull(result);
    }

    @Test
    public void testDelete() {
        HttpResponse<String> response = HttpRequest.url(DELETE_URL).delete();
        assertNotNull(response);
        assertEquals(200, response.statusCode());
    }

    @Test
    public void testDelete_BodyHandler() {
        HttpResponse<String> stringResponse = HttpRequest.url(DELETE_URL).delete(BodyHandlers.ofString());
        assertNotNull(stringResponse);
        assertEquals(200, stringResponse.statusCode());

        HttpResponse<byte[]> bytesResponse = HttpRequest.url(DELETE_URL).delete(BodyHandlers.ofByteArray());
        assertNotNull(bytesResponse);
        assertEquals(200, bytesResponse.statusCode());
    }

    @Test
    public void testDelete_ResultClass() {
        String result = HttpRequest.url(DELETE_URL).delete(String.class);
        assertNotNull(result);
    }

    @Test
    public void testHead() {
        HttpResponse<Void> response = HttpRequest.url(TEST_URL).head();
        assertNotNull(response);
        assertEquals(200, response.statusCode());
    }

    @Test
    public void testChainedBuilder() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");

        HttpRequest request = HttpRequest.url(testUrl)
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(30))
                .basicAuth("user", "pass")
                .headers(headers)
                .query("param=value")
                .jsonBody("{\"key\":\"value\"}");
        assertNotNull(request);
    }

    @Test
    public void testCleanupInputStreamRunsCleanupOnceWhenClosedConcurrently() throws Exception {
        final int threadCount = 32;
        final AtomicInteger delegateCloseCount = new AtomicInteger();
        final AtomicInteger cleanupCount = new AtomicInteger();
        final InputStream delegate = new InputStream() {
            @Override
            public int read() {
                return -1;
            }

            @Override
            public void close() {
                delegateCloseCount.incrementAndGet();
            }
        };

        final Class<?> cleanupStreamClass = Class.forName(HttpRequest.class.getName() + "$CleanupInputStream");
        final Constructor<?> constructor = cleanupStreamClass.getDeclaredConstructor(InputStream.class, Runnable.class);
        constructor.setAccessible(true);
        final InputStream stream = (InputStream) constructor.newInstance(delegate, (Runnable) cleanupCount::incrementAndGet);
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        try {
            final Future<?>[] closes = new Future<?>[threadCount];

            for (int i = 0; i < threadCount; i++) {
                closes[i] = executor.submit(() -> {
                    try {
                        stream.close();
                    } catch (final IOException e) {
                        throw new AssertionError(e);
                    }
                });
            }

            for (final Future<?> close : closes) {
                close.get(5, TimeUnit.SECONDS);
            }

            assertEquals(1, delegateCloseCount.get(), "the delegate stream must be closed exactly once");
            assertEquals(1, cleanupCount.get(), "per-request client cleanup must run exactly once");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void testGetInputStreamResultOnErrorStatusDoesNotLeakBodyStream() throws Exception {
        // On a non-2xx response, get(InputStream.class) must read AND close the deferred-cleanup body
        // stream (releasing the per-request HttpClient/connection) before throwing, rather than
        // rendering and leaking the raw stream object. The fixed getBody surfaces the actual body
        // content in the message; the old code did statusCode + ": " + body, rendering the
        // CleanupInputStream's identity and never closing it (leaking the client).
        final HttpServer server = startErrorServer(404, "not found");

        try {
            final com.landawn.abacus.exception.UncheckedIOException ex = assertThrows(com.landawn.abacus.exception.UncheckedIOException.class,
                    () -> HttpRequest.url(localUrl(server), 1_000L, 5_000L).get(InputStream.class));

            final String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
            assertEquals(true, msg.contains("404"), "message should report the status code: " + msg);
            assertEquals(true, msg.contains("not found"), "the error response body content should be read out and surfaced: " + msg);
            assertEquals(false, msg.contains("CleanupInputStream"),
                    "the raw response body stream must be read & closed, not rendered into the message: " + msg);
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testGetInputStreamErrorBodyHonorsCompressionAndCharset() throws Exception {
        final ByteArrayOutputStream compressed = new ByteArrayOutputStream();

        try (GZIPOutputStream gzip = new GZIPOutputStream(compressed)) {
            gzip.write("café".getBytes(StandardCharsets.ISO_8859_1));
        }

        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=ISO-8859-1");
            exchange.getResponseHeaders().set("Content-Encoding", "gzip");
            exchange.sendResponseHeaders(422, compressed.size());

            try (OutputStream outputStream = exchange.getResponseBody()) {
                compressed.writeTo(outputStream);
            }
        });
        server.start();

        try {
            final com.landawn.abacus.exception.UncheckedIOException ex = assertThrows(com.landawn.abacus.exception.UncheckedIOException.class,
                    () -> HttpRequest.url(localUrl(server), 1_000L, 5_000L).get(InputStream.class));
            final String message = ex.getCause() == null ? ex.getMessage() : ex.getCause().getMessage();

            assertEquals(true, message.contains("422"));
            assertEquals(true, message.contains("café"), "compressed error body should be decompressed and decoded using its declared charset: " + message);
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testStringConvenienceMethodsHonorResponseCharsetAndCompression() throws Exception {
        final ByteArrayOutputStream compressed = new ByteArrayOutputStream();

        try (GZIPOutputStream gzip = new GZIPOutputStream(compressed)) {
            gzip.write("café".getBytes(StandardCharsets.ISO_8859_1));
        }

        final HttpServer server = startResponseServer(compressed.toByteArray(), "text/plain; charset=ISO-8859-1", "gzip");

        try {
            assertEquals("café", HttpRequest.url(localUrl(server), 1_000L, 5_000L).get().body());
            assertEquals("café", HttpRequest.url(localUrl(server), 1_000L, 5_000L).asyncGet().join().body());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testBodylessResponsesIgnoreCompressionHeaders() throws Exception {
        final AtomicInteger statusCode = new AtomicInteger(200);
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            exchange.getResponseHeaders().set("Content-Encoding", "gzip");
            exchange.sendResponseHeaders(statusCode.get(), -1);
            exchange.close();
        });
        server.start();

        try {
            final String url = localUrl(server);
            assertEquals("", HttpRequest.url(url).execute(HttpMethod.HEAD).body());
            assertEquals("", HttpRequest.url(url).asyncExecute(HttpMethod.HEAD).get(5, TimeUnit.SECONDS).body());
            assertEquals("", HttpRequest.url(url).execute(HttpMethod.HEAD, String.class));
            assertEquals("", HttpRequest.url(url).asyncExecute(HttpMethod.HEAD, String.class).get(5, TimeUnit.SECONDS));

            for (final int code : new int[] { 204, 205 }) {
                statusCode.set(code);
                assertEquals("", HttpRequest.url(url).get().body());
                assertEquals("", HttpRequest.url(url).asyncGet().get(5, TimeUnit.SECONDS).body());
                assertEquals("", HttpRequest.url(url).get(String.class));
                assertEquals(0, HttpRequest.url(url).asyncGet(byte[].class).get(5, TimeUnit.SECONDS).length);
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testTypedStringUsesDeclaredResponseCharset() throws Exception {
        final byte[] body = "café".getBytes(StandardCharsets.ISO_8859_1);
        final HttpServer server = startResponseServer(body, "text/plain; charset=ISO-8859-1", null);

        try {
            assertEquals("café", HttpRequest.url(localUrl(server), 1_000L, 5_000L).get(String.class));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testTypedFormResponseUsesDeclaredCharsetForPercentEscapes() throws Exception {
        final byte[] body = "name=caf%E9".getBytes(StandardCharsets.ISO_8859_1);
        final HttpServer server = startResponseServer(body, "application/x-www-form-urlencoded; charset=ISO-8859-1", null);

        try {
            final Map<String, String> result = HttpRequest.url(localUrl(server), 1_000L, 5_000L).get(Map.class);
            assertEquals("café", result.get("name"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testTypedResponseIsDecompressedBeforeParsing() throws Exception {
        final ByteArrayOutputStream compressed = new ByteArrayOutputStream();

        try (GZIPOutputStream gzip = new GZIPOutputStream(compressed)) {
            gzip.write("{\"name\":\"compressed\",\"value\":7}".getBytes(StandardCharsets.UTF_8));
        }

        final HttpServer server = startResponseServer(compressed.toByteArray(), "application/json; charset=UTF-8", "gzip");

        try {
            final TestBean result = HttpRequest.url(localUrl(server), 1_000L, 5_000L).get(TestBean.class);
            assertEquals("compressed", result.getName());
            assertEquals(7, result.getValue());
        } finally {
            server.stop(0);
        }
    }

    // ==================== a06 F-1: owned/default clients follow redirects ====================

    @Test
    public void testDefaultAndOwnedClientsFollowRedirects() throws Exception {
        for (final int statusCode : new int[] { 301, 302, 303, 307, 308 }) {
            final HttpServer server = startRedirectServer(statusCode, "landed " + statusCode);

            try {
                final String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/redirect";
                final String expected = "landed " + statusCode;

                // shared default client
                final HttpResponse<String> response = HttpRequest.url(url).get();
                assertEquals(200, response.statusCode(), "status " + statusCode);
                assertEquals(expected, response.body());
                assertEquals(true, response.previousResponse().isPresent());
                assertEquals(statusCode, response.previousResponse().get().statusCode());
                assertEquals(expected, HttpRequest.url(url).get(String.class));
                assertEquals(expected, HttpRequest.url(url).asyncGet(String.class).get(5, TimeUnit.SECONDS));
                assertEquals(expected, HttpRequest.url(URI.create(url)).get(String.class));
                assertEquals(expected, HttpRequest.url(new URL(url)).execute(HttpMethod.GET, String.class));

                // clients owned through the timeout factories
                assertEquals(expected, HttpRequest.url(url, 1_000L, 5_000L).get(String.class));
                assertEquals(expected, HttpRequest.url(URI.create(url), 1_000L, 5_000L).get().body());
                assertEquals(expected, HttpRequest.url(new URL(url), 0L, 0L).asyncGet().get(5, TimeUnit.SECONDS).body());

                // client owned through fluent configuration of a default-client request
                assertEquals(expected, HttpRequest.url(url).connectTimeout(Duration.ofSeconds(1)).get(String.class));
                assertEquals(expected, HttpRequest.url(url).readTimeout(5_000L).connectTimeout(1_000L).asyncGet(String.class).get(5, TimeUnit.SECONDS));

                // a null caller client is the default client
                assertEquals(expected, HttpRequest.create(url, null).get(String.class));
            } finally {
                server.stop(0);
            }
        }
    }

    @Test
    public void testCallerSuppliedClientKeepsItsOwnRedirectPolicy() throws Exception {
        final HttpServer server = startRedirectServer(302, "landed");

        try {
            final String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/redirect";

            // JDK default policy is NEVER: the raw 302 is visible, and typed overloads throw.
            final HttpClient never = HttpClient.newHttpClient();
            assertEquals(302, HttpRequest.create(url, never).get().statusCode());
            assertEquals(302, HttpRequest.create(URI.create(url), never).asyncGet().get(5, TimeUnit.SECONDS).statusCode());
            final com.landawn.abacus.exception.HttpResponseException failure = assertThrows(com.landawn.abacus.exception.HttpResponseException.class,
                    () -> HttpRequest.create(url, never).get(String.class));
            assertEquals(302, failure.statusCode());

            // Fluent configuration copies the caller's policy into the replacement client.
            assertEquals(302, HttpRequest.create(url, never).connectTimeout(Duration.ofSeconds(1)).get().statusCode());
            assertEquals(302, HttpRequest.create(url, never).readTimeout(Duration.ofSeconds(5)).connectTimeout(Duration.ofSeconds(1)).get().statusCode());

            final HttpClient always = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
            assertEquals("landed", HttpRequest.create(url, always).get(String.class));
            assertEquals("landed", HttpRequest.create(url, always).connectTimeout(Duration.ofSeconds(1)).get(String.class));

            final HttpClient normal = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
            assertEquals("landed", HttpRequest.create(url, normal).get(String.class));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testInputStreamResultIsDecodedForCompressedResponses() throws Exception {
        final String text = "café中😀 compressed";
        final HttpServer server = startResponseServer(gzip(text), "text/plain; charset=UTF-8", "gzip");

        try {
            final String url = localUrl(server);

            try (InputStream in = HttpRequest.url(url).get(InputStream.class)) {
                assertEquals(text, new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }

            try (InputStream in = HttpRequest.url(url).asyncGet(InputStream.class).get(5, TimeUnit.SECONDS)) {
                assertEquals(text, new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }

            try (InputStream in = HttpRequest.url(url).execute(HttpMethod.GET, InputStream.class)) {
                assertEquals(text, new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }

            // owned client: the stream is readable after the call returns and released on close
            try (InputStream in = HttpRequest.url(url, 1_000L, 5_000L).get(InputStream.class)) {
                assertEquals(text, new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }

            try (InputStream in = HttpRequest.url(url, 1_000L, 5_000L).asyncGet(InputStream.class).get(5, TimeUnit.SECONDS)) {
                assertEquals(text, new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }

            // supertype path
            final Object object = HttpRequest.url(url).get(Object.class);
            assertTrue(object instanceof InputStream, String.valueOf(object));

            try (InputStream in = (InputStream) object) {
                assertEquals(text, new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }

            // single-byte read, available and skip go through the decoder too
            try (InputStream in = HttpRequest.url(url).get(InputStream.class)) {
                assertEquals('c', in.read());
                assertEquals(1L, in.skip(1));
                assertTrue(in.available() >= 0);
                final byte[] one = new byte[1];
                assertEquals(1, in.read(one));
                assertEquals("f".getBytes(StandardCharsets.UTF_8)[0], one[0]);
                assertEquals(text.substring(3), new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }

            // the other result types are unchanged
            assertEquals(text, HttpRequest.url(url).get(String.class));
            assertEquals(text, new String(HttpRequest.url(url).get(byte[].class), StandardCharsets.UTF_8));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testInputStreamResultPassesThroughPlainIdentityAndUnknownEncodings() throws Exception {
        final String text = "plain café body";

        for (final String contentEncoding : new String[] { null, "identity", "zebra" }) {
            final HttpServer server = startResponseServer(text.getBytes(StandardCharsets.UTF_8), "text/plain; charset=UTF-8", contentEncoding);

            try {
                final String url = localUrl(server);

                try (InputStream in = HttpRequest.url(url).get(InputStream.class)) {
                    assertEquals(text, new String(in.readAllBytes(), StandardCharsets.UTF_8), String.valueOf(contentEncoding));
                }

                try (InputStream in = HttpRequest.url(url, 1_000L, 5_000L).asyncGet(InputStream.class).get(5, TimeUnit.SECONDS)) {
                    assertEquals(text, new String(in.readAllBytes(), StandardCharsets.UTF_8), String.valueOf(contentEncoding));
                }

                try (InputStream in = (InputStream) HttpRequest.url(url).get(Object.class)) {
                    assertEquals(text, new String(in.readAllBytes(), StandardCharsets.UTF_8), String.valueOf(contentEncoding));
                }
            } finally {
                server.stop(0);
            }
        }

        // no Content-Type at all
        final HttpServer server = startLocalServer("plain");

        try {
            try (InputStream in = HttpRequest.url(localUrl(server)).get(InputStream.class)) {
                assertEquals("plain", new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testDecodedInputStreamResultReleasesOwnedClientOnlyWhenClosed() throws Exception {
        for (final boolean async : new boolean[] { false, true }) {
            final AtomicInteger closeCount = new AtomicInteger();
            final HttpClient ownedClient = mock(HttpClient.class, withSettings().extraInterfaces(AutoCloseable.class));
            final HttpRequest request = newOwnedSyncRequest(ownedClient, countingStream(gzip("café"), closeCount), "gzip");

            final InputStream stream = async ? request.asyncGet(InputStream.class).get(5, TimeUnit.SECONDS) : request.get(InputStream.class);

            verify((AutoCloseable) ownedClient, never()).close();
            assertEquals(0, closeCount.get());

            assertEquals("café", new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            verify((AutoCloseable) ownedClient, never()).close();
            assertEquals(0, closeCount.get());

            stream.close();
            assertEquals(1, closeCount.get(), "raw stream closed exactly once");
            verify((AutoCloseable) ownedClient, times(1)).close();

            stream.close();
            assertEquals(1, closeCount.get(), "a second close is a no-op");
            verify((AutoCloseable) ownedClient, times(1)).close();
        }
    }

    @Test
    public void testDecodedInputStreamResultWithCorruptEncodingFailsOnFirstReadAndReleasesOnce() throws Exception {
        for (final boolean async : new boolean[] { false, true }) {
            final AtomicInteger closeCount = new AtomicInteger();
            final HttpClient ownedClient = mock(HttpClient.class, withSettings().extraInterfaces(AutoCloseable.class));
            final HttpRequest request = newOwnedSyncRequest(ownedClient, countingStream("this is not gzip".getBytes(StandardCharsets.UTF_8), closeCount),
                    "gzip");

            // Delivery itself must not fail: the header is only read on first use.
            final InputStream stream = async ? request.asyncGet(InputStream.class).get(5, TimeUnit.SECONDS) : request.get(InputStream.class);
            verify((AutoCloseable) ownedClient, never()).close();
            assertEquals(0, closeCount.get());

            assertThrows(com.landawn.abacus.exception.UncheckedIOException.class, stream::read);
            assertEquals(1, closeCount.get(), "raw stream closed once after the failed wrap");
            verify((AutoCloseable) ownedClient, times(1)).close();

            // Later use reports the closed stream; nothing is closed twice.
            assertThrows(IOException.class, () -> stream.read(new byte[4], 0, 4));
            stream.close();
            assertEquals(1, closeCount.get());
            verify((AutoCloseable) ownedClient, times(1)).close();
        }

        // Same shape against a real server.
        final HttpServer server = startResponseServer("this is not gzip".getBytes(StandardCharsets.UTF_8), "text/plain; charset=UTF-8", "gzip");

        try {
            final InputStream stream = HttpRequest.url(localUrl(server), 1_000L, 5_000L).get(InputStream.class);
            assertThrows(com.landawn.abacus.exception.UncheckedIOException.class, stream::read);
            stream.close();

            // The eager byte[] path fails at the call, as before.
            assertThrows(com.landawn.abacus.exception.UncheckedIOException.class, () -> HttpRequest.url(localUrl(server)).get(String.class));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testDecodedInputStreamResultClosedBeforeAnyReadReleasesOnce() throws Exception {
        final AtomicInteger closeCount = new AtomicInteger();
        final HttpClient ownedClient = mock(HttpClient.class, withSettings().extraInterfaces(AutoCloseable.class));
        final HttpRequest request = newOwnedSyncRequest(ownedClient, countingStream(gzip("café"), closeCount), "gzip");

        final InputStream stream = request.get(InputStream.class);
        stream.close();
        assertEquals(1, closeCount.get());
        verify((AutoCloseable) ownedClient, times(1)).close();

        assertThrows(IOException.class, stream::read);
        stream.close();
        assertEquals(1, closeCount.get());
        verify((AutoCloseable) ownedClient, times(1)).close();
    }

    // ==================== a06 F-3: validation before the client is replaced ====================

    @Test
    public void testNegativeConnectTimeoutIsRejectedBeforeReplacingTheClient() throws Exception {
        final HttpClient caller = HttpClient.newHttpClient();
        final HttpRequest request = HttpRequest.create(testUrl, caller);

        assertThrows(IllegalArgumentException.class, () -> request.connectTimeout(Duration.ofMillis(-1)));
        assertThrows(IllegalArgumentException.class, () -> request.connectTimeout(Duration.ofSeconds(-30)));
        assertEquals(false, booleanField(request, "requireNewClient"));
        assertEquals(false, booleanField(request, "closeHttpClientAfterExecution"));
        assertEquals(null, field(request, "clientBuilder"));

        // The millis overload used to treat ANY non-positive value as "leave unchanged", which silently
        // discarded a meaningless negative while the Duration overload above rejects it. It now rejects a
        // negative too; 0 stays the documented "unset" sentinel and is still a no-op.
        assertThrows(IllegalArgumentException.class, () -> request.connectTimeout(-1L));
        assertSame(request, request.connectTimeout(0L));
        assertEquals(false, booleanField(request, "requireNewClient"));
        assertEquals(null, field(request, "clientBuilder"));

        // A shared-default-client request is left untouched as well.
        final HttpRequest shared = HttpRequest.url(testUrl);
        assertThrows(IllegalArgumentException.class, () -> shared.connectTimeout(Duration.ofNanos(-1)));
        assertEquals(false, booleanField(shared, "requireNewClient"));
        assertEquals(false, booleanField(shared, "closeHttpClientAfterExecution"));
        assertEquals(null, field(shared, "clientBuilder"));
        assertEquals("GET response", shared.get(String.class));

        // Valid values still replace the client (regression guard).
        final HttpRequest replaced = HttpRequest.create(testUrl, caller).connectTimeout(Duration.ofSeconds(1));
        assertEquals(true, booleanField(replaced, "requireNewClient"));
        assertEquals(true, booleanField(replaced, "closeHttpClientAfterExecution"));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testRejectedConnectTimeoutStillExecutesOnTheCallerClient() throws Exception {
        final HttpClient caller = mock(HttpClient.class, withSettings().extraInterfaces(AutoCloseable.class));
        final HttpResponse<byte[]> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("ok".getBytes(StandardCharsets.UTF_8));
        when(response.headers()).thenReturn(java.net.http.HttpHeaders.of(Map.of(), (a, b) -> true));
        when(caller.send(any(java.net.http.HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);

        final HttpRequest request = HttpRequest.create(testUrl, caller);
        assertThrows(IllegalArgumentException.class, () -> request.connectTimeout(Duration.ofMillis(-1)));

        assertEquals("ok", request.get(String.class));
        verify(caller, times(1)).send(any(java.net.http.HttpRequest.class), any(HttpResponse.BodyHandler.class));
        verify((AutoCloseable) caller, never()).close();
    }

    @Test
    public void testNullAuthenticatorIsRejectedBeforeReplacingTheClient() throws Exception {
        final HttpRequest request = HttpRequest.create(testUrl, HttpClient.newHttpClient());

        assertThrows(IllegalArgumentException.class, () -> request.authenticator(null));
        assertEquals(false, booleanField(request, "requireNewClient"));
        assertEquals(false, booleanField(request, "closeHttpClientAfterExecution"));
        assertEquals(null, field(request, "clientBuilder"));
        assertEquals("GET response", request.get(String.class));

        final HttpRequest shared = HttpRequest.url(testUrl);
        assertThrows(IllegalArgumentException.class, () -> shared.authenticator(null));
        assertEquals(null, field(shared, "clientBuilder"));

        // A real authenticator still replaces the client (regression guard).
        final Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("u", "p".toCharArray());
            }
        };
        final HttpRequest replaced = HttpRequest.url(testUrl).authenticator(authenticator);
        assertEquals(true, booleanField(replaced, "closeHttpClientAfterExecution"));
        assertEquals("GET response", replaced.get(String.class));
    }

    // ==================== a06 F-4 / F-6: pinned documented behaviour ====================

    @Test
    public void testRestrictedAndMalformedHeadersAreRejectedEagerly() {
        for (final String name : new String[] { "Connection", "Content-Length", "Expect", "Host", "Upgrade", "content-length", "HOST" }) {
            assertThrows(IllegalArgumentException.class, () -> HttpRequest.url(testUrl).header(name, "x"), name);
            assertThrows(IllegalArgumentException.class, () -> HttpRequest.url(testUrl).headers("Accept", "*/*", name, "x"), name);
            assertThrows(IllegalArgumentException.class, () -> HttpRequest.url(testUrl).headers("Accept", "*/*", "X-A", "a", name, "x"), name);
            final Map<String, Object> headers = new LinkedHashMap<>();
            headers.put(name, "x");
            assertThrows(IllegalArgumentException.class, () -> HttpRequest.url(testUrl).headers(headers), name);
        }

        assertThrows(IllegalArgumentException.class, () -> HttpRequest.url(testUrl).header(null, "x"));
        assertThrows(IllegalArgumentException.class, () -> HttpRequest.url(testUrl).header("X-Bad Name", "x"));
        assertThrows(IllegalArgumentException.class, () -> HttpRequest.url(testUrl).header("X-Name", "bad\r\nvalue"));
        assertThrows(IllegalArgumentException.class, () -> HttpRequest.url(testUrl).header("X-Name", "café中"));

        // ordinary headers are fine
        assertNotNull(HttpRequest.url(testUrl).header("Content-Type", "text/plain").header("X-Empty", "").header("User-Agent", "abacus"));
    }

    @Test
    public void testNullHeaderValueIsSentAsEmptyString() throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            final String value = exchange.getRequestHeaders().getFirst("X-Null");
            final byte[] bytes = (value == null ? "absent" : "[" + value + "]").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);

            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(bytes);
            }
        });
        server.start();

        try {
            assertEquals("[]", HttpRequest.url(localUrl(server)).header("X-Null", null).get(String.class));
            assertEquals("[]", HttpRequest.url(localUrl(server)).headers("Accept", "*/*", "X-Null", null).get(String.class));
            final Map<String, Object> headers = new HashMap<>();
            headers.put("X-Null", null);
            assertEquals("[]", HttpRequest.url(localUrl(server)).headers(headers).get(String.class));
            assertEquals("[7]", HttpRequest.url(localUrl(server)).header("X-Null", 7).get(String.class));
            assertEquals("absent", HttpRequest.url(localUrl(server)).get(String.class));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testNullCallerClientMeansTheSharedDefaultClient() throws Exception {
        assertEquals("GET response", HttpRequest.create(testUrl, null).get(String.class));
        assertEquals("GET response", HttpRequest.create(URI.create(testUrl), null).get(String.class));
        assertEquals("GET response", HttpRequest.create(new URL(testUrl), null).asyncGet(String.class).get(5, TimeUnit.SECONDS));
        assertEquals("POST response", HttpRequest.create(POST_URL, null).post(String.class));
    }

    @Test
    public void testZeroFactoryTimeoutMeansNoTimeoutAndNegativeIsRejected() throws Exception {
        // 0 is the factories' documented "no timeout" sentinel (the JDK default, unbounded).
        final HttpRequest request = HttpRequest.url(testUrl, 0L, 0L);
        final HttpClient.Builder clientBuilder = (HttpClient.Builder) field(request, "clientBuilder");
        assertNotNull(clientBuilder);
        assertEquals(true, clientBuilder.build().connectTimeout().isEmpty());
        assertEquals(true, booleanField(request, "closeHttpClientAfterExecution"));
        assertEquals("GET response", request.get(String.class));

        // A NEGATIVE value used to be accepted here as "no timeout" too - the same silent discard that
        // connectTimeout(long)/readTimeout(long) performed - while the Duration overloads rejected it.
        // Every millis entry point now refuses it, so this asserts the rejection rather than the discard.
        for (final long timeout : new long[] { -1L, Long.MIN_VALUE }) {
            assertThrows(IllegalArgumentException.class, () -> HttpRequest.url(testUrl, timeout, 0L));
            assertThrows(IllegalArgumentException.class, () -> HttpRequest.url(testUrl, 0L, timeout));
        }

        final HttpRequest bounded = HttpRequest.url(testUrl, 1_000L, 5_000L);
        assertEquals(Duration.ofSeconds(1), ((HttpClient.Builder) field(bounded, "clientBuilder")).build().connectTimeout().get());
    }

    // G04-103: decompress() ran every response body through wrapInputStream + readAllBytes, even for the formats
    // that carry no Content-Encoding, where the stream comes back unchanged - a full copy of every uncompressed
    // body. The short-circuit must leave compressed, uncompressed and empty responses reading the same.
    @Test
    public void fixG04_uncompressedResponsesSkipTheDecompressionCopy() throws Exception {
        final byte[] plain = "plain body".getBytes(StandardCharsets.UTF_8);
        HttpServer server = startResponseServer(plain, "text/plain; charset=UTF-8", null);

        try {
            assertArrayEquals(plain, HttpRequest.url(localUrl(server), 1_000L, 5_000L).get(byte[].class));
            assertEquals("plain body", HttpRequest.url(localUrl(server), 1_000L, 5_000L).get(String.class));
            assertEquals("plain body", HttpRequest.url(localUrl(server), 1_000L, 5_000L).get().body());
        } finally {
            server.stop(0);
        }

        server = startResponseServer(new byte[0], "text/plain; charset=UTF-8", null);

        try {
            assertArrayEquals(new byte[0], HttpRequest.url(localUrl(server), 1_000L, 5_000L).get(byte[].class));
            assertEquals("", HttpRequest.url(localUrl(server), 1_000L, 5_000L).get().body());
        } finally {
            server.stop(0);
        }

        final ByteArrayOutputStream compressed = new ByteArrayOutputStream();

        try (GZIPOutputStream gzip = new GZIPOutputStream(compressed)) {
            gzip.write(plain);
        }

        server = startResponseServer(compressed.toByteArray(), "text/plain; charset=UTF-8", "gzip");

        try {
            assertArrayEquals(plain, HttpRequest.url(localUrl(server), 1_000L, 5_000L).get(byte[].class));
            assertEquals("plain body", HttpRequest.url(localUrl(server), 1_000L, 5_000L).get().body());
        } finally {
            server.stop(0);
        }
    }

    // ------------------------------------------------------------------------------------------
    // 2026-09-08 spillover S3 ITEM 5 (finding 44): inside one class connectTimeout(Duration.ofMillis(-1))
    // threw while connectTimeout(-1L) was a silent no-op. Every millis entry point now rejects a negative,
    // matching the Duration overloads, HttpSettings, OkHttpRequest and the v1 HttpClient constructor.
    // ------------------------------------------------------------------------------------------

    @Test
    public void reviewFixes20260908_negativeMillisTimeoutsAreRejectedByEveryEntryPoint() throws Exception {
        final HttpRequest request = HttpRequest.url(testUrl);

        assertThrows(IllegalArgumentException.class, () -> request.connectTimeout(-1L));
        assertThrows(IllegalArgumentException.class, () -> request.connectTimeout(Long.MIN_VALUE));
        assertThrows(IllegalArgumentException.class, () -> request.readTimeout(-1L));
        assertThrows(IllegalArgumentException.class, () -> request.readTimeout(-30_000L));

        // rejected before anything is mutated: no replacement client is created and none is owned
        assertEquals(false, booleanField(request, "requireNewClient"));
        assertEquals(false, booleanField(request, "closeHttpClientAfterExecution"));
        assertEquals(null, field(request, "clientBuilder"));

        // 0 remains the "leave the current setting unchanged" sentinel on both
        assertSame(request, request.connectTimeout(0L));
        assertSame(request, request.readTimeout(0L));
        assertEquals(null, field(request, "clientBuilder"));
        assertEquals("GET response", request.get(String.class));

        // the three url(.., long, long) factories share withConnectTimeout/withReadTimeout
        assertThrows(IllegalArgumentException.class, () -> HttpRequest.url(testUrl, -1L, 0L));
        assertThrows(IllegalArgumentException.class, () -> HttpRequest.url(testUrl, 0L, -1L));
        assertThrows(IllegalArgumentException.class, () -> HttpRequest.url(new URL(testUrl), -1L, 0L));
        assertThrows(IllegalArgumentException.class, () -> HttpRequest.url(new URL(testUrl), 0L, -1L));
        assertThrows(IllegalArgumentException.class, () -> HttpRequest.url(testUri, -1L, 0L));
        assertThrows(IllegalArgumentException.class, () -> HttpRequest.url(testUri, 0L, -1L));

        // 0 still means "no timeout" there, and real values still work
        assertEquals("GET response", HttpRequest.url(testUrl, 0L, 0L).get(String.class));
        assertEquals("GET response", HttpRequest.url(testUrl, 5_000L, 5_000L).get(String.class));
        assertEquals("GET response", HttpRequest.url(testUri, 5_000L, 0L).get(String.class));
    }

    // ------------------------------------------------------------------------------------------
    // 2026-09-08 spillover S3 ITEM 3 (finding 46): a Cookie collection reaches the wire as ONE header
    // line whose cookie-pairs are separated by "; " (RFC 6265 5.4), not comma-joined.
    // ------------------------------------------------------------------------------------------

    @Test
    public void reviewFixes20260908_cookieCollectionIsSentAsOneSemicolonJoinedLine() throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

        server.createContext("/", exchange -> {
            final byte[] body = (exchange.getRequestHeaders().getFirst("Cookie") + "|" + exchange.getRequestHeaders().getFirst("Accept-Language"))
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);

            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });

        server.start();

        try {
            final String url = localUrl(server);

            // Cookie uses its own list grammar; every other multiply-valued field keeps ", ".
            assertEquals("a=1; b=2|en, fr", HttpRequest.url(url)
                    .header("Cookie", Arrays.asList("a=1", "b=2"))
                    .header("Accept-Language", Arrays.asList("en", "fr"))
                    .get(String.class));

            // the name is matched case-insensitively, and headers(Map) routes through header(..) too
            assertEquals("a=1; b=2|en, fr",
                    HttpRequest.url(url).headers(Map.of("cookie", Arrays.asList("a=1", "b=2"), "accept-language", Arrays.asList("en", "fr")))
                            .get(String.class));
        } finally {
            server.stop(0);
        }
    }

}

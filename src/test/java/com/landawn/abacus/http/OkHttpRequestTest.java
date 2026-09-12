package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.HttpResponseException;
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;

import okhttp3.CacheControl;
import okhttp3.Dispatcher;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;

public class OkHttpRequestTest extends TestBase {

    @Test
    public void testHeadersPropagatesNameAndValueValidation() {
        final Map<String, Object> nullName = new HashMap<>();
        nullName.put(null, "value");

        // Bulk headers must preserve the same validation as the single-header overload.
        assertThrows(IllegalArgumentException.class, () -> OkHttpRequest.url(baseUrl).headers(nullName));
        assertThrows(IllegalArgumentException.class, () -> OkHttpRequest.url(baseUrl).headers(Map.of("", "value")));
        assertThrows(IllegalArgumentException.class, () -> OkHttpRequest.url(baseUrl).headers(Map.of("bad\nname", "value")));
        assertThrows(IllegalArgumentException.class, () -> OkHttpRequest.url(baseUrl).headers(Map.of("X-Test", "bad\nvalue")));
        final OkHttpRequest request = OkHttpRequest.url(baseUrl);
        assertSame(request, request.headers((Map<String, ?>) null));
        assertSame(request, request.headers(Map.of()));
        assertSame(request, request.headers(Map.of("X-Test", "value")));
    }

    private MockWebServer server;
    private String baseUrl;
    private ExecutorService executor;

    @BeforeEach
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        baseUrl = server.url("/").toString();
        executor = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    public void tearDown() throws IOException {
        executor.shutdownNow();
        server.shutdown();
    }

    public static class TestBean {
        public String field1;
        public String field2;
        private String name;
        private String value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    // --- Factory methods ---

    @Test
    public void testCreateWithString() {
        OkHttpClient client = new OkHttpClient();
        OkHttpRequest request = OkHttpRequest.create("https://api.example.com", client);
        assertNotNull(request);
    }

    @Test
    public void testCreateWithHttpUrl() {
        OkHttpClient client = new OkHttpClient();
        HttpUrl httpUrl = HttpUrl.parse("https://api.example.com");
        OkHttpRequest request = OkHttpRequest.create(httpUrl, client);
        assertNotNull(request);
    }

    @Test
    public void testCreateWithURL() throws Exception {
        OkHttpClient client = new OkHttpClient();
        URL url = new URL("https://api.example.com");
        OkHttpRequest request = OkHttpRequest.create(url, client);
        assertNotNull(request);
    }

    @Test
    public void testInvalidUrlArguments() {
        assertThrows(IllegalArgumentException.class, () -> OkHttpRequest.create((String) null, new OkHttpClient()));
        assertThrows(IllegalArgumentException.class, () -> OkHttpRequest.create("", new OkHttpClient()));
    }

    @Test
    public void testUrlWithString() {
        OkHttpRequest request = OkHttpRequest.url("https://api.example.com");
        assertNotNull(request);
    }

    @Test
    public void testUrlWithHttpUrl() {
        HttpUrl httpUrl = HttpUrl.parse("https://api.example.com");
        OkHttpRequest request = OkHttpRequest.url(httpUrl);
        assertNotNull(request);
    }

    @Test
    public void testUrlWithHttpUrlAndTimeouts() {
        HttpUrl httpUrl = HttpUrl.parse("https://api.example.com");
        OkHttpRequest request = OkHttpRequest.url(httpUrl, 5000L, 10000L);
        assertNotNull(request);
    }

    @Test
    public void testUrlWithStringAndTimeouts() {
        OkHttpRequest request = OkHttpRequest.url("https://api.example.com", 5000L, 10000L);
        assertNotNull(request);
    }

    @Test
    public void testUrlWithURL() throws Exception {
        URL url = new URL("https://api.example.com");
        OkHttpRequest request = OkHttpRequest.url(url);
        assertNotNull(request);
    }

    @Test
    public void testUrlWithURLAndTimeouts() throws Exception {
        URL url = new URL("https://api.example.com");
        OkHttpRequest request = OkHttpRequest.url(url, 5000L, 10000L);
        assertNotNull(request);
    }

    @Test
    public void testInvalidUrl() {
        assertThrows(IllegalArgumentException.class, () -> OkHttpRequest.url(""));
        assertThrows(IllegalArgumentException.class, () -> OkHttpRequest.url((String) null));
    }

    // --- Configuration methods ---

    @Test
    public void testConnectTimeoutMillis() {
        OkHttpRequest request = OkHttpRequest.url("https://api.example.com").connectTimeout(5000L);
        assertNotNull(request);
    }

    @Test
    public void testConnectTimeoutDuration() {
        OkHttpRequest request = OkHttpRequest.url("https://api.example.com").connectTimeout(Duration.ofSeconds(5));
        assertNotNull(request);
    }

    @Test
    public void testReadTimeoutMillis() {
        OkHttpRequest request = OkHttpRequest.url("https://api.example.com").readTimeout(10000L);
        assertNotNull(request);
    }

    @Test
    public void testReadTimeoutDuration() {
        OkHttpRequest request = OkHttpRequest.url("https://api.example.com").readTimeout(Duration.ofSeconds(10));
        assertNotNull(request);
    }

    @Test
    public void testCacheControl() {
        CacheControl cacheControl = new CacheControl.Builder().noCache().build();
        OkHttpRequest request = OkHttpRequest.url("https://api.example.com").cacheControl(cacheControl);
        assertNotNull(request);
    }

    @Test
    public void testTag() {
        Object tag = new Object();
        OkHttpRequest request = OkHttpRequest.url("https://api.example.com").tag(tag);
        assertNotNull(request);

        OkHttpRequest requestWithNull = OkHttpRequest.url("https://api.example.com").tag(null);
        assertNotNull(requestWithNull);
    }

    @Test
    public void testTagWithType() {
        String tag = "test-tag";
        OkHttpRequest request = OkHttpRequest.url("https://api.example.com").tag(String.class, tag);
        assertNotNull(request);

        OkHttpRequest requestWithNull = OkHttpRequest.url("https://api.example.com").tag(String.class, null);
        assertNotNull(requestWithNull);
    }

    @Test
    public void testBasicAuth() {
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        OkHttpRequest result = request.basicAuth("user", "password");
        assertSame(request, result);
    }

    @Test
    public void testHeader() {
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        OkHttpRequest result = request.header("X-Custom-Header", "value");
        assertSame(request, result);
    }

    @Test
    public void testHeadersWithThreeHeaders() {
        OkHttpRequest request = OkHttpRequest.url("https://api.example.com")
                .headers("Accept", "application/json", "Content-Type", "application/json", "Authorization", "Bearer token");
        assertNotNull(request);
    }

    @Test
    public void testHeadersWithMap() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");

        OkHttpRequest request = OkHttpRequest.url("https://api.example.com").headers(headers);
        assertNotNull(request);

        OkHttpRequest requestWithEmpty = OkHttpRequest.url("https://api.example.com").headers(new HashMap<>());
        assertNotNull(requestWithEmpty);
    }

    @Test
    public void testHeadersWithHttpHeaders() {
        HttpHeaders headers = HttpHeaders.create();
        headers.set("Accept", "application/json");
        headers.set("Content-Type", "application/json");

        OkHttpRequest request = OkHttpRequest.url("https://api.example.com").setHeaders(headers);
        assertNotNull(request);

        OkHttpRequest requestWithNull = OkHttpRequest.url("https://api.example.com").setHeaders((HttpHeaders) null);
        assertNotNull(requestWithNull);

        OkHttpRequest requestWithEmpty = OkHttpRequest.url("https://api.example.com").setHeaders(HttpHeaders.create());
        assertNotNull(requestWithEmpty);
    }

    @Test
    public void testHeadersWithHttpHeadersCollectionValue() throws IOException, InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(200));

        HttpHeaders headers = HttpHeaders.create();
        headers.set("Accept", Arrays.asList("application/json", "text/plain"));

        OkHttpRequest.url(baseUrl).setHeaders(headers).execute(HttpMethod.GET).close();

        RecordedRequest recordedRequest = server.takeRequest();
        // A collection value is emitted as repeated header lines, which preserves the individual
        // values and their order; collapsing them into one comma-joined value is lossy for headers
        // such as Cookie and was additionally reordered by the HashMap the bridge used to go through.
        assertEquals(Arrays.asList("application/json", "text/plain"), recordedRequest.getHeaders().values("Accept"));
    }

    @Test
    public void testCookieCollectionIsSentAsOneSemicolonJoinedLine() throws IOException, InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(200));

        final HttpHeaders headers = HttpHeaders.create();
        headers.set("Cookie", Arrays.asList("a=1", "b=2"));

        OkHttpRequest.url(baseUrl).setHeaders(headers).execute(HttpMethod.GET).close();

        // RFC 6265 5.4: a request carries exactly one Cookie field line, its cookie-pairs separated by "; ".
        // Neither a comma join nor repeated lines is conformant here.
        assertEquals(List.of("a=1; b=2"), server.takeRequest().getHeaders().values("Cookie"));

        server.enqueue(new MockResponse().setResponseCode(200));
        OkHttpRequest.url(baseUrl).header("cookie", Arrays.asList("a=1", "b=2")).execute(HttpMethod.GET).close();
        assertEquals(List.of("a=1; b=2"), server.takeRequest().getHeaders().values("cookie"));

        server.enqueue(new MockResponse().setResponseCode(200));
        OkHttpRequest.url(baseUrl).addHeader("COOKIE", Arrays.asList("a=1", "b=2")).execute(HttpMethod.GET).close();
        assertEquals(List.of("a=1; b=2"), server.takeRequest().getHeaders().values("COOKIE"));
    }

    @Test
    public void testFormBodyMapNullKeyMessageNamesTheArgument() {
        final Map<Object, Object> formData = new HashMap<>();
        formData.put(null, "value");

        // "form field name" would have been passed through verbatim as the whole message by
        // N.checkArgNotNull(..): longer than 9 characters and containing a space.
        assertEquals("'formFieldName' cannot be null",
                assertThrows(IllegalArgumentException.class, () -> OkHttpRequest.url(baseUrl).formBody(formData)).getMessage());
    }

    @Test
    public void testHeadersWithTwoHeaders() {
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        OkHttpRequest result = request.headers("Header1", "value1", "Header2", "value2");
        assertSame(request, result);
    }

    @Test
    public void testHeadersWithMapWithCollectionValue() {
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        Map<String, Object> headers = new HashMap<>();
        headers.put("Accept", Arrays.asList("application/json", "text/plain"));
        OkHttpRequest result = request.headers(headers);
        assertSame(request, result);
    }

    @Test
    public void testHeadersWithHeaders() {
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        Headers headers = new Headers.Builder().add("Header1", "value1").add("Header2", "value2").build();
        OkHttpRequest result = request.setHeaders(headers);
        assertSame(request, result);
    }

    @Test
    public void testAddHeader() {
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        OkHttpRequest result = request.addHeader("Cookie", "sessionId=123");
        assertSame(request, result);
    }

    @Test
    public void testRemoveHeader() {
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        request.header("X-Header", "value");
        OkHttpRequest result = request.removeHeader("X-Header");
        assertSame(request, result);
    }

    @Test
    public void testQueryString() {
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        OkHttpRequest result = request.query("param1=value1&param2=value2");
        assertSame(request, result);
    }

    @Test
    public void testQueryMap() {
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        Map<String, Object> params = new HashMap<>();
        params.put("param1", "value1");
        params.put("param2", 123);
        OkHttpRequest result = request.query(params);
        assertSame(request, result);
    }

    @Test
    public void testJsonBodyString() {
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        OkHttpRequest result = request.jsonBody("{\"name\":\"John\"}");
        assertSame(request, result);
    }

    @Test
    public void testJsonBodyObject() {
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        Map<String, String> obj = new HashMap<>();
        obj.put("name", "John");
        OkHttpRequest result = request.jsonBody(obj);
        assertSame(request, result);
    }

    @Test
    public void testXmlBodyString() {
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        OkHttpRequest result = request.xmlBody("<user><n>John</n></user>");
        assertSame(request, result);
    }

    @Test
    public void testXmlBodyObject() {
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        Map<String, String> obj = new HashMap<>();
        obj.put("name", "John");
        OkHttpRequest result = request.xmlBody(obj);
        assertSame(request, result);
    }

    @Test
    public void testFormBodyMap() {
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        Map<String, String> formData = new HashMap<>();
        formData.put("field1", "value1");
        formData.put("field2", "value2");
        assertSame(request, request.formBody(formData));
        assertSame(request, request.formBody(new HashMap<>()));
    }

    @Test
    public void testFormBodyBean() {
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        TestBean bean = new TestBean();
        bean.field1 = "value1";
        bean.field2 = "value2";
        assertSame(request, request.formBody(bean));
        assertSame(request, request.formBody((Object) null));
    }

    @Test
    public void testFormBodyEmptyMapSendsFormContentType() throws IOException, InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(200));

        OkHttpRequest.url(baseUrl).formBody(new HashMap<>()).post().close();

        RecordedRequest recordedRequest = server.takeRequest();
        assertEquals("application/x-www-form-urlencoded", recordedRequest.getHeader("Content-Type"));
        assertEquals("", recordedRequest.getBody().readUtf8());
    }

    @Test
    public void testFormBodyNullBeanSendsFormContentType() throws IOException, InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(200));

        OkHttpRequest.url(baseUrl).formBody((Object) null).post().close();

        RecordedRequest recordedRequest = server.takeRequest();
        assertEquals("application/x-www-form-urlencoded", recordedRequest.getHeader("Content-Type"));
        assertEquals("", recordedRequest.getBody().readUtf8());
    }

    @Test
    public void testFormBodyInvalidObject() {
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        assertThrows(IllegalArgumentException.class, () -> request.formBody("not a bean"));
    }

    @Test
    public void testBodyWithByteArrayAndMediaType() {
        OkHttpRequest request = OkHttpRequest.url("https://api.example.com").body("test".getBytes(), MediaType.get("text/plain"));
        assertNotNull(request);
    }

    @Test
    public void testBodyWithByteArrayOffsetAndMediaType() {
        byte[] data = "test data".getBytes();
        OkHttpRequest request = OkHttpRequest.url("https://api.example.com").body(data, 0, 4, MediaType.get("text/plain"));
        assertNotNull(request);
    }

    @Test
    public void testBodyRequestBody() {
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        RequestBody body = RequestBody.create(MediaType.parse("text/plain"), "test");
        OkHttpRequest result = request.body(body);
        assertSame(request, result);
    }

    @Test
    public void testBodyStringWithMediaType() {
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        OkHttpRequest result = request.body("test content", MediaType.parse("text/plain"));
        assertSame(request, result);
    }

    @Test
    public void testBodyStringWithNullMediaType() {
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        OkHttpRequest result = request.body("test content", null);
        assertSame(request, result);
    }

    @Test
    public void testBodyBytesWithMediaType() {
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        byte[] content = "test content".getBytes();
        OkHttpRequest result = request.body(content, MediaType.parse("application/octet-stream"));
        assertSame(request, result);
    }

    @Test
    public void testBodyBytesWithOffsetAndLength() {
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        byte[] content = "test content".getBytes();
        OkHttpRequest result = request.body(content, 5, 7, MediaType.parse("application/octet-stream"));
        assertSame(request, result);
    }

    @Test
    public void testBodyWithFileAndMediaType() throws IOException {
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit();
        Files.write(tempFile.toPath(), "test content".getBytes());

        OkHttpRequest request = OkHttpRequest.url("https://api.example.com").body(tempFile, MediaType.get("text/plain"));
        assertNotNull(request);
    }

    @Test
    public void testBodyFile() throws IOException {
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit();
        OkHttpRequest result = request.body(tempFile, MediaType.parse("text/plain"));
        assertSame(request, result);
    }

    @Test
    public void testGet() throws IOException {
        server.enqueue(new MockResponse().setBody("GET response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);

        Response response = request.get();
        assertNotNull(response);
        assertEquals("GET response", response.body().string());

        IOUtil.close(response);
    }

    @Test
    public void testGetWithResultClass() throws IOException {
        server.enqueue(new MockResponse().setBody("GET response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);

        String result = request.get(String.class);
        assertEquals("GET response", result);
    }

    @Test
    public void testGetWithQuery() throws IOException, InterruptedException {
        server.enqueue(new MockResponse().setBody("Response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        request.query("param=value");

        String result = request.get(String.class);
        assertEquals("Response", result);

        RecordedRequest recordedRequest = server.takeRequest();
        assertTrue(recordedRequest.getPath().contains("param=value"));
    }

    @Test
    public void testGetWithMapQuery() throws IOException, InterruptedException {
        server.enqueue(new MockResponse().setBody("Response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        Map<String, Object> params = new HashMap<>();
        params.put("key", "value");
        params.put("num", 123);
        request.query(params);

        String result = request.get(String.class);
        assertEquals("Response", result);

        RecordedRequest recordedRequest = server.takeRequest();
        assertTrue(recordedRequest.getPath().contains("key=value"));
        assertTrue(recordedRequest.getPath().contains("num=123"));
    }

    @Test
    public void testPost() throws IOException {
        server.enqueue(new MockResponse().setBody("POST response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        request.jsonBody("{\"test\":true}");

        Response response = request.post();
        assertNotNull(response);
        assertEquals("POST response", response.body().string());

        IOUtil.close(response);
    }

    @Test
    public void testPostWithResultClass() throws IOException {
        server.enqueue(new MockResponse().setBody("POST response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        request.jsonBody("{\"test\":true}");

        String result = request.post(String.class);
        assertEquals("POST response", result);
    }

    @Test
    public void testPut() throws IOException {
        server.enqueue(new MockResponse().setBody("PUT response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        request.jsonBody("{\"test\":true}");

        Response response = request.put();
        assertNotNull(response);
        assertEquals("PUT response", response.body().string());

        IOUtil.close(response);
    }

    @Test
    public void testPutWithResultClass() throws IOException {
        server.enqueue(new MockResponse().setBody("PUT response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        request.jsonBody("{\"test\":true}");

        String result = request.put(String.class);
        assertEquals("PUT response", result);
    }

    @Test
    public void testPatch() throws IOException {
        server.enqueue(new MockResponse().setBody("PATCH response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        request.jsonBody("{\"test\":true}");

        Response response = request.patch();
        assertNotNull(response);
        assertEquals("PATCH response", response.body().string());

        IOUtil.close(response);
    }

    @Test
    public void testPatchWithResultClass() throws IOException {
        server.enqueue(new MockResponse().setBody("PATCH response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        request.jsonBody("{\"test\":true}");

        String result = request.patch(String.class);
        assertEquals("PATCH response", result);
    }

    @Test
    public void testDelete() throws IOException {
        server.enqueue(new MockResponse().setBody("DELETE response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);

        Response response = request.delete();
        assertNotNull(response);
        assertEquals("DELETE response", response.body().string());

        IOUtil.close(response);
    }

    @Test
    public void testDeleteWithResultClass() throws IOException {
        server.enqueue(new MockResponse().setBody("DELETE response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);

        String result = request.delete(String.class);
        assertEquals("DELETE response", result);
    }

    @Test
    public void testHead() throws IOException {
        server.enqueue(new MockResponse());
        OkHttpRequest request = OkHttpRequest.url(baseUrl);

        Response response = request.head();
        assertNotNull(response);

        IOUtil.close(response);
    }

    @Test
    public void testExecute() throws IOException {
        server.enqueue(new MockResponse().setBody("Execute response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);

        Response response = request.execute(HttpMethod.GET);
        assertNotNull(response);
        assertEquals("Execute response", response.body().string());

        IOUtil.close(response);
    }

    @Test
    public void testExecuteWithResultClass() throws IOException {
        server.enqueue(new MockResponse().setBody("Execute response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);

        String result = request.execute(HttpMethod.GET, String.class);
        assertEquals("Execute response", result);
    }

    @Test
    public void testExecuteWithResultClassNull() throws IOException {
        server.enqueue(new MockResponse().setBody("Execute response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);

        assertThrows(IllegalArgumentException.class, () -> request.execute(HttpMethod.GET, null));
    }

    @Test
    public void testExecuteWithHttpResponseClass() throws IOException {
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        assertThrows(IllegalArgumentException.class, () -> request.execute(HttpMethod.GET, HttpResponse.class));
    }

    @Test
    public void testExecuteWithVoidClass() throws IOException {
        server.enqueue(new MockResponse().setBody("Response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);

        Void result = request.execute(HttpMethod.GET, Void.class);
        assertNull(result);
    }

    @Test
    public void testExecuteWithByteArrayClass() throws IOException {
        server.enqueue(new MockResponse().setBody("Byte response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);

        byte[] result = request.execute(HttpMethod.GET, byte[].class);
        assertArrayEquals("Byte response".getBytes(), result);
    }

    @Test
    public void testExecuteWithJsonResponse() throws IOException {
        server.enqueue(new MockResponse().setBody("{\"name\":\"John\",\"age\":30}").setHeader("Content-Type", "application/json"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);

        Map<String, Object> result = request.execute(HttpMethod.GET, Map.class);
        assertEquals("John", result.get("name"));
        assertEquals(30, ((Number) result.get("age")).intValue());
    }

    @Test
    public void testExecuteWithFORM_URL_ENCODEDResponse() throws IOException {
        server.enqueue(new MockResponse().setBody("name=John&age=30").setHeader("Content-Type", "application/x-www-form-urlencoded"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);

        Map<String, String> result = request.execute(HttpMethod.GET, Map.class);
        assertEquals("John", result.get("name"));
        assertEquals("30", result.get("age"));
    }

    @Test
    public void testExecuteWithErrorResponse() throws IOException {
        server.enqueue(new MockResponse().setResponseCode(404).setBody("Not Found"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);

        // M11: OkHttpRequest now wraps IOException as UncheckedIOException (parity with the other builders).
        final com.landawn.abacus.exception.UncheckedIOException ex = assertThrows(com.landawn.abacus.exception.UncheckedIOException.class,
                () -> request.execute(HttpMethod.GET, String.class));
        final String message = ex.getCause() == null ? ex.getMessage() : ex.getCause().getMessage();

        assertTrue(message.contains("404"));
        assertTrue(message.contains("Not Found"), "the server's diagnostic response body should be retained: " + message);
    }

    @Test
    public void testPerRequestClientConfigurationDoesNotShutDownCallerDispatcher() throws Exception {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final OkHttpClient client = new OkHttpClient.Builder().dispatcher(new Dispatcher(executor)).build();
        server.enqueue(new MockResponse().setBody("configured"));

        try {
            assertEquals("configured", OkHttpRequest.create(baseUrl, client).readTimeout(2_000L).get(String.class));
            assertFalse(executor.isShutdown(), "a client produced by newBuilder() shares the caller's dispatcher");
            assertEquals(42, executor.submit(() -> 42).get());
        } finally {
            client.connectionPool().evictAll();
            executor.shutdownNow();
        }
    }

    @Test
    public void testExecuteWithResponseClass() throws IOException {
        server.enqueue(new MockResponse().setBody("Response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);

        Response response = request.execute(HttpMethod.GET, Response.class);
        assertNotNull(response);
        assertEquals("Response", response.body().string());

        IOUtil.close(response);
    }

    @Test
    public void testExecute_FormUrlEncodedResponseToBean() throws IOException {
        server.enqueue(new MockResponse().setHeader("Content-Type", "application/x-www-form-urlencoded").setBody("name=encoded&value=body"));

        TestBean result = OkHttpRequest.url(baseUrl).execute(HttpMethod.GET, TestBean.class);

        assertNotNull(result);
        assertEquals("encoded", result.getName());
        assertEquals("body", result.getValue());
    }

    @Test
    public void testExecute_HttpUrlWithQueryParameters() throws IOException, InterruptedException {
        server.enqueue(new MockResponse().setBody("Query response"));

        OkHttpRequest request = OkHttpRequest.url(server.url("/search"));
        request.query(Map.of("q", "hello world", "page", 2));

        String result = request.execute(HttpMethod.GET, String.class);
        RecordedRequest recordedRequest = server.takeRequest();

        assertEquals("Query response", result);
        assertEquals("hello world", recordedRequest.getRequestUrl().queryParameter("q"));
        assertEquals("2", recordedRequest.getRequestUrl().queryParameter("page"));
    }

    @Test
    public void testAsyncGet() throws Exception {
        server.enqueue(new MockResponse().setBody("Async GET response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);

        ContinuableFuture<Response> future = request.asyncGet();
        Response response = future.get();
        assertNotNull(response);
        assertEquals("Async GET response", response.body().string());

        IOUtil.close(response);
    }

    @Test
    public void testAsyncGetWithExecutor() throws Exception {
        server.enqueue(new MockResponse().setBody("Async GET response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);

        ContinuableFuture<Response> future = request.asyncGet(executor);
        Response response = future.get();
        assertNotNull(response);
        assertEquals("Async GET response", response.body().string());

        IOUtil.close(response);
    }

    @Test
    public void testAsyncGetWithResultClass() throws Exception {
        server.enqueue(new MockResponse().setBody("Async GET response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);

        ContinuableFuture<String> future = request.asyncGet(String.class);
        String result = future.get();
        assertEquals("Async GET response", result);
    }

    @Test
    public void testAsyncGetWithResultClassAndExecutor() throws Exception {
        server.enqueue(new MockResponse().setBody("Async GET response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);

        ContinuableFuture<String> future = request.asyncGet(String.class, executor);
        String result = future.get();
        assertEquals("Async GET response", result);
    }

    @Test
    public void testAsyncPost() throws Exception {
        server.enqueue(new MockResponse().setBody("Async POST response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        request.jsonBody("{\"test\":true}");

        ContinuableFuture<Response> future = request.asyncPost();
        Response response = future.get();
        assertNotNull(response);
        assertEquals("Async POST response", response.body().string());

        IOUtil.close(response);
    }

    @Test
    public void testAsyncPostWithExecutor() throws Exception {
        server.enqueue(new MockResponse().setBody("Async POST response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        request.jsonBody("{\"test\":true}");

        ContinuableFuture<Response> future = request.asyncPost(executor);
        Response response = future.get();
        assertNotNull(response);
        assertEquals("Async POST response", response.body().string());

        IOUtil.close(response);
    }

    @Test
    public void testAsyncPostWithResultClass() throws Exception {
        server.enqueue(new MockResponse().setBody("Async POST response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        request.jsonBody("{\"test\":true}");

        ContinuableFuture<String> future = request.asyncPost(String.class);
        String result = future.get();
        assertEquals("Async POST response", result);
    }

    @Test
    public void testAsyncPostWithResultClassAndExecutor() throws Exception {
        server.enqueue(new MockResponse().setBody("Async POST response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        request.jsonBody("{\"test\":true}");

        ContinuableFuture<String> future = request.asyncPost(String.class, executor);
        String result = future.get();
        assertEquals("Async POST response", result);
    }

    @Test
    public void testAsyncPut() throws Exception {
        server.enqueue(new MockResponse().setBody("Async PUT response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        request.jsonBody("{\"test\":true}");

        ContinuableFuture<Response> future = request.asyncPut();
        Response response = future.get();
        assertNotNull(response);
        assertEquals("Async PUT response", response.body().string());

        IOUtil.close(response);
    }

    @Test
    public void testAsyncPutWithExecutor() throws Exception {
        server.enqueue(new MockResponse().setBody("Async PUT response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        request.jsonBody("{\"test\":true}");

        ContinuableFuture<Response> future = request.asyncPut(executor);
        Response response = future.get();
        assertNotNull(response);
        assertEquals("Async PUT response", response.body().string());

        IOUtil.close(response);
    }

    @Test
    public void testAsyncPutWithResultClass() throws Exception {
        server.enqueue(new MockResponse().setBody("Async PUT response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        request.jsonBody("{\"test\":true}");

        ContinuableFuture<String> future = request.asyncPut(String.class);
        String result = future.get();
        assertEquals("Async PUT response", result);
    }

    @Test
    public void testAsyncPutWithResultClassAndExecutor() throws Exception {
        server.enqueue(new MockResponse().setBody("Async PUT response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        request.jsonBody("{\"test\":true}");

        ContinuableFuture<String> future = request.asyncPut(String.class, executor);
        String result = future.get();
        assertEquals("Async PUT response", result);
    }

    @Test
    public void testAsyncPatch() throws Exception {
        server.enqueue(new MockResponse().setBody("Async PATCH response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        request.jsonBody("{\"test\":true}");

        ContinuableFuture<Response> future = request.asyncPatch();
        Response response = future.get();
        assertNotNull(response);
        assertEquals("Async PATCH response", response.body().string());

        IOUtil.close(response);
    }

    @Test
    public void testAsyncPatchWithExecutor() throws Exception {
        server.enqueue(new MockResponse().setBody("Async PATCH response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        request.jsonBody("{\"test\":true}");

        ContinuableFuture<Response> future = request.asyncPatch(executor);
        Response response = future.get();
        assertNotNull(response);
        assertEquals("Async PATCH response", response.body().string());

        IOUtil.close(response);
    }

    @Test
    public void testAsyncPatchWithResultClass() throws Exception {
        server.enqueue(new MockResponse().setBody("Async PATCH response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        request.jsonBody("{\"test\":true}");

        ContinuableFuture<String> future = request.asyncPatch(String.class);
        String result = future.get();
        assertEquals("Async PATCH response", result);
    }

    @Test
    public void testAsyncPatchWithResultClassAndExecutor() throws Exception {
        server.enqueue(new MockResponse().setBody("Async PATCH response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        request.jsonBody("{\"test\":true}");

        ContinuableFuture<String> future = request.asyncPatch(String.class, executor);
        String result = future.get();
        assertEquals("Async PATCH response", result);
    }

    @Test
    public void testAsyncDelete() throws Exception {
        server.enqueue(new MockResponse().setBody("Async DELETE response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);

        ContinuableFuture<Response> future = request.asyncDelete();
        Response response = future.get();
        assertNotNull(response);
        assertEquals("Async DELETE response", response.body().string());

        IOUtil.close(response);
    }

    @Test
    public void testAsyncDeleteWithExecutor() throws Exception {
        server.enqueue(new MockResponse().setBody("Async DELETE response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);

        ContinuableFuture<Response> future = request.asyncDelete(executor);
        Response response = future.get();
        assertNotNull(response);
        assertEquals("Async DELETE response", response.body().string());

        IOUtil.close(response);
    }

    @Test
    public void testAsyncDeleteWithResultClass() throws Exception {
        server.enqueue(new MockResponse().setBody("Async DELETE response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);

        ContinuableFuture<String> future = request.asyncDelete(String.class);
        String result = future.get();
        assertEquals("Async DELETE response", result);
    }

    @Test
    public void testAsyncDeleteWithResultClassAndExecutor() throws Exception {
        server.enqueue(new MockResponse().setBody("Async DELETE response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);

        ContinuableFuture<String> future = request.asyncDelete(String.class, executor);
        String result = future.get();
        assertEquals("Async DELETE response", result);
    }

    @Test
    public void testAsyncHead() throws Exception {
        server.enqueue(new MockResponse());
        OkHttpRequest request = OkHttpRequest.url(baseUrl);

        ContinuableFuture<Response> future = request.asyncHead();
        Response response = future.get();
        assertNotNull(response);

        IOUtil.close(response);
    }

    @Test
    public void testAsyncHeadWithExecutor() throws Exception {
        server.enqueue(new MockResponse());
        OkHttpRequest request = OkHttpRequest.url(baseUrl);

        ContinuableFuture<Response> future = request.asyncHead(executor);
        Response response = future.get();
        assertNotNull(response);

        IOUtil.close(response);
    }

    @Test
    public void testAsyncExecute() throws Exception {
        server.enqueue(new MockResponse().setBody("Async Execute response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);

        ContinuableFuture<Response> future = request.asyncExecute(HttpMethod.GET);
        Response response = future.get();
        assertNotNull(response);
        assertEquals("Async Execute response", response.body().string());

        IOUtil.close(response);
    }

    @Test
    public void testAsyncExecuteWithExecutor() throws Exception {
        server.enqueue(new MockResponse().setBody("Async Execute response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);

        ContinuableFuture<Response> future = request.asyncExecute(HttpMethod.GET, executor);
        Response response = future.get();
        assertNotNull(response);
        assertEquals("Async Execute response", response.body().string());

        IOUtil.close(response);
    }

    @Test
    public void testAsyncExecuteWithResultClass() throws Exception {
        server.enqueue(new MockResponse().setBody("Async Execute response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);

        ContinuableFuture<String> future = request.asyncExecute(HttpMethod.GET, String.class);
        String result = future.get();
        assertEquals("Async Execute response", result);
    }

    @Test
    public void testAsyncExecuteWithResultClassAndExecutor() throws Exception {
        server.enqueue(new MockResponse().setBody("Async Execute response"));
        OkHttpRequest request = OkHttpRequest.url(baseUrl);

        ContinuableFuture<String> future = request.asyncExecute(HttpMethod.GET, String.class, executor);
        String result = future.get();
        assertEquals("Async Execute response", result);
    }

    // --- Bug fix: dead null-check after non-null assertion for resultClass ---

    @Test
    public void testExecute_nullResultClassIsRejected() {
        // resultClass must not be null; passing null must throw immediately.
        assertThrows(IllegalArgumentException.class, () -> OkHttpRequest.url(baseUrl).execute(HttpMethod.GET, (Class<?>) null));
    }

    @Test
    public void testExecute_voidResultClassReturnsNull() throws Exception {
        // Void.class as resultClass should return null without reading the response body.
        server.enqueue(new MockResponse().setBody("should be discarded").setResponseCode(200));
        final Object result = OkHttpRequest.url(baseUrl).execute(HttpMethod.GET, Void.class);
        assertNull(result, "Void.class result must produce a null return value");
    }

    @Test
    public void testExecute_voidResultClassStillThrowsOnHttpError() throws Exception {
        server.enqueue(new MockResponse().setBody("server error").setResponseCode(500));

        assertThrows(com.landawn.abacus.exception.UncheckedIOException.class, () -> OkHttpRequest.url(baseUrl).execute(HttpMethod.GET, Void.class));
    }

    @Test
    public void testEntityEnclosingMethodsWithoutBodySendEmptyBody() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200));
        server.enqueue(new MockResponse().setResponseCode(200));
        server.enqueue(new MockResponse().setResponseCode(200));

        IOUtil.close(OkHttpRequest.url(baseUrl).post());
        IOUtil.close(OkHttpRequest.url(baseUrl).put());
        IOUtil.close(OkHttpRequest.url(baseUrl).patch());

        RecordedRequest post = server.takeRequest();
        assertEquals("POST", post.getMethod());
        assertEquals("", post.getBody().readUtf8());

        RecordedRequest put = server.takeRequest();
        assertEquals("PUT", put.getMethod());
        assertEquals("", put.getBody().readUtf8());

        RecordedRequest patch = server.takeRequest();
        assertEquals("PATCH", patch.getMethod());
        assertEquals("", patch.getBody().readUtf8());
    }

    @Test
    public void testExecute_httpResponseClassIsRejected() {
        // HttpResponse (the abacus wrapper) is not allowed as a result type;
        // callers must use OkHttp's Response class directly.
        assertThrows(IllegalArgumentException.class, () -> OkHttpRequest.url(baseUrl).execute(HttpMethod.GET, com.landawn.abacus.http.HttpResponse.class));
    }

    @Test
    public void testExecute_responseClassReturnsOkHttpResponse() throws Exception {
        // Response.class (OkHttp) should be returned directly without deserialization.
        server.enqueue(new MockResponse().setBody("raw body").setResponseCode(200));
        Response resp = OkHttpRequest.url(baseUrl).execute(HttpMethod.GET, Response.class);
        assertNotNull(resp);
        assertEquals(200, resp.code());
        IOUtil.close(resp);
    }

    // --- B4: per-request options must not destroy connection reuse ---
    //
    // clientBuilder() used to install a private Dispatcher and ConnectionPool on every derived
    // client, and execute(Request) replaced them again and evicted the pool afterwards, so every
    // request with a timeout opened a brand-new TCP connection. A derived client now shares its
    // parent's dispatcher and pool, which is what OkHttpClient.newBuilder() does by default.

    @Test
    public void testExecuteWithCustomTimeout_completesSuccessfully() throws Exception {
        server.enqueue(new MockResponse().setBody("timeout-client body").setResponseCode(200));
        final String result = OkHttpRequest.url(baseUrl).connectTimeout(5_000L).readTimeout(10_000L).get(String.class);
        assertEquals("timeout-client body", result);
    }

    @Test
    public void testPerRequestTimeoutSharesDispatcherAndConnectionPool() throws Exception {
        final OkHttpClient base = new OkHttpClient();
        final OkHttpRequest request = OkHttpRequest.create(baseUrl, base).readTimeout(5_000L);
        final OkHttpClient.Builder builder = (OkHttpClient.Builder) getField(request, "httpClientBuilder");
        final OkHttpClient derived = builder.build();

        assertSame(base.dispatcher(), derived.dispatcher(), "a derived client must share the dispatcher");
        assertSame(base.connectionPool(), derived.connectionPool(), "a derived client must share the connection pool");
        assertEquals(5_000, derived.readTimeoutMillis());
    }

    @Test
    public void testTimeoutFactoryClientSharesDefaultClientPool() throws Exception {
        final OkHttpRequest request = OkHttpRequest.url(baseUrl, 5_000L, 10_000L);
        final OkHttpClient client = (OkHttpClient) getField(request, "httpClient");
        final OkHttpClient defaultClient = (OkHttpClient) getStaticField("DEFAULT_CLIENT");

        assertSame(defaultClient.dispatcher(), client.dispatcher());
        assertSame(defaultClient.connectionPool(), client.connectionPool());
        assertEquals(5_000, client.connectTimeoutMillis());
        assertEquals(10_000, client.readTimeoutMillis());
    }

    @Test
    public void testSequentialTimeoutRequestsReuseOneConnection() throws Exception {
        final OkHttpClient base = new OkHttpClient();
        enqueueResponses(4);

        for (int i = 0; i < 4; i++) {
            assertEquals("OK", OkHttpRequest.create(baseUrl, base).readTimeout(5_000L).get(String.class));
        }

        // RecordedRequest.getSequenceNumber() is the request's index on the connection that carried
        // it. Reuse gives 0,1,2,3; a fresh connection per request would give 0,0,0,0.
        for (int i = 0; i < 4; i++) {
            assertEquals(i, server.takeRequest().getSequenceNumber(), "request " + i + " must reuse the first connection");
        }
    }

    @Test
    public void testTimeoutRequestDoesNotShutDownTheSharedClient() throws Exception {
        server.enqueue(new MockResponse().setBody("raw body").setResponseCode(200));

        final OkHttpClient base = new OkHttpClient();
        final Response response = OkHttpRequest.create(baseUrl, base).readTimeout(5_000L).execute(HttpMethod.GET, Response.class);

        try {
            assertEquals("raw body", response.body().string());
        } finally {
            IOUtil.close(response);
        }

        assertFalse(base.dispatcher().executorService().isShutdown(), "the caller's client must never be shut down");

        server.enqueue(new MockResponse().setBody("second").setResponseCode(200));
        assertEquals("second", OkHttpRequest.create(baseUrl, base).readTimeout(5_000L).get(String.class));
    }

    @Test
    public void testTimeoutFactoryClientStaysUsableAfterExecution() throws Exception {
        server.enqueue(new MockResponse().setBody("first").setResponseCode(200));

        final OkHttpRequest request = OkHttpRequest.url(baseUrl, 5_000L, 10_000L);
        assertEquals("first", request.get(String.class));

        final OkHttpClient client = (OkHttpClient) getField(request, "httpClient");
        assertFalse(client.dispatcher().executorService().isShutdown());
        assertFalse(((OkHttpClient) getStaticField("DEFAULT_CLIENT")).dispatcher().executorService().isShutdown());
    }

    @Test
    public void testRawResponseIsStillReadableAfterExecutionReturns() throws Exception {
        server.enqueue(new MockResponse().setBody("raw body").setResponseCode(200));

        final Response response = OkHttpRequest.url(baseUrl).connectTimeout(5_000L).readTimeout(10_000L).execute(HttpMethod.GET, Response.class);

        try {
            assertEquals("raw body", response.body().string());
        } finally {
            IOUtil.close(response);
        }
    }

    @Test
    public void testRawResponseBodyIsReadableAsBytes() throws Exception {
        server.enqueue(new MockResponse().setBody("raw body").setResponseCode(200));

        final Response response = OkHttpRequest.url(baseUrl).connectTimeout(5_000L).readTimeout(10_000L).execute(HttpMethod.GET, Response.class);

        try {
            assertArrayEquals("raw body".getBytes(), response.body().bytes());
        } finally {
            IOUtil.close(response);
        }
    }

    @Test
    public void testRawResponseBodyIsReadableThroughItsSource() throws Exception {
        server.enqueue(new MockResponse().setBody("raw body").setResponseCode(200));

        final Response response = OkHttpRequest.url(baseUrl).connectTimeout(5_000L).readTimeout(10_000L).execute(HttpMethod.GET, Response.class);

        try (okio.BufferedSource source = response.body().source()) {
            assertEquals("raw body", source.readUtf8());
        } finally {
            IOUtil.close(response);
        }
    }

    @Test
    public void testTypedResponseClosesTheResponse() throws Exception {
        server.enqueue(new MockResponse().setBody("body").setResponseCode(200));

        assertEquals("body", OkHttpRequest.url(baseUrl).get(String.class));

        // A second request proves the socket from the first was released back to the pool.
        server.enqueue(new MockResponse().setBody("body2").setResponseCode(200));
        assertEquals("body2", OkHttpRequest.url(baseUrl).get(String.class));
    }

    // --- B6: error bodies are captured as structured, bounded data ---

    @Test
    public void testHttpErrorRaisesHttpResponseExceptionWithStructuredData() {
        server.enqueue(new MockResponse().setBody("boom").setResponseCode(503).setHeader("X-Trace", "abc"));

        final HttpResponseException e = assertThrows(HttpResponseException.class, () -> OkHttpRequest.url(baseUrl).get(String.class));

        assertEquals(503, e.statusCode());
        assertEquals("boom", e.responseBody());
        assertEquals("abc", e.header("x-trace"));
        assertTrue(e.getMessage().contains("503"));
    }

    @Test
    public void testLargeErrorBodyIsTruncatedInMessageAndCapture() {
        final String huge = "E".repeat(200_000);
        server.enqueue(new MockResponse().setBody(huge).setResponseCode(500));

        final HttpResponseException e = assertThrows(HttpResponseException.class, () -> OkHttpRequest.url(baseUrl).get(String.class));

        assertTrue(e.responseBody().length() <= HttpUtil.MAX_ERROR_BODY_SIZE, "captured body must be bounded: " + e.responseBody().length());
        assertTrue(e.getMessage().length() < 2_000, "message must be bounded: " + e.getMessage().length());
        assertTrue(e.getMessage().endsWith("... (truncated)"));
    }

    private static Object getStaticField(final String name) throws Exception {
        final Field field = OkHttpRequest.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }

    private void enqueueResponses(final int count) {
        for (int i = 0; i < count; i++) {
            server.enqueue(new MockResponse().setBody("OK"));
        }
    }

    @SafeVarargs
    private static void awaitAndClose(final ContinuableFuture<Response>... futures) throws Exception {
        for (final ContinuableFuture<Response> future : futures) {
            IOUtil.close(future.get(5, TimeUnit.SECONDS));
        }
    }

    @SafeVarargs
    private static void await(final ContinuableFuture<?>... futures) throws Exception {
        for (final ContinuableFuture<?> future : futures) {
            assertNotNull(future.get(5, TimeUnit.SECONDS));
        }
    }

    private static Object getField(final Object target, final String name) throws Exception {
        final Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    // ------------------------------------------------------------------------------------------------
    // a05 F-1: the request-derived format/charset fallback must see the body's media type, because
    // OkHttp never materialises Content-Type on the built Request (only in BridgeInterceptor).
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testXmlBodyDrivesFallbackFormatForContentTypeLessResponse() throws Exception {
        final TestBean bean = new TestBean();
        bean.setName("café");
        bean.setValue("v");
        final String xml = N.toXml(bean);
        server.enqueue(new MockResponse().setBody(xml)); // deliberately no Content-Type header

        final TestBean result = OkHttpRequest.url(baseUrl).xmlBody(bean).post(TestBean.class);

        assertNotNull(result);
        assertEquals("café", result.getName());
        assertEquals("v", result.getValue());

        final RecordedRequest recorded = server.takeRequest();
        assertTrue(String.valueOf(recorded.getHeader("Content-Type")).startsWith("application/xml"), recorded.getHeader("Content-Type"));
        assertEquals(xml, recorded.getBody().readUtf8());
    }

    @Test
    public void testJsonBodyStillDeserializesContentTypeLessJsonResponse() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"name\":\"n\",\"value\":\"v\"}"));

        final TestBean result = OkHttpRequest.url(baseUrl).jsonBody(Map.of("q", 1)).post(TestBean.class);

        assertEquals("n", result.getName());
        assertEquals("v", result.getValue());
    }

    @Test
    public void testBodyMediaTypeCharsetDrivesFallbackDecodingOfContentTypeLessResponse() throws Exception {
        server.enqueue(new MockResponse().setBody(new Buffer().write("café".getBytes(StandardCharsets.ISO_8859_1))));

        final String result = OkHttpRequest.url(baseUrl).body("x", MediaType.get("text/plain; charset=ISO-8859-1")).post(String.class);

        assertEquals("café", result, "Latin-1 bytes must be decoded with the request body's charset, not UTF-8");
    }

    @Test
    public void testFormBodyDrivesFallbackFormatForContentTypeLessResponse() throws Exception {
        server.enqueue(new MockResponse().setBody("a=1&b=2"));

        final Map<String, Object> result = OkHttpRequest.url(baseUrl).formBody(Map.of("k", "v")).post(Map.class);

        assertEquals("1", result.get("a"));
        assertEquals("2", result.get("b"));
        assertEquals(2, result.size());
    }

    @Test
    public void testExplicitContentTypeHeaderWinsOverBodyMediaType() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"name\":\"n\"}"));

        final TestBean result = OkHttpRequest.url(baseUrl)
                .header("Content-Type", "application/json")
                .body("<x/>", MediaType.get("application/xml"))
                .post(TestBean.class);

        // The explicitly set header drives the request-side fallback (the JSON body above was parsed as JSON,
        // not handed to the XML parser). On the wire, however, OkHttp's BridgeInterceptor overwrites
        // Content-Type from the body's media type whenever the body carries one - pinned here so the
        // two contracts are not confused.
        assertEquals("n", result.getName());
        assertEquals("application/xml; charset=utf-8", server.takeRequest().getHeader("Content-Type"));
    }

    @Test
    public void testResponseContentTypeWinsOverRequestBodyMediaType() throws Exception {
        server.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody("{\"name\":\"n\"}"));

        final TestBean result = OkHttpRequest.url(baseUrl).xmlBody(new TestBean()).post(TestBean.class);

        assertEquals("n", result.getName());
    }

    @Test
    public void testGetWithoutBodyIsUnaffectedByBodyDerivedContentType() throws Exception {
        server.enqueue(new MockResponse().setBody("plain"));

        assertEquals("plain", OkHttpRequest.url(baseUrl).get(String.class));
        assertNull(server.takeRequest().getHeader("Content-Type"));

        // A POST without a configured body sends the synthetic empty body, whose media type is null.
        server.enqueue(new MockResponse().setBody("{\"name\":\"p\"}"));
        assertEquals("p", OkHttpRequest.url(baseUrl).post(TestBean.class).getName());
        assertNull(server.takeRequest().getHeader("Content-Type"));
    }

    // ------------------------------------------------------------------------------------------------
    // a05 F-2 / F-3: timeout validation surfaces the documented IllegalArgumentException.
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testSubMillisecondDurationTimeoutsAreRejected() {
        for (final Duration tooSmall : List.of(Duration.ofNanos(1), Duration.ofNanos(999_999))) {
            final IllegalArgumentException connect = assertThrows(IllegalArgumentException.class, () -> OkHttpRequest.url(baseUrl).connectTimeout(tooSmall));
            assertTrue(connect.getMessage().contains("at least 1 ms"), connect.getMessage());

            final IllegalArgumentException read = assertThrows(IllegalArgumentException.class, () -> OkHttpRequest.url(baseUrl).readTimeout(tooSmall));
            assertTrue(read.getMessage().contains("at least 1 ms"), read.getMessage());
        }
    }

    @Test
    public void testOneMillisecondAndZeroDurationTimeoutsAreAccepted() throws Exception {
        OkHttpRequest request = OkHttpRequest.url(baseUrl).connectTimeout(Duration.ofMillis(1)).readTimeout(Duration.ofMillis(1));
        OkHttpClient client = ((OkHttpClient.Builder) getField(request, "httpClientBuilder")).build();
        assertEquals(1, client.connectTimeoutMillis());
        assertEquals(1, client.readTimeoutMillis());

        request = OkHttpRequest.url(baseUrl).connectTimeout(Duration.ZERO).readTimeout(Duration.ZERO);
        client = ((OkHttpClient.Builder) getField(request, "httpClientBuilder")).build();
        assertEquals(0, client.connectTimeoutMillis(), "ZERO disables the timeout");
        assertEquals(0, client.readTimeoutMillis(), "ZERO disables the timeout");

        request = OkHttpRequest.url(baseUrl).connectTimeout(Duration.ofSeconds(5)).readTimeout(Duration.ofMillis(1_500));
        client = ((OkHttpClient.Builder) getField(request, "httpClientBuilder")).build();
        assertEquals(5_000, client.connectTimeoutMillis());
        assertEquals(1_500, client.readTimeoutMillis());
    }

    private static void assertConventionalIllegalArgument(final Executable executable) {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, executable);
        assertFalse(String.valueOf(ex.getMessage()).contains("okhttp3"), "must not leak OkHttp's internals: " + ex.getMessage());
    }

    @Test
    public void testNegativeTimeoutsThrowIllegalArgumentExceptionNotOkHttpIllegalState() throws Exception {
        assertConventionalIllegalArgument(() -> OkHttpRequest.url(baseUrl).connectTimeout(-1L));
        assertConventionalIllegalArgument(() -> OkHttpRequest.url(baseUrl).readTimeout(-1L));
        assertConventionalIllegalArgument(() -> OkHttpRequest.url(baseUrl).connectTimeout(Long.MIN_VALUE));
        assertConventionalIllegalArgument(() -> OkHttpRequest.url(baseUrl).readTimeout(Long.MIN_VALUE));

        assertConventionalIllegalArgument(() -> OkHttpRequest.url(baseUrl).connectTimeout(Duration.ofMillis(-1)));
        assertConventionalIllegalArgument(() -> OkHttpRequest.url(baseUrl).readTimeout(Duration.ofMillis(-1)));
        assertConventionalIllegalArgument(() -> OkHttpRequest.url(baseUrl).connectTimeout(Duration.ofNanos(-1)));

        // Duration.toMillis() overflow must not escape as ArithmeticException.
        assertConventionalIllegalArgument(() -> OkHttpRequest.url(baseUrl).connectTimeout(Duration.ofSeconds(Long.MAX_VALUE)));
        assertConventionalIllegalArgument(() -> OkHttpRequest.url(baseUrl).readTimeout(Duration.ofSeconds(Long.MAX_VALUE)));

        // Factories validate before OkHttp sees the value.
        assertConventionalIllegalArgument(() -> OkHttpRequest.url(baseUrl, -1L, 1_000L));
        assertConventionalIllegalArgument(() -> OkHttpRequest.url(baseUrl, 1_000L, -1L));
        final URL url = new URL(baseUrl);
        assertConventionalIllegalArgument(() -> OkHttpRequest.url(url, -1L, 1_000L));
        assertConventionalIllegalArgument(() -> OkHttpRequest.url(url, 1_000L, Long.MIN_VALUE));
        final HttpUrl httpUrl = HttpUrl.get(baseUrl);
        assertConventionalIllegalArgument(() -> OkHttpRequest.url(httpUrl, Long.MIN_VALUE, 1_000L));
        assertConventionalIllegalArgument(() -> OkHttpRequest.url(httpUrl, 1_000L, -1L));

        // Too large for an int stays an IllegalArgumentException (locked).
        assertThrows(IllegalArgumentException.class, () -> OkHttpRequest.url(baseUrl).connectTimeout(Integer.MAX_VALUE + 1L));
        assertThrows(IllegalArgumentException.class, () -> OkHttpRequest.url(baseUrl).readTimeout(Duration.ofMillis(Integer.MAX_VALUE + 1L)));
        assertThrows(IllegalArgumentException.class, () -> OkHttpRequest.url(baseUrl, Integer.MAX_VALUE + 1L, 1_000L));

        // Zero and valid values are still accepted by the factories.
        assertNotNull(OkHttpRequest.url(baseUrl, 0L, 0L));
        assertNotNull(OkHttpRequest.url(url, 3_000L, 10_000L));
    }

    // ------------------------------------------------------------------------------------------------
    // a05 F-6: a configured body is rejected on GET/HEAD by OkHttp (documented); DELETE accepts it.
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testConfiguredBodyIsRejectedOnGetAndHeadButAcceptedOnDelete() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> OkHttpRequest.url(baseUrl).jsonBody("{}").get());
        assertThrows(IllegalArgumentException.class, () -> OkHttpRequest.url(baseUrl).jsonBody("{}").get(String.class));
        assertThrows(IllegalArgumentException.class, () -> OkHttpRequest.url(baseUrl).formBody(new HashMap<>()).get());
        assertThrows(IllegalArgumentException.class, () -> OkHttpRequest.url(baseUrl).jsonBody("{}").head());
        assertThrows(IllegalArgumentException.class, () -> OkHttpRequest.url(baseUrl).jsonBody("{}").execute(HttpMethod.GET));

        // Through the async variants the same failure surfaces as the future's failure.
        final ContinuableFuture<Response> future = OkHttpRequest.url(baseUrl).jsonBody("{}").asyncGet();
        final ExecutionException ee = assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
        assertTrue(ee.getCause() instanceof IllegalArgumentException, String.valueOf(ee.getCause()));

        server.enqueue(new MockResponse().setBody("deleted"));
        assertEquals("deleted", OkHttpRequest.url(baseUrl).jsonBody("{}").delete(String.class));
        final RecordedRequest recorded = server.takeRequest();
        assertEquals("DELETE", recorded.getMethod());
        assertEquals("{}", recorded.getBody().readUtf8());
    }

}

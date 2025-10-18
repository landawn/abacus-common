package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.IOUtil;

import okhttp3.CacheControl;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@Tag("new-test")
public class OkHttpRequest100Test extends TestBase {

    private MockWebServer server;
    private String baseUrl;

    @BeforeEach
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        baseUrl = server.url("/").toString();
    }

    @AfterEach
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void testCreateWithStringUrl() {
        OkHttpClient client = new OkHttpClient();
        OkHttpRequest request = OkHttpRequest.create("https://api.example.com", client);
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
    public void testCreateWithHttpUrl() {
        OkHttpClient client = new OkHttpClient();
        HttpUrl url = HttpUrl.parse("https://api.example.com");
        OkHttpRequest request = OkHttpRequest.create(url, client);
        assertNotNull(request);
    }

    @Test
    public void testUrlString() {
        OkHttpRequest request = OkHttpRequest.url("https://api.example.com");
        assertNotNull(request);
    }

    @Test
    public void testUrlWithURL() throws Exception {
        URL url = new URL("https://api.example.com");
        OkHttpRequest request = OkHttpRequest.url(url);
        assertNotNull(request);
    }

    @Test
    public void testUrlWithHttpUrl() {
        HttpUrl url = HttpUrl.parse("https://api.example.com");
        OkHttpRequest request = OkHttpRequest.url(url);
        assertNotNull(request);
    }

    @Test
    public void testUrlWithTimeouts() {
        OkHttpRequest request = OkHttpRequest.url("https://api.example.com", 5000L, 10000L);
        assertNotNull(request);
    }

    @Test
    public void testUrlWithURLAndTimeouts() throws Exception {
        URL url = new URL("https://api.example.com");
        OkHttpRequest request = OkHttpRequest.url(url, 5000L, 10000L);
        assertNotNull(request);
    }

    @Test
    public void testUrlWithHttpUrlAndTimeouts() {
        HttpUrl url = HttpUrl.parse("https://api.example.com");
        OkHttpRequest request = OkHttpRequest.url(url, 5000L, 10000L);
        assertNotNull(request);
    }

    @Test
    public void testCacheControl() {
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        CacheControl cacheControl = new CacheControl.Builder().noCache().build();
        OkHttpRequest result = request.cacheControl(cacheControl);
        assertSame(request, result);
    }

    @Test
    public void testTag() {
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        String tag = "test-tag";
        OkHttpRequest result = request.tag(tag);
        assertSame(request, result);
    }

    @Test
    public void testTagWithType() {
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        String tag = "test-tag";
        OkHttpRequest result = request.tag(String.class, tag);
        assertSame(request, result);
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
    public void testHeadersWithTwoHeaders() {
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        OkHttpRequest result = request.headers("Header1", "value1", "Header2", "value2");
        assertSame(request, result);
    }

    @Test
    public void testHeadersWithThreeHeaders() {
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        OkHttpRequest result = request.headers("Header1", "value1", "Header2", "value2", "Header3", "value3");
        assertSame(request, result);
    }

    @Test
    public void testHeadersWithMap() {
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        Map<String, String> headers = new HashMap<>();
        headers.put("Header1", "value1");
        headers.put("Header2", "value2");
        OkHttpRequest result = request.headers(headers);
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
        OkHttpRequest result = request.headers(headers);
        assertSame(request, result);
    }

    @Test
    public void testHeadersWithHttpHeaders() {
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        HttpHeaders headers = HttpHeaders.create().set("Header1", "value1").set("Header2", "value2");
        OkHttpRequest result = request.headers(headers);
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
        OkHttpRequest result = request.formBody(formData);
        assertSame(request, result);
    }

    @Test
    public void testFormBodyEmptyMap() {
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        Map<String, String> formData = new HashMap<>();
        OkHttpRequest result = request.formBody(formData);
        assertSame(request, result);
    }

    @Test
    public void testFormBodyBean() {
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        TestBean bean = new TestBean();
        bean.field1 = "value1";
        bean.field2 = "value2";
        OkHttpRequest result = request.formBody(bean);
        assertSame(request, result);
    }

    @Test
    public void testFormBodyNull() {
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        OkHttpRequest result = request.formBody((Object) null);
        assertSame(request, result);
    }

    @Test
    public void testFormBodyInvalidObject() {
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        assertThrows(IllegalArgumentException.class, () -> request.formBody("not a bean"));
    }

    @Test
    public void testBodyMap() {
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        Map<String, String> formData = new HashMap<>();
        formData.put("field1", "value1");
        OkHttpRequest result = request.body(formData);
        assertSame(request, result);
    }

    @Test
    public void testBodyObject() {
        OkHttpRequest request = OkHttpRequest.url(baseUrl);
        TestBean bean = new TestBean();
        bean.field1 = "value1";
        OkHttpRequest result = request.body(bean);
        assertSame(request, result);
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
    public void testExecuteWithFormUrlEncodedResponse() throws IOException {
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

        assertThrows(IOException.class, () -> request.execute(HttpMethod.GET, String.class));
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
        Executor executor = Executors.newSingleThreadExecutor();

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
        Executor executor = Executors.newSingleThreadExecutor();

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
        Executor executor = Executors.newSingleThreadExecutor();

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
        Executor executor = Executors.newSingleThreadExecutor();

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
        Executor executor = Executors.newSingleThreadExecutor();

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
        Executor executor = Executors.newSingleThreadExecutor();

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
        Executor executor = Executors.newSingleThreadExecutor();

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
        Executor executor = Executors.newSingleThreadExecutor();

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
        Executor executor = Executors.newSingleThreadExecutor();

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
        Executor executor = Executors.newSingleThreadExecutor();

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
        Executor executor = Executors.newSingleThreadExecutor();

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
        Executor executor = Executors.newSingleThreadExecutor();

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
        Executor executor = Executors.newSingleThreadExecutor();

        ContinuableFuture<String> future = request.asyncExecute(HttpMethod.GET, String.class, executor);
        String result = future.get();
        assertEquals("Async Execute response", result);
    }

    @Test
    public void testInvalidUrlArguments() {
        assertThrows(IllegalArgumentException.class, () -> OkHttpRequest.create((String) null, new OkHttpClient()));
        assertThrows(IllegalArgumentException.class, () -> OkHttpRequest.create("", new OkHttpClient()));
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

    public static class TestBean {
        private String field1;
        private String field2;

        public String getField1() {
            return field1;
        }

        public void setField1(String field1) {
            this.field1 = field1;
        }

        public String getField2() {
            return field2;
        }

        public void setField2(String field2) {
            this.field2 = field2;
        }
    }
}

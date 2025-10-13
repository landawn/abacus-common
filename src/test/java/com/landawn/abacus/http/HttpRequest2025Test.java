package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.util.ContinuableFuture;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class HttpRequest2025Test extends TestBase {

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
    public void testCreate() {
        HttpClient client = HttpClient.create(baseUrl);
        HttpRequest request = HttpRequest.create(client);
        assertNotNull(request);
    }

    @Test
    public void testUrlString() {
        HttpRequest request = HttpRequest.url("https://api.example.com");
        assertNotNull(request);
    }

    @Test
    public void testUrlStringWithTimeouts() {
        HttpRequest request = HttpRequest.url("https://api.example.com", 5000L, 10000L);
        assertNotNull(request);
    }

    @Test
    public void testUrlURL() throws MalformedURLException {
        URL url = new URL("https://api.example.com");
        HttpRequest request = HttpRequest.url(url);
        assertNotNull(request);
    }

    @Test
    public void testUrlURLWithTimeouts() throws MalformedURLException {
        URL url = new URL("https://api.example.com");
        HttpRequest request = HttpRequest.url(url, 5000L, 10000L);
        assertNotNull(request);
    }

    @Test
    public void testSettings() {
        HttpRequest request = HttpRequest.url(baseUrl);
        HttpSettings settings = HttpSettings.create().header("Accept", "application/json");
        HttpRequest result = request.settings(settings);
        assertNotNull(result);
    }

    @Test
    public void testSettingsWithNull() {
        HttpRequest request = HttpRequest.url(baseUrl);
        HttpRequest result = request.settings(null);
        assertNotNull(result);
    }

    @Test
    public void testBasicAuth() {
        HttpRequest request = HttpRequest.url(baseUrl);
        HttpRequest result = request.basicAuth("user", "password");
        assertNotNull(result);
    }

    @Test
    public void testHeader() {
        HttpRequest request = HttpRequest.url(baseUrl);
        HttpRequest result = request.header("X-Custom-Header", "value");
        assertNotNull(result);
    }

    @Test
    public void testHeadersWithTwoParameters() {
        HttpRequest request = HttpRequest.url(baseUrl);
        HttpRequest result = request.headers("Header1", "value1", "Header2", "value2");
        assertNotNull(result);
    }

    @Test
    public void testHeadersWithThreeParameters() {
        HttpRequest request = HttpRequest.url(baseUrl);
        HttpRequest result = request.headers("Header1", "value1", "Header2", "value2", "Header3", "value3");
        assertNotNull(result);
    }

    @Test
    public void testHeadersWithMap() {
        HttpRequest request = HttpRequest.url(baseUrl);
        Map<String, String> headers = new HashMap<>();
        headers.put("Header1", "value1");
        headers.put("Header2", "value2");
        HttpRequest result = request.headers(headers);
        assertNotNull(result);
    }

    @Test
    public void testHeadersWithHttpHeaders() {
        HttpRequest request = HttpRequest.url(baseUrl);
        HttpHeaders headers = HttpHeaders.create().set("Header1", "value1").set("Header2", "value2");
        HttpRequest result = request.headers(headers);
        assertNotNull(result);
    }

    @Test
    public void testConnectionTimeoutMillis() {
        HttpRequest request = HttpRequest.url(baseUrl);
        HttpRequest result = request.connectionTimeout(5000L);
        assertNotNull(result);
    }

    @Test
    public void testConnectionTimeoutDuration() {
        HttpRequest request = HttpRequest.url(baseUrl);
        HttpRequest result = request.connectionTimeout(Duration.ofSeconds(5));
        assertNotNull(result);
    }

    @Test
    public void testReadTimeoutMillis() {
        HttpRequest request = HttpRequest.url(baseUrl);
        HttpRequest result = request.readTimeout(10000L);
        assertNotNull(result);
    }

    @Test
    public void testReadTimeoutDuration() {
        HttpRequest request = HttpRequest.url(baseUrl);
        HttpRequest result = request.readTimeout(Duration.ofSeconds(10));
        assertNotNull(result);
    }

    @Test
    public void testUseCaches() {
        HttpRequest request = HttpRequest.url(baseUrl);
        HttpRequest result = request.useCaches(true);
        assertNotNull(result);
    }

    @Test
    public void testUseCachesFalse() {
        HttpRequest request = HttpRequest.url(baseUrl);
        HttpRequest result = request.useCaches(false);
        assertNotNull(result);
    }

    @Test
    public void testSslSocketFactory() throws Exception {
        HttpRequest request = HttpRequest.url(baseUrl);
        SSLContext sslContext = SSLContext.getDefault();
        HttpRequest result = request.sslSocketFactory(sslContext.getSocketFactory());
        assertNotNull(result);
    }

    @Test
    public void testProxy() {
        HttpRequest request = HttpRequest.url(baseUrl);
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxy.example.com", 8080));
        HttpRequest result = request.proxy(proxy);
        assertNotNull(result);
    }

    @Test
    public void testQueryString() {
        HttpRequest request = HttpRequest.url(baseUrl);
        HttpRequest result = request.query("param1=value1&param2=value2");
        assertNotNull(result);
    }

    @Test
    public void testQueryMap() {
        HttpRequest request = HttpRequest.url(baseUrl);
        Map<String, Object> params = new HashMap<>();
        params.put("param1", "value1");
        params.put("param2", "value2");
        HttpRequest result = request.query(params);
        assertNotNull(result);
    }

    @Test
    public void testJsonBodyString() {
        HttpRequest request = HttpRequest.url(baseUrl);
        HttpRequest result = request.jsonBody("{\"name\":\"John\"}");
        assertNotNull(result);
    }

    @Test
    public void testJsonBodyObject() {
        HttpRequest request = HttpRequest.url(baseUrl);
        Map<String, String> obj = new HashMap<>();
        obj.put("name", "John");
        HttpRequest result = request.jsonBody(obj);
        assertNotNull(result);
    }

    @Test
    public void testXmlBodyString() {
        HttpRequest request = HttpRequest.url(baseUrl);
        HttpRequest result = request.xmlBody("<user><name>John</name></user>");
        assertNotNull(result);
    }

    @Test
    public void testXmlBodyObject() {
        HttpRequest request = HttpRequest.url(baseUrl);
        Map<String, String> obj = new HashMap<>();
        obj.put("name", "John");
        HttpRequest result = request.xmlBody(obj);
        assertNotNull(result);
    }

    @Test
    public void testFormBodyMap() {
        HttpRequest request = HttpRequest.url(baseUrl);
        Map<String, String> formData = new HashMap<>();
        formData.put("field1", "value1");
        formData.put("field2", "value2");
        HttpRequest result = request.formBody(formData);
        assertNotNull(result);
    }

    @Test
    public void testFormBodyObject() {
        HttpRequest request = HttpRequest.url(baseUrl);
        TestBean bean = new TestBean();
        bean.field1 = "value1";
        bean.field2 = "value2";
        HttpRequest result = request.formBody(bean);
        assertNotNull(result);
    }

    @Test
    public void testBodyString() {
        HttpRequest request = HttpRequest.url(baseUrl);
        HttpRequest result = request.body("test body");
        assertNotNull(result);
    }

    @Test
    public void testBodyByteArray() {
        HttpRequest request = HttpRequest.url(baseUrl);
        HttpRequest result = request.body(new byte[] { 1, 2, 3 });
        assertNotNull(result);
    }

    @Test
    public void testGet() throws IOException {
        server.enqueue(new MockResponse().setBody("GET response"));
        HttpRequest request = HttpRequest.url(baseUrl);
        HttpResponse response = request.get();
        assertNotNull(response);
        assertEquals("GET response", response.body(String.class));
    }

    @Test
    public void testGetWithResultClass() throws IOException {
        server.enqueue(new MockResponse().setBody("GET response"));
        HttpRequest request = HttpRequest.url(baseUrl);
        String response = request.get(String.class);
        assertEquals("GET response", response);
    }

    @Test
    public void testPost() throws IOException {
        server.enqueue(new MockResponse().setBody("POST response"));
        HttpRequest request = HttpRequest.url(baseUrl);
        request.body("test body");
        HttpResponse response = request.post();
        assertNotNull(response);
        assertEquals("POST response", response.body(String.class));
    }

    @Test
    public void testPostWithResultClass() throws IOException {
        server.enqueue(new MockResponse().setBody("POST response"));
        HttpRequest request = HttpRequest.url(baseUrl);
        request.body("test body");
        String response = request.post(String.class);
        assertEquals("POST response", response);
    }

    @Test
    public void testPut() throws IOException {
        server.enqueue(new MockResponse().setBody("PUT response"));
        HttpRequest request = HttpRequest.url(baseUrl);
        request.body("test body");
        HttpResponse response = request.put();
        assertNotNull(response);
        assertEquals("PUT response", response.body(String.class));
    }

    @Test
    public void testPutWithResultClass() throws IOException {
        server.enqueue(new MockResponse().setBody("PUT response"));
        HttpRequest request = HttpRequest.url(baseUrl);
        request.body("test body");
        String response = request.put(String.class);
        assertEquals("PUT response", response);
    }

    @Test
    public void testDelete() throws IOException {
        server.enqueue(new MockResponse().setBody("DELETE response"));
        HttpRequest request = HttpRequest.url(baseUrl);
        HttpResponse response = request.delete();
        assertNotNull(response);
        assertEquals("DELETE response", response.body(String.class));
    }

    @Test
    public void testDeleteWithResultClass() throws IOException {
        server.enqueue(new MockResponse().setBody("DELETE response"));
        HttpRequest request = HttpRequest.url(baseUrl);
        String response = request.delete(String.class);
        assertEquals("DELETE response", response);
    }

    @Test
    public void testHead() throws IOException {
        server.enqueue(new MockResponse());
        HttpRequest request = HttpRequest.url(baseUrl);
        HttpResponse response = request.head();
        assertNotNull(response);
    }

    @Test
    public void testExecute() throws IOException {
        server.enqueue(new MockResponse().setBody("Execute response"));
        HttpRequest request = HttpRequest.url(baseUrl);
        HttpResponse response = request.execute(HttpMethod.GET);
        assertNotNull(response);
        assertEquals("Execute response", response.body(String.class));
    }

    @Test
    public void testExecuteWithResultClass() throws IOException {
        server.enqueue(new MockResponse().setBody("Execute response"));
        HttpRequest request = HttpRequest.url(baseUrl);
        String response = request.execute(HttpMethod.GET, String.class);
        assertEquals("Execute response", response);
    }

    @Test
    public void testExecuteWithNullHttpMethod() {
        HttpRequest request = HttpRequest.url(baseUrl);
        assertThrows(IllegalArgumentException.class, () -> request.execute(null, String.class));
    }

    @Test
    public void testExecuteToFile() throws IOException {
        server.enqueue(new MockResponse().setBody("File content"));
        HttpRequest request = HttpRequest.url(baseUrl);

        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit();

        request.execute(HttpMethod.GET, tempFile);

        String content = new String(Files.readAllBytes(tempFile.toPath()));
        assertEquals("File content", content);
    }

    @Test
    public void testExecuteToFileWithNullHttpMethod() throws IOException {
        HttpRequest request = HttpRequest.url(baseUrl);
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit();

        assertThrows(IllegalArgumentException.class, () -> request.execute(null, tempFile));
    }

    @Test
    public void testExecuteToOutputStream() throws IOException {
        server.enqueue(new MockResponse().setBody("Stream content"));
        HttpRequest request = HttpRequest.url(baseUrl);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        request.execute(HttpMethod.GET, baos);

        assertEquals("Stream content", baos.toString());
    }

    @Test
    public void testExecuteToOutputStreamWithNullHttpMethod() {
        HttpRequest request = HttpRequest.url(baseUrl);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        assertThrows(IllegalArgumentException.class, () -> request.execute(null, baos));
    }

    @Test
    public void testExecuteToWriter() throws IOException {
        server.enqueue(new MockResponse().setBody("Writer content"));
        HttpRequest request = HttpRequest.url(baseUrl);

        StringWriter writer = new StringWriter();
        request.execute(HttpMethod.GET, writer);

        assertEquals("Writer content", writer.toString());
    }

    @Test
    public void testExecuteToWriterWithNullHttpMethod() {
        HttpRequest request = HttpRequest.url(baseUrl);
        StringWriter writer = new StringWriter();

        assertThrows(IllegalArgumentException.class, () -> request.execute(null, writer));
    }

    @Test
    public void testAsyncGet() throws Exception {
        server.enqueue(new MockResponse().setBody("Async GET response"));
        HttpRequest request = HttpRequest.url(baseUrl);

        ContinuableFuture<HttpResponse> future = request.asyncGet();
        HttpResponse response = future.get();
        assertNotNull(response);
        assertEquals("Async GET response", response.body(String.class));
    }

    @Test
    public void testAsyncGetWithExecutor() throws Exception {
        server.enqueue(new MockResponse().setBody("Async GET response"));
        HttpRequest request = HttpRequest.url(baseUrl);
        Executor executor = Executors.newSingleThreadExecutor();

        ContinuableFuture<HttpResponse> future = request.asyncGet(executor);
        HttpResponse response = future.get();
        assertNotNull(response);
        assertEquals("Async GET response", response.body(String.class));
    }

    @Test
    public void testAsyncGetWithResultClass() throws Exception {
        server.enqueue(new MockResponse().setBody("Async GET response"));
        HttpRequest request = HttpRequest.url(baseUrl);

        ContinuableFuture<String> future = request.asyncGet(String.class);
        String response = future.get();
        assertEquals("Async GET response", response);
    }

    @Test
    public void testAsyncGetWithResultClassAndExecutor() throws Exception {
        server.enqueue(new MockResponse().setBody("Async GET response"));
        HttpRequest request = HttpRequest.url(baseUrl);
        Executor executor = Executors.newSingleThreadExecutor();

        ContinuableFuture<String> future = request.asyncGet(String.class, executor);
        String response = future.get();
        assertEquals("Async GET response", response);
    }

    @Test
    public void testAsyncPost() throws Exception {
        server.enqueue(new MockResponse().setBody("Async POST response"));
        HttpRequest request = HttpRequest.url(baseUrl);
        request.body("test body");

        ContinuableFuture<HttpResponse> future = request.asyncPost();
        HttpResponse response = future.get();
        assertNotNull(response);
        assertEquals("Async POST response", response.body(String.class));
    }

    @Test
    public void testAsyncPostWithExecutor() throws Exception {
        server.enqueue(new MockResponse().setBody("Async POST response"));
        HttpRequest request = HttpRequest.url(baseUrl);
        request.body("test body");
        Executor executor = Executors.newSingleThreadExecutor();

        ContinuableFuture<HttpResponse> future = request.asyncPost(executor);
        HttpResponse response = future.get();
        assertNotNull(response);
        assertEquals("Async POST response", response.body(String.class));
    }

    @Test
    public void testAsyncPostWithResultClass() throws Exception {
        server.enqueue(new MockResponse().setBody("Async POST response"));
        HttpRequest request = HttpRequest.url(baseUrl);
        request.body("test body");

        ContinuableFuture<String> future = request.asyncPost(String.class);
        String response = future.get();
        assertEquals("Async POST response", response);
    }

    @Test
    public void testAsyncPostWithResultClassAndExecutor() throws Exception {
        server.enqueue(new MockResponse().setBody("Async POST response"));
        HttpRequest request = HttpRequest.url(baseUrl);
        request.body("test body");
        Executor executor = Executors.newSingleThreadExecutor();

        ContinuableFuture<String> future = request.asyncPost(String.class, executor);
        String response = future.get();
        assertEquals("Async POST response", response);
    }

    @Test
    public void testAsyncPut() throws Exception {
        server.enqueue(new MockResponse().setBody("Async PUT response"));
        HttpRequest request = HttpRequest.url(baseUrl);
        request.body("test body");

        ContinuableFuture<HttpResponse> future = request.asyncPut();
        HttpResponse response = future.get();
        assertNotNull(response);
        assertEquals("Async PUT response", response.body(String.class));
    }

    @Test
    public void testAsyncPutWithExecutor() throws Exception {
        server.enqueue(new MockResponse().setBody("Async PUT response"));
        HttpRequest request = HttpRequest.url(baseUrl);
        request.body("test body");
        Executor executor = Executors.newSingleThreadExecutor();

        ContinuableFuture<HttpResponse> future = request.asyncPut(executor);
        HttpResponse response = future.get();
        assertNotNull(response);
        assertEquals("Async PUT response", response.body(String.class));
    }

    @Test
    public void testAsyncPutWithResultClass() throws Exception {
        server.enqueue(new MockResponse().setBody("Async PUT response"));
        HttpRequest request = HttpRequest.url(baseUrl);
        request.body("test body");

        ContinuableFuture<String> future = request.asyncPut(String.class);
        String response = future.get();
        assertEquals("Async PUT response", response);
    }

    @Test
    public void testAsyncPutWithResultClassAndExecutor() throws Exception {
        server.enqueue(new MockResponse().setBody("Async PUT response"));
        HttpRequest request = HttpRequest.url(baseUrl);
        request.body("test body");
        Executor executor = Executors.newSingleThreadExecutor();

        ContinuableFuture<String> future = request.asyncPut(String.class, executor);
        String response = future.get();
        assertEquals("Async PUT response", response);
    }

    @Test
    public void testAsyncDelete() throws Exception {
        server.enqueue(new MockResponse().setBody("Async DELETE response"));
        HttpRequest request = HttpRequest.url(baseUrl);

        ContinuableFuture<HttpResponse> future = request.asyncDelete();
        HttpResponse response = future.get();
        assertNotNull(response);
        assertEquals("Async DELETE response", response.body(String.class));
    }

    @Test
    public void testAsyncDeleteWithExecutor() throws Exception {
        server.enqueue(new MockResponse().setBody("Async DELETE response"));
        HttpRequest request = HttpRequest.url(baseUrl);
        Executor executor = Executors.newSingleThreadExecutor();

        ContinuableFuture<HttpResponse> future = request.asyncDelete(executor);
        HttpResponse response = future.get();
        assertNotNull(response);
        assertEquals("Async DELETE response", response.body(String.class));
    }

    @Test
    public void testAsyncDeleteWithResultClass() throws Exception {
        server.enqueue(new MockResponse().setBody("Async DELETE response"));
        HttpRequest request = HttpRequest.url(baseUrl);

        ContinuableFuture<String> future = request.asyncDelete(String.class);
        String response = future.get();
        assertEquals("Async DELETE response", response);
    }

    @Test
    public void testAsyncDeleteWithResultClassAndExecutor() throws Exception {
        server.enqueue(new MockResponse().setBody("Async DELETE response"));
        HttpRequest request = HttpRequest.url(baseUrl);
        Executor executor = Executors.newSingleThreadExecutor();

        ContinuableFuture<String> future = request.asyncDelete(String.class, executor);
        String response = future.get();
        assertEquals("Async DELETE response", response);
    }

    @Test
    public void testAsyncHead() throws Exception {
        server.enqueue(new MockResponse());
        HttpRequest request = HttpRequest.url(baseUrl);

        ContinuableFuture<HttpResponse> future = request.asyncHead();
        HttpResponse response = future.get();
        assertNotNull(response);
    }

    @Test
    public void testAsyncHeadWithExecutor() throws Exception {
        server.enqueue(new MockResponse());
        HttpRequest request = HttpRequest.url(baseUrl);
        Executor executor = Executors.newSingleThreadExecutor();

        ContinuableFuture<HttpResponse> future = request.asyncHead(executor);
        HttpResponse response = future.get();
        assertNotNull(response);
    }

    @Test
    public void testAsyncExecute() throws Exception {
        server.enqueue(new MockResponse().setBody("Async Execute response"));
        HttpRequest request = HttpRequest.url(baseUrl);

        ContinuableFuture<HttpResponse> future = request.asyncExecute(HttpMethod.GET);
        HttpResponse response = future.get();
        assertNotNull(response);
        assertEquals("Async Execute response", response.body(String.class));
    }

    @Test
    public void testAsyncExecuteWithExecutor() throws Exception {
        server.enqueue(new MockResponse().setBody("Async Execute response"));
        HttpRequest request = HttpRequest.url(baseUrl);
        Executor executor = Executors.newSingleThreadExecutor();

        ContinuableFuture<HttpResponse> future = request.asyncExecute(HttpMethod.GET, executor);
        HttpResponse response = future.get();
        assertNotNull(response);
        assertEquals("Async Execute response", response.body(String.class));
    }

    @Test
    public void testAsyncExecuteWithResultClass() throws Exception {
        server.enqueue(new MockResponse().setBody("Async Execute response"));
        HttpRequest request = HttpRequest.url(baseUrl);

        ContinuableFuture<String> future = request.asyncExecute(HttpMethod.GET, String.class);
        String response = future.get();
        assertEquals("Async Execute response", response);
    }

    @Test
    public void testAsyncExecuteWithResultClassAndExecutor() throws Exception {
        server.enqueue(new MockResponse().setBody("Async Execute response"));
        HttpRequest request = HttpRequest.url(baseUrl);
        Executor executor = Executors.newSingleThreadExecutor();

        ContinuableFuture<String> future = request.asyncExecute(HttpMethod.GET, String.class, executor);
        String response = future.get();
        assertEquals("Async Execute response", response);
    }

    @Test
    public void testAsyncExecuteWithNullHttpMethod() {
        HttpRequest request = HttpRequest.url(baseUrl);
        assertThrows(IllegalArgumentException.class, () -> request.asyncExecute(null, String.class));
    }

    @Test
    public void testAsyncExecuteToFile() throws Exception {
        server.enqueue(new MockResponse().setBody("File content"));
        HttpRequest request = HttpRequest.url(baseUrl);

        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit();

        ContinuableFuture<Void> future = request.asyncExecute(HttpMethod.GET, tempFile);
        future.get();

        String content = new String(Files.readAllBytes(tempFile.toPath()));
        assertEquals("File content", content);
    }

    @Test
    public void testAsyncExecuteToFileWithExecutor() throws Exception {
        server.enqueue(new MockResponse().setBody("File content"));
        HttpRequest request = HttpRequest.url(baseUrl);
        Executor executor = Executors.newSingleThreadExecutor();

        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit();

        ContinuableFuture<Void> future = request.asyncExecute(HttpMethod.GET, tempFile, executor);
        future.get();

        String content = new String(Files.readAllBytes(tempFile.toPath()));
        assertEquals("File content", content);
    }

    @Test
    public void testAsyncExecuteToOutputStream() throws Exception {
        server.enqueue(new MockResponse().setBody("Stream content"));
        HttpRequest request = HttpRequest.url(baseUrl);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ContinuableFuture<Void> future = request.asyncExecute(HttpMethod.GET, baos);
        future.get();

        assertEquals("Stream content", baos.toString());
    }

    @Test
    public void testAsyncExecuteToOutputStreamWithExecutor() throws Exception {
        server.enqueue(new MockResponse().setBody("Stream content"));
        HttpRequest request = HttpRequest.url(baseUrl);
        Executor executor = Executors.newSingleThreadExecutor();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ContinuableFuture<Void> future = request.asyncExecute(HttpMethod.GET, baos, executor);
        future.get();

        assertEquals("Stream content", baos.toString());
    }

    @Test
    public void testAsyncExecuteToWriter() throws Exception {
        server.enqueue(new MockResponse().setBody("Writer content"));
        HttpRequest request = HttpRequest.url(baseUrl);

        StringWriter writer = new StringWriter();
        ContinuableFuture<Void> future = request.asyncExecute(HttpMethod.GET, writer);
        future.get();

        assertEquals("Writer content", writer.toString());
    }

    @Test
    public void testAsyncExecuteToWriterWithExecutor() throws Exception {
        server.enqueue(new MockResponse().setBody("Writer content"));
        HttpRequest request = HttpRequest.url(baseUrl);
        Executor executor = Executors.newSingleThreadExecutor();

        StringWriter writer = new StringWriter();
        ContinuableFuture<Void> future = request.asyncExecute(HttpMethod.GET, writer, executor);
        future.get();

        assertEquals("Writer content", writer.toString());
    }

    @Test
    public void testExecuteHttpMethodPost() throws IOException {
        server.enqueue(new MockResponse().setBody("POST via execute"));
        HttpRequest request = HttpRequest.url(baseUrl);
        request.body("test");
        String response = request.execute(HttpMethod.POST, String.class);
        assertEquals("POST via execute", response);
    }

    @Test
    public void testExecuteHttpMethodPut() throws IOException {
        server.enqueue(new MockResponse().setBody("PUT via execute"));
        HttpRequest request = HttpRequest.url(baseUrl);
        request.body("test");
        String response = request.execute(HttpMethod.PUT, String.class);
        assertEquals("PUT via execute", response);
    }

    @Test
    public void testExecuteHttpMethodDelete() throws IOException {
        server.enqueue(new MockResponse().setBody("DELETE via execute"));
        HttpRequest request = HttpRequest.url(baseUrl);
        String response = request.execute(HttpMethod.DELETE, String.class);
        assertEquals("DELETE via execute", response);
    }

    @Test
    public void testExecuteHttpMethodHead() throws IOException {
        server.enqueue(new MockResponse());
        HttpRequest request = HttpRequest.url(baseUrl);
        HttpResponse response = request.execute(HttpMethod.HEAD);
        assertNotNull(response);
    }

    @Test
    public void testQueryWithEmptyString() {
        HttpRequest request = HttpRequest.url(baseUrl);
        HttpRequest result = request.query("");
        assertNotNull(result);
    }

    @Test
    public void testHeadersWithEmptyMap() {
        HttpRequest request = HttpRequest.url(baseUrl);
        Map<String, String> headers = new HashMap<>();
        HttpRequest result = request.headers(headers);
        assertNotNull(result);
    }

    @Test
    public void testJsonBodyWithComplexObject() {
        HttpRequest request = HttpRequest.url(baseUrl);
        TestBean bean = new TestBean();
        bean.field1 = "test1";
        bean.field2 = "test2";
        HttpRequest result = request.jsonBody(bean);
        assertNotNull(result);
    }

    @Test
    public void testConnectionTimeoutZero() {
        HttpRequest request = HttpRequest.url(baseUrl);
        HttpRequest result = request.connectionTimeout(0L);
        assertNotNull(result);
    }

    @Test
    public void testReadTimeoutZero() {
        HttpRequest request = HttpRequest.url(baseUrl);
        HttpRequest result = request.readTimeout(0L);
        assertNotNull(result);
    }

    @Test
    public void testMultipleHeaderCalls() {
        HttpRequest request = HttpRequest.url(baseUrl);
        request.header("Header1", "value1");
        request.header("Header2", "value2");
        request.header("Header3", "value3");
        assertNotNull(request);
    }

    @Test
    public void testChainedMethodCalls() {
        HttpRequest request = HttpRequest.url(baseUrl).header("Accept", "application/json").connectionTimeout(5000L).readTimeout(10000L).useCaches(false);
        assertNotNull(request);
    }

    @Test
    public void testFormBodyWithEmptyMap() {
        HttpRequest request = HttpRequest.url(baseUrl);
        Map<String, String> formData = new HashMap<>();
        HttpRequest result = request.formBody(formData);
        assertNotNull(result);
    }

    @Test
    public void testXmlBodyWithEmptyObject() {
        HttpRequest request = HttpRequest.url(baseUrl);
        Map<String, String> obj = new HashMap<>();
        HttpRequest result = request.xmlBody(obj);
        assertNotNull(result);
    }

    public static class TestBean {
        public String field1;
        public String field2;

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

    private static class MockWebServer {
        private ServerSocket serverSocket;
        private Thread serverThread;
        private final Queue<MockResponse> responses = new LinkedList<>();

        public void start() throws IOException {
            serverSocket = new ServerSocket(0);
            serverThread = new Thread(() -> {
                try {
                    while (!serverSocket.isClosed()) {
                        Socket socket = serverSocket.accept();
                        handleConnection(socket);
                    }
                } catch (IOException e) {
                }
            });
            serverThread.start();
        }

        private void handleConnection(Socket socket) throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream());

            String requestLine = reader.readLine();
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
            }

            MockResponse response = responses.poll();
            if (response == null) {
                response = new MockResponse().setBody("Default response");
            }

            writer.println("HTTP/1.1 200 OK");
            writer.println("Content-Length: " + response.body.length());
            writer.println();
            writer.print(response.body);
            writer.flush();

            socket.close();
        }

        public URL url(String path) throws MalformedURLException {
            return new URL("http://localhost:" + serverSocket.getLocalPort() + path);
        }

        public void enqueue(MockResponse response) {
            responses.offer(response);
        }

        public void shutdown() throws IOException {
            serverSocket.close();
            try {
                serverThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static class MockResponse {
        private String body = "";

        public MockResponse setBody(String body) {
            this.body = body;
            return this;
        }
    }
}

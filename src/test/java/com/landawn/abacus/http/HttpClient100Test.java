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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ContinuableFuture;

@Tag("new-test")
public class HttpClient100Test extends TestBase {

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
    public void testUrl() {
        HttpClient client = HttpClient.create("https://api.example.com");
        assertEquals("https://api.example.com", client.url());
    }

    @Test
    public void testCreateWithString() {
        HttpClient client = HttpClient.create("https://api.example.com");
        assertNotNull(client);
        assertEquals("https://api.example.com", client.url());
    }

    @Test
    public void testCreateWithStringAndMaxConnection() {
        HttpClient client = HttpClient.create("https://api.example.com", 32);
        assertNotNull(client);
    }

    @Test
    public void testCreateWithStringAndTimeouts() {
        HttpClient client = HttpClient.create("https://api.example.com", 5000L, 10000L);
        assertNotNull(client);
    }

    @Test
    public void testCreateWithAllParameters() {
        HttpClient client = HttpClient.create("https://api.example.com", 16, 5000L, 10000L);
        assertNotNull(client);
    }

    @Test
    public void testCreateWithSettings() {
        HttpSettings settings = HttpSettings.create().header("Accept", "application/json");
        HttpClient client = HttpClient.create("https://api.example.com", 16, 5000L, 10000L, settings);
        assertNotNull(client);
    }

    @Test
    public void testCreateWithSharedConnectionCounter() {
        AtomicInteger counter = new AtomicInteger(0);
        HttpClient client = HttpClient.create("https://api.example.com", 16, 5000L, 10000L, null, counter);
        assertNotNull(client);
    }

    @Test
    public void testCreateWithExecutor() {
        Executor executor = Executors.newSingleThreadExecutor();
        HttpClient client = HttpClient.create("https://api.example.com", 16, 5000L, 10000L, executor);
        assertNotNull(client);
    }

    @Test
    public void testCreateWithSettingsAndExecutor() {
        HttpSettings settings = HttpSettings.create();
        Executor executor = Executors.newSingleThreadExecutor();
        HttpClient client = HttpClient.create("https://api.example.com", 16, 5000L, 10000L, settings, executor);
        assertNotNull(client);
    }

    @Test
    public void testCreateWithAllOptions() {
        HttpSettings settings = HttpSettings.create();
        AtomicInteger counter = new AtomicInteger(0);
        Executor executor = Executors.newSingleThreadExecutor();
        HttpClient client = HttpClient.create("https://api.example.com", 16, 5000L, 10000L, settings, counter, executor);
        assertNotNull(client);
    }

    @Test
    public void testCreateWithURL() throws MalformedURLException {
        URL url = new URL("https://api.example.com");
        HttpClient client = HttpClient.create(url);
        assertNotNull(client);
    }

    @Test
    public void testCreateWithURLAndMaxConnection() throws MalformedURLException {
        URL url = new URL("https://api.example.com");
        HttpClient client = HttpClient.create(url, 32);
        assertNotNull(client);
    }

    @Test
    public void testCreateWithURLAndTimeouts() throws MalformedURLException {
        URL url = new URL("https://api.example.com");
        HttpClient client = HttpClient.create(url, 5000L, 10000L);
        assertNotNull(client);
    }

    @Test
    public void testCreateWithURLAndAllParameters() throws MalformedURLException {
        URL url = new URL("https://api.example.com");
        HttpClient client = HttpClient.create(url, 16, 5000L, 10000L);
        assertNotNull(client);
    }

    @Test
    public void testCreateWithURLAndSettings() throws MalformedURLException {
        URL url = new URL("https://api.example.com");
        HttpSettings settings = HttpSettings.create();
        HttpClient client = HttpClient.create(url, 16, 5000L, 10000L, settings);
        assertNotNull(client);
    }

    @Test
    public void testCreateWithURLAndSharedCounter() throws MalformedURLException {
        URL url = new URL("https://api.example.com");
        AtomicInteger counter = new AtomicInteger(0);
        HttpClient client = HttpClient.create(url, 16, 5000L, 10000L, null, counter);
        assertNotNull(client);
    }

    @Test
    public void testCreateWithURLAndExecutor() throws MalformedURLException {
        URL url = new URL("https://api.example.com");
        Executor executor = Executors.newSingleThreadExecutor();
        HttpClient client = HttpClient.create(url, 16, 5000L, 10000L, executor);
        assertNotNull(client);
    }

    @Test
    public void testCreateWithURLAndSettingsAndExecutor() throws MalformedURLException {
        URL url = new URL("https://api.example.com");
        HttpSettings settings = HttpSettings.create();
        Executor executor = Executors.newSingleThreadExecutor();
        HttpClient client = HttpClient.create(url, 16, 5000L, 10000L, settings, executor);
        assertNotNull(client);
    }

    @Test
    public void testCreateWithURLAndAllOptions() throws MalformedURLException {
        URL url = new URL("https://api.example.com");
        HttpSettings settings = HttpSettings.create();
        AtomicInteger counter = new AtomicInteger(0);
        Executor executor = Executors.newSingleThreadExecutor();
        HttpClient client = HttpClient.create(url, 16, 5000L, 10000L, settings, counter, executor);
        assertNotNull(client);
    }

    @Test
    public void testGet() throws IOException {
        server.enqueue(new MockResponse().setBody("Hello World"));
        HttpClient client = HttpClient.create(baseUrl);
        String response = client.get();
        assertEquals("Hello World", response);
    }

    @Test
    public void testGetWithSettings() throws IOException {
        server.enqueue(new MockResponse().setBody("Hello World"));
        HttpClient client = HttpClient.create(baseUrl);
        HttpSettings settings = HttpSettings.create().header("Accept", "text/plain");
        String response = client.get(settings);
        assertEquals("Hello World", response);
    }

    @Test
    public void testGetWithQueryParameters() throws IOException {
        server.enqueue(new MockResponse().setBody("Hello World"));
        HttpClient client = HttpClient.create(baseUrl);
        String response = client.get("param=value");
        assertEquals("Hello World", response);

        RecordedRequest request = server.takeRequest();
        assertTrue(request.getPath().contains("param=value"));
    }

    @Test
    public void testGetWithQueryParametersMap() throws IOException {
        server.enqueue(new MockResponse().setBody("Hello World"));
        HttpClient client = HttpClient.create(baseUrl);
        Map<String, Object> params = new HashMap<>();
        params.put("key", "value");
        String response = client.get(params);
        assertEquals("Hello World", response);
    }

    @Test
    public void testGetWithQueryParametersAndSettings() throws IOException {
        server.enqueue(new MockResponse().setBody("Hello World"));
        HttpClient client = HttpClient.create(baseUrl);
        HttpSettings settings = HttpSettings.create();
        String response = client.get("param=value", settings);
        assertEquals("Hello World", response);
    }

    @Test
    public void testGetWithResultClass() throws IOException {
        server.enqueue(new MockResponse().setBody("test response"));
        HttpClient client = HttpClient.create(baseUrl);
        String response = client.get(String.class);
        assertEquals("test response", response);
    }

    @Test
    public void testGetWithSettingsAndResultClass() throws IOException {
        server.enqueue(new MockResponse().setBody("test response"));
        HttpClient client = HttpClient.create(baseUrl);
        HttpSettings settings = HttpSettings.create();
        String response = client.get(settings, String.class);
        assertEquals("test response", response);
    }

    @Test
    public void testGetWithQueryParametersAndResultClass() throws IOException {
        server.enqueue(new MockResponse().setBody("test response"));
        HttpClient client = HttpClient.create(baseUrl);
        String response = client.get("param=value", String.class);
        assertEquals("test response", response);
    }

    @Test
    public void testGetWithAllParametersAndResultClass() throws IOException {
        server.enqueue(new MockResponse().setBody("test response"));
        HttpClient client = HttpClient.create(baseUrl);
        HttpSettings settings = HttpSettings.create();
        String response = client.get("param=value", settings, String.class);
        assertEquals("test response", response);
    }

    @Test
    public void testDelete() throws IOException {
        server.enqueue(new MockResponse().setBody("Deleted"));
        HttpClient client = HttpClient.create(baseUrl);
        String response = client.delete();
        assertEquals("Deleted", response);
    }

    @Test
    public void testDeleteWithSettings() throws IOException {
        server.enqueue(new MockResponse().setBody("Deleted"));
        HttpClient client = HttpClient.create(baseUrl);
        HttpSettings settings = HttpSettings.create();
        String response = client.delete(settings);
        assertEquals("Deleted", response);
    }

    @Test
    public void testDeleteWithQueryParameters() throws IOException {
        server.enqueue(new MockResponse().setBody("Deleted"));
        HttpClient client = HttpClient.create(baseUrl);
        String response = client.delete("id=123");
        assertEquals("Deleted", response);
    }

    @Test
    public void testDeleteWithQueryParametersAndSettings() throws IOException {
        server.enqueue(new MockResponse().setBody("Deleted"));
        HttpClient client = HttpClient.create(baseUrl);
        HttpSettings settings = HttpSettings.create();
        String response = client.delete("id=123", settings);
        assertEquals("Deleted", response);
    }

    @Test
    public void testDeleteWithResultClass() throws IOException {
        server.enqueue(new MockResponse().setBody("Deleted"));
        HttpClient client = HttpClient.create(baseUrl);
        String response = client.delete(String.class);
        assertEquals("Deleted", response);
    }

    @Test
    public void testDeleteWithQueryParametersAndResultClass() throws IOException {
        server.enqueue(new MockResponse().setBody("Deleted"));
        HttpClient client = HttpClient.create(baseUrl);
        String response = client.delete("id=123", String.class);
        assertEquals("Deleted", response);
    }

    @Test
    public void testDeleteWithSettingsAndResultClass() throws IOException {
        server.enqueue(new MockResponse().setBody("Deleted"));
        HttpClient client = HttpClient.create(baseUrl);
        HttpSettings settings = HttpSettings.create();
        String response = client.delete(settings, String.class);
        assertEquals("Deleted", response);
    }

    @Test
    public void testDeleteWithAllParametersAndResultClass() throws IOException {
        server.enqueue(new MockResponse().setBody("Deleted"));
        HttpClient client = HttpClient.create(baseUrl);
        HttpSettings settings = HttpSettings.create();
        String response = client.delete("id=123", settings, String.class);
        assertEquals("Deleted", response);
    }

    @Test
    public void testPost() throws IOException {
        server.enqueue(new MockResponse().setBody("Created"));
        HttpClient client = HttpClient.create(baseUrl);
        String response = client.post("test data");
        assertEquals("Created", response);
    }

    @Test
    public void testPostWithResultClass() throws IOException {
        server.enqueue(new MockResponse().setBody("Created"));
        HttpClient client = HttpClient.create(baseUrl);
        String response = client.post("test data", String.class);
        assertEquals("Created", response);
    }

    @Test
    public void testPostWithSettings() throws IOException {
        server.enqueue(new MockResponse().setBody("Created"));
        HttpClient client = HttpClient.create(baseUrl);
        HttpSettings settings = HttpSettings.create();
        String response = client.post("test data", settings);
        assertEquals("Created", response);
    }

    @Test
    public void testPostWithSettingsAndResultClass() throws IOException {
        server.enqueue(new MockResponse().setBody("Created"));
        HttpClient client = HttpClient.create(baseUrl);
        HttpSettings settings = HttpSettings.create();
        String response = client.post("test data", settings, String.class);
        assertEquals("Created", response);
    }

    @Test
    public void testPut() throws IOException {
        server.enqueue(new MockResponse().setBody("Updated"));
        HttpClient client = HttpClient.create(baseUrl);
        String response = client.put("test data");
        assertEquals("Updated", response);
    }

    @Test
    public void testPutWithResultClass() throws IOException {
        server.enqueue(new MockResponse().setBody("Updated"));
        HttpClient client = HttpClient.create(baseUrl);
        String response = client.put("test data", String.class);
        assertEquals("Updated", response);
    }

    @Test
    public void testPutWithSettings() throws IOException {
        server.enqueue(new MockResponse().setBody("Updated"));
        HttpClient client = HttpClient.create(baseUrl);
        HttpSettings settings = HttpSettings.create();
        String response = client.put("test data", settings);
        assertEquals("Updated", response);
    }

    @Test
    public void testPutWithSettingsAndResultClass() throws IOException {
        server.enqueue(new MockResponse().setBody("Updated"));
        HttpClient client = HttpClient.create(baseUrl);
        HttpSettings settings = HttpSettings.create();
        String response = client.put("test data", settings, String.class);
        assertEquals("Updated", response);
    }

    @Test
    public void testHead() throws IOException {
        server.enqueue(new MockResponse());
        HttpClient client = HttpClient.create(baseUrl);
        client.head();

        RecordedRequest request = server.takeRequest();
        assertEquals("HEAD", request.getMethod());
    }

    @Test
    public void testHeadWithSettings() throws IOException {
        server.enqueue(new MockResponse());
        HttpClient client = HttpClient.create(baseUrl);
        HttpSettings settings = HttpSettings.create();
        client.head(settings);

        RecordedRequest request = server.takeRequest();
        assertEquals("HEAD", request.getMethod());
    }

    @Test
    public void testExecuteWithHttpMethod() throws IOException {
        server.enqueue(new MockResponse().setBody("Response"));
        HttpClient client = HttpClient.create(baseUrl);
        String response = client.execute(HttpMethod.GET, null);
        assertEquals("Response", response);
    }

    @Test
    public void testExecuteWithHttpMethodAndResultClass() throws IOException {
        server.enqueue(new MockResponse().setBody("Response"));
        HttpClient client = HttpClient.create(baseUrl);
        String response = client.execute(HttpMethod.GET, null, String.class);
        assertEquals("Response", response);
    }

    @Test
    public void testExecuteWithHttpMethodAndSettings() throws IOException {
        server.enqueue(new MockResponse().setBody("Response"));
        HttpClient client = HttpClient.create(baseUrl);
        HttpSettings settings = HttpSettings.create();
        String response = client.execute(HttpMethod.GET, null, settings);
        assertEquals("Response", response);
    }

    @Test
    public void testExecuteWithAllParameters() throws IOException {
        server.enqueue(new MockResponse().setBody("Response"));
        HttpClient client = HttpClient.create(baseUrl);
        HttpSettings settings = HttpSettings.create();
        String response = client.execute(HttpMethod.GET, null, settings, String.class);
        assertEquals("Response", response);
    }

    @Test
    public void testExecuteToFile() throws IOException {
        server.enqueue(new MockResponse().setBody("File content"));
        HttpClient client = HttpClient.create(baseUrl);
        HttpSettings settings = HttpSettings.create();

        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit();

        client.execute(HttpMethod.GET, null, settings, tempFile);

        String content = new String(Files.readAllBytes(tempFile.toPath()));
        assertEquals("File content", content);
    }

    @Test
    public void testExecuteToOutputStream() throws IOException {
        server.enqueue(new MockResponse().setBody("Stream content"));
        HttpClient client = HttpClient.create(baseUrl);
        HttpSettings settings = HttpSettings.create();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        client.execute(HttpMethod.GET, null, settings, baos);

        assertEquals("Stream content", baos.toString());
    }

    @Test
    public void testExecuteToWriter() throws IOException {
        server.enqueue(new MockResponse().setBody("Writer content"));
        HttpClient client = HttpClient.create(baseUrl);
        HttpSettings settings = HttpSettings.create();

        StringWriter writer = new StringWriter();
        client.execute(HttpMethod.GET, null, settings, writer);

        assertEquals("Writer content", writer.toString());
    }

    @Test
    public void testOpenConnection() throws IOException {
        HttpClient client = HttpClient.create(baseUrl);
        HttpURLConnection connection = client.openConnection(HttpMethod.GET, null, false, String.class);
        assertNotNull(connection);
        assertEquals("GET", connection.getRequestMethod());
    }

    @Test
    public void testOpenConnectionWithQueryParameters() throws IOException {
        HttpClient client = HttpClient.create(baseUrl);
        HttpURLConnection connection = client.openConnection(HttpMethod.GET, "param=value", null, false, String.class);
        assertNotNull(connection);
        String urlStr = connection.getURL().toString();
        assertTrue(urlStr.contains("param=value"));
    }

    @Test
    public void testAsyncGet() throws Exception {
        server.enqueue(new MockResponse().setBody("Async response"));
        HttpClient client = HttpClient.create(baseUrl);

        ContinuableFuture<String> future = client.asyncGet();
        String response = future.get();
        assertEquals("Async response", response);
    }

    @Test
    public void testAsyncGetWithSettings() throws Exception {
        server.enqueue(new MockResponse().setBody("Async response"));
        HttpClient client = HttpClient.create(baseUrl);
        HttpSettings settings = HttpSettings.create();

        ContinuableFuture<String> future = client.asyncGet(settings);
        String response = future.get();
        assertEquals("Async response", response);
    }

    @Test
    public void testAsyncGetWithQueryParameters() throws Exception {
        server.enqueue(new MockResponse().setBody("Async response"));
        HttpClient client = HttpClient.create(baseUrl);

        ContinuableFuture<String> future = client.asyncGet("param=value");
        String response = future.get();
        assertEquals("Async response", response);
    }

    @Test
    public void testAsyncGetWithQueryParametersAndSettings() throws Exception {
        server.enqueue(new MockResponse().setBody("Async response"));
        HttpClient client = HttpClient.create(baseUrl);
        HttpSettings settings = HttpSettings.create();

        ContinuableFuture<String> future = client.asyncGet("param=value", settings);
        String response = future.get();
        assertEquals("Async response", response);
    }

    @Test
    public void testAsyncGetWithResultClass() throws Exception {
        server.enqueue(new MockResponse().setBody("Async response"));
        HttpClient client = HttpClient.create(baseUrl);

        ContinuableFuture<String> future = client.asyncGet(String.class);
        String response = future.get();
        assertEquals("Async response", response);
    }

    @Test
    public void testAsyncGetWithQueryParametersAndResultClass() throws Exception {
        server.enqueue(new MockResponse().setBody("Async response"));
        HttpClient client = HttpClient.create(baseUrl);

        ContinuableFuture<String> future = client.asyncGet("param=value", String.class);
        String response = future.get();
        assertEquals("Async response", response);
    }

    @Test
    public void testAsyncGetWithSettingsAndResultClass() throws Exception {
        server.enqueue(new MockResponse().setBody("Async response"));
        HttpClient client = HttpClient.create(baseUrl);
        HttpSettings settings = HttpSettings.create();

        ContinuableFuture<String> future = client.asyncGet(settings, String.class);
        String response = future.get();
        assertEquals("Async response", response);
    }

    @Test
    public void testAsyncGetWithAllParametersAndResultClass() throws Exception {
        server.enqueue(new MockResponse().setBody("Async response"));
        HttpClient client = HttpClient.create(baseUrl);
        HttpSettings settings = HttpSettings.create();

        ContinuableFuture<String> future = client.asyncGet("param=value", settings, String.class);
        String response = future.get();
        assertEquals("Async response", response);
    }

    @Test
    public void testAsyncDelete() throws Exception {
        server.enqueue(new MockResponse().setBody("Async deleted"));
        HttpClient client = HttpClient.create(baseUrl);

        ContinuableFuture<String> future = client.asyncDelete();
        String response = future.get();
        assertEquals("Async deleted", response);
    }

    @Test
    public void testAsyncDeleteWithQueryParameters() throws Exception {
        server.enqueue(new MockResponse().setBody("Async deleted"));
        HttpClient client = HttpClient.create(baseUrl);

        ContinuableFuture<String> future = client.asyncDelete("id=123");
        String response = future.get();
        assertEquals("Async deleted", response);
    }

    @Test
    public void testAsyncDeleteWithSettings() throws Exception {
        server.enqueue(new MockResponse().setBody("Async deleted"));
        HttpClient client = HttpClient.create(baseUrl);
        HttpSettings settings = HttpSettings.create();

        ContinuableFuture<String> future = client.asyncDelete(settings);
        String response = future.get();
        assertEquals("Async deleted", response);
    }

    @Test
    public void testAsyncDeleteWithQueryParametersAndSettings() throws Exception {
        server.enqueue(new MockResponse().setBody("Async deleted"));
        HttpClient client = HttpClient.create(baseUrl);
        HttpSettings settings = HttpSettings.create();

        ContinuableFuture<String> future = client.asyncDelete("id=123", settings);
        String response = future.get();
        assertEquals("Async deleted", response);
    }

    @Test
    public void testAsyncDeleteWithResultClass() throws Exception {
        server.enqueue(new MockResponse().setBody("Async deleted"));
        HttpClient client = HttpClient.create(baseUrl);

        ContinuableFuture<String> future = client.asyncDelete(String.class);
        String response = future.get();
        assertEquals("Async deleted", response);
    }

    @Test
    public void testAsyncDeleteWithQueryParametersAndResultClass() throws Exception {
        server.enqueue(new MockResponse().setBody("Async deleted"));
        HttpClient client = HttpClient.create(baseUrl);

        ContinuableFuture<String> future = client.asyncDelete("id=123", String.class);
        String response = future.get();
        assertEquals("Async deleted", response);
    }

    @Test
    public void testAsyncDeleteWithSettingsAndResultClass() throws Exception {
        server.enqueue(new MockResponse().setBody("Async deleted"));
        HttpClient client = HttpClient.create(baseUrl);
        HttpSettings settings = HttpSettings.create();

        ContinuableFuture<String> future = client.asyncDelete(settings, String.class);
        String response = future.get();
        assertEquals("Async deleted", response);
    }

    @Test
    public void testAsyncDeleteWithAllParametersAndResultClass() throws Exception {
        server.enqueue(new MockResponse().setBody("Async deleted"));
        HttpClient client = HttpClient.create(baseUrl);
        HttpSettings settings = HttpSettings.create();

        ContinuableFuture<String> future = client.asyncDelete("id=123", settings, String.class);
        String response = future.get();
        assertEquals("Async deleted", response);
    }

    @Test
    public void testAsyncPost() throws Exception {
        server.enqueue(new MockResponse().setBody("Async created"));
        HttpClient client = HttpClient.create(baseUrl);

        ContinuableFuture<String> future = client.asyncPost("test data");
        String response = future.get();
        assertEquals("Async created", response);
    }

    @Test
    public void testAsyncPostWithResultClass() throws Exception {
        server.enqueue(new MockResponse().setBody("Async created"));
        HttpClient client = HttpClient.create(baseUrl);

        ContinuableFuture<String> future = client.asyncPost("test data", String.class);
        String response = future.get();
        assertEquals("Async created", response);
    }

    @Test
    public void testAsyncPostWithSettings() throws Exception {
        server.enqueue(new MockResponse().setBody("Async created"));
        HttpClient client = HttpClient.create(baseUrl);
        HttpSettings settings = HttpSettings.create();

        ContinuableFuture<String> future = client.asyncPost("test data", settings);
        String response = future.get();
        assertEquals("Async created", response);
    }

    @Test
    public void testAsyncPostWithSettingsAndResultClass() throws Exception {
        server.enqueue(new MockResponse().setBody("Async created"));
        HttpClient client = HttpClient.create(baseUrl);
        HttpSettings settings = HttpSettings.create();

        ContinuableFuture<String> future = client.asyncPost("test data", settings, String.class);
        String response = future.get();
        assertEquals("Async created", response);
    }

    @Test
    public void testAsyncPut() throws Exception {
        server.enqueue(new MockResponse().setBody("Async updated"));
        HttpClient client = HttpClient.create(baseUrl);

        ContinuableFuture<String> future = client.asyncPut("test data");
        String response = future.get();
        assertEquals("Async updated", response);
    }

    @Test
    public void testAsyncPutWithSettings() throws Exception {
        server.enqueue(new MockResponse().setBody("Async updated"));
        HttpClient client = HttpClient.create(baseUrl);
        HttpSettings settings = HttpSettings.create();

        ContinuableFuture<String> future = client.asyncPut("test data", settings);
        String response = future.get();
        assertEquals("Async updated", response);
    }

    @Test
    public void testAsyncPutWithSettingsAndResultClass() throws Exception {
        server.enqueue(new MockResponse().setBody("Async updated"));
        HttpClient client = HttpClient.create(baseUrl);
        HttpSettings settings = HttpSettings.create();

        ContinuableFuture<String> future = client.asyncPut("test data", settings, String.class);
        String response = future.get();
        assertEquals("Async updated", response);
    }

    @Test
    public void testAsyncHead() throws Exception {
        server.enqueue(new MockResponse());
        HttpClient client = HttpClient.create(baseUrl);

        ContinuableFuture<Void> future = client.asyncHead();
        future.get();

        RecordedRequest request = server.takeRequest();
        assertEquals("HEAD", request.getMethod());
    }

    @Test
    public void testAsyncHeadWithSettings() throws Exception {
        server.enqueue(new MockResponse());
        HttpClient client = HttpClient.create(baseUrl);
        HttpSettings settings = HttpSettings.create();

        ContinuableFuture<Void> future = client.asyncHead(settings);
        future.get();

        RecordedRequest request = server.takeRequest();
        assertEquals("HEAD", request.getMethod());
    }

    @Test
    public void testAsyncExecute() throws Exception {
        server.enqueue(new MockResponse().setBody("Async response"));
        HttpClient client = HttpClient.create(baseUrl);

        ContinuableFuture<String> future = client.asyncExecute(HttpMethod.GET, null);
        String response = future.get();
        assertEquals("Async response", response);
    }

    @Test
    public void testAsyncExecuteWithResultClass() throws Exception {
        server.enqueue(new MockResponse().setBody("Async response"));
        HttpClient client = HttpClient.create(baseUrl);

        ContinuableFuture<String> future = client.asyncExecute(HttpMethod.GET, null, String.class);
        String response = future.get();
        assertEquals("Async response", response);
    }

    @Test
    public void testAsyncExecuteWithSettings() throws Exception {
        server.enqueue(new MockResponse().setBody("Async response"));
        HttpClient client = HttpClient.create(baseUrl);
        HttpSettings settings = HttpSettings.create();

        ContinuableFuture<String> future = client.asyncExecute(HttpMethod.GET, null, settings);
        String response = future.get();
        assertEquals("Async response", response);
    }

    @Test
    public void testAsyncExecuteWithAllParameters() throws Exception {
        server.enqueue(new MockResponse().setBody("Async response"));
        HttpClient client = HttpClient.create(baseUrl);
        HttpSettings settings = HttpSettings.create();

        ContinuableFuture<String> future = client.asyncExecute(HttpMethod.GET, null, settings, String.class);
        String response = future.get();
        assertEquals("Async response", response);
    }

    @Test
    public void testAsyncExecuteToFile() throws Exception {
        server.enqueue(new MockResponse().setBody("File content"));
        HttpClient client = HttpClient.create(baseUrl);
        HttpSettings settings = HttpSettings.create();

        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit();

        ContinuableFuture<Void> future = client.asyncExecute(HttpMethod.GET, null, settings, tempFile);
        future.get();

        String content = new String(Files.readAllBytes(tempFile.toPath()));
        assertEquals("File content", content);
    }

    @Test
    public void testAsyncExecuteToOutputStream() throws Exception {
        server.enqueue(new MockResponse().setBody("Stream content"));
        HttpClient client = HttpClient.create(baseUrl);
        HttpSettings settings = HttpSettings.create();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ContinuableFuture<Void> future = client.asyncExecute(HttpMethod.GET, null, settings, baos);
        future.get();

        assertEquals("Stream content", baos.toString());
    }

    @Test
    public void testAsyncExecuteToWriter() throws Exception {
        server.enqueue(new MockResponse().setBody("Writer content"));
        HttpClient client = HttpClient.create(baseUrl);
        HttpSettings settings = HttpSettings.create();

        StringWriter writer = new StringWriter();
        ContinuableFuture<Void> future = client.asyncExecute(HttpMethod.GET, null, settings, writer);
        future.get();

        assertEquals("Writer content", writer.toString());
    }

    @Test
    public void testClose() {
        HttpClient client = HttpClient.create("https://api.example.com");
        client.close();
    }

    @Test
    public void testInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> HttpClient.create(""));
        assertThrows(IllegalArgumentException.class, () -> HttpClient.create((String) null, 16, 5000L, 10000L));
        assertThrows(IllegalArgumentException.class, () -> HttpClient.create("https://api.example.com", -1, 5000L, 10000L));
        assertThrows(IllegalArgumentException.class, () -> HttpClient.create("https://api.example.com", 16, -1L, 10000L));
        assertThrows(IllegalArgumentException.class, () -> HttpClient.create("https://api.example.com", 16, 5000L, -1L));
    }

    private static class MockWebServer {
        private ServerSocket serverSocket;
        private Thread serverThread;
        private final Queue<MockResponse> responses = new LinkedList<>();
        private final Queue<RecordedRequest> requests = new LinkedList<>();

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
            Map<String, String> headers = new HashMap<>();
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                String[] parts = line.split(": ", 2);
                if (parts.length == 2) {
                    headers.put(parts[0], parts[1]);
                }
            }

            RecordedRequest request = new RecordedRequest(requestLine, headers);
            requests.offer(request);

            MockResponse response = responses.poll();
            if (response == null) {
                response = new MockResponse().setResponseCode(404).setBody("Not Found");
            }

            writer.println("HTTP/1.1 " + response.responseCode + " " + response.reasonPhrase);
            writer.println("Content-Length: " + response.body.length());
            writer.println();
            writer.print(response.body);
            writer.flush();

            socket.close();
        }

        public URL url(String path) {
            try {
                return new URL("http://localhost:" + serverSocket.getLocalPort() + path);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        public void enqueue(MockResponse response) {
            responses.offer(response);
        }

        public RecordedRequest takeRequest() {
            return requests.poll();
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
        private int responseCode = 200;
        private String reasonPhrase = "OK";
        private String body = "";

        public MockResponse setResponseCode(int code) {
            this.responseCode = code;
            return this;
        }

        public MockResponse setBody(String body) {
            this.body = body;
            return this;
        }
    }

    private static class RecordedRequest {
        private final String requestLine;
        private final Map<String, String> headers;

        public RecordedRequest(String requestLine, Map<String, String> headers) {
            this.requestLine = requestLine;
            this.headers = headers;
        }

        public String getMethod() {
            return requestLine.split(" ")[0];
        }

        public String getPath() {
            return requestLine.split(" ")[1];
        }
    }
}

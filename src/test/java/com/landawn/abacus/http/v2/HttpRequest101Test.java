package com.landawn.abacus.http.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.net.Authenticator;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.PushPromiseHandler;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.http.HttpHeaders;
import com.landawn.abacus.http.HttpMethod;

import lombok.Data;


@ExtendWith(MockitoExtension.class)
public class HttpRequest101Test extends TestBase {

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<String> mockStringResponse;

    @Mock
    private HttpResponse<byte[]> mockByteArrayResponse;

    @Mock
    private HttpResponse<InputStream> mockInputStreamResponse;

    @Mock
    private HttpResponse<Void> mockVoidResponse;

    @Mock
    private CompletableFuture<HttpResponse<String>> mockFutureResponse;

    private String testUrl = "https://api.example.com/test";
    private URI testUri;

    @BeforeEach
    public void setUp() throws Exception {
        testUri = new URI(testUrl);
        //    when(mockStringResponse.statusCode()).thenReturn(200);
        //    when(mockStringResponse.body()).thenReturn("test response");
        //    when(mockByteArrayResponse.statusCode()).thenReturn(200);
        //    when(mockByteArrayResponse.body()).thenReturn("test response".getBytes());
        //    when(mockInputStreamResponse.statusCode()).thenReturn(200);
        //    when(mockInputStreamResponse.body()).thenReturn(new java.io.ByteArrayInputStream("test response".getBytes()));
        //    when(mockVoidResponse.statusCode()).thenReturn(200);
        //    when(mockFutureResponse.get()).thenReturn(mockStringResponse);
        //    when(mockFutureResponse.getNow(null)).thenReturn(mockStringResponse);
        //    when(mockHttpClient.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(mockFutureResponse);
        //    when(mockHttpClient.send(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(mockStringResponse);
    }

    // Test static factory methods

    @Test
    public void testCreateWithUrlAndHttpClient() {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        assertNotNull(request);
    }

    @Test
    public void testCreateWithURLObjectAndHttpClient() throws Exception {
        URL url = new URL(testUrl);
        HttpRequest request = HttpRequest.create(url, mockHttpClient);
        assertNotNull(request);
    }

    @Test
    public void testCreateWithURIAndHttpClient() {
        HttpRequest request = HttpRequest.create(testUri, mockHttpClient);
        assertNotNull(request);
    }

    @Test
    public void testUrlWithString() {
        HttpRequest request = HttpRequest.url(testUrl);
        assertNotNull(request);
    }

    @Test
    public void testUrlWithStringAndTimeouts() {
        HttpRequest request = HttpRequest.url(testUrl, 5000L, 30000L);
        assertNotNull(request);
    }

    @Test
    public void testUrlWithURLObject() throws Exception {
        URL url = new URL(testUrl);
        HttpRequest request = HttpRequest.url(url);
        assertNotNull(request);
    }

    @Test
    public void testUrlWithURLObjectAndTimeouts() throws Exception {
        URL url = new URL(testUrl);
        HttpRequest request = HttpRequest.url(url, 5000L, 30000L);
        assertNotNull(request);
    }

    @Test
    public void testUrlWithURI() {
        HttpRequest request = HttpRequest.url(testUri);
        assertNotNull(request);
    }

    @Test
    public void testUrlWithURIAndTimeouts() {
        HttpRequest request = HttpRequest.url(testUri, 5000L, 30000L);
        assertNotNull(request);
    }

    // Test configuration methods

    @Test
    public void testConnectTimeout() {
        HttpRequest request = HttpRequest.url(testUrl).connectTimeout(Duration.ofSeconds(10));
        assertNotNull(request);
    }

    @Test
    public void testReadTimeout() {
        HttpRequest request = HttpRequest.url(testUrl).readTimeout(Duration.ofSeconds(30));
        assertNotNull(request);
    }

    @Test
    public void testAuthenticator() {
        Authenticator authenticator = mock(Authenticator.class);
        HttpRequest request = HttpRequest.url(testUrl).authenticator(authenticator);
        assertNotNull(request);
    }

    @Test
    public void testBasicAuth() {
        HttpRequest request = HttpRequest.url(testUrl).basicAuth("username", "password");
        assertNotNull(request);
    }

    // Test header methods

    @Test
    public void testHeader() {
        HttpRequest request = HttpRequest.url(testUrl).header("Accept", "application/json");
        assertNotNull(request);
    }

    @Test
    public void testHeadersWithTwoHeaders() {
        HttpRequest request = HttpRequest.url(testUrl).headers("Accept", "application/json", "Content-Type", "application/json");
        assertNotNull(request);
    }

    @Test
    public void testHeadersWithThreeHeaders() {
        HttpRequest request = HttpRequest.url(testUrl)
                .headers("Accept", "application/json", "Content-Type", "application/json", "Authorization", "Bearer token");
        assertNotNull(request);
    }

    @Test
    public void testHeadersWithMap() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");

        HttpRequest request = HttpRequest.url(testUrl).headers(headers);
        assertNotNull(request);
    }

    @Test
    public void testHeadersWithHttpHeaders() {
        HttpHeaders headers = HttpHeaders.create();
        headers.set("Accept", "application/json");

        HttpRequest request = HttpRequest.url(testUrl).headers(headers);
        assertNotNull(request);
    }

    // Test query methods

    @Test
    public void testQueryWithString() {
        HttpRequest request = HttpRequest.url(testUrl).query("param1=value1&param2=value2");
        assertNotNull(request);
    }

    @Test
    public void testQueryWithMap() {
        Map<String, Object> params = new HashMap<>();
        params.put("param1", "value1");
        params.put("param2", "value2");

        HttpRequest request = HttpRequest.url(testUrl).query(params);
        assertNotNull(request);
    }

    // Test body methods

    @Test
    public void testJsonBodyWithString() {
        HttpRequest request = HttpRequest.url(testUrl).jsonBody("{\"key\":\"value\"}");
        assertNotNull(request);
    }

    @Test
    public void testJsonBodyWithObject() {
        Map<String, String> obj = new HashMap<>();
        obj.put("key", "value");

        HttpRequest request = HttpRequest.url(testUrl).jsonBody(obj);
        assertNotNull(request);
    }

    @Test
    public void testXmlBodyWithString() {
        HttpRequest request = HttpRequest.url(testUrl).xmlBody("<root><key>value</key></root>");
        assertNotNull(request);
    }

    @Test
    public void testXmlBodyWithObject() {
        Map<String, String> obj = new HashMap<>();
        obj.put("key", "value");

        HttpRequest request = HttpRequest.url(testUrl).xmlBody(obj);
        assertNotNull(request);
    }

    @Test
    public void testFormBodyWithMap() {
        Map<String, String> formData = new HashMap<>();
        formData.put("username", "user");
        formData.put("password", "pass");

        HttpRequest request = HttpRequest.url(testUrl).formBody(formData);
        assertNotNull(request);
    }

    @Test
    public void testFormBodyWithObject() {
        TestBean bean = new TestBean("user", "pass");

        HttpRequest request = HttpRequest.url(testUrl).formBody(bean);
        assertNotNull(request);
    }

    @Test
    public void testBodyWithBodyPublisher() {
        BodyPublisher publisher = BodyPublishers.ofString("test");
        HttpRequest request = HttpRequest.url(testUrl).body(publisher);
        assertNotNull(request);
    }

    // Test synchronous execution methods

    @Test
    public void testGet() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        when(mockStringResponse.statusCode()).thenReturn(200);
        when(mockStringResponse.body()).thenReturn("test response");
        when(mockHttpClient.send(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(mockStringResponse);

        HttpResponse<String> response = request.get();
        assertEquals(200, response.statusCode());
        assertEquals("test response", response.body());
    }

    @Test
    public void testGetWithBodyHandler() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        when(mockHttpClient.send(any(java.net.http.HttpRequest.class), eq(BodyHandlers.ofString()))).thenReturn(mockStringResponse);

        HttpResponse<String> response = request.get(BodyHandlers.ofString());
        assertNotNull(response);
    }

    @Test
    public void testGetWithResultClass() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        when(mockStringResponse.statusCode()).thenReturn(200);
        when(mockStringResponse.body()).thenReturn("test response");
        when(mockHttpClient.send(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(mockStringResponse);

        String result = request.get(String.class);
        assertEquals("test response", result);
    }

    @Test
    public void testPost() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        when(mockHttpClient.send(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(mockStringResponse);

        HttpResponse<String> response = request.post();
        assertNotNull(response);
    }

    @Test
    public void testPostWithBodyHandler() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        when(mockHttpClient.send(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(mockStringResponse);

        HttpResponse<String> response = request.post(BodyHandlers.ofString());
        assertNotNull(response);
    }

    @Test
    public void testPostWithResultClass() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        when(mockStringResponse.statusCode()).thenReturn(200);
        when(mockStringResponse.body()).thenReturn("test response");
        when(mockHttpClient.send(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(mockStringResponse);

        String result = request.post(String.class);
        assertEquals("test response", result);
    }

    @Test
    public void testPut() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        when(mockHttpClient.send(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(mockStringResponse);

        HttpResponse<String> response = request.put();
        assertNotNull(response);
    }

    @Test
    public void testPutWithBodyHandler() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        when(mockHttpClient.send(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(mockStringResponse);

        HttpResponse<String> response = request.put(BodyHandlers.ofString());
        assertNotNull(response);
    }

    @Test
    public void testPutWithResultClass() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        when(mockStringResponse.statusCode()).thenReturn(200);
        when(mockStringResponse.body()).thenReturn("test response");
        when(mockHttpClient.send(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(mockStringResponse);

        String result = request.put(String.class);
        assertEquals("test response", result);
    }

    @Test
    public void testPatch() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        when(mockHttpClient.send(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(mockStringResponse);

        HttpResponse<String> response = request.patch();
        assertNotNull(response);
    }

    @Test
    public void testPatchWithBodyHandler() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        when(mockHttpClient.send(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(mockStringResponse);

        HttpResponse<String> response = request.patch(BodyHandlers.ofString());
        assertNotNull(response);
    }

    @Test
    public void testPatchWithResultClass() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        when(mockStringResponse.statusCode()).thenReturn(200);
        when(mockStringResponse.body()).thenReturn("test response");
        when(mockHttpClient.send(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(mockStringResponse);

        String result = request.patch(String.class);
        assertEquals("test response", result);
    }

    @Test
    public void testDelete() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        when(mockHttpClient.send(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(mockStringResponse);

        HttpResponse<String> response = request.delete();
        assertNotNull(response);
    }

    @Test
    public void testDeleteWithBodyHandler() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        when(mockHttpClient.send(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(mockStringResponse);

        HttpResponse<String> response = request.delete(BodyHandlers.ofString());
        assertNotNull(response);
    }

    @Test
    public void testDeleteWithResultClass() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        when(mockStringResponse.statusCode()).thenReturn(200);
        when(mockStringResponse.body()).thenReturn("test response");
        when(mockHttpClient.send(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(mockStringResponse);

        String result = request.delete(String.class);
        assertEquals("test response", result);
    }

    @Test
    public void testHead() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        when(mockHttpClient.send(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(mockVoidResponse);

        HttpResponse<Void> response = request.head();
        assertNotNull(response);
    }

    @Test
    public void testExecuteWithHttpMethod() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        when(mockHttpClient.send(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(mockStringResponse);

        HttpResponse<String> response = request.execute(HttpMethod.GET);
        assertNotNull(response);
    }

    @Test
    public void testExecuteWithHttpMethodAndBodyHandler() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        when(mockHttpClient.send(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(mockStringResponse);

        HttpResponse<String> response = request.execute(HttpMethod.POST, BodyHandlers.ofString());
        assertNotNull(response);
    }

    @Test
    public void testExecuteWithHttpMethodAndResultClass() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        when(mockStringResponse.statusCode()).thenReturn(200);
        when(mockStringResponse.body()).thenReturn("test response");
        when(mockHttpClient.send(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(mockStringResponse);

        String result = request.execute(HttpMethod.PUT, String.class);
        assertEquals("test response", result);
    }

    // Test asynchronous execution methods

    @Test
    public void testAsyncGet() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        when(mockHttpClient.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(mockFutureResponse);

        CompletableFuture<HttpResponse<String>> future = request.asyncGet();
        assertNotNull(future);
    }

    @Test
    public void testAsyncGetWithBodyHandler() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        when(mockHttpClient.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(mockFutureResponse);

        CompletableFuture<HttpResponse<String>> future = request.asyncGet(BodyHandlers.ofString());
        assertNotNull(future);
    }

    @Test
    public void testAsyncGetWithResultClass() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        when(mockStringResponse.statusCode()).thenReturn(200);
        when(mockStringResponse.body()).thenReturn("test response");
        CompletableFuture<HttpResponse<String>> completedFuture = CompletableFuture.completedFuture(mockStringResponse);
        when(mockHttpClient.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(completedFuture);

        CompletableFuture<String> future = request.asyncGet(String.class);
        assertNotNull(future);
        assertEquals("test response", future.get());
    }

    @Test
    public void testAsyncGetWithBodyHandlerAndPushPromiseHandler() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        PushPromiseHandler<String> pushHandler = mock(PushPromiseHandler.class);
        when(mockHttpClient.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class), eq(pushHandler))).thenReturn(mockFutureResponse);

        CompletableFuture<HttpResponse<String>> future = request.asyncGet(BodyHandlers.ofString(), pushHandler);
        assertNotNull(future);
    }

    @Test
    public void testAsyncPost() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        when(mockHttpClient.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(mockFutureResponse);

        CompletableFuture<HttpResponse<String>> future = request.asyncPost();
        assertNotNull(future);
    }

    @Test
    public void testAsyncPostWithBodyHandler() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        when(mockHttpClient.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(mockFutureResponse);

        CompletableFuture<HttpResponse<String>> future = request.asyncPost(BodyHandlers.ofString());
        assertNotNull(future);
    }

    @Test
    public void testAsyncPostWithResultClass() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        CompletableFuture<HttpResponse<String>> completedFuture = CompletableFuture.completedFuture(mockStringResponse);
        when(mockHttpClient.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(completedFuture);

        CompletableFuture<String> future = request.asyncPost(String.class);
        assertNotNull(future);
    }

    @Test
    public void testAsyncPostWithBodyHandlerAndPushPromiseHandler() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        PushPromiseHandler<String> pushHandler = mock(PushPromiseHandler.class);
        when(mockHttpClient.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class), eq(pushHandler))).thenReturn(mockFutureResponse);

        CompletableFuture<HttpResponse<String>> future = request.asyncPost(BodyHandlers.ofString(), pushHandler);
        assertNotNull(future);
    }

    @Test
    public void testAsyncPut() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        when(mockHttpClient.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(mockFutureResponse);

        CompletableFuture<HttpResponse<String>> future = request.asyncPut();
        assertNotNull(future);
    }

    @Test
    public void testAsyncPutWithBodyHandler() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        when(mockHttpClient.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(mockFutureResponse);

        CompletableFuture<HttpResponse<String>> future = request.asyncPut(BodyHandlers.ofString());
        assertNotNull(future);
    }

    @Test
    public void testAsyncPutWithResultClass() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        CompletableFuture<HttpResponse<String>> completedFuture = CompletableFuture.completedFuture(mockStringResponse);
        when(mockHttpClient.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(completedFuture);

        CompletableFuture<String> future = request.asyncPut(String.class);
        assertNotNull(future);
    }

    @Test
    public void testAsyncPutWithBodyHandlerAndPushPromiseHandler() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        PushPromiseHandler<String> pushHandler = mock(PushPromiseHandler.class);
        when(mockHttpClient.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class), eq(pushHandler))).thenReturn(mockFutureResponse);

        CompletableFuture<HttpResponse<String>> future = request.asyncPut(BodyHandlers.ofString(), pushHandler);
        assertNotNull(future);
    }

    @Test
    public void testAsyncPatch() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        when(mockHttpClient.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(mockFutureResponse);

        CompletableFuture<HttpResponse<String>> future = request.asyncPatch();
        assertNotNull(future);
    }

    @Test
    public void testAsyncPatchWithBodyHandler() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        when(mockHttpClient.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(mockFutureResponse);

        CompletableFuture<HttpResponse<String>> future = request.asyncPatch(BodyHandlers.ofString());
        assertNotNull(future);
    }

    @Test
    public void testAsyncPatchWithResultClass() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        CompletableFuture<HttpResponse<String>> completedFuture = CompletableFuture.completedFuture(mockStringResponse);
        when(mockHttpClient.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(completedFuture);

        CompletableFuture<String> future = request.asyncPatch(String.class);
        assertNotNull(future);
    }

    @Test
    public void testAsyncPatchWithBodyHandlerAndPushPromiseHandler() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        PushPromiseHandler<String> pushHandler = mock(PushPromiseHandler.class);
        when(mockHttpClient.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class), eq(pushHandler))).thenReturn(mockFutureResponse);

        CompletableFuture<HttpResponse<String>> future = request.asyncPatch(BodyHandlers.ofString(), pushHandler);
        assertNotNull(future);
    }

    @Test
    public void testAsyncDelete() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        when(mockHttpClient.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(mockFutureResponse);

        CompletableFuture<HttpResponse<String>> future = request.asyncDelete();
        assertNotNull(future);
    }

    @Test
    public void testAsyncDeleteWithBodyHandler() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        when(mockHttpClient.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(mockFutureResponse);

        CompletableFuture<HttpResponse<String>> future = request.asyncDelete(BodyHandlers.ofString());
        assertNotNull(future);
    }

    @Test
    public void testAsyncDeleteWithResultClass() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        CompletableFuture<HttpResponse<String>> completedFuture = CompletableFuture.completedFuture(mockStringResponse);
        when(mockHttpClient.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(completedFuture);

        CompletableFuture<String> future = request.asyncDelete(String.class);
        assertNotNull(future);
    }

    @Test
    public void testAsyncDeleteWithBodyHandlerAndPushPromiseHandler() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        PushPromiseHandler<String> pushHandler = mock(PushPromiseHandler.class);
        when(mockHttpClient.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class), eq(pushHandler))).thenReturn(mockFutureResponse);

        CompletableFuture<HttpResponse<String>> future = request.asyncDelete(BodyHandlers.ofString(), pushHandler);
        assertNotNull(future);
    }

    @Test
    public void testAsyncHead() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        CompletableFuture<HttpResponse<Void>> voidFuture = CompletableFuture.completedFuture(mockVoidResponse);
        when(mockHttpClient.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(voidFuture);

        CompletableFuture<HttpResponse<Void>> future = request.asyncHead();
        assertNotNull(future);
    }

    @Test
    public void testAsyncExecuteWithHttpMethod() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        when(mockHttpClient.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(mockFutureResponse);

        CompletableFuture<HttpResponse<String>> future = request.asyncExecute(HttpMethod.GET);
        assertNotNull(future);
    }

    @Test
    public void testAsyncExecuteWithHttpMethodAndBodyHandler() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        when(mockHttpClient.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(mockFutureResponse);

        CompletableFuture<HttpResponse<String>> future = request.asyncExecute(HttpMethod.POST, BodyHandlers.ofString());
        assertNotNull(future);
    }

    @Test
    public void testAsyncExecuteWithHttpMethodAndResultClass() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        CompletableFuture<HttpResponse<String>> completedFuture = CompletableFuture.completedFuture(mockStringResponse);
        when(mockHttpClient.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(completedFuture);

        CompletableFuture<String> future = request.asyncExecute(HttpMethod.PUT, String.class);
        assertNotNull(future);
    }

    @Test
    public void testAsyncExecuteWithHttpMethodBodyHandlerAndPushPromiseHandler() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        PushPromiseHandler<String> pushHandler = mock(PushPromiseHandler.class);
        when(mockHttpClient.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class), eq(pushHandler))).thenReturn(mockFutureResponse);

        CompletableFuture<HttpResponse<String>> future = request.asyncExecute(HttpMethod.PATCH, BodyHandlers.ofString(), pushHandler);
        assertNotNull(future);
    }

    // Test error cases

    @Test
    public void testGetWithNon200StatusCode() throws Exception {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        when(mockStringResponse.statusCode()).thenReturn(404);
        when(mockStringResponse.body()).thenReturn("Not Found");
        when(mockHttpClient.send(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(mockStringResponse);

        assertThrows(UncheckedIOException.class, () -> request.get(String.class));
    }

    @Test
    public void testExecuteWithNullHttpMethod() {
        HttpRequest request = HttpRequest.url(testUrl);
        assertThrows(IllegalArgumentException.class, () -> request.execute(null));
    }

    @Test
    public void testAsyncExecuteWithNullHttpMethod() {
        HttpRequest request = HttpRequest.url(testUrl);
        assertThrows(IllegalArgumentException.class, () -> request.asyncExecute(null));
    }

    @Data
    public static class TestBean {
        private final String username;
        private final String password;

        public TestBean(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }
}

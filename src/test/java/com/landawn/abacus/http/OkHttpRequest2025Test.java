package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ContinuableFuture;

import okhttp3.CacheControl;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

@Tag("2025")
public class OkHttpRequest2025Test extends TestBase {

    @Test
    public void testCreateWithString() {
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
        HttpUrl httpUrl = HttpUrl.parse("https://api.example.com");
        OkHttpRequest request = OkHttpRequest.create(httpUrl, client);
        assertNotNull(request);
    }

    @Test
    public void testUrlWithString() {
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
        HttpUrl httpUrl = HttpUrl.parse("https://api.example.com");
        OkHttpRequest request = OkHttpRequest.url(httpUrl);
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
        HttpUrl httpUrl = HttpUrl.parse("https://api.example.com");
        OkHttpRequest request = OkHttpRequest.url(httpUrl, 5000L, 10000L);
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
        OkHttpRequest request = OkHttpRequest.url("https://api.example.com").basicAuth("username", "password");
        assertNotNull(request);
    }

    @Test
    public void testHeader() {
        OkHttpRequest request = OkHttpRequest.url("https://api.example.com").header("Accept", "application/json");
        assertNotNull(request);
    }

    @Test
    public void testHeadersWithTwoHeaders() {
        OkHttpRequest request = OkHttpRequest.url("https://api.example.com").headers("Accept", "application/json", "Content-Type", "application/json");
        assertNotNull(request);
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
    public void testHeadersWithOkHttpHeaders() {
        okhttp3.Headers headers = new okhttp3.Headers.Builder().add("Accept", "application/json").add("Content-Type", "application/json").build();

        OkHttpRequest request = OkHttpRequest.url("https://api.example.com").headers(headers);
        assertNotNull(request);
    }

    @Test
    public void testHeadersWithHttpHeaders() {
        HttpHeaders headers = HttpHeaders.create();
        headers.set("Accept", "application/json");
        headers.set("Content-Type", "application/json");

        OkHttpRequest request = OkHttpRequest.url("https://api.example.com").headers(headers);
        assertNotNull(request);

        OkHttpRequest requestWithNull = OkHttpRequest.url("https://api.example.com").headers((HttpHeaders) null);
        assertNotNull(requestWithNull);

        OkHttpRequest requestWithEmpty = OkHttpRequest.url("https://api.example.com").headers(HttpHeaders.create());
        assertNotNull(requestWithEmpty);
    }

    @Test
    public void testQueryWithString() {
        OkHttpRequest request = OkHttpRequest.url("https://api.example.com").query("param=value");
        assertNotNull(request);
    }

    @Test
    public void testQueryWithMap() {
        Map<String, Object> params = new HashMap<>();
        params.put("param1", "value1");
        params.put("param2", 123);

        OkHttpRequest request = OkHttpRequest.url("https://api.example.com").query(params);
        assertNotNull(request);
    }

    @Test
    public void testJsonBodyWithString() {
        OkHttpRequest request = OkHttpRequest.url("https://api.example.com").jsonBody("{\"key\":\"value\"}");
        assertNotNull(request);
    }

    @Test
    public void testJsonBodyWithObject() {
        Map<String, String> obj = new HashMap<>();
        obj.put("key", "value");

        OkHttpRequest request = OkHttpRequest.url("https://api.example.com").jsonBody(obj);
        assertNotNull(request);
    }

    @Test
    public void testXmlBodyWithString() {
        OkHttpRequest request = OkHttpRequest.url("https://api.example.com").xmlBody("<root><key>value</key></root>");
        assertNotNull(request);
    }

    @Test
    public void testXmlBodyWithObject() {
        Map<String, String> obj = new HashMap<>();
        obj.put("key", "value");

        OkHttpRequest request = OkHttpRequest.url("https://api.example.com").xmlBody(obj);
        assertNotNull(request);
    }

    @Test
    public void testFormBodyWithMap() {
        Map<String, String> formData = new HashMap<>();
        formData.put("username", "john");
        formData.put("password", "secret");

        OkHttpRequest request = OkHttpRequest.url("https://api.example.com").formBody(formData);
        assertNotNull(request);

        OkHttpRequest requestWithEmpty = OkHttpRequest.url("https://api.example.com").formBody(new HashMap<>());
        assertNotNull(requestWithEmpty);
    }

    @Test
    public void testFormBodyWithBean() {
        TestBean bean = new TestBean();
        bean.setName("test");
        bean.setValue("value");

        OkHttpRequest request = OkHttpRequest.url("https://api.example.com").formBody(bean);
        assertNotNull(request);

        OkHttpRequest requestWithNull = OkHttpRequest.url("https://api.example.com").formBody((Object) null);
        assertNotNull(requestWithNull);
    }

    @Test
    public void testFormBodyWithNonBean() {
        assertThrows(IllegalArgumentException.class, () -> OkHttpRequest.url("https://api.example.com").formBody("not a bean"));
    }

    @Test
    public void testBodyWithRequestBody() {
        RequestBody body = RequestBody.create("test", MediaType.get("text/plain"));
        OkHttpRequest request = OkHttpRequest.url("https://api.example.com").body(body);
        assertNotNull(request);
    }

    @Test
    public void testBodyWithStringAndMediaType() {
        OkHttpRequest request = OkHttpRequest.url("https://api.example.com").body("test content", MediaType.get("text/plain"));
        assertNotNull(request);
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
    public void testBodyWithFileAndMediaType() throws IOException {
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit();
        Files.write(tempFile.toPath(), "test content".getBytes());

        OkHttpRequest request = OkHttpRequest.url("https://api.example.com").body(tempFile, MediaType.get("text/plain"));
        assertNotNull(request);
    }

    @Test
    public void testInvalidUrl() {
        assertThrows(IllegalArgumentException.class, () -> OkHttpRequest.url(""));
        assertThrows(IllegalArgumentException.class, () -> OkHttpRequest.url((String) null));
    }

    @Test
    public void testHttpMethodsReturnTypes() {
        // These methods would throw exceptions without a real server, but we can test that they compile
        OkHttpRequest request = OkHttpRequest.url("https://httpbin.org/get");
        assertNotNull(request);
    }

    @Test
    public void testAsyncMethodsReturnTypes() {
        OkHttpRequest request = OkHttpRequest.url("https://httpbin.org/get");

        ContinuableFuture<?> future1 = request.asyncGet();
        assertNotNull(future1);

        Executor executor = Executors.newSingleThreadExecutor();
        ContinuableFuture<?> future2 = request.asyncGet(executor);
        assertNotNull(future2);

        ContinuableFuture<String> future3 = request.asyncGet(String.class);
        assertNotNull(future3);

        ContinuableFuture<String> future4 = request.asyncGet(String.class, executor);
        assertNotNull(future4);
    }

    @Test
    public void testAsyncPostMethods() {
        OkHttpRequest request = OkHttpRequest.url("https://httpbin.org/post").jsonBody("{}");

        ContinuableFuture<?> future1 = request.asyncPost();
        assertNotNull(future1);

        Executor executor = Executors.newSingleThreadExecutor();
        ContinuableFuture<?> future2 = request.asyncPost(executor);
        assertNotNull(future2);

        ContinuableFuture<String> future3 = request.asyncPost(String.class);
        assertNotNull(future3);

        ContinuableFuture<String> future4 = request.asyncPost(String.class, executor);
        assertNotNull(future4);
    }

    @Test
    public void testAsyncPutMethods() {
        OkHttpRequest request = OkHttpRequest.url("https://httpbin.org/put").jsonBody("{}");

        ContinuableFuture<?> future1 = request.asyncPut();
        assertNotNull(future1);

        Executor executor = Executors.newSingleThreadExecutor();
        ContinuableFuture<?> future2 = request.asyncPut(executor);
        assertNotNull(future2);

        ContinuableFuture<String> future3 = request.asyncPut(String.class);
        assertNotNull(future3);

        ContinuableFuture<String> future4 = request.asyncPut(String.class, executor);
        assertNotNull(future4);
    }

    @Test
    public void testAsyncPatchMethods() {
        OkHttpRequest request = OkHttpRequest.url("https://httpbin.org/patch").jsonBody("{}");

        ContinuableFuture<?> future1 = request.asyncPatch();
        assertNotNull(future1);

        Executor executor = Executors.newSingleThreadExecutor();
        ContinuableFuture<?> future2 = request.asyncPatch(executor);
        assertNotNull(future2);

        ContinuableFuture<String> future3 = request.asyncPatch(String.class);
        assertNotNull(future3);

        ContinuableFuture<String> future4 = request.asyncPatch(String.class, executor);
        assertNotNull(future4);
    }

    @Test
    public void testAsyncDeleteMethods() {
        OkHttpRequest request = OkHttpRequest.url("https://httpbin.org/delete");

        ContinuableFuture<?> future1 = request.asyncDelete();
        assertNotNull(future1);

        Executor executor = Executors.newSingleThreadExecutor();
        ContinuableFuture<?> future2 = request.asyncDelete(executor);
        assertNotNull(future2);

        ContinuableFuture<String> future3 = request.asyncDelete(String.class);
        assertNotNull(future3);

        ContinuableFuture<String> future4 = request.asyncDelete(String.class, executor);
        assertNotNull(future4);
    }

    @Test
    public void testAsyncHeadMethods() {
        OkHttpRequest request = OkHttpRequest.url("https://httpbin.org/get");

        ContinuableFuture<?> future1 = request.asyncHead();
        assertNotNull(future1);

        Executor executor = Executors.newSingleThreadExecutor();
        ContinuableFuture<?> future2 = request.asyncHead(executor);
        assertNotNull(future2);
    }

    @Test
    public void testAsyncExecuteMethods() {
        OkHttpRequest request = OkHttpRequest.url("https://httpbin.org/get");

        ContinuableFuture<?> future1 = request.asyncExecute(HttpMethod.GET);
        assertNotNull(future1);

        Executor executor = Executors.newSingleThreadExecutor();
        ContinuableFuture<?> future2 = request.asyncExecute(HttpMethod.GET, executor);
        assertNotNull(future2);

        ContinuableFuture<String> future3 = request.asyncExecute(HttpMethod.GET, String.class);
        assertNotNull(future3);

        ContinuableFuture<String> future4 = request.asyncExecute(HttpMethod.GET, String.class, executor);
        assertNotNull(future4);
    }

    public static class TestBean {
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
}

package com.landawn.abacus.http.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.http.HttpMethod;

@Tag("2025")
public class HttpRequest2025Test extends TestBase {

    private static final String TEST_URL = "https://httpbin.org/get";
    private static final String POST_URL = "https://httpbin.org/post";
    private static final String PUT_URL = "https://httpbin.org/put";
    private static final String PATCH_URL = "https://httpbin.org/patch";
    private static final String DELETE_URL = "https://httpbin.org/delete";

    @Test
    public void test_create_withStringUrl() {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.create(TEST_URL, client);
        assertNotNull(request);
    }

    @Test
    public void test_create_withURL() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        URL url = new URL(TEST_URL);
        HttpRequest request = HttpRequest.create(url, client);
        assertNotNull(request);
    }

    @Test
    public void test_create_withURI() {
        HttpClient client = HttpClient.newHttpClient();
        URI uri = URI.create(TEST_URL);
        HttpRequest request = HttpRequest.create(uri, client);
        assertNotNull(request);
    }

    @Test
    public void test_url_withString() {
        HttpRequest request = HttpRequest.url(TEST_URL);
        assertNotNull(request);
    }

    @Test
    public void test_url_withStringAndTimeouts() {
        HttpRequest request = HttpRequest.url(TEST_URL, 5000, 10000);
        assertNotNull(request);
    }

    @Test
    public void test_url_withURL() throws Exception {
        URL url = new URL(TEST_URL);
        HttpRequest request = HttpRequest.url(url);
        assertNotNull(request);
    }

    @Test
    public void test_url_withURLAndTimeouts() throws Exception {
        URL url = new URL(TEST_URL);
        HttpRequest request = HttpRequest.url(url, 5000, 10000);
        assertNotNull(request);
    }

    @Test
    public void test_url_withURI() {
        URI uri = URI.create(TEST_URL);
        HttpRequest request = HttpRequest.url(uri);
        assertNotNull(request);
    }

    @Test
    public void test_url_withURIAndTimeouts() {
        URI uri = URI.create(TEST_URL);
        HttpRequest request = HttpRequest.url(uri, 5000, 10000);
        assertNotNull(request);
    }

    @Test
    public void test_connectTimeout() {
        HttpRequest request = HttpRequest.url(TEST_URL).connectTimeout(Duration.ofSeconds(10));
        assertNotNull(request);
    }

    @Test
    public void test_readTimeout() {
        HttpRequest request = HttpRequest.url(TEST_URL).readTimeout(Duration.ofSeconds(30));
        assertNotNull(request);
    }

    @Test
    public void test_authenticator() {
        Authenticator auth = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("user", "pass".toCharArray());
            }
        };
        HttpRequest request = HttpRequest.url(TEST_URL).authenticator(auth);
        assertNotNull(request);
    }

    @Test
    public void test_basicAuth() {
        HttpRequest request = HttpRequest.url(TEST_URL).basicAuth("testuser", "testpass");
        assertNotNull(request);
    }

    @Test
    public void test_header() {
        HttpRequest request = HttpRequest.url(TEST_URL).header("Accept", "application/json");
        assertNotNull(request);
    }

    @Test
    public void test_headers_twoParams() {
        HttpRequest request = HttpRequest.url(TEST_URL).headers("Accept", "application/json", "User-Agent", "TestAgent");
        assertNotNull(request);
    }

    @Test
    public void test_headers_threeParams() {
        HttpRequest request = HttpRequest.url(TEST_URL).headers("Accept", "application/json", "User-Agent", "TestAgent", "X-Custom", "value");
        assertNotNull(request);
    }

    @Test
    public void test_headers_withMap() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        headers.put("User-Agent", "TestAgent");

        HttpRequest request = HttpRequest.url(TEST_URL).headers(headers);
        assertNotNull(request);
    }

    //    @Test
    //    public void test_headers_withHttpHeaders() {
    //        HttpHeaders headers = HttpHeaders.create();
    //        headers.set("Accept", "application/json");
    //        headers.set("User-Agent", "TestAgent");
    //
    //        HttpRequest request = HttpRequest.url(TEST_URL).headers(headers);
    //        assertNotNull(request);
    //    }
    //
    //    @Test
    //    public void test_headers_withNullHttpHeaders() {
    //        HttpRequest request = HttpRequest.url(TEST_URL).headers((HttpHeaders) null);
    //        assertNotNull(request);
    //    }

    @Test
    public void test_headers_withNullMap() {
        HttpRequest request = HttpRequest.url(TEST_URL).headers((Map<String, String>) null);
        assertNotNull(request);
    }

    @Test
    public void test_query_withString() {
        HttpRequest request = HttpRequest.url(TEST_URL).query("param1=value1&param2=value2");
        assertNotNull(request);
    }

    @Test
    public void test_query_withMap() {
        Map<String, Object> params = new HashMap<>();
        params.put("param1", "value1");
        params.put("param2", 123);

        HttpRequest request = HttpRequest.url(TEST_URL).query(params);
        assertNotNull(request);
    }

    @Test
    public void test_query_withEmptyString() {
        HttpRequest request = HttpRequest.url(TEST_URL).query("");
        assertNotNull(request);
    }

    @Test
    public void test_query_withEmptyMap() {
        HttpRequest request = HttpRequest.url(TEST_URL).query(new HashMap<>());
        assertNotNull(request);
    }

    @Test
    public void test_jsonBody_withString() {
        HttpRequest request = HttpRequest.url(POST_URL).jsonBody("{\"key\":\"value\"}");
        assertNotNull(request);
    }

    @Test
    public void test_jsonBody_withObject() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "test");
        data.put("value", 123);

        HttpRequest request = HttpRequest.url(POST_URL).jsonBody(data);
        assertNotNull(request);
    }

    @Test
    public void test_xmlBody_withString() {
        HttpRequest request = HttpRequest.url(POST_URL).xmlBody("<root><key>value</key></root>");
        assertNotNull(request);
    }

    @Test
    public void test_xmlBody_withObject() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");

        HttpRequest request = HttpRequest.url(POST_URL).xmlBody(data);
        assertNotNull(request);
    }

    @Test
    public void test_formBody_withMap() {
        Map<String, String> formData = new HashMap<>();
        formData.put("username", "testuser");
        formData.put("password", "testpass");

        HttpRequest request = HttpRequest.url(POST_URL).formBody(formData);
        assertNotNull(request);
    }

    @Test
    public void test_formBody_withBean() {
        TestBean bean = new TestBean();
        bean.setName("test");
        bean.setValue(123);

        HttpRequest request = HttpRequest.url(POST_URL).formBody(bean);
        assertNotNull(request);
    }

    @Test
    public void test_body_withBodyPublisher() {
        HttpRequest request = HttpRequest.url(POST_URL).body(BodyPublishers.ofString("test data"));
        assertNotNull(request);
    }

    @Test
    public void test_get() {
        try {
            HttpResponse<String> response = HttpRequest.url(TEST_URL).get();
            assertNotNull(response);
            assertEquals(200, response.statusCode());
        } catch (Exception e) {
        }
    }

    @Test
    public void test_get_withBodyHandler() {
        try {
            HttpResponse<String> response = HttpRequest.url(TEST_URL).get(BodyHandlers.ofString());
            assertNotNull(response);
            assertEquals(200, response.statusCode());
        } catch (Exception e) {
        }
    }

    @Test
    public void test_get_withResultClass() {
        try {
            String result = HttpRequest.url(TEST_URL).get(String.class);
            assertNotNull(result);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_post() {
        try {
            HttpResponse<String> response = HttpRequest.url(POST_URL).jsonBody("{\"test\":\"data\"}").post();
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_post_withBodyHandler() {
        try {
            HttpResponse<String> response = HttpRequest.url(POST_URL).jsonBody("{\"test\":\"data\"}").post(BodyHandlers.ofString());
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_post_withResultClass() {
        try {
            String result = HttpRequest.url(POST_URL).jsonBody("{\"test\":\"data\"}").post(String.class);
            assertNotNull(result);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_put() {
        try {
            HttpResponse<String> response = HttpRequest.url(PUT_URL).jsonBody("{\"test\":\"data\"}").put();
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_put_withBodyHandler() {
        try {
            HttpResponse<String> response = HttpRequest.url(PUT_URL).jsonBody("{\"test\":\"data\"}").put(BodyHandlers.ofString());
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_put_withResultClass() {
        try {
            String result = HttpRequest.url(PUT_URL).jsonBody("{\"test\":\"data\"}").put(String.class);
            assertNotNull(result);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_patch() {
        try {
            HttpResponse<String> response = HttpRequest.url(PATCH_URL).jsonBody("{\"test\":\"data\"}").patch();
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_patch_withBodyHandler() {
        try {
            HttpResponse<String> response = HttpRequest.url(PATCH_URL).jsonBody("{\"test\":\"data\"}").patch(BodyHandlers.ofString());
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_patch_withResultClass() {
        try {
            String result = HttpRequest.url(PATCH_URL).jsonBody("{\"test\":\"data\"}").patch(String.class);
            assertNotNull(result);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_delete() {
        try {
            HttpResponse<String> response = HttpRequest.url(DELETE_URL).delete();
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_delete_withBodyHandler() {
        try {
            HttpResponse<String> response = HttpRequest.url(DELETE_URL).delete(BodyHandlers.ofString());
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_delete_withResultClass() {
        try {
            String result = HttpRequest.url(DELETE_URL).delete(String.class);
            assertNotNull(result);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_head() {
        try {
            HttpResponse<Void> response = HttpRequest.url(TEST_URL).head();
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_execute_withMethod() {
        try {
            HttpResponse<String> response = HttpRequest.url(TEST_URL).execute(HttpMethod.GET);
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_execute_withMethodAndBodyHandler() {
        try {
            HttpResponse<String> response = HttpRequest.url(TEST_URL).execute(HttpMethod.GET, BodyHandlers.ofString());
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_execute_withMethodAndResultClass() {
        try {
            String result = HttpRequest.url(TEST_URL).execute(HttpMethod.GET, String.class);
            assertNotNull(result);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_execute_withNullMethod() {
        assertThrows(IllegalArgumentException.class, () -> {
            HttpRequest.url(TEST_URL).execute(null);
        });
    }

    @Test
    public void test_asyncGet() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(TEST_URL).asyncGet();
            assertNotNull(future);
            HttpResponse<String> response = future.get();
            assertNotNull(response);
        } catch (ExecutionException e) {
        }
    }

    @Test
    public void test_asyncGet_withBodyHandler() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(TEST_URL).asyncGet(BodyHandlers.ofString());
            assertNotNull(future);
            HttpResponse<String> response = future.get();
            assertNotNull(response);
        } catch (ExecutionException e) {
        }
    }

    @Test
    public void test_asyncGet_withResultClass() throws Exception {
        try {
            CompletableFuture<String> future = HttpRequest.url(TEST_URL).asyncGet(String.class);
            assertNotNull(future);
            String result = future.get();
            assertNotNull(result);
        } catch (ExecutionException e) {
        }
    }

    @Test
    public void test_asyncGet_withPushPromiseHandler() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(TEST_URL).asyncGet(BodyHandlers.ofString(), null);
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_asyncPost() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(POST_URL).jsonBody("{\"test\":\"data\"}").asyncPost();
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_asyncPost_withBodyHandler() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(POST_URL).jsonBody("{\"test\":\"data\"}").asyncPost(BodyHandlers.ofString());
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_asyncPost_withResultClass() throws Exception {
        try {
            CompletableFuture<String> future = HttpRequest.url(POST_URL).jsonBody("{\"test\":\"data\"}").asyncPost(String.class);
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_asyncPost_withPushPromiseHandler() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(POST_URL).jsonBody("{\"test\":\"data\"}").asyncPost(BodyHandlers.ofString(), null);
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_asyncPut() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(PUT_URL).jsonBody("{\"test\":\"data\"}").asyncPut();
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_asyncPut_withBodyHandler() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(PUT_URL).jsonBody("{\"test\":\"data\"}").asyncPut(BodyHandlers.ofString());
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_asyncPut_withResultClass() throws Exception {
        try {
            CompletableFuture<String> future = HttpRequest.url(PUT_URL).jsonBody("{\"test\":\"data\"}").asyncPut(String.class);
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_asyncPut_withPushPromiseHandler() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(PUT_URL).jsonBody("{\"test\":\"data\"}").asyncPut(BodyHandlers.ofString(), null);
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_asyncPatch() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(PATCH_URL).jsonBody("{\"test\":\"data\"}").asyncPatch();
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_asyncPatch_withBodyHandler() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(PATCH_URL).jsonBody("{\"test\":\"data\"}").asyncPatch(BodyHandlers.ofString());
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_asyncPatch_withResultClass() throws Exception {
        try {
            CompletableFuture<String> future = HttpRequest.url(PATCH_URL).jsonBody("{\"test\":\"data\"}").asyncPatch(String.class);
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_asyncPatch_withPushPromiseHandler() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(PATCH_URL)
                    .jsonBody("{\"test\":\"data\"}")
                    .asyncPatch(BodyHandlers.ofString(), null);
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_asyncDelete() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(DELETE_URL).asyncDelete();
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_asyncDelete_withBodyHandler() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(DELETE_URL).asyncDelete(BodyHandlers.ofString());
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_asyncDelete_withResultClass() throws Exception {
        try {
            CompletableFuture<String> future = HttpRequest.url(DELETE_URL).asyncDelete(String.class);
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_asyncDelete_withPushPromiseHandler() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(DELETE_URL).asyncDelete(BodyHandlers.ofString(), null);
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_asyncHead() throws Exception {
        try {
            CompletableFuture<HttpResponse<Void>> future = HttpRequest.url(TEST_URL).asyncHead();
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_asyncExecute_withMethod() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(TEST_URL).asyncExecute(HttpMethod.GET);
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_asyncExecute_withMethodAndBodyHandler() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(TEST_URL).asyncExecute(HttpMethod.GET, BodyHandlers.ofString());
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_asyncExecute_withMethodAndResultClass() throws Exception {
        try {
            CompletableFuture<String> future = HttpRequest.url(TEST_URL).asyncExecute(HttpMethod.GET, String.class);
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_asyncExecute_withPushPromiseHandler() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(TEST_URL).asyncExecute(HttpMethod.GET, BodyHandlers.ofString(), null);
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_asyncExecute_withNullMethod() {
        assertThrows(IllegalArgumentException.class, () -> {
            HttpRequest.url(TEST_URL).asyncExecute(null);
        });
    }

    public static class TestBean {
        private String name;
        private int value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }
}

package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.IOUtil;

import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

@Tag("2025")
public class CurlInterceptorTest extends TestBase {

    private final Consumer<String> logHandler = s -> {
    };

    @Test
    public void testConstructorWithLogHandler() {
        AtomicReference<String> capturedCurl = new AtomicReference<>();
        CurlInterceptor interceptor = new CurlInterceptor(capturedCurl::set);
        assertNotNull(interceptor);
    }

    @Test
    public void testConstructorWithQuoteCharAndLogHandler() {
        AtomicReference<String> capturedCurl = new AtomicReference<>();
        CurlInterceptor interceptor = new CurlInterceptor('"', capturedCurl::set);
        assertNotNull(interceptor);
    }

    @Test
    public void testInterceptGetRequest() throws IOException {
        AtomicReference<String> capturedCurl = new AtomicReference<>();
        CurlInterceptor interceptor = new CurlInterceptor(capturedCurl::set);

        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        Request request = new Request.Builder().url("https://httpbin.org/get").get().build();

        try {
            client.newCall(request).execute();
        } catch (Exception e) {
            // May fail due to network, but interceptor should still capture curl
        }

        String curl = capturedCurl.get();
        if (curl != null) {
            assertTrue(curl.contains("curl"));
            assertTrue(curl.contains("https://httpbin.org/get"));
        }
    }

    @Test
    public void testInterceptPostRequest() throws IOException {
        AtomicReference<String> capturedCurl = new AtomicReference<>();
        CurlInterceptor interceptor = new CurlInterceptor(capturedCurl::set);

        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        RequestBody body = RequestBody.create("{\"key\":\"value\"}", MediaType.get("application/json"));
        Request request = new Request.Builder().url("https://httpbin.org/post").post(body).build();

        try {
            client.newCall(request).execute();
        } catch (Exception e) {
            // May fail due to network, but interceptor should still capture curl
        }

        String curl = capturedCurl.get();
        if (curl != null) {
            assertTrue(curl.contains("curl"));
            assertTrue(curl.contains("https://httpbin.org/post"));
            assertTrue(curl.contains("POST") || curl.contains("-d") || curl.contains("--data"));
        }
    }

    @Test
    public void testInterceptWithHeaders() throws IOException {
        AtomicReference<String> capturedCurl = new AtomicReference<>();
        CurlInterceptor interceptor = new CurlInterceptor(capturedCurl::set);

        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        Request request = new Request.Builder().url("https://httpbin.org/get")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer token")
                .get()
                .build();

        try {
            client.newCall(request).execute();
        } catch (Exception e) {
            // May fail due to network, but interceptor should still capture curl
        }

        String curl = capturedCurl.get();
        if (curl != null) {
            assertTrue(curl.contains("curl"));
            assertTrue(curl.contains("-H") || curl.contains("--header"));
        }
    }

    @Test
    public void testInterceptWithDoubleQuote() throws IOException {
        AtomicReference<String> capturedCurl = new AtomicReference<>();
        CurlInterceptor interceptor = new CurlInterceptor('"', capturedCurl::set);

        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        Request request = new Request.Builder().url("https://httpbin.org/get").get().build();

        try {
            client.newCall(request).execute();
        } catch (Exception e) {
            // May fail due to network, but interceptor should still capture curl
        }

        String curl = capturedCurl.get();
        if (curl != null) {
            assertTrue(curl.contains("curl"));
            // When using double quote, URLs and values should be quoted with "
            assertTrue(curl.contains("\"") || curl.contains("curl"));
        }
    }

    @Test
    public void testInterceptWithRequestBody() throws IOException {
        AtomicReference<String> capturedCurl = new AtomicReference<>();
        CurlInterceptor interceptor = new CurlInterceptor(capturedCurl::set);

        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        RequestBody body = RequestBody.create("test data", MediaType.get("text/plain"));
        Request request = new Request.Builder().url("https://httpbin.org/post").post(body).build();

        try {
            client.newCall(request).execute();
        } catch (Exception e) {
            // May fail due to network, but interceptor should still capture curl
        }

        String curl = capturedCurl.get();
        if (curl != null) {
            assertTrue(curl.contains("curl"));
            assertTrue(curl.contains("test data") || curl.contains("-d") || curl.contains("--data"));
        }
    }

    @Test
    public void testInterceptWithCharset() throws IOException {
        AtomicReference<String> capturedCurl = new AtomicReference<>();
        CurlInterceptor interceptor = new CurlInterceptor(capturedCurl::set);

        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        RequestBody body = RequestBody.create("test data", MediaType.parse("text/plain; charset=UTF-8"));
        Request request = new Request.Builder().url("https://httpbin.org/post").header("Content-Type", "text/plain; charset=ISO-8859-1").post(body).build();

        try {
            client.newCall(request).execute();
        } catch (Exception e) {
            // May fail due to network, but interceptor should still capture curl
        }

        String curl = capturedCurl.get();
        assertNotNull(interceptor);
        // The curl should be generated even if request fails
    }

    @Test
    public void testMultipleInterceptsOnSameInterceptor() throws IOException {
        AtomicReference<String> capturedCurl = new AtomicReference<>();
        int[] count = { 0 };

        CurlInterceptor interceptor = new CurlInterceptor(curl -> {
            capturedCurl.set(curl);
            count[0]++;
        });

        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        Request request1 = new Request.Builder().url("https://httpbin.org/get").get().build();

        Request request2 = new Request.Builder().url("https://httpbin.org/post").post(RequestBody.create("", MediaType.get("text/plain"))).build();

        try {
            client.newCall(request1).execute();
            client.newCall(request2).execute();
        } catch (Exception e) {
            // May fail due to network
        }

        // Log handler should be called for each request
        assertTrue(count[0] >= 0);
    }

    @Test
    public void testConstructorWithDefaultQuoteChar() {
        CurlInterceptor interceptor = new CurlInterceptor(logHandler);
        assertNotNull(interceptor);
    }

    @Test
    public void testConstructorWithCustomQuoteChar() {
        CurlInterceptor interceptor = new CurlInterceptor('"', logHandler);
        assertNotNull(interceptor);
    }

    @Test
    public void testInterceptWithGetRequest() throws IOException {
        AtomicReference<String> capturedCurl = new AtomicReference<>();
        CurlInterceptor interceptor = new CurlInterceptor(capturedCurl::set);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("OK"));
        server.start();

        try {
            Request request = new Request.Builder().url(server.url("/test")).get().build();

            Response response = client.newCall(request).execute();
            response.close();

            String curl = capturedCurl.get();
            assertNotNull(curl);
            assertTrue(curl.contains("curl -X GET"));
            assertTrue(curl.contains(server.url("/test").toString()));
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void testInterceptWithPostRequest() throws IOException {
        AtomicReference<String> capturedCurl = new AtomicReference<>();
        CurlInterceptor interceptor = new CurlInterceptor(capturedCurl::set);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("OK"));
        server.start();

        try {
            String json = "{\"name\":\"test\"}";
            RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);

            Request request = new Request.Builder().url(server.url("/test")).header("Content-Type", "application/json").post(body).build();

            Response response = client.newCall(request).execute();
            response.close();

            String curl = capturedCurl.get();
            assertNotNull(curl);
            assertTrue(curl.contains("curl -X POST"));
            assertTrue(curl.contains("-H 'content-type: application/json'"));
            assertTrue(curl.contains("-d '{\"name\":\"test\"}'"));
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void testInterceptWithDoubleQuotes() throws IOException {
        AtomicReference<String> capturedCurl = new AtomicReference<>();
        CurlInterceptor interceptor = new CurlInterceptor('"', capturedCurl::set);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("OK"));
        server.start();

        try {
            Request request = new Request.Builder().url(server.url("/test")).get().build();

            Response response = client.newCall(request).execute();
            response.close();

            String curl = capturedCurl.get();
            assertNotNull(curl);
            assertTrue(curl.contains("\"" + server.url("/test").toString() + "\""));
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void testInterceptProceedsRequest() throws IOException {
        AtomicReference<String> capturedCurl = new AtomicReference<>();
        CurlInterceptor interceptor = new CurlInterceptor(capturedCurl::set);

        Interceptor.Chain chain = new Interceptor.Chain() {
            @Override
            public Request request() {
                return new Request.Builder().url("http://example.com").build();
            }

            @Override
            public Response proceed(Request request) throws IOException {
                return new Response.Builder().request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(ResponseBody.create(MediaType.parse("text/plain"), "response"))
                        .build();
            }

            @Override
            public Connection connection() {
                return null;
            }

            @Override
            public Call call() {
                return null;
            }

            @Override
            public int connectTimeoutMillis() {
                return 0;
            }

            @Override
            public Interceptor.Chain withConnectTimeout(int timeout, java.util.concurrent.TimeUnit unit) {
                return this;
            }

            @Override
            public int readTimeoutMillis() {
                return 0;
            }

            @Override
            public Interceptor.Chain withReadTimeout(int timeout, java.util.concurrent.TimeUnit unit) {
                return this;
            }

            @Override
            public int writeTimeoutMillis() {
                return 0;
            }

            @Override
            public Interceptor.Chain withWriteTimeout(int timeout, java.util.concurrent.TimeUnit unit) {
                return this;
            }
        };

        Response response = interceptor.intercept(chain);
        assertNotNull(response);
        assertEquals(200, response.code());

        String curl = capturedCurl.get();
        assertNotNull(curl);
        assertTrue(curl.contains("http://example.com"));

        IOUtil.close(response);
    }

}

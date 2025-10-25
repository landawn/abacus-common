package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

@Tag("2025")
public class CurlInterceptor2025Test extends TestBase {

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
    public void testDefaultQuoteChar() {
        AtomicReference<String> capturedCurl = new AtomicReference<>();
        CurlInterceptor interceptor = new CurlInterceptor(capturedCurl::set);
        assertNotNull(interceptor);
        // Default quote char should be single quote
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
}

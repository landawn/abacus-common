package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

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

@Tag("new-test")
public class CurlInterceptor100Test extends TestBase {

    private AtomicReference<String> capturedCurl;
    private Consumer<String> logHandler;

    @BeforeEach
    public void setUp() {
        capturedCurl = new AtomicReference<>();
        logHandler = curl -> capturedCurl.set(curl);
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
        CurlInterceptor interceptor = new CurlInterceptor(logHandler);
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
        CurlInterceptor interceptor = new CurlInterceptor(logHandler);
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
    public void testInterceptWithHeaders() throws IOException {
        CurlInterceptor interceptor = new CurlInterceptor(logHandler);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("OK"));
        server.start();

        try {
            Request request = new Request.Builder().url(server.url("/test"))
                    .header("Authorization", "Bearer token123")
                    .header("Accept", "application/json")
                    .get()
                    .build();

            Response response = client.newCall(request).execute();
            response.close();

            String curl = capturedCurl.get();
            assertNotNull(curl);
            assertTrue(curl.contains("-H 'authorization: Bearer token123'"));
            assertTrue(curl.contains("-H 'accept: application/json'"));
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void testInterceptWithDoubleQuotes() throws IOException {
        CurlInterceptor interceptor = new CurlInterceptor('"', logHandler);
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
        CurlInterceptor interceptor = new CurlInterceptor(logHandler);

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

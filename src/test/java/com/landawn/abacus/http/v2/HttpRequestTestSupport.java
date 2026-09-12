package com.landawn.abacus.http.v2;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.http.HttpMethod;
import com.sun.net.httpserver.HttpServer;

public abstract class HttpRequestTestSupport extends TestBase {

    protected static final HttpServer SHARED_TEST_SERVER = startSharedTestServer();
    protected static final String SHARED_TEST_SERVER_URL = localUrl(SHARED_TEST_SERVER);
    protected static final String TEST_URL = SHARED_TEST_SERVER_URL + "get";
    protected static final String POST_URL = SHARED_TEST_SERVER_URL + "post";
    protected static final String PUT_URL = SHARED_TEST_SERVER_URL + "put";
    protected static final String PATCH_URL = SHARED_TEST_SERVER_URL + "patch";
    protected static final String DELETE_URL = SHARED_TEST_SERVER_URL + "delete";

    protected final String testUrl = TEST_URL;
    protected final URI testUri = URI.create(testUrl);
    protected final HttpClient mockHttpClient = HttpClient.newHttpClient();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> SHARED_TEST_SERVER.stop(0), "http-v2-shared-test-server-stop"));
    }

    public static class TestBean {
        protected String name;
        protected int value;

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

    protected static HttpRequest newOwnedRequest(final HttpClient ownedClient, final java.net.http.HttpRequest.Builder requestBuilder) {
        final HttpClient.Builder clientBuilder = mock(HttpClient.Builder.class);
        when(clientBuilder.build()).thenReturn(ownedClient);

        return new HttpRequest(TEST_URL, null, null, clientBuilder, requestBuilder).closeHttpClientAfterExecution(true);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected static HttpRequest newOwnedAsyncInputStreamRequest(final HttpClient ownedClient, final InputStream delegate) {
        final HttpResponse<InputStream> response = mock(HttpResponse.class);
        when(response.body()).thenReturn(delegate);
        when(ownedClient.sendAsync(any(java.net.http.HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn((CompletableFuture) CompletableFuture.completedFuture(response));

        return newOwnedRequest(ownedClient, java.net.http.HttpRequest.newBuilder());
    }

    protected static HttpServer startErrorServer(final int statusCode, final String body) throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

        server.createContext("/", exchange -> {
            final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, bytes.length);

            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(bytes);
            }
        });

        server.start();
        return server;
    }

    protected static HttpServer startSharedTestServer() {
        try {
            final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

            server.createContext("/", exchange -> {
                final String requestMethod = exchange.getRequestMethod();
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                exchange.getResponseHeaders().set("X-Request-Method", requestMethod);

                try (InputStream requestBody = exchange.getRequestBody()) {
                    requestBody.transferTo(OutputStream.nullOutputStream());
                }

                if (HttpMethod.HEAD.name().equals(requestMethod)) {
                    exchange.sendResponseHeaders(200, -1);
                    exchange.close();
                    return;
                }

                final byte[] responseBody = (requestMethod + " response").getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, responseBody.length);

                try (OutputStream outputStream = exchange.getResponseBody()) {
                    outputStream.write(responseBody);
                }
            });

            server.start();
            return server;
        } catch (final IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    protected static HttpServer startLocalServer(final String body) throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

        server.createContext("/", exchange -> {
            final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);

            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(bytes);
            }
        });

        server.start();
        return server;
    }

    protected static HttpServer startResponseServer(final byte[] body, final String contentType, final String contentEncoding) throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

        server.createContext("/", exchange -> {
            if (contentType != null) {
                exchange.getResponseHeaders().set("Content-Type", contentType);
            }

            if (contentEncoding != null) {
                exchange.getResponseHeaders().set("Content-Encoding", contentEncoding);
            }

            exchange.sendResponseHeaders(200, body.length);

            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });

        server.start();
        return server;
    }

    protected static HttpServer startRedirectServer(final String body) throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().add("Location", "/target");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });

        server.createContext("/target", exchange -> {
            final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);

            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(bytes);
            }
        });

        server.start();
        return server;
    }

    protected static String localUrl(final HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/";
    }

    protected static boolean booleanField(final HttpRequest request, final String fieldName) throws Exception {
        return (boolean) field(request, fieldName);
    }

    protected static Object field(final HttpRequest request, final String fieldName) throws Exception {
        final java.lang.reflect.Field field = HttpRequest.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(request);
    }

    protected static byte[] gzip(final String text) throws IOException {
        final ByteArrayOutputStream compressed = new ByteArrayOutputStream();

        try (GZIPOutputStream gzip = new GZIPOutputStream(compressed)) {
            gzip.write(text.getBytes(StandardCharsets.UTF_8));
        }

        return compressed.toByteArray();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected static HttpRequest newOwnedSyncRequest(final HttpClient ownedClient, final InputStream body, final String contentEncoding) throws Exception {
        final HttpResponse<InputStream> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(body);
        when(response.headers()).thenReturn(java.net.http.HttpHeaders.of(
                Map.of("Content-Type", java.util.List.of("text/plain; charset=UTF-8"), "Content-Encoding", java.util.List.of(contentEncoding)),
                (a, b) -> true));
        when(ownedClient.send(any(java.net.http.HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        when(ownedClient.sendAsync(any(java.net.http.HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn((CompletableFuture) CompletableFuture.completedFuture(response));

        return newOwnedRequest(ownedClient, java.net.http.HttpRequest.newBuilder());
    }

    protected static InputStream countingStream(final byte[] bytes, final AtomicInteger closeCount) {
        return new java.io.ByteArrayInputStream(bytes) {
            @Override
            public void close() throws IOException {
                closeCount.incrementAndGet();
                super.close();
            }
        };
    }

    protected static HttpServer startRedirectServer(final int statusCode, final String body) throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().add("Location", "/target");
            exchange.sendResponseHeaders(statusCode, -1);
            exchange.close();
        });

        server.createContext("/target", exchange -> {
            final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);

            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(bytes);
            }
        });

        server.start();
        return server;
    }
}

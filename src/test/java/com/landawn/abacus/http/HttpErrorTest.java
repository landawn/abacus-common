package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.HttpResponseException;
import com.landawn.abacus.exception.UncheckedIOException;

public class HttpErrorTest extends TestBase {
    @Test
    public void testMissingErrorStreamPreservesStatusAndHeaders() throws Exception {
        final Fixture fixture = fixture(404, null, false);
        final HttpResponseException error = assertThrows(HttpResponseException.class, () -> fixture.client.get(String.class));
        assertEquals(404, error.statusCode());
        assertEquals("missing", error.header("X-Detail"));
        assertEquals("", error.responseBody());
        assertEquals(0, fixture.active.get());
    }

    @Test
    public void testMalformedEmptyAndTruncatedGzipPreserveStatusAndCloseRawStream() throws Exception {
        final byte[] compressed = gzip("error".getBytes(StandardCharsets.UTF_8));
        for (final byte[] bytes : new byte[][] { {}, { 1, 2, 3 }, Arrays.copyOf(compressed, compressed.length - 4) }) {
            final TrackingInput input = new TrackingInput(bytes);
            final Fixture fixture = fixture(500, input, true);
            final HttpResponseException error = assertThrows(HttpResponseException.class, () -> fixture.client.get(String.class));
            assertEquals(500, error.statusCode());
            assertEquals("", error.responseBody());
            assertTrue(input.closed);
            assertEquals(0, fixture.active.get());
        }
    }

    @Test
    public void testBoundedUnicodeErrorPrefixWithAndWithoutCompression() throws Exception {
        final byte[] bytes = ("x".repeat(HttpUtil.MAX_ERROR_BODY_SIZE - 1) + "\u4e2d\ud83d\ude00".repeat(5000)).getBytes(StandardCharsets.UTF_8);
        for (final boolean compressed : new boolean[] { false, true }) {
            final TrackingInput input = new TrackingInput(compressed ? gzip(bytes) : bytes);
            final Fixture fixture = fixture(422, input, compressed);
            final HttpResponseException error = assertThrows(HttpResponseException.class, () -> fixture.client.get(byte[].class));
            assertEquals(new String(Arrays.copyOf(bytes, HttpUtil.MAX_ERROR_BODY_SIZE), StandardCharsets.UTF_8), error.responseBody());
            if (!compressed) {
                assertEquals(HttpUtil.MAX_ERROR_BODY_SIZE, input.consumed());
            }
            assertTrue(input.closed);
        }
    }

    @Test
    public void testReadFailurePreservesStatusAndClosesStream() throws Exception {
        final AtomicBoolean closed = new AtomicBoolean();
        final InputStream input = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("body read failed");
            }

            @Override
            public void close() {
                closed.set(true);
            }
        };
        final Fixture fixture = fixture(503, input, false);
        final HttpResponseException error = assertThrows(HttpResponseException.class, () -> fixture.client.get(String.class));
        assertEquals(503, error.statusCode());
        assertEquals("", error.responseBody());
        assertTrue(closed.get());
    }

    @Test
    public void testHeadErrorDoesNotAcquireOrDecompressBody() throws Exception {
        final Fixture fixture = fixture(404, null, true);
        final HttpResponseException error = assertThrows(HttpResponseException.class, () -> fixture.client.execute(HttpMethod.HEAD, null, String.class));
        assertEquals(404, error.statusCode());
        assertEquals("", error.responseBody());
        verify(fixture.connection, never()).getErrorStream();
        verify(fixture.connection, never()).getInputStream();
    }

    @Test
    public void testMalformedErrorDoesNotTruncateOutputFile(@TempDir final Path directory) throws Exception {
        final Path file = directory.resolve("response.txt");
        Files.writeString(file, "keep this");
        final Fixture fixture = fixture(500, new TrackingInput(new byte[0]), true);
        assertThrows(HttpResponseException.class, () -> fixture.client.execute(HttpMethod.GET, null, null, file.toFile()));
        assertEquals("keep this", Files.readString(file));
    }

    @Test
    public void testSuccessfulDecodingRemainsStrictAndRawErrorInspectionStillWorks() throws Exception {
        final Fixture success = fixture(200, null, true);
        doReturn(new TrackingInput(new byte[] { 1, 2, 3 })).when(success.connection).getInputStream();
        assertThrows(UncheckedIOException.class, () -> success.client.get(String.class));
        final Fixture error = fixture(404, new TrackingInput("not found".getBytes(StandardCharsets.UTF_8)), false);
        final HttpResponse response = error.client.get(HttpResponse.class);
        assertEquals(404, response.statusCode());
        assertEquals("not found", response.body(String.class));
    }

    private record Fixture(HttpClient client, HttpURLConnection connection, AtomicInteger active) {
    }

    private static Fixture fixture(final int status, final InputStream errorStream, final boolean gzip) throws Exception {
        final HttpURLConnection connection = mock(HttpURLConnection.class);
        final URL url = new URL(null, "http://example.test/error", new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(final URL ignored) {
                return connection;
            }
        });
        when(connection.getURL()).thenReturn(url);
        when(connection.getResponseCode()).thenReturn(status);
        when(connection.getResponseMessage()).thenReturn("test status");
        when(connection.getHeaderFields()).thenReturn(Map.of("Content-Type", List.of("text/plain; charset=UTF-8"), "Content-Encoding",
                List.of(gzip ? "gzip" : "identity"), "X-Detail", List.of("missing")));
        when(connection.getInputStream()).thenThrow(new IOException("no response input"));
        when(connection.getErrorStream()).thenReturn(errorStream);
        final AtomicInteger active = new AtomicInteger();
        return new Fixture(HttpClient.create(url, 1, 1000, 1000, null, active), connection, active);
    }

    private static byte[] gzip(final byte[] bytes) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            gzip.write(bytes);
        }
        return output.toByteArray();
    }

    private static final class TrackingInput extends ByteArrayInputStream {
        private boolean closed;

        private TrackingInput(final byte[] bytes) {
            super(bytes);
        }

        private int consumed() {
            return pos;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    // ---- bug review 2026-09-27 G003 begin ----
    // G003-01: an error status sent without a body has no error stream; HttpResponse must still be returned
    @Test
    public void testGetHttpResponse_errorStatusWithoutBody() throws Exception {
        for (final boolean gzip : new boolean[] { false, true }) {
            final Fixture fixture = fixture(404, null, gzip);
            final HttpResponse response = fixture.client.get(HttpResponse.class);
            assertEquals(404, response.statusCode());
            assertEquals(0, response.body().length);
            assertEquals("missing", response.headers().get("X-Detail").get(0));
            assertEquals(0, fixture.active.get());
        }
    }

    // G003-01: same scenario against the real JDK HttpURLConnection (404 and 500+gzip with an empty body)
    @Test
    public void testGetHttpResponse_errorStatusWithoutBody_realServer() throws Exception {
        final com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/notFound", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.createContext("/serverError", exchange -> {
            exchange.getResponseHeaders().add("Content-Encoding", "gzip");
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();

        try {
            final String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();

            final HttpResponse notFound = HttpClient.create(baseUrl + "/notFound").get(HttpResponse.class);
            assertEquals(404, notFound.statusCode());
            assertEquals("", notFound.body(String.class));

            final HttpResponse serverError = HttpRequest.url(baseUrl + "/serverError").get();
            assertEquals(500, serverError.statusCode());
            assertEquals(0, serverError.body().length);

            // Non-HttpResponse results keep failing with the status exception.
            final HttpResponseException error = assertThrows(HttpResponseException.class, () -> HttpClient.create(baseUrl + "/notFound").get(String.class));
            assertEquals(404, error.statusCode());
        } finally {
            server.stop(0);
        }
    }
    // ---- bug review 2026-09-27 G003 end ----

    // ---- bug review 2026-09-27 verify G115 begin ----
    // Loopback server: /r?s=<status>&b=none|text&z=0|1 (b=none sends Content-Length 0; z=1 adds Content-Encoding: gzip).
    private static com.sun.net.httpserver.HttpServer startStatusServer() throws IOException {
        final com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/r", exchange -> {
            try {
                final Map<String, String> query = new java.util.HashMap<>();
                for (final String pair : exchange.getRequestURI().getQuery().split("&")) {
                    final String[] keyValue = pair.split("=");
                    query.put(keyValue[0], keyValue[1]);
                }
                exchange.getRequestBody().readAllBytes();
                exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
                exchange.getResponseHeaders().add("X-Detail", "missing");
                final boolean gzip = "1".equals(query.get("z"));
                if (gzip) {
                    exchange.getResponseHeaders().add("Content-Encoding", "gzip");
                }
                final int status = Integer.parseInt(query.get("s"));
                if ("none".equals(query.get("b")) || "HEAD".equals(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(status, -1);
                } else {
                    final byte[] plain = "error-detail".getBytes(StandardCharsets.UTF_8);
                    final byte[] bytes = gzip ? gzip(plain) : plain;
                    exchange.sendResponseHeaders(status, bytes.length);
                    exchange.getResponseBody().write(bytes);
                }
            } finally {
                exchange.close();
            }
        });
        server.start();
        return server;
    }

    // Body-less 4xx/5xx is returned as an HttpResponse for every body-capable method, with and without a gzip Content-Encoding.
    @Test
    public void testHttpResponseForBodylessErrorStatus_allMethods() throws Exception {
        final com.sun.net.httpserver.HttpServer server = startStatusServer();
        try {
            final String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/r";
            for (final HttpMethod method : new HttpMethod[] { HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.OPTIONS }) {
                for (final int status : new int[] { 400, 401, 404, 410, 500, 503 }) {
                    for (final String gzip : new String[] { "0", "1" }) {
                        final Object request = method == HttpMethod.POST || method == HttpMethod.PUT ? "payload" : null;
                        final HttpClient client = HttpClient.create(baseUrl + "?s=" + status + "&b=none&z=" + gzip);
                        final HttpResponse response = client.execute(method, request, HttpResponse.class);
                        final String label = method + " " + status + " z" + gzip;
                        assertEquals(status, response.statusCode(), label);
                        assertEquals(0, response.body().length, label);
                        assertTrue(response.headers()
                                .entrySet()
                                .stream()
                                .anyMatch(header -> "X-Detail".equalsIgnoreCase(header.getKey()) && header.getValue().contains("missing")), label);
                    }
                }
            }
        } finally {
            server.stop(0);
        }
    }

    // Body-less 4xx through the async and HttpRequest entry points; String/byte[]/output-stream results still raise HttpResponseException.
    @Test
    public void testHttpResponseForBodylessErrorStatus_asyncAndRequestAndOtherResults() throws Exception {
        final com.sun.net.httpserver.HttpServer server = startStatusServer();
        try {
            final String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/r?s=404&b=none&z=1";
            assertEquals(404, HttpClient.create(url).asyncGet(HttpResponse.class).get().statusCode());
            assertEquals(404, HttpRequest.url(url).delete().statusCode());
            assertEquals(404, HttpRequest.url(url).body("payload").post().statusCode());
            assertEquals(404, assertThrows(HttpResponseException.class, () -> HttpClient.create(url).get(byte[].class)).statusCode());
            assertEquals(404, assertThrows(HttpResponseException.class, () -> HttpClient.create(url).post("payload", String.class)).statusCode());
            final ByteArrayOutputStream output = new ByteArrayOutputStream();
            assertEquals(404,
                    assertThrows(HttpResponseException.class, () -> HttpClient.create(url).execute(HttpMethod.GET, null, null, output)).statusCode());
            assertEquals(0, output.size());
            // A one-way request never reads the body and keeps returning null.
            assertEquals(null, HttpClient.create(url).get(HttpSettings.create().setOneWayRequest(true), HttpResponse.class));
        } finally {
            server.stop(0);
        }
    }

    // Neighbours of the fix: error bodies (plain and gzip) are still delivered, HEAD and a body-less 3xx are unchanged.
    @Test
    public void testHttpResponseForErrorStatusWithBody_andNonErrorNeighbours() throws Exception {
        final com.sun.net.httpserver.HttpServer server = startStatusServer();
        try {
            final String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/r";
            for (final String gzip : new String[] { "0", "1" }) {
                for (final HttpMethod method : new HttpMethod[] { HttpMethod.GET, HttpMethod.POST }) {
                    final Object request = method == HttpMethod.POST ? "payload" : null;
                    final HttpResponse response = HttpClient.create(baseUrl + "?s=500&b=text&z=" + gzip).execute(method, request, HttpResponse.class);
                    assertEquals(500, response.statusCode());
                    assertEquals("error-detail", response.body(String.class));
                }
                final HttpResponseException error = assertThrows(HttpResponseException.class,
                        () -> HttpClient.create(baseUrl + "?s=404&b=text&z=" + gzip).get(String.class));
                assertEquals("error-detail", error.responseBody());
            }
            final HttpResponse head = HttpClient.create(baseUrl + "?s=404&b=none&z=1").execute(HttpMethod.HEAD, null, HttpResponse.class);
            assertEquals(404, head.statusCode());
            assertEquals(0, head.body().length);
            final HttpResponse redirect = HttpClient.create(baseUrl + "?s=302&b=none&z=0").get(HttpResponse.class);
            assertEquals(302, redirect.statusCode());
            assertEquals(0, redirect.body().length);
        } finally {
            server.stop(0);
        }
    }
    // ---- bug review 2026-09-27 verify G115 end ----
}

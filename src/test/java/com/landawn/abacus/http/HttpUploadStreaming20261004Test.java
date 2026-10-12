package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.sun.net.httpserver.HttpServer;

@Tag("unit")
class HttpUploadStreaming20261004Test extends TestBase {
    @TempDir
    Path directory;

    static final class RecordingConnection extends HttpURLConnection {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        RecordingConnection(URL url) {
            super(url);
        }

        @Override
        public void connect() {
        }

        @Override
        public void disconnect() {
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public OutputStream getOutputStream() {
            return bytes;
        }

        @Override
        public int getResponseCode() {
            return 204;
        }

        @Override
        public String getResponseMessage() {
            return "No Content";
        }

        @Override
        public Map<String, List<String>> getHeaderFields() {
            return Map.of();
        }

        @Override
        public InputStream getInputStream() {
            return InputStream.nullInputStream();
        }

        long fixedLength() {
            return fixedContentLengthLong >= 0 ? fixedContentLengthLong : fixedContentLength;
        }

        int chunkLength() {
            return chunkLength;
        }
    }

    static final class Fixture {
        RecordingConnection connection;
        final URL url;

        Fixture() throws Exception {
            url = new URL(null, "http://streaming.test/upload", new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(URL url) {
                    return connection = new RecordingConnection(url);
                }
            });
        }

        HttpClient client(HttpSettings defaults) {
            return HttpClient.create(url, 4, 5000, 5000, defaults);
        }
    }

    @Test
    void defaultsAndSettingsLayeringPreserveOptIn() throws Exception {
        Fixture fixture = new Fixture();
        byte[] body = new byte[1024];
        fixture.client(null).post(body, Void.class);
        assertEquals(-1, fixture.connection.fixedLength());
        assertEquals(-1, fixture.connection.chunkLength());
        HttpSettings defaults = HttpSettings.create().streamRequestBody(true);
        HttpClient client = fixture.client(defaults);
        defaults.streamRequestBody(false); // client owns a settings copy
        client.post(body, HttpSettings.create().header("X-Test", "1"), Void.class);
        assertEquals(body.length, fixture.connection.fixedLength());
        client.post(body, HttpSettings.create().streamRequestBody(false), Void.class);
        assertEquals(-1, fixture.connection.fixedLength());
        assertEquals(-1, fixture.connection.chunkLength());
        HttpSettings settings = HttpSettings.create().streamRequestBody(true);
        assertTrue(settings.copy().streamRequestBody());
        assertNull(HttpSettings.create().copy().streamRequestBodyOrNull());
        assertTrue(HttpRequest.url(fixture.url.toString()).settings(settings).checkSettings().streamRequestBody());
    }

    @Test
    void knownRawLengthsAndUnknownCompressedBodiesChooseCorrectModes() throws Exception {
        Fixture fixture = new Fixture();
        HttpClient client = fixture.client(HttpSettings.create().streamRequestBody(true));
        byte[] body = new byte[1024 * 1024];
        for (int i = 0; i < body.length; i++) {
            body[i] = (byte) i;
        }
        Path file = directory.resolve("upload.bin");
        Files.write(file, body);
        client.post(file.toFile(), Void.class);
        assertEquals(body.length, fixture.connection.fixedLength());
        assertEquals(-1, fixture.connection.chunkLength());
        assertArrayEquals(body, fixture.connection.bytes.toByteArray());
        client.post(new byte[0], Void.class);
        assertEquals(0, fixture.connection.fixedLength());
        AtomicInteger closed = new AtomicInteger();
        InputStream input = new ByteArrayInputStream(body) {
            @Override
            public void close() {
                closed.incrementAndGet();
            }
        };
        client.post(input, Void.class);
        assertEquals(-1, fixture.connection.fixedLength());
        assertEquals(8192, fixture.connection.chunkLength());
        assertEquals(body.length, fixture.connection.bytes.size());
        assertEquals(0, closed.get());
        client.post(body, HttpSettings.create().setContentFormat(ContentFormat.GZIP), Void.class);
        assertEquals(-1, fixture.connection.fixedLength());
        assertEquals(8192, fixture.connection.chunkLength());
        try (InputStream decoded = new GZIPInputStream(new ByteArrayInputStream(fixture.connection.bytes.toByteArray()))) {
            assertArrayEquals(body, decoded.readAllBytes());
        }
        client.post(body, HttpSettings.create().setContentFormat(ContentFormat.KRYO), Void.class);
        assertEquals(body.length, fixture.connection.fixedLength()); // encoding label does not compress raw bytes
        client.post(body, HttpSettings.create().doOutput(false), Void.class);
        assertEquals(-1, fixture.connection.fixedLength());
        assertEquals(-1, fixture.connection.chunkLength());
    }

    record Received(byte[] bytes, String length, String transfer, String encoding) {
    }

    @Test
    void localServerReceivesFixedChunkedAndCompressedBodies() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<Received> received = new AtomicReference<>();
        server.createContext("/upload", exchange -> {
            try (exchange) {
                byte[] bytes = exchange.getRequestBody().readAllBytes();
                received.set(new Received(bytes, exchange.getRequestHeaders().getFirst("Content-length"),
                        exchange.getRequestHeaders().getFirst("Transfer-encoding"), exchange.getRequestHeaders().getFirst("Content-encoding")));
                exchange.sendResponseHeaders(204, -1);
            }
        });
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/upload";
            HttpSettings settings = HttpSettings.create().streamRequestBody(true).setReadTimeout(5000);
            HttpClient client = HttpClient.create(url);
            byte[] body = "abc漢字".repeat(10000).getBytes(StandardCharsets.UTF_8);
            client.post(body, settings, Void.class);
            assertArrayEquals(body, received.get().bytes());
            assertEquals(Integer.toString(body.length), received.get().length());
            assertNull(received.get().transfer());
            client.post(new ByteArrayInputStream(body), settings, Void.class);
            assertArrayEquals(body, received.get().bytes());
            assertEquals("chunked", received.get().transfer());
            assertNull(received.get().length());
            HttpRequest.url(url).settings(settings).body(body).post(Void.class);
            assertEquals(Integer.toString(body.length), received.get().length());
            client.post(body, settings.copy().setContentFormat(ContentFormat.GZIP), Void.class);
            assertEquals("chunked", received.get().transfer());
            assertEquals("gzip", received.get().encoding());
            try (InputStream decoded = new GZIPInputStream(new ByteArrayInputStream(received.get().bytes()))) {
                assertArrayEquals(body, decoded.readAllBytes());
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    void defaultBodyStillFollowsRedirectAndOptInDoesNotReplay() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger redirects = new AtomicInteger(), targets = new AtomicInteger();
        server.createContext("/redirect", exchange -> {
            try (exchange) {
                exchange.getRequestBody().readAllBytes();
                redirects.incrementAndGet();
                exchange.getResponseHeaders().set("Location", "/target");
                exchange.sendResponseHeaders(307, -1);
            }
        });
        server.createContext("/target", exchange -> {
            try (exchange) {
                exchange.getRequestBody().readAllBytes();
                targets.incrementAndGet();
                exchange.sendResponseHeaders(204, -1);
            }
        });
        server.start();
        try {
            HttpClient client = HttpClient.create("http://127.0.0.1:" + server.getAddress().getPort() + "/redirect");
            client.post(new byte[] { 1, 2, 3 }, Void.class);
            assertEquals(1, targets.get());
            assertThrows(RuntimeException.class, () -> client.post(new byte[] { 1, 2, 3 }, HttpSettings.create().streamRequestBody(true), Void.class));
            assertEquals(1, targets.get());
            assertEquals(2, redirects.get());
        } finally {
            server.stop(0);
        }
    }
}

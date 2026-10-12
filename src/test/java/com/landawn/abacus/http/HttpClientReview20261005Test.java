package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.sun.net.httpserver.HttpServer;

/**
 * Code review 2026-10-05: with {@code streamRequestBody(true)} a {@code String} request body was always sent chunked,
 * although its bytes were computed in full before writing and its length was therefore known. Servers that reject
 * chunked uploads (411 Length Required) and HTTP/1.0 proxies failed for no reason. An uncompressed {@code String} is now
 * encoded once and sent in fixed-length mode, like {@code byte[]} and {@code File} bodies; compressed bodies stay chunked.
 */
@Tag("unit")
@SuppressWarnings("deprecation") // URL(URL, String, URLStreamHandler) installs the recording connection
class HttpClientReview20261005Test extends TestBase {
    private static final String TEXT = "abc漢字😀".repeat(5_000);

    private static final class RecordingConnection extends HttpURLConnection {
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        RecordingConnection(final URL url) {
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

    private static final class Fixture {
        private RecordingConnection connection;
        private final URL url;

        Fixture() throws Exception {
            url = new URL(null, "http://streaming.test/upload", new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(final URL u) {
                    return connection = new RecordingConnection(u);
                }
            });
        }

        HttpClient client(final HttpSettings defaults) {
            return HttpClient.create(url, 4, 5000, 5000, defaults);
        }
    }

    @Test
    void uncompressedStringBodiesUseFixedLengthStreaming() throws Exception {
        final Fixture fixture = new Fixture();
        final HttpClient client = fixture.client(HttpSettings.create().streamRequestBody(true));
        final byte[] utf8 = TEXT.getBytes(StandardCharsets.UTF_8);

        client.post(TEXT, Void.class);
        assertEquals(utf8.length, fixture.connection.fixedLength(), "the encoded length, not the char count, is announced");
        assertEquals(-1, fixture.connection.chunkLength());
        assertArrayEquals(utf8, fixture.connection.bytes.toByteArray());

        client.post("", Void.class);
        assertEquals(0, fixture.connection.fixedLength());
        assertEquals(-1, fixture.connection.chunkLength());

        // The length is that of the request charset's encoding, which is also the encoding written.
        final byte[] utf16 = TEXT.getBytes(StandardCharsets.UTF_16BE);
        client.post(TEXT, HttpSettings.create().setContentType("text/plain; charset=UTF-16BE"), Void.class);
        assertEquals(utf16.length, fixture.connection.fixedLength());
        assertArrayEquals(utf16, fixture.connection.bytes.toByteArray());
    }

    @Test
    void compressedOrNonStreamedStringBodiesKeepTheirModes() throws Exception {
        final Fixture fixture = new Fixture();
        final HttpClient client = fixture.client(HttpSettings.create().streamRequestBody(true));
        final byte[] utf8 = TEXT.getBytes(StandardCharsets.UTF_8);

        client.post(TEXT, HttpSettings.create().setContentFormat(ContentFormat.GZIP), Void.class);
        assertEquals(-1, fixture.connection.fixedLength(), "the compressed length is unknown before writing");
        assertEquals(8192, fixture.connection.chunkLength());

        try (InputStream decoded = new GZIPInputStream(new ByteArrayInputStream(fixture.connection.bytes.toByteArray()))) {
            assertArrayEquals(utf8, decoded.readAllBytes());
        }

        // Opt-in only: without streamRequestBody neither streaming mode is set and the JDK buffers the body as before.
        fixture.client(null).post(TEXT, Void.class);
        assertEquals(-1, fixture.connection.fixedLength());
        assertEquals(-1, fixture.connection.chunkLength());
        assertArrayEquals(utf8, fixture.connection.bytes.toByteArray());
    }

    private record Received(byte[] bytes, String length, String transfer) {
    }

    @Test
    void localServerReceivesAStringBodyWithContentLength() throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        final AtomicReference<Received> received = new AtomicReference<>();

        server.createContext("/upload", exchange -> {
            try (exchange) {
                final byte[] bytes = exchange.getRequestBody().readAllBytes();
                received.set(new Received(bytes, exchange.getRequestHeaders().getFirst("Content-length"),
                        exchange.getRequestHeaders().getFirst("Transfer-encoding")));
                exchange.sendResponseHeaders(204, -1);
            }
        });

        server.start();

        try {
            final String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/upload";
            final HttpSettings settings = HttpSettings.create().streamRequestBody(true).setReadTimeout(5000);
            final byte[] utf8 = TEXT.getBytes(StandardCharsets.UTF_8);

            HttpClient.create(url).post(TEXT, settings, Void.class);
            assertArrayEquals(utf8, received.get().bytes());
            assertEquals(Integer.toString(utf8.length), received.get().length());
            assertNull(received.get().transfer(), "a String body must not be sent chunked");

            HttpRequest.url(url).settings(settings).body(TEXT).post(Void.class);
            assertArrayEquals(utf8, received.get().bytes());
            assertEquals(Integer.toString(utf8.length), received.get().length());
            assertNull(received.get().transfer());
        } finally {
            server.stop(0);
        }
    }
}

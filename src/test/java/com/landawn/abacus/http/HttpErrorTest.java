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
}

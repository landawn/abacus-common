package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;

@SuppressWarnings("deprecation")
public class HttpClientBodyCompletionTest extends TestBase {
    private static final ContentFormat[] FORMATS = { ContentFormat.JSON, ContentFormat.JSON_SNAPPY, ContentFormat.JSON_GZIP, ContentFormat.JSON_LZ4 };

    private static final class Connection extends HttpURLConnection {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        boolean outputClosed;
        boolean responseRequested;
        boolean closedBeforeResponse;
        boolean disconnected;
        int lateFlushes;
        Throwable closeFailure;

        final OutputStream body = new OutputStream() {
            @Override
            public void write(final int b) throws IOException {
                if (outputClosed) {
                    throw new IOException("closed");
                }
                bytes.write(b);
            }

            @Override
            public void flush() throws IOException {
                if (outputClosed) {
                    lateFlushes++;
                    throw new IOException("closed");
                }
            }

            @Override
            public void close() throws IOException {
                outputClosed = true;
                final Throwable failure = closeFailure;
                closeFailure = null;
                if (failure instanceof IOException e) {
                    throw e;
                } else if (failure instanceof RuntimeException e) {
                    throw e;
                } else if (failure instanceof Error e) {
                    throw e;
                }
            }
        };

        Connection(final URL url) {
            super(url);
        }

        @Override
        public void connect() {
        }

        @Override
        public void disconnect() {
            disconnected = true;
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public OutputStream getOutputStream() {
            return body;
        }

        @Override
        public InputStream getInputStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public Map<String, List<String>> getHeaderFields() {
            return Map.of();
        }

        @Override
        public String getResponseMessage() {
            return "No Content";
        }

        @Override
        public int getResponseCode() throws IOException {
            responseRequested = true;
            closedBeforeResponse = outputClosed;
            body.close(); // HttpURLConnection finishes the request before reading the response.
            return 204;
        }
    }

    private static final class Fixture {
        Connection connection;
        Throwable nextCloseFailure;
        final AtomicInteger active = new AtomicInteger();
        final HttpClient client;

        Fixture() throws Exception {
            final URL url = new URL(null, "http://completion.test/upload", new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(final URL url) {
                    connection = new Connection(url);
                    connection.closeFailure = nextCloseFailure;
                    nextCloseFailure = null;
                    return connection;
                }
            });
            client = HttpClient.create(url, 1, 5000, 5000, HttpSettings.create().streamRequestBody(true), active);
        }
    }

    // The encoder must close while the JDK request stream is writable. Snappy flushes again on close;
    // doing that after getResponseCode used to log an IOException for every successful streamed upload.
    @Test
    public void closesEveryStreamedEncoderBeforeReadingTheResponse() throws Exception {
        for (final ContentFormat format : FORMATS) {
            final Fixture fixture = new Fixture();
            final String payload = "{\"value\":\"hello 世界\"}";
            fixture.client.post(payload, HttpSettings.create().setContentFormat(format), Void.class);
            assertTrue(fixture.connection.closedBeforeResponse, format.toString());
            assertEquals(0, fixture.connection.lateFlushes, format.toString());
            assertEquals(0, fixture.active.get());
            try (InputStream decoded = HttpUtil.wrapInputStream(new ByteArrayInputStream(fixture.connection.bytes.toByteArray()), format)) {
                assertEquals(payload, new String(decoded.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
    }

    // Finishing a request is part of sending it: a close failure must reach the caller, abort the connection,
    // release its in-flight slot, and leave the client usable for a subsequent request.
    @Test
    public void requestCloseFailuresPropagateAndAbortBeforeResponse() throws Exception {
        for (final ContentFormat format : FORMATS) {
            for (final Throwable failure : new Throwable[] { new IOException("close failed"), new IllegalStateException("close failed"),
                    new AssertionError("close failed") }) {
                final Fixture fixture = new Fixture();
                final HttpSettings settings = HttpSettings.create().setContentFormat(format);
                fixture.nextCloseFailure = failure;
                final Throwable thrown = assertThrows(Throwable.class, () -> fixture.client.post("\"body\"", settings, Void.class));
                if (failure instanceof IOException) {
                    assertInstanceOf(UncheckedIOException.class, thrown);
                    assertSame(failure, thrown.getCause());
                } else {
                    assertSame(failure, thrown);
                }
                assertFalse(fixture.connection.responseRequested);
                assertTrue(fixture.connection.disconnected);
                assertEquals(0, fixture.active.get());
                assertDoesNotThrow(() -> fixture.client.post("\"next\"", settings, Void.class));
                assertEquals(0, fixture.active.get());
            }
        }
    }
}

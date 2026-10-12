package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.HttpResponseException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.sun.net.httpserver.HttpServer;

public class IgnoredHttpResponseBodyReview20261003Test extends TestBase {
    @Test
    public void voidAndNullResultsSkipDecodingAndCloseResponse() throws Exception {
        for (final Class<?> resultType : new Class<?>[] { Void.class, null }) {
            final Fixture fixture = fixture(200, null, "not gzip".getBytes(StandardCharsets.UTF_8));
            assertNull(fixture.client.get(resultType));
            assertIgnoredAndClosed(fixture);
        }
    }

    @Test
    public void oneWayClientAndPerRequestSettingsSkipDecoding() throws Exception {
        final Fixture clientDefault = fixture(200, HttpSettings.create().setOneWayRequest(true), new byte[0]);
        assertNull(clientDefault.client.get(String.class));
        assertIgnoredAndClosed(clientDefault);

        final Fixture perRequest = fixture(200, null, new byte[0]);
        assertNull(perRequest.client.get(HttpSettings.create().setOneWayRequest(true), HttpResponse.class));
        assertIgnoredAndClosed(perRequest);
    }

    @Test
    public void oneWayRawResponseKeepsExistingNonSuccessSemantics() throws Exception {
        final Fixture fixture = fixture(404, HttpSettings.create().setOneWayRequest(true), new byte[0]);
        assertNull(fixture.client.get(HttpResponse.class));
        assertIgnoredAndClosed(fixture);
    }

    @Test
    public void nonSuccessStillThrowsStatusExceptionForIgnoredResults() throws Exception {
        for (final Class<?> resultType : new Class<?>[] { Void.class, null, String.class }) {
            final Fixture fixture = fixture(503, HttpSettings.create().setOneWayRequest(true), gzip("diagnostic"));
            final HttpResponseException error = assertThrows(HttpResponseException.class, () -> fixture.client.get(resultType));
            assertEquals(503, error.statusCode());
            assertEquals("diagnostic", error.responseBody());
            assertTrue(fixture.input.closed);
            assertEquals(0, fixture.active.get());
        }
    }

    @Test
    public void requestedBodiesStillDecodeStrictlyAndOptOutStillWorks() throws Exception {
        final Fixture strict = fixture(200, null, new byte[0]);
        assertThrows(UncheckedIOException.class, () -> strict.client.get(String.class));
        assertTrue(strict.input.closed);
        assertEquals(0, strict.active.get());

        final Fixture optOut = fixture(200, HttpSettings.create().setOneWayRequest(true), gzip("received"));
        assertEquals("received", optOut.client.get(HttpSettings.create().setOneWayRequest(false), String.class));
        assertTrue(optOut.input.closed);
        assertEquals(0, optOut.active.get());
    }

    @Test
    public void explicitOutputStillReceivesDecodedBodyDespiteOneWayFlag() throws Exception {
        final Fixture fixture = fixture(200, HttpSettings.create().setOneWayRequest(true), gzip("forwarded"));
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        fixture.client.execute(HttpMethod.GET, null, null, output);
        assertEquals("forwarded", output.toString(StandardCharsets.UTF_8));
        assertTrue(fixture.input.closed);
        assertEquals(0, fixture.active.get());
    }

    @Test
    public void malformedGzipSuccessIsIgnoredWithRealUrlConnection() throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ignored", exchange -> {
            final byte[] body = "not gzip".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Encoding", "gzip");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            } finally {
                exchange.close();
            }
        });
        server.start();
        try {
            final HttpClient client = HttpClient.create("http://127.0.0.1:" + server.getAddress().getPort() + "/ignored", 1, 1000, 1000);
            assertNull(client.get(Void.class));
            assertNull(client.get(HttpSettings.create().setOneWayRequest(true), String.class));
        } finally {
            server.stop(0);
        }
    }

    private static void assertIgnoredAndClosed(final Fixture fixture) {
        assertEquals(0, fixture.input.consumed(), "ignored bodies must not be read by a decoder");
        assertTrue(fixture.input.closed, "the transport stream still belongs to HttpClient");
        assertEquals(0, fixture.active.get());
    }

    private static Fixture fixture(final int status, final HttpSettings settings, final byte[] body) throws Exception {
        final TrackingInput input = new TrackingInput(body);
        final URL url = new URL(null, "http://example.test/ignored", new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(final URL requestUrl) {
                return new HttpURLConnection(requestUrl) {
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
                    public int getResponseCode() {
                        return status;
                    }

                    @Override
                    public String getResponseMessage() {
                        return "test response";
                    }

                    @Override
                    public Map<String, List<String>> getHeaderFields() {
                        return Map.of("Content-Type", List.of("text/plain; charset=UTF-8"), "Content-Encoding", List.of("gzip"));
                    }

                    @Override
                    public TrackingInput getInputStream() throws IOException {
                        if (status >= 400) {
                            throw new IOException("HTTP error");
                        }
                        return input;
                    }

                    @Override
                    public TrackingInput getErrorStream() {
                        return status >= 400 ? input : null;
                    }
                };
            }
        });
        final AtomicInteger active = new AtomicInteger();
        return new Fixture(HttpClient.create(url, 1, 1000, 1000, settings, active), input, active);
    }

    private static byte[] gzip(final String body) throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream output = new GZIPOutputStream(bytes)) {
            output.write(body.getBytes(StandardCharsets.UTF_8));
        }
        return bytes.toByteArray();
    }

    private record Fixture(HttpClient client, TrackingInput input, AtomicInteger active) {
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

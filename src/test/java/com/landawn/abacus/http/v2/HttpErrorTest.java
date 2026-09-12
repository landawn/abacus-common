package com.landawn.abacus.http.v2;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.BodySubscriber;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.HttpResponseException;
import com.landawn.abacus.http.HttpUtil;
import com.sun.net.httpserver.HttpServer;

public class HttpErrorTest extends TestBase {
    private static final Class<?>[] RESULT_TYPES = { String.class, byte[].class, InputStream.class, Void.class, null };

    static Stream<Arguments> errorCases() {
        return Stream.of(false, true).flatMap(compressed -> Arrays.stream(RESULT_TYPES).map(type -> Arguments.of(compressed, type)));
    }

    static Stream<Arguments> fatalReadCases() {
        return Stream.of(false, true).flatMap(async -> Stream.of(false, true).map(owned -> Arguments.of(async, owned)));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testCompletedResponseDoesNotReadErrorOnAsyncCaller(final boolean cancel) throws Exception {
        final CountDownLatch reading = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final CountDownLatch closed = new CountDownLatch(1);
        final CountDownLatch clientClosed = new CountDownLatch(1);
        final AtomicInteger closeCount = new AtomicInteger();
        final InputStream input = new InputStream() {
            @Override
            public int read() throws IOException {
                reading.countDown();
                try {
                    if (!release.await(10, TimeUnit.SECONDS)) {
                        throw new IOException("body release timed out");
                    }
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException(e);
                }
                return -1;
            }

            @Override
            public void close() {
                closeCount.incrementAndGet();
                closed.countDown();
            }
        };
        final CompletableFuture<HttpResponse<Object>> upstream = new CompletableFuture<>() {
            @Override
            public <U> CompletableFuture<U> thenApplyAsync(final Function<? super HttpResponse<Object>, ? extends U> fn) {
                // Force the legal completion-before-registration timing without a scheduling race.
                final CompletableFuture<U> mapped = super.thenApplyAsync(fn);
                mapped.join();
                return mapped;
            }
        };
        upstream.complete(response(input));
        final HttpClient client = mock(HttpClient.class);
        when(client.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(upstream);
        org.mockito.Mockito.doAnswer(invocation -> {
            clientClosed.countDown();
            return null;
        }).when(client).close();
        final HttpRequest request = HttpRequest.create("http://example.test/", client).closeHttpClientAfterExecution(true);
        final ExecutorService caller = Executors.newSingleThreadExecutor();
        try {
            final CompletableFuture<String> result = caller.submit(() -> request.asyncGet(String.class)).get(2, TimeUnit.SECONDS);
            assertTrue(reading.await(2, TimeUnit.SECONDS));
            assertTrue(!result.isDone());
            if (cancel) {
                assertTrue(result.cancel(true));
            }
            // This executor must remain available to deliver the body after asyncGet returns.
            caller.submit(release::countDown).get(2, TimeUnit.SECONDS);
            if (cancel) {
                assertTrue(result.isCancelled());
            } else {
                final CompletionException failure = assertThrows(CompletionException.class, result::join);
                assertEquals(500, assertInstanceOf(HttpResponseException.class, failure.getCause()).statusCode());
            }
            assertTrue(closed.await(5, TimeUnit.SECONDS));
            assertTrue(clientClosed.await(5, TimeUnit.SECONDS));
            assertEquals(1, closeCount.get());
            verify(client, times(1)).close();
        } finally {
            release.countDown();
            caller.shutdownNow();
            assertTrue(caller.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @ParameterizedTest
    @MethodSource("fatalReadCases")
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testFatalReadClosesStreamAndPreservesPrimary(final boolean async, final boolean owned) throws Exception {
        final AssertionError primary = new AssertionError("fatal read failed");
        for (final Throwable cleanup : new Throwable[] { null, new IOException("close IO"), new IllegalStateException("close runtime"),
                new AssertionError("close error"), primary }) {
            final AtomicInteger closes = new AtomicInteger();
            final InputStream input = new InputStream() {
                @Override
                public int read() {
                    throw primary;
                }

                @Override
                public void close() throws IOException {
                    closes.incrementAndGet();
                    if (cleanup instanceof IOException e) {
                        throw e;
                    }
                    if (cleanup instanceof RuntimeException e) {
                        throw e;
                    }
                    if (cleanup instanceof Error e) {
                        throw e;
                    }
                }
            };
            final HttpResponse<Object> response = response(input);
            final HttpClient client = mock(HttpClient.class);
            when(client.send(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(response);
            when(client.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class)))
                    .thenReturn((CompletableFuture) CompletableFuture.completedFuture(response));
            final HttpRequest request = HttpRequest.create("http://example.test/", client).closeHttpClientAfterExecution(owned);
            final int previousSuppressed = primary.getSuppressed().length;
            if (async) {
                final CompletionException completion = assertThrows(CompletionException.class, () -> request.asyncGet(String.class).join());
                assertSame(primary, completion.getCause());
            } else {
                assertSame(primary, assertThrows(AssertionError.class, () -> request.get(String.class)));
            }
            assertEquals(1, closes.get());
            verify(client, times(owned ? 1 : 0)).close();
            assertEquals(previousSuppressed + (cleanup == null || cleanup == primary ? 0 : 1), primary.getSuppressed().length);
            if (cleanup != null && cleanup != primary) {
                assertSame(cleanup, primary.getSuppressed()[previousSuppressed]);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("errorCases")
    public void testSyncAndAsyncErrorsCaptureBoundedDecodedUnicode(final boolean compressed, final Class<?> resultType) throws Exception {
        final byte[] body = ("x".repeat(HttpUtil.MAX_ERROR_BODY_SIZE - 1) + "\u4e2d\ud83d\ude00".repeat(5000)).getBytes(StandardCharsets.UTF_8);
        try (ResponseServer server = server(422, compressed ? gzip(body) : body, "text/plain; charset=UTF-8", compressed, false)) {
            final HttpResponseException sync = assertThrows(HttpResponseException.class, () -> HttpRequest.url(server.url(), 1000, 5000).get(resultType));
            assertError(sync, server.url(), 422, new String(Arrays.copyOf(body, HttpUtil.MAX_ERROR_BODY_SIZE), StandardCharsets.UTF_8));
            final CompletionException async = assertThrows(CompletionException.class,
                    () -> HttpRequest.url(server.url(), 1000, 5000).asyncGet(resultType).join());
            assertError(assertInstanceOf(HttpResponseException.class, async.getCause()), server.url(), 422, sync.responseBody());
        }
    }

    @Test
    public void testMalformedAndEmptyCompressedErrorsRetainDiagnostics() throws Exception {
        final byte[] complete = gzip("bad".getBytes(StandardCharsets.UTF_8));
        for (final byte[] bytes : new byte[][] { {}, { 1, 2, 3 }, Arrays.copyOf(complete, complete.length - 4) }) {
            try (ResponseServer server = server(500, bytes, "text/plain", true, false)) {
                final HttpResponseException error = assertThrows(HttpResponseException.class, () -> HttpRequest.url(server.url()).get(String.class));
                assertError(error, server.url(), 500, "");
            }
        }
    }

    @Test
    public void testErrorCharsetIsAppliedAfterDecompression() throws Exception {
        try (ResponseServer server = server(400, gzip("caf\u00e9".getBytes(StandardCharsets.ISO_8859_1)), "text/plain; charset=ISO-8859-1", true, false)) {
            final HttpResponseException error = assertThrows(HttpResponseException.class, () -> HttpRequest.url(server.url()).get(InputStream.class));
            assertEquals("caf\u00e9", error.responseBody());
        }
    }

    @Test
    public void testAsyncDelayedErrorWithSingleThreadClientExecutor() throws Exception {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final HttpClient client = HttpClient.newBuilder().executor(executor).build();
        try (ResponseServer server = server(503, "delayed".getBytes(StandardCharsets.UTF_8), "text/plain", false, true)) {
            final CompletableFuture<String> future = HttpRequest.create(server.url(), client).asyncGet(String.class);
            final java.util.concurrent.ExecutionException error = assertThrows(java.util.concurrent.ExecutionException.class,
                    () -> future.get(10, TimeUnit.SECONDS));
            assertEquals("delayed", assertInstanceOf(HttpResponseException.class, error.getCause()).responseBody());
        } finally {
            client.shutdownNow();
            client.close();
            executor.shutdownNow();
        }
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testErrorSubscriberIsStreamingAndCaptureDoesNotDrainBody() throws Exception {
        for (final Class<?> resultType : RESULT_TYPES) {
            final CountingInput input = new CountingInput();
            final HttpResponse response = response(input);
            final HttpClient client = mock(HttpClient.class);
            when(client.send(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenAnswer(invocation -> {
                final BodyHandler<?> handler = invocation.getArgument(1);
                final HttpResponse.ResponseInfo info = mock(HttpResponse.ResponseInfo.class);
                when(info.statusCode()).thenReturn(500);
                final BodySubscriber<?> subscriber = handler.apply(info);
                subscriber.onSubscribe(mock(Subscription.class));
                // A streaming subscriber exposes its stream before any body or completion event.
                final InputStream streamed = assertInstanceOf(InputStream.class, subscriber.getBody().toCompletableFuture().get(2, TimeUnit.SECONDS));
                streamed.close();
                return response;
            });
            final HttpResponseException error = assertThrows(HttpResponseException.class,
                    () -> HttpRequest.create("http://example.test/", client).get(resultType));
            assertEquals(HttpUtil.MAX_ERROR_BODY_SIZE, input.consumed);
            assertEquals("x".repeat(HttpUtil.MAX_ERROR_BODY_SIZE), error.responseBody());
            assertEquals(0, input.closed.getCount());
            verify(client, never()).close();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testCleanupFailuresAreSuppressedOnHttpErrorAndOwnedClientClosesOnce(final boolean async) throws Exception {
        final IOException closeFailure = new IOException("stream close failed");
        final AssertionError clientFailure = new AssertionError("client close failed");
        final InputStream input = new InputStream() {
            @Override
            public int read() {
                return -1;
            }

            @Override
            public void close() throws IOException {
                throw closeFailure;
            }
        };
        final HttpClient client = mock(HttpClient.class);
        final HttpResponse<Object> response = response(input);
        when(client.send(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(response);
        when(client.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class)))
                .thenReturn((CompletableFuture) CompletableFuture.completedFuture(response));
        doThrow(clientFailure).when(client).close();
        final HttpRequest request = HttpRequest.create("http://example.test/", client).closeHttpClientAfterExecution(true);
        final HttpResponseException error;
        if (async) {
            final CompletionException completion = assertThrows(CompletionException.class, () -> request.asyncGet(String.class).join());
            error = assertInstanceOf(HttpResponseException.class, completion.getCause());
        } else {
            error = assertThrows(HttpResponseException.class, () -> request.get(String.class));
        }
        assertEquals(500, error.statusCode());
        assertArrayEquals(new Throwable[] { closeFailure }, error.getSuppressed());
        assertArrayEquals(new Throwable[] { clientFailure }, closeFailure.getSuppressed());
        verify(client, times(1)).close();
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testReadFailureClosesRawBodyAndRetainsStatus() throws Exception {
        final CountDownLatch closed = new CountDownLatch(1);
        final InputStream input = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("read failed");
            }

            @Override
            public void close() {
                closed.countDown();
            }
        };
        final HttpResponse<Object> response = response(input);
        final HttpClient client = mock(HttpClient.class);
        when(client.send(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(response);
        final HttpResponseException error = assertThrows(HttpResponseException.class,
                () -> HttpRequest.create("http://example.test/", client).get(String.class));
        assertEquals(500, error.statusCode());
        assertEquals("", error.responseBody());
        assertEquals(0, closed.getCount());
        verify(client, never()).close();
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testCancelledTypedAsyncErrorStillClosesBodyAndOwnedClient() throws Exception {
        final CountingInput input = new CountingInput();
        final HttpClient client = mock(HttpClient.class);
        final CountDownLatch clientClosed = new CountDownLatch(1);
        org.mockito.Mockito.doAnswer(invocation -> {
            clientClosed.countDown();
            return null;
        }).when(client).close();
        final CompletableFuture<HttpResponse<Object>> upstream = new CompletableFuture<>();
        when(client.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(upstream);
        final CompletableFuture<String> result = HttpRequest.create("http://example.test/", client).closeHttpClientAfterExecution(true).asyncGet(String.class);
        assertTrue(result.cancel(true));
        upstream.complete(response(input));
        assertTrue(input.closed.await(5, TimeUnit.SECONDS));
        assertTrue(clientClosed.await(5, TimeUnit.SECONDS));
        assertTrue(result.isCancelled());
        verify(client, times(1)).close();
    }

    @Test
    public void testCustomResponseHandlerCanStillReadFullErrorBody() throws Exception {
        final byte[] body = "full error".repeat(1000).getBytes(StandardCharsets.UTF_8);
        try (ResponseServer server = server(500, body, "text/plain", false, false)) {
            final HttpResponse<byte[]> response = HttpRequest.url(server.url()).get(BodyHandlers.ofByteArray());
            assertEquals(500, response.statusCode());
            assertArrayEquals(body, response.body());
        }
    }

    private static void assertError(final HttpResponseException error, final String url, final int status, final String body) {
        assertEquals(status, error.statusCode());
        assertEquals(url, error.requestUrl());
        assertEquals("retained", error.header("X-Detail"));
        assertNull(error.responseMessage());
        assertEquals(body, error.responseBody());
    }

    @SuppressWarnings("unchecked")
    private static HttpResponse<Object> response(final InputStream input) {
        final HttpResponse<Object> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(500);
        when(response.uri()).thenReturn(URI.create("http://example.test/"));
        when(response.headers()).thenReturn(java.net.http.HttpHeaders.of(Map.of("Content-Type", List.of("text/plain")), (name, value) -> true));
        when(response.body()).thenReturn(input);
        return response;
    }

    private static final class CountingInput extends InputStream {
        private int consumed;
        private final CountDownLatch closed = new CountDownLatch(1);

        @Override
        public int read() {
            if (++consumed > HttpUtil.MAX_ERROR_BODY_SIZE) {
                throw new AssertionError("error body was drained beyond its capture limit");
            }
            return 'x';
        }

        @Override
        public void close() {
            closed.countDown();
        }
    }

    private record ResponseServer(HttpServer server, String url) implements AutoCloseable {
        @Override
        public void close() {
            server.stop(0);
        }
    }

    private static ResponseServer server(final int status, final byte[] bytes, final String contentType, final boolean compressed, final boolean delayed)
            throws IOException {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try {
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.getResponseHeaders().set("X-Detail", "retained");
                if (compressed) {
                    exchange.getResponseHeaders().set("Content-Encoding", "gzip");
                }
                exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
                if (delayed) {
                    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100));
                }
                exchange.getResponseBody().write(bytes);
            } catch (final IOException ignored) {
                // Bounded capture intentionally closes the client before a large body is drained.
            } finally {
                exchange.close();
            }
        });
        server.start();
        return new ResponseServer(server, "http://127.0.0.1:" + server.getAddress().getPort() + "/");
    }

    private static byte[] gzip(final byte[] bytes) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            gzip.write(bytes);
        }
        return output.toByteArray();
    }
}

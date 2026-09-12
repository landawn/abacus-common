package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.HttpResponseException;
import com.landawn.abacus.util.ContinuableFuture;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;

public class OkHttpCancellationTest extends TestBase {
    static Stream<Arguments> rawCases() {
        return IntStream.range(0, 13)
                .boxed()
                .flatMap(route -> Stream.of(false, true).flatMap(interrupt -> Stream.of(200, 500).map(status -> Arguments.of(route, interrupt, status))));
    }

    @ParameterizedTest
    @MethodSource("rawCases")
    public void testEveryAsyncRouteClosesResponseWhenCancellationWins(final int route, final boolean interrupt, final int status) throws Exception {
        try (Exchange exchange = new Exchange(status, null)) {
            final ContinuableFuture<Response> result = submit(exchange.request, route, exchange.executor);
            assertTrue(exchange.entered.await(10, TimeUnit.SECONDS));
            assertTrue(result.cancel(interrupt));
            if (interrupt) {
                assertTrue(exchange.interrupted.await(5, TimeUnit.SECONDS));
            }
            exchange.release.countDown();
            exchange.drain();
            assertThrows(CancellationException.class, result::get);
            assertEquals(1, exchange.closes.get());
            assertNull(exchange.escapedFailure.get());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void testDefaultExecutorsCloseCancelledResponses(final boolean interrupt) throws Exception {
        for (int route = 0; route < 13; route++) {
            try (Exchange exchange = new Exchange(200, null)) {
                final ContinuableFuture<Response> result = submit(exchange.request, route, null);
                assertTrue(exchange.entered.await(10, TimeUnit.SECONDS));
                assertTrue(result.cancel(interrupt));
                exchange.release.countDown();
                assertTrue(exchange.closed.await(10, TimeUnit.SECONDS));
                assertEquals(1, exchange.closes.get());
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void testBeforeStartCancellationDoesNotExecuteRequest(final boolean interrupt) throws Exception {
        for (int route = 0; route < 13; route++) {
            try (Exchange exchange = new Exchange(200, null)) {
                final List<Runnable> queued = new ArrayList<>();
                final ContinuableFuture<Response> result = submit(exchange.request, route, queued::add);
                assertTrue(result.cancel(interrupt));
                assertEquals(1, queued.size());
                queued.get(0).run();
                assertEquals(1, exchange.entered.getCount());
                assertEquals(0, exchange.closes.get());
            }
        }
    }

    @Test
    public void testCompletedResponsesRemainCallerOwnedWithDirectExecutor() throws Exception {
        for (int route = 0; route < 13; route++) {
            try (Exchange exchange = new Exchange(500, null)) {
                exchange.release.countDown();
                final ContinuableFuture<Response> result = submit(exchange.request, route, Runnable::run);
                assertFalse(result.cancel(true));
                try (Response response = result.get()) {
                    assertSame(exchange.body, response.body());
                    assertEquals(0, exchange.closes.get());
                }
                assertEquals(1, exchange.closes.get());
            }
        }
    }

    @Test
    public void testPublicationCancellationRaceHasExactlyOneOwner() throws Exception {
        final ExecutorService canceller = Executors.newSingleThreadExecutor();
        try {
            for (int round = 0; round < 20; round++) {
                try (Exchange exchange = new Exchange(200, null)) {
                    final ContinuableFuture<Response> result = submit(exchange.request, round % 13, exchange.executor);
                    assertTrue(exchange.entered.await(10, TimeUnit.SECONDS));
                    final CountDownLatch race = new CountDownLatch(1);
                    final var cancelled = canceller.submit(() -> {
                        race.await();
                        return result.cancel(false);
                    });
                    race.countDown();
                    exchange.release.countDown();
                    final boolean cancellationWon = cancelled.get(10, TimeUnit.SECONDS);
                    exchange.drain();
                    if (cancellationWon) {
                        assertThrows(CancellationException.class, result::get);
                        assertEquals(1, exchange.closes.get());
                    } else {
                        try (Response response = result.get()) {
                            assertSame(exchange.body, response.body());
                            assertEquals(0, exchange.closes.get());
                        }
                        assertEquals(1, exchange.closes.get());
                    }
                }
            }
        } finally {
            canceller.shutdownNow();
        }
    }

    @Test
    public void testOrphanCloseFailuresDoNotEscapeOrChangeCancellation() throws Exception {
        for (final Throwable failure : new Throwable[] { new IOException("close"), new IllegalStateException("close"), new AssertionError("close") }) {
            try (Exchange exchange = new Exchange(200, failure)) {
                final ContinuableFuture<Response> result = exchange.request.asyncGet(exchange.executor);
                assertTrue(exchange.entered.await(10, TimeUnit.SECONDS));
                assertTrue(result.cancel(false));
                exchange.release.countDown();
                exchange.drain();
                assertThrows(CancellationException.class, result::get);
                assertEquals(1, exchange.closes.get());
                assertNull(exchange.escapedFailure.get());
            }
        }
    }

    @Test
    public void testTypedResultsRetainTheirOwnCleanup() throws Exception {
        for (final Class<?> type : new Class<?>[] { String.class, byte[].class, Void.class }) {
            for (final int status : new int[] { 200, 500 }) {
                for (final boolean cancel : new boolean[] { false, true }) {
                    try (Exchange exchange = new Exchange(status, null)) {
                        final ContinuableFuture<?> result = exchange.request.asyncGet(type, exchange.executor);
                        assertTrue(exchange.entered.await(10, TimeUnit.SECONDS));
                        if (cancel) {
                            assertTrue(result.cancel(false));
                        }
                        exchange.release.countDown();
                        exchange.drain();
                        if (cancel) {
                            assertThrows(CancellationException.class, result::get);
                        } else if (status == 500) {
                            assertInstanceOf(HttpResponseException.class, assertThrows(ExecutionException.class, result::get).getCause());
                        } else if (type == String.class) {
                            assertEquals("caf\u00e9\u4e2d", result.get());
                        } else if (type == Void.class) {
                            assertNull(result.get());
                        } else {
                            assertInstanceOf(byte[].class, result.get());
                        }
                        assertEquals(1, exchange.closes.get());
                    }
                }
            }
        }
    }

    @Test
    public void testRejectionAndContinuationExecutorArePreserved() throws Exception {
        for (int route = 0; route < 13; route++) {
            try (Exchange exchange = new Exchange(200, null)) {
                final int selected = route;
                assertThrows(RejectedExecutionException.class, () -> submit(exchange.request, selected, command -> {
                    throw new RejectedExecutionException("rejected");
                }));
                assertEquals(1, exchange.entered.getCount());
            }
        }
        try (Exchange exchange = new Exchange(200, null)) {
            exchange.release.countDown();
            final ContinuableFuture<Response> result = exchange.request.asyncGet(exchange.executor);
            assertEquals("okhttp-review-worker", result.thenCallAsync(response -> {
                response.close();
                return Thread.currentThread().getName();
            }).get(10, TimeUnit.SECONDS));
            assertEquals(1, exchange.closes.get());
        }
    }

    private static ContinuableFuture<Response> submit(final OkHttpRequest request, final int route, final Executor executor) {
        return switch (route) {
            case 0 -> executor == null ? request.asyncGet() : request.asyncGet(executor);
            case 1 -> executor == null ? request.asyncGet(Response.class) : request.asyncGet(Response.class, executor);
            case 2 -> executor == null ? request.asyncPost() : request.asyncPost(executor);
            case 3 -> executor == null ? request.asyncPost(Response.class) : request.asyncPost(Response.class, executor);
            case 4 -> executor == null ? request.asyncPut() : request.asyncPut(executor);
            case 5 -> executor == null ? request.asyncPut(Response.class) : request.asyncPut(Response.class, executor);
            case 6 -> executor == null ? request.asyncPatch() : request.asyncPatch(executor);
            case 7 -> executor == null ? request.asyncPatch(Response.class) : request.asyncPatch(Response.class, executor);
            case 8 -> executor == null ? request.asyncDelete() : request.asyncDelete(executor);
            case 9 -> executor == null ? request.asyncDelete(Response.class) : request.asyncDelete(Response.class, executor);
            case 10 -> executor == null ? request.asyncHead() : request.asyncHead(executor);
            case 11 -> executor == null ? request.asyncExecute(HttpMethod.GET) : request.asyncExecute(HttpMethod.GET, executor);
            case 12 -> executor == null ? request.asyncExecute(HttpMethod.GET, Response.class) : request.asyncExecute(HttpMethod.GET, Response.class, executor);
            default -> throw new AssertionError(route);
        };
    }

    private static final class Exchange implements AutoCloseable {
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final CountDownLatch interrupted = new CountDownLatch(1);
        final CountDownLatch closed = new CountDownLatch(1);
        final AtomicInteger closes = new AtomicInteger();
        final AtomicReference<Throwable> escapedFailure = new AtomicReference<>();
        final ExecutorService executor = Executors.newSingleThreadExecutor(command -> {
            final Thread thread = new Thread(command, "okhttp-review-worker");
            thread.setUncaughtExceptionHandler((t, failure) -> escapedFailure.set(failure));
            return thread;
        });
        final ResponseBody body;
        final OkHttpClient client;
        final OkHttpRequest request;

        Exchange(final int status, final Throwable closeFailure) {
            final BufferedSource source = Okio.buffer(new ForwardingSource(new Buffer().writeUtf8("caf\u00e9\u4e2d")) {
                @Override
                public void close() throws IOException {
                    closes.incrementAndGet();
                    closed.countDown();
                    super.close();
                    if (closeFailure instanceof IOException io) {
                        throw io;
                    }
                    if (closeFailure instanceof RuntimeException runtime) {
                        throw runtime;
                    }
                    if (closeFailure instanceof Error error) {
                        throw error;
                    }
                }
            });
            body = new ResponseBody() {
                @Override
                public MediaType contentType() {
                    return MediaType.get("text/plain; charset=UTF-8");
                }

                @Override
                public long contentLength() {
                    return -1;
                }

                @Override
                public BufferedSource source() {
                    return source;
                }
            };
            client = new OkHttpClient.Builder().addInterceptor(chain -> {
                entered.countDown();
                // Simulate a call that can finish even after an interruption request.
                boolean released = false;
                while (!released) {
                    try {
                        released = release.await(15, TimeUnit.SECONDS);
                    } catch (final InterruptedException e) {
                        interrupted.countDown();
                    }
                    if (!released && interrupted.getCount() != 0) {
                        throw new IOException("release timed out");
                    }
                }
                return new Response.Builder().request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(status)
                        .message("test")
                        .header("Content-Type", "text/plain; charset=UTF-8")
                        .body(body)
                        .build();
            }).build();
            request = OkHttpRequest.create("https://example.test/", client);
        }

        void drain() throws Exception {
            executor.submit(() -> {
            }).get(10, TimeUnit.SECONDS);
        }

        @Override
        public void close() throws Exception {
            release.countDown();
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
            try {
                body.close();
            } catch (final Exception | Error ignored) {
            }
            client.connectionPool().evictAll();
            client.dispatcher().executorService().shutdownNow();
        }
    }
}

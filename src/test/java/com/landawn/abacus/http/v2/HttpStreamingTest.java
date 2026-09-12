package com.landawn.abacus.http.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.http.HttpMethod;
import com.sun.net.httpserver.HttpServer;

public class HttpStreamingTest extends TestBase {
    private static final String URL = "http://localhost:18080/stream";

    static Stream<Arguments> deliveryCases() {
        return Stream.of(false, true)
                .flatMap(async -> Stream.of(false, true)
                        .flatMap(publisher -> Stream.of(false, true)
                                .flatMap(owned -> Stream.of("full", "empty", "early").map(mode -> Arguments.of(async, publisher, owned, mode)))));
    }

    static Stream<Arguments> cleanupCases() {
        return Stream.of(false, true)
                .flatMap(publisher -> Stream.of(false, true)
                        .flatMap(owned -> Stream.of(false, true)
                                .flatMap(push -> Stream.of(false, true).map(race -> Arguments.of(publisher, owned, push, race)))));
    }

    static Stream<Arguments> failureCases() {
        return Stream.of(false, true).flatMap(async -> Stream.of(false, true).map(publisher -> Arguments.of(async, publisher)));
    }

    static Stream<Arguments> realOrphanCases() {
        return Stream.of(false, true).flatMap(publisher -> Stream.of(false, true).map(owned -> Arguments.of(publisher, owned)));
    }

    @ParameterizedTest
    @MethodSource("realOrphanCases")
    public void testCancelledRealExchangeReleasesStandardStreamingBody(final boolean publisher, final boolean owned) throws Exception {
        final CountDownLatch received = new CountDownLatch(1);
        final CountDownLatch releaseHeaders = new CountDownLatch(1);
        final CountDownLatch releaseBody = new CountDownLatch(1);
        final CountDownLatch disposed = new CountDownLatch(1);
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        final ExecutorService io = Executors.newSingleThreadExecutor();
        final HttpClient client = HttpClient.newBuilder().executor(io).build();
        server.createContext("/", exchange -> {
            try (exchange) {
                received.countDown();
                try {
                    if (!releaseHeaders.await(15, TimeUnit.SECONDS)) {
                        return;
                    }
                    exchange.sendResponseHeaders(200, 1000000);
                    exchange.getResponseBody().write("first\n".getBytes(StandardCharsets.UTF_8));
                    exchange.getResponseBody().flush();
                    releaseBody.await(15, TimeUnit.SECONDS);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        server.start();
        try {
            final HttpRequest request = HttpRequest.create("http://127.0.0.1:" + server.getAddress().getPort(), client).closeHttpClientAfterExecution(owned);
            final BodyHandler<?> handler;
            if (publisher) {
                handler = info -> BodySubscribers.mapping(BodyHandlers.ofPublisher().apply(info),
                        original -> (Flow.Publisher<List<ByteBuffer>>) subscriber -> original.subscribe(new Flow.Subscriber<>() {
                            @Override
                            public void onSubscribe(final Flow.Subscription subscription) {
                                subscriber.onSubscribe(new Flow.Subscription() {
                                    @Override
                                    public void request(final long n) {
                                        subscription.request(n);
                                    }

                                    @Override
                                    public void cancel() {
                                        try {
                                            subscription.cancel();
                                        } finally {
                                            disposed.countDown();
                                        }
                                    }
                                });
                            }

                            @Override
                            public void onNext(final List<ByteBuffer> item) {
                                subscriber.onNext(item);
                            }

                            @Override
                            public void onError(final Throwable failure) {
                                subscriber.onError(failure);
                            }

                            @Override
                            public void onComplete() {
                                subscriber.onComplete();
                            }
                        }));
            } else {
                handler = info -> BodySubscribers.mapping(BodyHandlers.ofLines().apply(info), lines -> lines.onClose(disposed::countDown));
            }
            final CompletableFuture<?> result = request.asyncGet(handler);
            assertTrue(received.await(10, TimeUnit.SECONDS));
            assertTrue(result.cancel(true));
            releaseHeaders.countDown();
            assertTrue(disposed.await(10, TimeUnit.SECONDS), "late response body must be closed/cancelled without a consumer");
            if (owned) {
                assertTrue(client.awaitTermination(Duration.ofSeconds(10)));
            } else {
                assertFalse(client.isTerminated());
            }
        } finally {
            releaseHeaders.countDown();
            releaseBody.countDown();
            client.shutdownNow();
            server.stop(0);
            io.shutdownNow();
        }
    }

    @ParameterizedTest
    @MethodSource("deliveryCases")
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testStreamingDeliveryPrecedesConsumptionAndPreservesOwnership(final boolean async, final boolean publisher, final boolean owned,
            final String mode) throws Exception {
        final CountDownLatch releaseBody = new CountDownLatch(1);
        final String content = mode.equals("empty") ? "" : "first\n" + "caf\u00e9\u4e2d\ud83d\ude00\n".repeat(12000);
        final byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        final ExecutorService io = Executors.newSingleThreadExecutor();
        final ExecutorService caller = Executors.newSingleThreadExecutor();
        final HttpClient client = HttpClient.newBuilder().executor(io).build();
        server.createContext("/", exchange -> {
            try (exchange) {
                if (exchange.getRequestURI().getPath().equals("/reuse")) {
                    exchange.sendResponseHeaders(200, 2);
                    exchange.getResponseBody().write(new byte[] { 'o', 'k' });
                } else if (bytes.length == 0) {
                    exchange.sendResponseHeaders(200, -1);
                } else {
                    exchange.sendResponseHeaders(200, bytes.length);
                    exchange.getResponseBody().write(bytes, 0, 6);
                    exchange.getResponseBody().flush();
                    try {
                        if (!releaseBody.await(20, TimeUnit.SECONDS)) {
                            return;
                        }
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    exchange.getResponseBody().write(bytes, 6, bytes.length - 6);
                }
            }
        });
        server.start();
        try {
            final String url = "http://127.0.0.1:" + server.getAddress().getPort();
            final HttpRequest request = HttpRequest.create(url, client).closeHttpClientAfterExecution(owned);
            final BodyHandler handler = publisher ? BodyHandlers.ofPublisher() : BodyHandlers.ofLines();
            // Both caller and client have one thread. Delivery must leave the caller free to release the body gate.
            final HttpResponse<?> response = caller
                    .<HttpResponse<?>> submit(() -> async ? (HttpResponse<?>) request.asyncGet(handler).get(10, TimeUnit.SECONDS) : request.get(handler))
                    .get(12, TimeUnit.SECONDS);
            if (mode.equals("early")) {
                dispose(response.body());
                releaseBody.countDown();
            } else {
                caller.submit(releaseBody::countDown).get(5, TimeUnit.SECONDS);
                if (publisher) {
                    assertEquals(content, consume((Flow.Publisher<List<ByteBuffer>>) response.body()).get(10, TimeUnit.SECONDS));
                } else {
                    try (Stream<String> lines = (Stream<String>) response.body()) {
                        assertEquals(content.lines().toList(), lines.toList());
                    }
                }
            }
            if (owned) {
                assertTrue(client.awaitTermination(Duration.ofSeconds(10)), "owned client must terminate after consumption/cancellation");
            } else {
                assertFalse(client.isTerminated());
                assertEquals("ok", HttpRequest.create(url + "/reuse", client).get(String.class));
            }
        } finally {
            releaseBody.countDown();
            client.shutdownNow();
            server.stop(0);
            caller.shutdownNow();
            io.shutdownNow();
        }
    }

    @ParameterizedTest
    @MethodSource("cleanupCases")
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testCancelledStreamingResponseDisposesOrphanExactlyOnce(final boolean publisher, final boolean owned, final boolean push, final boolean race)
            throws Exception {
        final HttpClient client = mock(HttpClient.class);
        final AtomicInteger disposed = new AtomicInteger();
        final Object body = publisher ? cancellingPublisher(disposed) : Stream.of("unused").onClose(disposed::incrementAndGet);
        final HttpResponse response = mock(HttpResponse.class);
        final AtomicReference<CompletableFuture<?>> publicResult = new AtomicReference<>();
        when(response.body()).thenAnswer(invocation -> {
            if (race) {
                publicResult.get().cancel(true);
            }
            return body;
        });
        final CompletableFuture<HttpResponse<Object>> upstream = new CompletableFuture<>();
        when(client.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(upstream);
        when(client.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class), any(HttpResponse.PushPromiseHandler.class))).thenReturn(upstream);
        final HttpRequest request = HttpRequest.create(URL, client).closeHttpClientAfterExecution(owned);
        final BodyHandler handler = publisher ? BodyHandlers.ofPublisher() : BodyHandlers.ofLines();
        final CompletableFuture<?> result = push ? request.asyncExecute(HttpMethod.GET, handler, (a, b, c) -> {
        }) : request.asyncGet(handler);
        publicResult.set(result);
        if (!race) {
            assertTrue(result.cancel(true));
        }
        assertFalse(upstream.isCancelled());
        upstream.complete(response);
        assertTrue(result.isCancelled());
        assertEquals(1, disposed.get());
        verify(client, times(owned ? 1 : 0)).shutdown();
        verify(client, never()).close();
        verify(client, never()).shutdownNow();
    }

    @ParameterizedTest
    @MethodSource("failureCases")
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testShutdownFailureDisposesUndeliveredBodyWithoutBlockingClose(final boolean async, final boolean publisher) throws Exception {
        for (final Throwable failure : new Throwable[] { new IllegalStateException("shutdown failed"), new AssertionError("shutdown failed") }) {
            final HttpClient client = mock(HttpClient.class);
            final AtomicInteger disposed = new AtomicInteger();
            final Object body = publisher ? cancellingPublisher(disposed) : Stream.empty().onClose(disposed::incrementAndGet);
            final HttpResponse response = mock(HttpResponse.class);
            when(response.body()).thenReturn(body);
            when(client.send(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(response);
            when(client.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(CompletableFuture.completedFuture(response));
            doThrow(failure).when(client).shutdown();
            final HttpRequest request = HttpRequest.create(URL, client).closeHttpClientAfterExecution(true);
            final BodyHandler handler = publisher ? BodyHandlers.ofPublisher() : BodyHandlers.ofLines();
            if (async) {
                assertSame(failure, assertThrows(CompletionException.class, () -> request.asyncGet(handler).join()).getCause());
            } else {
                assertSame(failure, assertThrows(failure.getClass(), () -> request.get(handler)));
            }
            assertEquals(1, disposed.get());
            verify(client, times(1)).shutdown();
            verify(client, never()).close();
        }
    }

    private static Flow.Publisher<Object> cancellingPublisher(final AtomicInteger disposed) {
        return subscriber -> subscriber.onSubscribe(new Flow.Subscription() {
            @Override
            public void request(final long n) {
                throw new AssertionError("orphan cleanup must not request bytes");
            }

            @Override
            public void cancel() {
                disposed.incrementAndGet();
            }
        });
    }

    private static void dispose(final Object body) {
        if (body instanceof Stream<?> stream) {
            stream.close();
        } else if (body instanceof Flow.Publisher<?> publisher) {
            publisher.subscribe(new Flow.Subscriber<Object>() {
                @Override
                public void onSubscribe(final Flow.Subscription subscription) {
                    subscription.cancel();
                }

                @Override
                public void onNext(final Object item) {
                    throw new AssertionError("no demand");
                }

                @Override
                public void onError(final Throwable failure) {
                    throw new AssertionError(failure);
                }

                @Override
                public void onComplete() {
                }
            });
        }
    }

    private static CompletableFuture<String> consume(final Flow.Publisher<List<ByteBuffer>> publisher) {
        final CompletableFuture<String> result = new CompletableFuture<>();
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        publisher.subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(final Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(final List<ByteBuffer> items) {
                for (final ByteBuffer item : items) {
                    final byte[] chunk = new byte[item.remaining()];
                    item.get(chunk);
                    bytes.writeBytes(chunk);
                }
            }

            @Override
            public void onError(final Throwable failure) {
                result.completeExceptionally(failure);
            }

            @Override
            public void onComplete() {
                result.complete(bytes.toString(StandardCharsets.UTF_8));
            }
        });
        return result;
    }
}

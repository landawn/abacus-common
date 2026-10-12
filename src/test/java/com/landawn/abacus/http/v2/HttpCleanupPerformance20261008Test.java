package com.landawn.abacus.http.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

// Extends TestBase and is tagged "slow-test": performance/memory-tuning regressions stay out of
// the regular suite (AbacusCoreTestSuite excludes "slow-test"). Run this class directly when changing that code.
@Tag("slow-test")
class HttpCleanupPerformance20261008Test extends TestBase {
    @Test
    void syncAndAsyncCloseReleaseCallbacksAndRespectClientOwnership() throws Exception {
        for (final boolean async : new boolean[] { false, true }) {
            for (final boolean owned : new boolean[] { false, true }) {
                final CountingInputStream delegate = new CountingInputStream(null);
                final StubClient client = new StubClient(delegate, null);
                final InputStream stream = open(client, async, owned);

                assertNotNull(cleanupCallback(stream));
                stream.close();
                stream.close();

                assertNull(cleanupCallback(stream), "a closed response must release its request-capturing callback");
                assertEquals(1, delegate.closeCount.get());
                assertEquals(owned ? 1 : 0, client.closeCount.get());
                assertEquals(42, stream.read(), "the wrapper must keep delegating the stream's existing post-close behavior");
            }
        }
    }

    @Test
    void delegateCloseFailuresStillRunAndReleaseCleanupWithTheSameSuppression() throws Exception {
        for (final boolean async : new boolean[] { false, true }) {
            for (int failureKind = 0; failureKind < 3; failureKind++) {
                final Throwable primary = switch (failureKind) {
                    case 0 -> new IOException("delegate I/O failure");
                    case 1 -> new IllegalStateException("delegate runtime failure");
                    default -> new AssertionError("delegate error");
                };
                final AssertionError secondary = new AssertionError("client cleanup failure");
                final CountingInputStream delegate = new CountingInputStream(primary);
                final StubClient client = new StubClient(delegate, secondary);
                final InputStream stream = open(client, async, true);

                assertSame(primary, assertThrows(Throwable.class, stream::close));
                assertEquals(1, primary.getSuppressed().length);
                assertSame(secondary, primary.getSuppressed()[0]);
                assertNull(cleanupCallback(stream));
                stream.close();
                assertEquals(1, delegate.closeCount.get());
                assertEquals(1, client.closeCount.get());
            }
        }
    }

    @Test
    void cleanupFailureAloneStillPropagatesAndReleasesCallback() throws Exception {
        for (final boolean async : new boolean[] { false, true }) {
            for (final Throwable failure : List.of(new IllegalStateException("cleanup runtime failure"), new AssertionError("cleanup error"))) {
                final CountingInputStream delegate = new CountingInputStream(null);
                final StubClient client = new StubClient(delegate, failure);
                final InputStream stream = open(client, async, true);

                assertSame(failure, assertThrows(Throwable.class, stream::close));
                assertNull(cleanupCallback(stream));
                stream.close();
                assertEquals(1, delegate.closeCount.get());
                assertEquals(1, client.closeCount.get());
            }
        }
    }

    @Test
    void identicalDelegateAndCleanupFailureDoesNotSelfSuppress() throws Exception {
        for (final boolean async : new boolean[] { false, true }) {
            final AssertionError failure = new AssertionError("same failure");
            final StubClient client = new StubClient(new CountingInputStream(failure), failure);
            final InputStream stream = open(client, async, true);

            assertSame(failure, assertThrows(AssertionError.class, stream::close));
            assertEquals(0, failure.getSuppressed().length);
            assertNull(cleanupCallback(stream));
            stream.close();
            assertEquals(1, client.closeCount.get());
        }
    }

    @Test
    void concurrentCloseReleasesCallbackAndClosesDelegateAndClientOnce() throws Exception {
        final int threads = 8;
        for (final boolean async : new boolean[] { false, true }) {
            final CountingInputStream delegate = new CountingInputStream(null);
            final StubClient client = new StubClient(delegate, null);
            final InputStream stream = open(client, async, true);
            final CountDownLatch ready = new CountDownLatch(threads);
            final CountDownLatch start = new CountDownLatch(1);
            final ExecutorService executor = Executors.newFixedThreadPool(threads);

            try {
                final List<Future<?>> closes = new ArrayList<>();
                for (int i = 0; i < threads; i++) {
                    closes.add(executor.submit(() -> {
                        ready.countDown();
                        start.await();
                        stream.close();
                        return null;
                    }));
                }
                assertTrue(ready.await(5, TimeUnit.SECONDS));
                start.countDown();
                for (final Future<?> close : closes) {
                    close.get(5, TimeUnit.SECONDS);
                }
                assertEquals(1, delegate.closeCount.get());
                assertEquals(1, client.closeCount.get());
                assertNull(cleanupCallback(stream));
            } finally {
                start.countDown();
                executor.shutdownNow();
            }
        }
    }

    private static InputStream open(final StubClient client, final boolean async, final boolean owned) throws Exception {
        final HttpRequest request = HttpRequest.create("http://unused.invalid/review", client)
                .body(BodyPublishers.ofByteArray(new byte[1024]))
                .closeHttpClientAfterExecution(owned);
        return async ? request.asyncPost(InputStream.class).get(5, TimeUnit.SECONDS) : request.post(InputStream.class);
    }

    private static Object cleanupCallback(final InputStream stream) throws Exception {
        // Deterministic retention check; a separate bounded weak-reference probe checks payload reclamation.
        final Field field = stream.getClass().getDeclaredField("cleanup");
        field.setAccessible(true);
        return field.get(stream);
    }

    private static final class CountingInputStream extends InputStream {
        private final AtomicInteger closeCount = new AtomicInteger();
        private final Throwable failure;

        private CountingInputStream(final Throwable failure) {
            this.failure = failure;
        }

        @Override
        public int read() {
            return 42;
        }

        @Override
        public void close() throws IOException {
            closeCount.incrementAndGet();
            if (failure instanceof IOException e) {
                throw e;
            } else if (failure instanceof RuntimeException e) {
                throw e;
            } else if (failure instanceof Error e) {
                throw e;
            }
        }
    }

    private static final class StubClient extends HttpClient {
        private final InputStream responseBody;
        private final Throwable failure;
        private final AtomicInteger closeCount = new AtomicInteger();

        private StubClient(final InputStream responseBody, final Throwable failure) {
            this.responseBody = responseBody;
            this.failure = failure;
        }

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return Optional.empty();
        }

        @Override
        public Redirect followRedirects() {
            return Redirect.NEVER;
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return Optional.empty();
        }

        @Override
        public SSLContext sslContext() {
            return null;
        }

        @Override
        public SSLParameters sslParameters() {
            return new SSLParameters();
        }

        @Override
        public Optional<Authenticator> authenticator() {
            return Optional.empty();
        }

        @Override
        public Version version() {
            return Version.HTTP_1_1;
        }

        @Override
        public Optional<Executor> executor() {
            return Optional.empty();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> HttpResponse<T> send(final java.net.http.HttpRequest request, final HttpResponse.BodyHandler<T> handler) {
            return (HttpResponse<T>) new StubResponse(responseBody);
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(final java.net.http.HttpRequest request, final HttpResponse.BodyHandler<T> handler) {
            return CompletableFuture.completedFuture(send(request, handler));
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(final java.net.http.HttpRequest request, final HttpResponse.BodyHandler<T> handler,
                final HttpResponse.PushPromiseHandler<T> pushHandler) {
            return sendAsync(request, handler);
        }

        @Override
        public void close() {
            closeCount.incrementAndGet();
            if (failure instanceof RuntimeException e) {
                throw e;
            } else if (failure instanceof Error e) {
                throw e;
            }
        }
    }

    private record StubResponse(InputStream body) implements HttpResponse<InputStream> {
        @Override
        public int statusCode() {
            return 200;
        }

        @Override
        public java.net.http.HttpRequest request() {
            return null;
        }

        @Override
        public Optional<HttpResponse<InputStream>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(Map.of(), (name, value) -> true);
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return URI.create("http://unused.invalid/review");
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }
}

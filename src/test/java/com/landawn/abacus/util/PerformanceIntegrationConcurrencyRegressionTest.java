package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;

import performanceintegration.PerformanceIntegrationXmlModel;

@Tag("unit")
class PerformanceIntegrationConcurrencyRegressionTest {
    @Test
    void failedAnyOfListenerRegistrationsReleaseCallbacksAndRemainRetryable() throws Exception {
        final IllegalStateException rejected = new IllegalStateException("listener registration rejected");
        final CompletableFuture<Integer> input = new CompletableFuture<>() {
            private int attempts;

            @Override
            public CompletableFuture<Integer> whenComplete(final BiConsumer<? super Integer, ? super Throwable> action) {
                if (++attempts <= 20) {
                    throw rejected;
                }
                return super.whenComplete(action);
            }
        };
        final ContinuableFuture<Integer> aggregate = Futures.anyOf(input);
        final Field callbacksField = aggregate.future.getClass().getDeclaredField("completionListeners");
        callbacksField.setAccessible(true);
        final List<?> callbacks = (List<?>) callbacksField.get(aggregate.future);
        for (int i = 0; i < 20; i++) {
            assertSame(rejected, assertThrows(IllegalStateException.class, () -> aggregate.get(1, TimeUnit.MILLISECONDS)));
            assertTrue(callbacks.isEmpty(), "A failed registration must not accumulate a retained callback");
        }
        assertThrows(TimeoutException.class, () -> aggregate.get(1, TimeUnit.MILLISECONDS));
        assertEquals(1, callbacks.size(), "The successful pending registration stays owned by the aggregate");
        input.complete(42);
        assertEquals(42, aggregate.get());
        assertTrue(callbacks.isEmpty(), "Terminal publication releases the successful registration");
    }

    @Test
    @Timeout(15)
    void completedAnyOfDoesNotRetainItsWinnerThroughPendingLosers() throws Exception {
        CompletableFuture<byte[]> loser = new CompletableFuture<>();
        WeakReference<byte[]> payload = raceAndRelease(loser);
        awaitCollected(payload);
        assertFalse(loser.isDone(), "Losing input must remain caller-owned");
        Reference.reachabilityFence(loser);
    }

    private static WeakReference<byte[]> raceAndRelease(CompletableFuture<byte[]> loser) throws Exception {
        CompletableFuture<byte[]> winner = new CompletableFuture<>();
        ContinuableFuture<byte[]> aggregate = Futures.anyOf(winner, loser);
        assertThrows(TimeoutException.class, () -> aggregate.get(1, TimeUnit.MILLISECONDS));
        byte[] value = new byte[1024 * 1024];
        WeakReference<byte[]> reference = new WeakReference<>(value);
        winner.complete(value);
        assertSame(value, aggregate.get());
        return reference;
    }

    @Test
    void retainedAnyOfStillReceivesCompletionsAfterTimeoutsAndGc() throws Exception {
        CompletableFuture<Integer> first = new CompletableFuture<>(), second = new CompletableFuture<>();
        ContinuableFuture<Integer> aggregate = Futures.anyOf(first, second);
        for (int i = 0; i < 3; i++) assertThrows(TimeoutException.class, () -> aggregate.get(1, TimeUnit.MILLISECONDS));
        System.gc();
        first.completeExceptionally(new IllegalStateException("first"));
        assertThrows(TimeoutException.class, () -> aggregate.get(1, TimeUnit.MILLISECONDS));
        second.complete(42);
        assertEquals(42, aggregate.get());
        assertEquals(42, aggregate.get(0, TimeUnit.NANOSECONDS));

        CompletableFuture<Integer> failed = new CompletableFuture<>();
        ContinuableFuture<Integer> failure = Futures.anyOf(failed);
        assertThrows(TimeoutException.class, () -> failure.get(1, TimeUnit.MILLISECONDS));
        failed.completeExceptionally(new IllegalArgumentException("failure"));
        assertThrows(ExecutionException.class, failure::get);
    }

    @Test
    void competingCompletionsPublishOneStableWinner() throws Exception {
        for (int i = 0; i < 30; i++) {
            CompletableFuture<Integer> first = new CompletableFuture<>(), second = new CompletableFuture<>();
            ContinuableFuture<Integer> aggregate = Futures.anyOf(first, second);
            assertThrows(TimeoutException.class, () -> aggregate.get(1, TimeUnit.MILLISECONDS));
            CompletableFuture<Void> left = CompletableFuture.runAsync(() -> first.complete(1));
            CompletableFuture<Void> right = CompletableFuture.runAsync(() -> second.complete(2));
            int winner = aggregate.get(5, TimeUnit.SECONDS);
            assertTrue(winner == 1 || winner == 2);
            left.join();
            right.join();
            assertEquals(winner, aggregate.get());
        }
    }

    @Test
    @Timeout(15)
    void xmlNodeNameMetadataDoesNotPinItsTargetClassLoader() throws Exception {
        awaitCollected(cacheDisposableXmlTarget());
    }

    private static WeakReference<ClassLoader> cacheDisposableXmlTarget() throws Exception {
        ClassLoader loader = isolatedLoader(PerformanceIntegrationXmlModel.Inner.class);
        Class<?> target = loader.loadClass(PerformanceIntegrationXmlModel.Inner.class.getName());
        var lookup = Class.forName("com.landawn.abacus.parser.AbacusXmlParserImpl").getDeclaredMethod("getClassByNodeName", String.class, Class.class);
        lookup.setAccessible(true);
        assertNull(lookup.invoke(null, "unknownDisposableType", target));
        return new WeakReference<>(loader);
    }

    private static ClassLoader isolatedLoader(Class<?> fixture) throws Exception {
        String name = fixture.getName();
        byte[] bytes;
        try (var input = fixture.getResourceAsStream("/" + name.replace('.', '/') + ".class")) {
            bytes = input.readAllBytes();
        }
        return new ClassLoader(fixture.getClassLoader()) {
            @Override protected Class<?> loadClass(String requested, boolean resolve) throws ClassNotFoundException {
                if (!requested.equals(name)) return super.loadClass(requested, resolve);
                synchronized (getClassLoadingLock(requested)) {
                    Class<?> type = findLoadedClass(requested);
                    if (type == null) type = defineClass(requested, bytes, 0, bytes.length);
                    if (resolve) resolveClass(type);
                    return type;
                }
            }
        };
    }

    private static void awaitCollected(WeakReference<?> reference) throws Exception {
        for (int i = 0; i < 100 && reference.get() != null; i++) {
            System.gc();
            Thread.sleep(20);
        }
        assertNull(reference.get(), "Discarded payload/class loader is still strongly retained");
    }
}

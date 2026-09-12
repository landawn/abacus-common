package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class FutureComposeSnapshotTest {
    @Test
    void sourceAndCallbackMutationsCannotChangeCapturedMembership() throws Exception {
        CompletableFuture<Integer> first = CompletableFuture.completedFuture(7);
        CompletableFuture<Integer> replacement = new CompletableFuture<>();
        List<Future<Integer>> source = new ArrayList<>(List.of(first, first));
        AtomicInteger callbacks = new AtomicInteger();
        ContinuableFuture<Integer> composed = Futures.compose(source, snapshot -> {
            callbacks.incrementAndGet();
            assertEquals(2, snapshot.size());
            assertSame(first, snapshot.get(0));
            assertSame(first, snapshot.get(1));
            assertThrows(UnsupportedOperationException.class, snapshot::clear);
            return snapshot.get(0).get();
        }, tuple -> {
            assertThrows(UnsupportedOperationException.class, () -> tuple._1.set(0, replacement));
            return tuple._1.get(1).get(tuple._2, tuple._3);
        });
        assertTrue(composed.isDone());
        assertEquals(7, composed.get());
        source.clear();
        source.add(replacement);
        assertTrue(composed.isDone());
        assertEquals(7, composed.get(1, TimeUnit.SECONDS));
        assertEquals(7, composed.get());
        assertEquals(2, callbacks.get(), "zip computation remains lazy per get");
        assertFalse(replacement.isDone());
    }

    @Test
    void cancellationTargetsOriginalHandlesIncludingDuplicates() {
        AtomicInteger cancellations = new AtomicInteger();
        FutureTask<Integer> first = new FutureTask<>(() -> 1) {
            @Override
            public boolean cancel(boolean interrupt) {
                cancellations.incrementAndGet();
                return super.cancel(interrupt);
            }
        };
        CompletableFuture<Integer> second = new CompletableFuture<>();
        List<Future<Integer>> source = new ArrayList<>(List.of(first, first));
        ContinuableFuture<Integer> composed = Futures.compose(source, snapshot -> 1);
        source.clear();
        source.add(second);
        composed.cancel(true);
        assertEquals(2, cancellations.get());
        assertTrue(first.isCancelled());
        assertFalse(second.isCancelled());
    }

    @Test
    void nullEntriesFixedArityAndValidationRetainTheirContracts() throws Exception {
        List<Future<Integer>> source = new ArrayList<>(Collections.singletonList(null));
        ContinuableFuture<Integer> nullable = Futures.compose(source, snapshot -> snapshot.get(0) == null ? 9 : 0);
        source.clear();
        assertEquals(9, nullable.get());
        assertThrows(NullPointerException.class, nullable::isDone);
        assertThrows(IllegalArgumentException.class, () -> Futures.compose(Collections.<Future<Integer>> emptyList(), snapshot -> 0));
        assertThrows(IllegalArgumentException.class, () -> Futures.compose((Collection<Future<Integer>>) null, snapshot -> 0));
        CompletableFuture<Integer> one = CompletableFuture.completedFuture(1);
        assertEquals(2, Futures.compose(one, one, (a, b) -> a.get() + b.get()).get());
        assertEquals(3, Futures.compose(one, one, one, (a, b, c) -> a.get() + b.get() + c.get()).get());
    }
}

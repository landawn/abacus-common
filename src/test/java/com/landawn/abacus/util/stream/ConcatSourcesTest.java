package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

class ConcatSourcesTest extends TestBase {
    @Test
    void concatIterablesKeepsSnapshotAndInitializesSourcesLazily() {
        final AtomicInteger calls = new AtomicInteger();
        final Iterable<Integer> first = () -> {
            calls.incrementAndGet();
            return Arrays.asList(1, null).iterator();
        };
        final Iterable<Integer> second = () -> {
            calls.incrementAndGet();
            return List.of(2).iterator();
        };
        final List<Iterable<Integer>> sources = new ArrayList<>(Arrays.asList(null, List.of(), first, null, second));
        try (Stream<Integer> stream = Stream.concatIterables(sources)) {
            sources.clear();
            assertEquals(0, calls.get());
            final Iterator<Integer> iterator = stream.iteratorEx();
            assertEquals(1, iterator.next());
            assertEquals(1, calls.get());
            assertNull(iterator.next());
            assertEquals(2, iterator.next());
            assertEquals(2, calls.get());
            assertFalse(iterator.hasNext());
            assertFalse(iterator.hasNext());
            assertThrows(NoSuchElementException.class, iterator::next);
        }
    }

    @Test
    void concatIteratorsKeepsSnapshotAndNullElements() {
        final List<Iterator<Integer>> sources = new ArrayList<>(
                Arrays.asList(null, List.<Integer> of().iterator(), Arrays.asList(1, null).iterator(), null, List.of(2).iterator()));
        try (Stream<Integer> stream = Stream.concatIterators(sources)) {
            sources.clear();
            assertEquals(Arrays.asList(1, null, 2), stream.toList());
        }
    }

    @Test
    void concatIterablesContinuesAfterAnIteratorFactoryFailure() {
        final IllegalStateException failure = new IllegalStateException("iterator factory");
        final AtomicInteger attempts = new AtomicInteger();
        final Iterable<Integer> failing = () -> {
            attempts.incrementAndGet();
            throw failure;
        };

        try (Stream<Integer> stream = Stream.concatIterables(List.of(List.of(1), failing, List.of(2)))) {
            final Iterator<Integer> iterator = stream.iteratorEx();
            assertEquals(1, iterator.next());
            assertSame(failure, assertThrows(IllegalStateException.class, iterator::hasNext));
            assertEquals(2, iterator.next());
            assertFalse(iterator.hasNext());
            assertEquals(1, attempts.get());
        }
    }
}

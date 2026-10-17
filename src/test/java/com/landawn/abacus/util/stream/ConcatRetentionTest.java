package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("slow-test")
class ConcatRetentionTest extends TestBase {
    @Test
    void concatIterablesReleasesConsumedSources() throws Exception {
        final TrackedList first = new TrackedList(1);
        final TrackedList second = new TrackedList(2);
        final List<Iterable<Integer>> sources = new ArrayList<>(List.of(first, second));

        try (Stream<Integer> stream = Stream.concatIterables(sources)) {
            final Iterator<Integer> iterator = stream.iteratorEx();
            assertEquals(1, iterator.next());
            assertTrue(iterator.hasNext());
            assertNotRetained(iterator, first);
            assertNotRetained(iterator, first.lastIterator);
            assertEquals(2, iterator.next());
            assertFalse(iterator.hasNext());
            assertNotRetained(iterator, second);
            assertNotRetained(iterator, second.lastIterator);
            assertEquals(List.of(first, second), sources);
            assertThrows(NoSuchElementException.class, iterator::next);
        }
    }

    @Test
    void concatIteratorsReleasesConsumedSources() throws Exception {
        final Iterator<Integer> first = List.of(1).iterator();
        final Iterator<Integer> second = List.of(2).iterator();
        final List<Iterator<Integer>> sources = new ArrayList<>(List.of(first, second));

        try (Stream<Integer> stream = Stream.concatIterators(sources)) {
            final Iterator<Integer> iterator = stream.iteratorEx();
            assertEquals(1, iterator.next());
            assertTrue(iterator.hasNext());
            assertNotRetained(iterator, first);
            assertEquals(2, iterator.next());
            assertFalse(iterator.hasNext());
            assertNotRetained(iterator, second);
            assertEquals(List.of(first, second), sources);
            assertThrows(NoSuchElementException.class, iterator::next);
        }
    }

    @Test
    void concatIterablesReleasesASourceWhoseIteratorFactoryFails() throws Exception {
        final IllegalStateException failure = new IllegalStateException("iterator factory");
        final Iterable<Integer> failing = () -> {
            throw failure;
        };
        final List<Iterable<Integer>> sources = List.of(failing, List.of(2));

        try (Stream<Integer> stream = Stream.concatIterables(sources)) {
            final Iterator<Integer> iterator = stream.iteratorEx();
            assertSame(failure, assertThrows(IllegalStateException.class, iterator::hasNext));
            assertNotRetained(iterator, failing);
            assertEquals(2, iterator.next());
            assertFalse(iterator.hasNext());
            assertSame(failing, sources.get(0), "The caller's source list must remain intact");
        }
    }

    @Test
    void concatIterablesReleasesTheCorrectSourceWhenItsFactoryReenters() throws Exception {
        final AtomicReference<Iterator<Integer>> active = new AtomicReference<>();
        final Iterable<Integer> reentrant = () -> {
            assertEquals(2, active.get().next());
            return List.of(1).iterator();
        };

        try (Stream<Integer> stream = Stream.concatIterables(List.of(reentrant, List.of(2), List.of(3)))) {
            final Iterator<Integer> iterator = stream.iteratorEx();
            active.set(iterator);
            assertEquals(1, iterator.next());
            assertNotRetained(iterator, reentrant);
            assertEquals(3, iterator.next());
            assertFalse(iterator.hasNext());
        }
    }

    private static final class TrackedList extends ArrayList<Integer> {
        private Iterator<Integer> lastIterator;

        TrackedList(final int value) {
            add(value);
        }

        @Override
        public Iterator<Integer> iterator() {
            lastIterator = super.iterator();
            return lastIterator;
        }
    }

    private static void assertNotRetained(final Iterator<?> iterator, final Object source) throws Exception {
        // Inspect the owned snapshot and current source directly, without relying on GC timing.
        // The caller's source collection remains intact and is deliberately outside this check.
        boolean snapshotChecked = false;
        for (final Field field : iterator.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            final Object value = field.get(iterator);
            assertNotSame(source, value, field.toString());
            if (value instanceof List<?> snapshot) {
                snapshotChecked = true;
                for (final Object retained : snapshot) {
                    assertNotSame(source, retained, field.toString());
                }
            }
        }
        assertTrue(snapshotChecked, "The concat iterator must expose its owned source snapshot to this regression");
    }
}

package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ObjIterator;

@Tag("unit")
class StreamLookaheadReview20261010Test extends TestBase {
    @ParameterizedTest
    @ValueSource(strings = { "filter", "takeWhile", "dropWhile", "mapLast" })
    void repeatedLookaheadPreservesValuesAndCallbackCounts(final String operation) {
        final AtomicInteger calls = new AtomicInteger();
        try (Stream<Object> stream = stage(generated(), operation, calls)) {
            final ObjIteratorEx<Object> iterator = stream.iteratorEx();
            assertTrue(iterator.hasNext());
            assertTrue(iterator.hasNext());
            final Object first = iterator.next();
            assertTrue(iterator.hasNext());
            final Object second = iterator.next();
            assertNotSame(first, second);
            assertFalse(iterator.hasNext());
            assertEquals(operation.equals("dropWhile") || operation.equals("mapLast") ? 1 : 2, calls.get());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "filter", "takeWhile", "dropWhile" })
    void rejectedValuesAreNotDelivered(final String operation) {
        final AtomicInteger calls = new AtomicInteger();
        final Stream<Object> source = generated();
        final Stream<Object> stream = switch (operation) {
            case "filter" -> source.filter(value -> {
                calls.incrementAndGet();
                return false;
            });
            case "takeWhile" -> source.takeWhile(value -> {
                calls.incrementAndGet();
                return false;
            });
            case "dropWhile" -> source.dropWhile(value -> {
                calls.incrementAndGet();
                return true;
            });
            default -> throw new AssertionError(operation);
        };
        try (stream) {
            final ObjIteratorEx<Object> iterator = stream.iteratorEx();
            assertFalse(iterator.hasNext());
            assertFalse(iterator.hasNext());
            assertEquals(operation.equals("takeWhile") ? 1 : 2, calls.get());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "filter", "takeWhile", "dropWhile", "mapLast" })
    void nullElementsAndLookaheadOrderArePreserved(final String operation) {
        final AtomicInteger calls = new AtomicInteger();
        try (Stream<Object> stream = stage(Stream.of(Arrays.<Object> asList(null, "value", null).iterator()), operation, calls)) {
            assertEquals(Arrays.asList(null, "value", null), stream.toList());
            assertEquals(operation.equals("dropWhile") || operation.equals("mapLast") ? 1 : 3, calls.get());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { 2, 3, 4 })
    void slidingMapPreservesCompleteWindows(final int increment) {
        final List<List<Integer>> pairs = switch (increment) {
            case 2 -> List.of(List.of(1, 2), List.of(3, 4), List.of(5, 6));
            case 3 -> List.of(List.of(1, 2), List.of(4, 5));
            case 4 -> List.of(List.of(1, 2), List.of(5, 6));
            default -> throw new AssertionError(increment);
        };
        final List<List<Integer>> triples = switch (increment) {
            case 2 -> List.of(List.of(1, 2, 3), List.of(3, 4, 5));
            case 3 -> List.of(List.of(1, 2, 3), List.of(4, 5, 6));
            case 4 -> List.of(List.of(1, 2, 3));
            default -> throw new AssertionError(increment);
        };
        try (Stream<List<Integer>> stream = Stream.of(List.of(1, 2, 3, 4, 5, 6).iterator()).slidingMap(increment, true, (a, b) -> List.of(a, b))) {
            assertEquals(pairs, stream.toList());
        }
        try (Stream<List<Integer>> stream = Stream.of(List.of(1, 2, 3, 4, 5, 6).iterator()).slidingMap(increment, true, (a, b, c) -> List.of(a, b, c))) {
            assertEquals(triples, stream.toList());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "arrayCollection", "iteratorCollection", "arrayArray", "iteratorArray" })
    void flatteningPreservesOrderAndSkipsEmptyChildren(final String operation) {
        final Object payload = new Object();
        final Object[] childArray = { payload, null };
        final Iterator<Object> childIterator = Arrays.asList(childArray).iterator();
        final Collection<Object> childCollection = new AbstractCollection<>() {
            @Override
            public Iterator<Object> iterator() {
                return childIterator;
            }

            @Override
            public int size() {
                return childArray.length;
            }
        };
        final Stream<Integer> source = operation.startsWith("array") ? Stream.of(1, 2, 3, 4) : Stream.of(List.of(1, 2, 3, 4).iterator());
        final Stream<Object> flattened = operation.endsWith("Collection") ? source.flatmap(value -> {
            if (value == 1) {
                return childCollection;
            }
            return value == 2 ? null : value == 3 ? List.of() : List.of("tail");
        }) : source.flatMapArray(value -> {
            if (value == 1) {
                return childArray;
            }
            return value == 2 ? null : value == 3 ? new Object[0] : new Object[] { "tail" };
        });
        try (flattened) {
            final ObjIteratorEx<Object> iterator = flattened.iteratorEx();
            assertTrue(iterator.hasNext());
            assertSame(payload, iterator.next());
            assertNull(iterator.next());
            assertTrue(iterator.hasNext());
            assertEquals("tail", iterator.next());
            assertFalse(iterator.hasNext());
        }
    }

    private static Stream<Object> generated() {
        return Stream.of(new ObjIterator<>() {
            private int remaining = 2;

            @Override
            public boolean hasNext() {
                return remaining > 0;
            }

            @Override
            public Object next() {
                remaining--;
                return new Object();
            }
        });
    }

    private static Stream<Object> stage(final Stream<Object> source, final String operation, final AtomicInteger calls) {
        return switch (operation) {
            case "filter" -> source.filter(value -> {
                calls.incrementAndGet();
                return true;
            });
            case "takeWhile" -> source.takeWhile(value -> {
                calls.incrementAndGet();
                return true;
            });
            case "dropWhile" -> source.dropWhile(value -> {
                calls.incrementAndGet();
                return false;
            });
            case "mapLast" -> source.mapLast(value -> {
                calls.incrementAndGet();
                return value;
            });
            default -> throw new AssertionError(operation);
        };
    }

}

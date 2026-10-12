package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ObjIterator;

@Tag("unit")
class ReorderedStreamArrayAliasReview20261003Test extends TestBase {
    static java.util.stream.Stream<Arguments> aliasedRanges() {
        return java.util.stream.Stream.of(false, true)
                .flatMap(reverse -> java.util.stream.Stream.of(Arguments.of(reverse, 0, 5, 0), Arguments.of(reverse, 1, 6, 0), Arguments.of(reverse, 0, 5, 1),
                        Arguments.of(reverse, 1, 6, 1)));
    }

    @ParameterizedTest
    @MethodSource("aliasedRanges")
    void aliasedDestinationPreservesRemainingEncounterOrder(boolean reverse, int from, int to, int consumed) {
        Integer[] source = { 1, 2, 3, 4, 5, 6, 7 };
        List<Integer> ordered = ordered(Arrays.asList(source).subList(from, to), reverse);
        Integer[] expected = source.clone();
        List<Integer> remaining = ordered.subList(consumed, ordered.size());
        remaining.toArray(expected);

        try (Stream<Integer> stream = reorder(Stream.of(source, from, to), reverse)) {
            ObjIterator<Integer> iterator = stream.iterator();
            for (int i = 0; i < consumed; i++) {
                assertEquals(ordered.get(i), iterator.next());
            }
            assertSame(source, iterator.toArray(source));
            assertArrayEquals(expected, source);
            assertFalse(iterator.hasNext());
            assertThrows(NoSuchElementException.class, iterator::next);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void separateArraysRetainTypeNullSentinelAndUnusedSuffix(boolean reverse) {
        String[] source = { "a", null, "c" };
        List<String> expected = ordered(Arrays.asList(source), reverse);
        CharSequence[] destination = { "keep", "keep", "keep", "keep", "keep" };
        CharSequence[] fullExpected = destination.clone();
        expected.toArray(fullExpected);
        try (Stream<String> stream = reorder(Stream.of(source), reverse)) {
            assertSame(destination, stream.iterator().toArray(destination));
            assertArrayEquals(fullExpected, destination);
            assertArrayEquals(new String[] { "a", null, "c" }, source);
        }
        try (Stream<String> stream = reorder(Stream.of(source), reverse)) {
            String[] allocated = stream.iterator().toArray(new String[0]);
            assertEquals(String[].class, allocated.getClass());
            assertArrayEquals(expected.toArray(String[]::new), allocated);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void incompatibleArrayPreservesPartialCopyAndIteratorPosition(boolean reverse) {
        Object[] source = { 1, "incompatible", 2 };
        List<Object> expected = ordered(Arrays.asList(source), reverse);
        Number[] destination = { -1, -1, -1 };
        Number[] prefix = destination.clone();
        for (int i = 0; i < expected.size() && expected.get(i) instanceof Number; i++) {
            prefix[i] = (Number) expected.get(i);
        }
        try (Stream<Object> stream = reorder(Stream.of(source), reverse)) {
            ObjIterator<Object> iterator = stream.iterator();
            assertThrows(ArrayStoreException.class, () -> iterator.toArray(destination));
            assertArrayEquals(prefix, destination);
            assertArrayEquals(expected.toArray(), iterator.toArray());
            assertFalse(iterator.hasNext());
        }
        assertArrayEquals(new Object[] { 1, "incompatible", 2 }, source);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void emptyAndSingletonAliasedRangesPreserveArrayContract(boolean reverse) {
        for (int length = 0; length <= 1; length++) {
            Integer[] source = { 1, 2, 3 };
            Integer[] expected = source.clone();
            ordered(Arrays.asList(source).subList(1, 1 + length), reverse).toArray(expected);
            try (Stream<Integer> stream = reorder(Stream.of(source, 1, 1 + length), reverse)) {
                ObjIterator<Integer> iterator = stream.iterator();
                assertSame(source, iterator.toArray(source));
                assertArrayEquals(expected, source);
                assertFalse(iterator.hasNext());
            }
        }
    }

    private static <T> Stream<T> reorder(Stream<T> source, boolean reverse) {
        return reverse ? source.reversed() : source.rotated(2);
    }

    private static <T> List<T> ordered(List<T> source, boolean reverse) {
        List<T> result = new ArrayList<>(source);
        if (reverse) {
            Collections.reverse(result);
        } else {
            Collections.rotate(result, 2);
        }
        return result;
    }
}

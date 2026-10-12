package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("unit")
class SeqReviewContract20261010Test extends TestBase {
    @Test
    void stagesPreserveNullsAndCallbackCounts() {
        final List<Integer> values = Arrays.asList(null, 1, null, 2);
        final List<Integer> seen = new ArrayList<>();
        assertEquals(values, Seq.<Integer, RuntimeException> of(values).filter(value -> {
            seen.add(value);
            return true;
        }).toList());
        assertEquals(values, seen);
        seen.clear();
        assertEquals(values, Seq.<Integer, RuntimeException> of(values).takeWhile(value -> {
            seen.add(value);
            return true;
        }).toList());
        assertEquals(values, seen);
        seen.clear();
        assertEquals(Arrays.asList(1, null, 2), Seq.<Integer, RuntimeException> of(values).dropWhile(value -> {
            seen.add(value);
            return value == null;
        }).toList());
        assertEquals(Arrays.asList(null, 1), seen);
        assertEquals(Arrays.asList(null, 1, null, 20), Seq.<Integer, RuntimeException> of(values).mapLast(value -> value * 10).toList());
        assertEquals(Arrays.asList(null, 1, null, 20), Seq.<Integer, RuntimeException> of(values).mapLastOrElse(value -> value * 10, value -> value).toList());
        seen.clear();
        assertEquals(values, Seq.<Integer, RuntimeException> of(values).onLast(seen::add).toList());
        assertEquals(List.of(2), seen);
        assertEquals(Arrays.asList(null, 1, null, 3),
                Seq.<Integer, RuntimeException> of(1, 2, 3).flatmap(value -> value == 2 ? null : Arrays.asList(null, value)).toList());
        assertEquals(Arrays.asList(null, 1, null, 3),
                Seq.<Integer, RuntimeException> of(1, 2, 3).flatMapArray(value -> value == 2 ? new Integer[0] : new Integer[] { null, value }).toList());
    }

    @Test
    void slidingMapsPreserveOverlappingGappedAndPartialWindows() {
        for (int length = 0; length <= 8; length++) {
            final List<Integer> source = new ArrayList<>();
            for (int i = 0; i < length; i++) {
                source.add(i % 2 == 0 ? i : null);
            }
            for (int increment = 1; increment <= 6; increment++) {
                for (final boolean ignorePartial : new boolean[] { false, true }) {
                    final String description = "length=" + length + ", increment=" + increment + ", ignorePartial=" + ignorePartial;
                    assertEquals(expectedWindows(source, 2, increment, ignorePartial),
                            Seq.<Integer, RuntimeException> of(source).slidingMap(increment, ignorePartial, (a, b) -> Arrays.asList(a, b)).toList(),
                            description);
                    assertEquals(expectedWindows(source, 3, increment, ignorePartial),
                            Seq.<Integer, RuntimeException> of(source).slidingMap(increment, ignorePartial, (a, b, c) -> Arrays.asList(a, b, c)).toList(),
                            description);
                }
            }
        }
    }

    private static List<List<Integer>> expectedWindows(final List<Integer> source, final int windowSize, final int increment, final boolean ignorePartial) {
        final List<List<Integer>> result = new ArrayList<>();
        int previousEnd = 0;
        for (int start = 0; start < source.size(); start += increment) {
            final int end = Math.min(start + windowSize, source.size());
            if (end <= previousEnd || ignorePartial && end - start < windowSize) {
                break;
            }
            final List<Integer> window = new ArrayList<>();
            for (int i = 0; i < windowSize; i++) {
                window.add(start + i < source.size() ? source.get(start + i) : null);
            }
            result.add(window);
            previousEnd = end;
        }
        return result;
    }

}
